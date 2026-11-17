# agent-perf-discipline Specification

## Purpose

Defines the behavioral constraints an AI agent must satisfy when authoring or modifying a feature in the `mod` module: lifecycle gating for disabled features, ownership of global event listeners and timers, zero-allocation frame loops, and bounded long-lived collections. These constraints exist because the mod targets Android under Mindustry's software renderer, where per-frame dispatch and per-frame allocation are both expensive, and because unbounded caches in this codebase retain GL textures for the process lifetime.

## Requirements

### Requirement: Disabled features perform no work

A feature that is disabled SHALL NOT consume frame-loop time for its own purposes. Any handler registered on a per-frame trigger (`Trigger.update`, `Trigger.draw`, `Trigger.uiDrawBegin`, `Trigger.postDraw`) SHALL return immediately when `isEnabled()` is false, and that guard SHALL be the first statement in the handler body, before any field read, allocation, or game-API call.

#### Scenario: Disabled draw feature emits no draw calls

- **WHEN** a feature registers a `Trigger.draw` handler and the feature is disabled
- **THEN** the handler SHALL return before issuing any draw call, and the feature SHALL contribute zero draw calls to that frame

#### Scenario: Disabled update feature touches no game state

- **WHEN** a feature registers a `Trigger.update` handler and the feature is disabled
- **THEN** the handler SHALL return before reading or writing any game state

#### Scenario: Work owned by a disabled feature's settings

- **WHEN** a configuration change would cause a disabled feature to recompute expensive state
- **THEN** the recomputation SHALL be deferred until the feature is enabled again

### Requirement: Frame loops are allocation-free

Code that executes on a per-frame trigger SHALL NOT allocate heap memory during steady-state execution. Specifically, it SHALL NOT: box primitives (`Float`/`Integer` via `signal().peek()`, `Boolean.TRUE.equals` on a boxed read, or any autoboxing), concatenate strings, create lambdas that capture mutable state, allocate `Seq`/`ObjectMap`/collections, allocate iterators over collections that are not already iterator-free (prefer `each(Boolf)` / `each(Cons)` with pre-allocated scratch fields), or perform dictionary or reflection lookups whose result does not change between frames.

Scratch values SHALL be reused across frames via pre-allocated fields, `arc.util.Tmp` (`Tmp.r1`, `Tmp.c1`), or static lookup tables.

#### Scenario: Config read in a frame loop does not box

- **WHEN** a `Trigger.draw` or `Trigger.update` handler reads a float or boolean configuration value
- **THEN** the value SHALL be read into a primitive field or a pre-boxed value, and SHALL NOT allocate a new wrapper object per frame

#### Scenario: Per-frame label does not concatenate

- **WHEN** a frame loop renders a formatted label such as a countdown
- **THEN** the label SHALL be computed only when its displayed value changes, or SHALL be written into a reused `GlyphLayout`, and SHALL NOT build a new `String` per frame

#### Scenario: Per-frame bundle lookup is hoisted

- **WHEN** a frame loop renders a static or rarely-changing localized string
- **THEN** the string SHALL be resolved once outside the frame loop or refreshed on state change, and SHALL NOT call `Core.bundle.get(...)` per frame

#### Scenario: Allocation is asserted by test

- **WHEN** a feature implements a `Trigger.draw` handler that draws every frame
- **THEN** a test SHALL assert zero bytes allocated on the render thread across a simulated render loop, using `com.sun.management.ThreadMXBean.getThreadAllocatedBytes`, in the manner of the existing `RenderLoopAllocationTest`

#### Scenario: Team resource HUD elements assert zero allocation

- **WHEN** `SplitBar.draw()` and `TeamResourceHudView` update handlers run during steady-state gameplay
- **THEN** a test using `com.sun.management.ThreadMXBean.getThreadAllocatedBytes` SHALL assert zero heap bytes allocated across simulated frames

### Requirement: Global listeners and timers have an explicit owner and are released

Every registration of a global event listener (`Events.on`, `Events.run`, `Core.app.addListener`, `Core.app.addCaptureListener`, `Timer.schedule`) SHALL have exactly one declared owner and SHALL be released by that owner's teardown. Teardown SHALL occur in one of:

1. `Feature.onDisable()`, paired with registration in `Feature.onEnable()` — **preferred for anything a disabled feature needs.**
2. The owning Solim component's `dispose()` — **mandatory for listeners registered from a component**, via `Component.listen(eventType, listener)` or `Component.registerDisposable(...)`, which wrap the removal automatically.
3. An explicit `onDispose()` override that calls `Events.remove(...)` or `Timer.cancel()`.

A repeating `Timer.schedule` SHALL retain its `Timer.Task` handle in a field so it can be cancelled.

