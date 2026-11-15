## ADDED Requirements

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