Registration inside a constructor is permitted **only** when the object is created exactly once per process lifetime and never re-registered. Any handler registered this way SHALL still satisfy the disabled-feature gating requirement.

#### Scenario: Component-owned listener is released on dispose

- **WHEN** a Solim component registers a listener through `Component.listen(...)`
- **THEN** disposing the component SHALL remove the listener from the event bus

#### Scenario: Repeated start-stop does not accumulate listeners

- **WHEN** a listener is registered by `start()` or a toggle handler and later stopped
- **THEN** stopping SHALL remove the listener, so that repeatedly starting and stopping SHALL NOT increase the number of live listeners

#### Scenario: Repeating timer is cancellable

- **WHEN** a repeating task is scheduled
- **THEN** its `Timer.Task` handle SHALL be stored in a field and SHALL be cancelled when the owning feature is disabled or its work is no longer needed

#### Scenario: Construction-time registration is gated

- **WHEN** a handler is registered in a constructor for a process-lifetime object
- **THEN** the handler SHALL still return immediately when its feature is disabled

### Requirement: HUD views are created on enable and disposed on disable

A feature that owns a HUD element SHALL create it in `onEnable()` and destroy it in `onDisable()`, following the established pattern: guard creation with a null check on a view field, mount via `Core.app.post(...)` into `Vars.ui.hudGroup` (or `Core.scene` for chat overlay, which is intentionally layered above the HUD), and on teardown null the field first, then `Core.app.post(() -> { view.element().remove(); view.dispose(); })`.

Nulling the field before posting the removal prevents a queued mount from resurrecting a view that was disabled in the same frame.

A feature SHALL NOT construct a HUD view in its constructor, and SHALL NOT reuse a HUD view across an enable/disable cycle.

#### Scenario: Disable after enable leaves no element in the HUD

- **WHEN** a feature is enabled and then disabled
- **THEN** its HUD element SHALL no longer be a child of `Vars.ui.hudGroup` and its Solim component SHALL report `isDisposed()`

#### Scenario: Rapid toggle does not resurrect the HUD

- **WHEN** a feature is enabled and disabled within the same frame such that the mount is still queued
- **THEN** the mount SHALL be a no-op and no element SHALL remain in the HUD

#### Scenario: Repeated enable-disable cycles do not accumulate elements

- **WHEN** a feature is enabled and disabled ten times
- **THEN** the HUD group SHALL contain at most one instance of that feature's element at any point, and zero after the final disable

### Requirement: Long-lived collections are bounded

Any collection retained for longer than a single call — `static` caches, per-feature maps, per-channel lists, `Signal`-keyed lookup maps — SHALL have an explicit bound with an eviction or trimming policy. A collection with no bound is not acceptable merely because it "grows slowly".

Acceptable policies: LRU eviction via `LinkedHashMap` with `removeEldestEntry`, size-capped insertion with rejection of new entries, time-based cleanup driven from a feature's existing frame handler, or explicit trimming to a configured maximum.

Caches that exist to avoid per-frame allocation SHALL keep that property; adding a bound does not license removing the cache.

Accessing a `Signal` to build or mutate a cached entry SHALL NOT leave a long-lived subscription attached to that signal unless the subscription's lifetime is explicitly bounded and documented.

#### Scenario: Parse cache evicts oldest entries

- **WHEN** a message-parse cache exceeds its configured maximum entry count
- **THEN** the least recently used entry SHALL be evicted

#### Scenario: Height cache evicts oldest entries

- **WHEN** a message-height cache exceeds its configured maximum entry count
- **THEN** the least recently used entry SHALL be evicted

#### Scenario: Image cache evicts oldest textures

- **WHEN** the in-memory network-image cache exceeds its configured maximum entry count
- **THEN** the least recently used entry SHALL be evicted and its Java-side reference dropped

#### Scenario: Eviction does not dispose live textures

- **WHEN** an image cache entry is evicted
- **THEN** the underlying texture SHALL NOT be disposed, because Arc may still hold a draw reference to it

#### Scenario: Per-channel message list is trimmed

- **WHEN** messages are appended or prepended to a channel's message list
- **THEN** the list SHALL be trimmed to a configured maximum so it does not grow without bound across a long session

#### Scenario: A test asserts the bound

- **WHEN** a bounded cache is introduced
- **THEN** a test SHALL assert that inserting more than the maximum entry count leaves the cache at the maximum, and that eviction preserves recently used entries

### Requirement: Existing violations are recorded, not silently tolerated

The change that introduces these constraints SHALL enumerate every known existing violation, so that a count of outstanding violations can be tracked over time. New code SHALL NOT add violations. Reducing the recorded count is encouraged but not required by this capability.

#### Scenario: Known debt is enumerable

- **WHEN** a contributor audits the codebase against these requirements
- **THEN** each outstanding violation SHALL correspond to a row in a recorded table with a file path and line reference

### Requirement: Constraints are stated in agent-facing documentation

These requirements SHALL be restated as prescriptive guidance in `AGENTS.md` alongside the other mandatory-rule sections, and summarized as checklist items in the existing `Before Completing Any Task` section, so that an agent reads them before authoring code rather than after.

#### Scenario: Agent reads constraints before implementation

- **WHEN** an agent loads `AGENTS.md` at the start of a task
- **THEN** the frame-loop, listener-ownership, and bounded-collection constraints SHALL be present in the document

#### Scenario: Constraints are re-checked at completion

- **WHEN** an agent reaches the end of a task
- **THEN** the completion checklist SHALL include items for disabled-feature gating, listener teardown, bounded collections, and allocation-free frame loops

### Requirement: Long-lived references are released on world load

A feature that retains references to world-scoped objects (`PowerGraph`, `Item`, `Unit`, `Building`, bridge flow states, music tracks) in maps or pools SHALL clear those references on `WorldLoadEvent` / `ResetEvent` and on `onDisable()`, in addition to clearing buffer contents. Retaining such references across worlds is not acceptable even when the collection itself is bounded, because the referenced objects hold (directly or transitively) large amounts of game state.

#### Scenario: Team resource histories release PowerGraph references

- **WHEN** a world loads or the selected team changes
- **THEN** `TeamResourceState` SHALL clear `itemHistories` and `graphHistories` maps entirely (not only their buffer contents), so no `PowerGraph` or `Item` from the previous world is retained

#### Scenario: Visualizer state pool releases bridge states

- **WHEN** a world loads or resets
- **THEN** `VisualizerFeature` SHALL clear `statePool`, so evicted `BridgeFlowState` objects (and their cached glyph layouts) are released

### Requirement: Hot features draw with zero per-frame allocation

In addition to the features already covered (`VisualizerFeature`, `RangeDisplayFeature`, `HealthBarFeature`), the following features that register a `Trigger.draw` or `Trigger.update` handler SHALL satisfy the zero-allocation frame-loop requirement, and SHALL be added to `RenderLoopAllocationTest` (or an equivalent) covering their steady-state draw/update path:

- `BlockStatsDisplayFeature` (no `Float` boxing from `ConfigValue.signal().peek()`; cache as primitives via `Effect`)
- `ToggleRenderingFeature` (no lambda capture, no `removeAll` scan, no per-unit reflection in steady state)
- `AutoplayFeature` (no per-frame `String` concatenation for the cooldown label)
- `LogicVisualizerFeature` (no per-frame allocation in the unit iteration; frustum-culled)
- `PathfindingFeature` (no per-frame `Arrays.copyOf` growth in steady state; cache pre-sized)

#### Scenario: BlockStats draw does not box

- **WHEN** `BlockStatsDisplayFeature.draw()` runs for 60 simulated frames
- **THEN** `ThreadMXBean.getThreadAllocatedBytes` SHALL report zero bytes allocated on the render thread

#### Scenario: ToggleRendering steady-state draw allocates nothing

- **WHEN** `ToggleRenderingFeature.updateVisibility()` runs for 60 simulated frames with a stable unit set
- **THEN** no lambda, iterator, or reflection result shall be allocated per frame

#### Scenario: Autoplay cooldown label is cached

- **WHEN** the FactorySupply cooldown value does not change between frames
- **THEN** no new `String` shall be allocated for the label

### Requirement: Hot (draw/update) listeners gate on isEnabled()

Of the constructor-registered listeners recorded as outstanding debt (D7), those registered on `Trigger.draw` or `Trigger.update` SHALL be gated by `isEnabled()` as the first statement of their handler body. Listeners on UI/event triggers (dialog, click, state change) MAY remain ungated and SHALL be recorded as deferred debt.

#### Scenario: A disabled feature's draw listener does not run

- **WHEN** a feature with a `Trigger.draw` listener is disabled
- **THEN** the listener SHALL return before any field read, allocation, or game-API call

### Requirement: Pathfinding cache is bounded

`PathfindingCacheManager` SHALL enforce a hard maximum entry count (4096) in addition to its age-based cleanup. When the cap is exceeded, entries with the oldest `lastUsedTime` SHALL be evicted. Age-based cleanup SHALL run at least every 30 frames.

#### Scenario: Cache does not exceed cap

- **WHEN** more than 4096 distinct cache keys are inserted
- **THEN** the cache size SHALL remain at 4096 and the oldest entries SHALL be evicted

#### Scenario: Cleanup runs frequently enough

- **WHEN** the feature is enabled and drawing
- **THEN** age-based cleanup SHALL execute at least once every 30 frames