# solim-reactivity Specification

## Purpose

Mechanical merge of 10 specs per change `spec-domain-merge` (stage 3 core, concat-then-dedupe). Sources: solim-reactivity, automatic-effect-ownership, config-value-signal, contextual-config-value, orientation-signal, signal-callback-cleanup, solim-binding, solim-property-bindings, solim-signal-dispatcher, two-way-binding. Each source below appears under a `**Source:` marker with its purpose body and requirement blocks verbatim; per-source `## Purpose` / `## Requirements` header lines are removed so all requirements parse inside the single `## Requirements` section. TBD purposes carried forward; requirement dedupe is follow-up work.
## Requirements

**Source: solim-reactivity**

TBD - created by archiving change create-solim-core. Update Purpose after archive.

### Requirement: Signal primitive
`Signal<T>` SHALL be a mutable reactive value created via `Signal.of(initial)` with `T get()`, `void set(T)`, equality-guarded notification, `Subscription subscribe(Consumer<T>)` returning disposable handle, and `Computed<U> map(Function<T,U>)` convenience. `get()` SHALL register as dependency when called inside a `Computed` or `Effect` evaluation.

#### Scenario: Equality-guarded notification
- **WHEN** `Signal<Integer> c = Signal.of(0)` and a subscriber is registered, then `c.set(0)` is called with equal value
- **THEN** subscriber is NOT notified and dependent Computeds/Effects do not re-run

#### Scenario: Subscription dispose stops notifications
- **WHEN** `Subscription s = count.subscribe(v -> log(v))` then `s.dispose()` is called
- **THEN** subsequent `count.set(...)` does not invoke the disposed callback

#### Scenario: Map creates derived Computed
- **WHEN** `Computed<String> t = enabled.map(v -> v ? "Enabled" : "Disabled")`
- **THEN** `t.get()` returns mapped value and re-computes when `enabled` changes, with disposal support

#### Scenario: Signal get tracks dependency in Computed and Effect
- **WHEN** `Computed<String> c = Signal.computed(() -> count.get() + "")` or `Effect.of(() -> log(count.get()))` reads `count.get()` during evaluation
- **THEN** the Computed/Effect is automatically subscribed to `count` and re-evaluates when `count` changes

### Requirement: Computed with lazy recomputation and dynamic dependencies
`Computed<T>` SHALL be created via `Signal.computed(Supplier<T>)` or `signal.map(...)`, with automatic dependency tracking, lazy recomputation (recompute on `get()` after invalidation), dynamic dependency cleanup when source branch changes, cycle detection, and `Subscription subscribe(Consumer<T>)` + `void dispose()`.

#### Scenario: Lazy recomputation only on get
- **WHEN** `Computed<String> title = Signal.computed(() -> "Count: " + count.get())` and `count.set(10)` invalidates `title`
- **THEN** supplier does NOT re-execute until `title.get()` or a subscriber/effect reads it

#### Scenario: Dynamic dependencies cleanup
- **WHEN** `Computed<String> v = Signal.computed(() -> darkMode.get() ? username.get() : email.get())` and `darkMode` toggles from true to false
- **THEN** `v` unsubscribes from `username` and subscribes to `email`; subsequent `username.set(...)` does NOT invalidate `v` while `email.set(...)` does

#### Scenario: Cycle detection does not infinite loop
- **WHEN** a `Computed` directly or transitively reads itself during evaluation
- **THEN** implementation detects the cycle and throws or logs without stack overflow / infinite loop, and old dependencies are not leaked

#### Scenario: Invalidation propagates to dependent Computeds
- **WHEN** `Computed b = a.map(...)` and `Computed c = b.map(...)` chain and `a` changes
- **THEN** both `b` and `c` are marked invalid and recompute correctly on next `get()` in topological order

#### Scenario: Computed subscription and dispose
- **WHEN** `Subscription s = computed.subscribe(v -> render(v))` then `computed.dispose()` is called
- **THEN** `computed` unsubscribes from all dependencies, clears listeners, and no longer recomputes or notifies

### Requirement: Effect with auto-tracking and dynamic dependencies
`Effect` SHALL be created via `Effect.of(Runnable|Supplier<Disposable>|Consumer<Cleanup>)` or `Ui.effect(...)`, automatically track every `Signal`/`Computed` read during execution, subscribe to those dependencies, re-run when any dependency changes, remove old dependencies before collecting new ones, handle dynamic branches, prevent leaks, log errors without crashing, and be disposable via `void dispose()`.

#### Scenario: Auto-tracking signals and computeds
- **WHEN** `Effect e = Effect.of(() -> Log.info(darkMode.get() + username.get()))`
- **THEN** `e` subscribes to `darkMode` and `username`; changing either re-runs the effect

#### Scenario: Dynamic dependency branch switch
- **WHEN** `Effect e = Effect.of(() -> { if (enabled.get()) Log.info(username.get()); else Log.info(email.get()); })` and `enabled` toggles
- **THEN** after toggle, `e` stops depending on the old branch value and starts depending on the new one; old dependency changes no longer trigger `e`

#### Scenario: Old dependencies removed before new collection
- **WHEN** Effect re-runs due to dependency change
- **THEN** it first unsubscribes from previous dependencies before executing supplier to collect new set, preventing leak of stale deps

#### Scenario: Dispose stops reacting
- **WHEN** `Effect e = Effect.of(...)` then `e.dispose()` is called
- **THEN** `e` unsubscribes from all dependencies, runs cleanup, and no longer re-runs on signal changes

#### Scenario: Effect errors are logged safely
- **WHEN** effect supplier throws an exception
- **THEN** exception is caught and logged via `arc.util.Log` (or equivalent) and does not break reactive graph or prevent other effects from running

### Requirement: Effect cleanup
Effects SHALL support cleanup that runs before re-execution and on dispose. Two acceptable APIs: returning `Disposable`/`Runnable` from supplier, or accepting `Cleanup` parameter with `cleanup.add(Runnable)`. Cleanup errors SHALL be logged and not prevent other cleanups or disposal.

#### Scenario: Cleanup runs before re-run
- **WHEN** `Effect.of(cleanup -> { Subscription s = api.events().subscribe(...); cleanup.add(s::dispose); })` and a dependency changes causing re-run
- **THEN** previous `s.dispose()` is invoked before new subscription is created

#### Scenario: Cleanup runs on dispose
- **WHEN** effect with registered cleanups is disposed
- **THEN** all cleanups are executed, errors are caught/logged, and disposal completes

#### Scenario: Cleanup failure does not block others
- **WHEN** multiple cleanups are registered and one throws
- **THEN** remaining cleanups still execute and the exception is logged

### Requirement: ReactiveContext and ReactiveObserver shared mechanism
A shared dependency-tracking mechanism SHALL allow the currently executing `Computed` or `Effect` (both implementing internal `ReactiveObserver`) to collect dependencies when `signal.get()`/`computed.get()` is called. Implementation SHALL use a stack-based context (e.g., `Deque<ReactiveObserver>`) suitable for single-threaded Arc/Mindustry UI thread and SHALL NOT use `ThreadLocal` unless necessary. Context SHALL support `push(observer)`, `pop()`, `current()`, and `track(dependency)`.

#### Scenario: Stack push/pop during evaluation
- **WHEN** `Effect` starts execution it pushes itself onto `ReactiveContext` stack, then `signal.get()` registers dependency via `ReactiveContext.current().addDependency(signal)`, then effect pops
- **THEN** only the active observer collects dependencies and nested evaluations correctly push/pop without corrupting outer observer

#### Scenario: Computed and Effect both use same context
- **WHEN** `Computed` evaluation reads a `Signal` that is also read by an `Effect`
- **THEN** both correctly register dependencies via the same `ReactiveContext`/`ReactiveObserver` abstraction without duplicate code paths

#### Scenario: No ThreadLocal by default
- **WHEN** `src/solim/signal/ReactiveContext.java` is inspected
- **THEN** it uses a plain static stack/Deque (or ArrayDeque) and does not import `java.lang.ThreadLocal` unless justified by test for threading

### Requirement: Subscription and Disposable contracts
`Subscription` (or `Disposable`) SHALL expose `void dispose()` idempotent. `Signal.subscribe`, `Computed.subscribe`, and `Computed/Effect.dispose` SHALL all return or implement this contract, clean up listener lists, and be safe to call multiple times.

#### Scenario: Idempotent dispose
- **WHEN** `subscription.dispose()` is called twice
- **THEN** second call is no-op and does not throw

#### Scenario: No leak after dispose
- **WHEN** 100 signals/computeds/effects are created and disposed
- **THEN** no strong references remain in dependency graphs (verifiable via listener count == 0 and no retained observer entries)

### Requirement: Signal.map convenience
`Signal<T>.map(Function<T,R>)` SHALL return a `Computed<R>` equivalent to `Signal.computed(() -> fn.apply(signal.get()))` with same lazy/dynamic/disposal semantics.

#### Scenario: Map reactive updates
- **WHEN** `Signal<Boolean> dark = Signal.of(false)` and `Computed<String> label = dark.map(v -> v ? "On" : "Off")`
- **THEN** `label.get()` is "Off", after `dark.set(true)` `label.get()` is "On", and `label` notifies subscribers

### Requirement: Untracked Execution in ReactiveContext
`ReactiveContext` SHALL provide an `untracked(Supplier<T>)` and `untracked(Runnable)` mechanism that temporarily suspends active dependency tracking so that any signal or computed reads occurring inside the block are not recorded as dependencies of the active `ReactiveObserver`.

#### Scenario: Reading signals inside untracked block
- **WHEN** an `Effect` is executing and invokes `ReactiveContext.untracked(() -> signal.get())`
- **THEN** `signal` is not added as a dependency to the running `Effect`, and changes to `signal` do not trigger the effect to re-run.

#### Scenario: Dynamic component factory isolation
- **WHEN** `Dynamic` executes its child component factory
- **THEN** the factory is invoked within an untracked scope, preventing child signal evaluations during component construction from leaking into the `Dynamic` switcher effect.

**Source: automatic-effect-ownership**

TBD - created by archiving change solim-architecture-refactor. Update Purpose after archive.

### Requirement: Effects auto-register inside component scope
When `Effect.of(...)` is called while a component's `build()` is executing (i.e., `ComponentContext` has an active component), the Effect SHALL be registered with that component's ownership list before its initial execution.

#### Scenario: Effect created during build() is auto-owned
- **WHEN** `Effect.of(() -> ...)` is called inside `build()`
- **THEN** the Effect is added to the component's disposable list before `runEffect()` is called

#### Scenario: Effect is disposed when owning component is disposed
- **WHEN** the owning component is disposed
- **THEN** the Effect is disposed and unsubscribes from all its signal dependencies

### Requirement: Effects outside component scope are caller-managed
When `Effect.of(...)` is called outside any active component context, it SHALL not be auto-registered anywhere and SHALL be the caller's responsibility to dispose.

#### Scenario: Effect outside component scope has no implicit owner
- **WHEN** `Effect.of(...)` is called outside a `build()` execution
- **THEN** no component owns the Effect

### Requirement: All Effect.of() overloads share the same registration path
All three `Effect.of(Runnable)`, `Effect.of(Supplier<Runnable>)`, and `Effect.of(Consumer<Cleanup>)` SHALL use a single internal `create(...)` factory that performs registration before execution.

#### Scenario: Runnable overload registers before execution
- **WHEN** `Effect.of(runnable)` is called inside a build scope
- **THEN** the Effect is owned before `runnable` runs

#### Scenario: Supplier overload registers before execution
- **WHEN** `Effect.of(supplier)` is called inside a build scope
- **THEN** the Effect is owned before the supplier function runs

#### Scenario: Consumer overload registers before execution
- **WHEN** `Effect.of(cleanupConsumer)` is called inside a build scope
- **THEN** the Effect is owned before the consumer function runs

**Source: config-value-signal**

Enables `ConfigValue<T>` configuration instances to provide stable, cached Solim reactive signals with bidirectional synchronization against underlying preference storage.

### Requirement: Reactive Signal Exposure
`ConfigValue<T>` SHALL provide a stable, cached reactive `Signal<T>` instance via its `signal()` method.

#### Scenario: Accessing signal returns stable instance
- **WHEN** caller invokes `configValue.signal()` multiple times
- **THEN** the exact same `Signal<T>` reference is returned without creating new instances.

#### Scenario: Signal initialized with current configuration value
- **WHEN** `ConfigValue<T>` is created
- **THEN** its `signal()` holds the value returned by the preference getter or default value.

### Requirement: Bidirectional Synchronization
`ConfigValue<T>` SHALL synchronize mutations bidirectionally between its imperative setter and its reactive signal without entering infinite recursive update loops. Additionally, `ContextualConfigValue<T, K>` SHALL apply the same bidirectional guarantee: mutations via `set()` persist to the currently-active key and update the signal; discriminant changes save to the old key, load from the new key, and update the signal — all without re-entrant loops.

#### Scenario: Updating ConfigValue updates signal
- **WHEN** `configValue.set(newValue)` is called
- **THEN** the underlying preference is updated and `configValue.signal().get()` immediately reflects `newValue`.

#### Scenario: Updating signal updates preference storage
- **WHEN** `configValue.signal().set(newValue)` is called
- **THEN** the underlying configuration persistence setter is executed with `newValue`.

#### Scenario: Updating ContextualConfigValue updates signal and active key
- **WHEN** `contextualConfig.set(newValue)` is called while discriminant is in state `K`
- **THEN** `Core.settings` is updated at the key derived from `K` and `contextualConfig.signal()` emits `newValue`

#### Scenario: Discriminant change saves old value and loads new
- **WHEN** `contextualConfig` holds `valA` under key `K1` and the discriminant changes to `K2`
- **THEN** `valA` is persisted at key derived from `K1`, the value at key derived from `K2` is loaded, and the signal emits it — without triggering the set-listener for `K2`'s write

**Source: contextual-config-value**

Enables configuration values to dynamically switch their backing persistence storage key based on an ambient reactive discriminant signal (such as screen orientation).

### Requirement: ContextualConfigValue switches storage key on discriminant change
`ContextualConfigValue<T, K>` SHALL be a reactive config value that accepts a `Readable<K>` discriminant and a `Function<K, String>` key-suffix mapper. When the discriminant emits a new value, the currently-active storage key SHALL change, the old value SHALL be persisted under the old key, the new value SHALL be loaded from `Core.settings` under the new key (falling back to `defaultValue`), and the reactive signal SHALL be updated to reflect the new value.

#### Scenario: Discriminant change reloads value from new key
- **WHEN** a `ContextualConfigValue<Float, Boolean>` with discriminant `isPortrait` holds value `100f` under key `foo.x.landscape` and `isPortrait` changes to `true`
- **THEN** the value `100f` is persisted under `foo.x.landscape`, `Core.settings` is read at `foo.x.portrait`, and the signal emits that loaded value (or default if not yet stored)

#### Scenario: Mutation writes to active key
- **WHEN** `contextualConfig.set(42f)` is called while discriminant is `true` (portrait)
- **THEN** `Core.settings` stores `42f` at `foo.x.portrait` and the reactive signal emits `42f`

#### Scenario: Signal is reactive
- **WHEN** UI binds to `contextualConfig.signal()` and the discriminant changes
- **THEN** the signal emits the newly-loaded value so reactive components update automatically

#### Scenario: Default value used for unvisited keys
- **WHEN** a discriminant value is seen for the first time and no value exists in `Core.settings` for the derived key
- **THEN** the signal holds the `defaultValue` provided at construction

### Requirement: ConfigGroup factory methods for contextual values
`ConfigGroup` SHALL expose factory methods `boolValueKeyed`, `intValueKeyed`, `floatValueKeyed`, and `stringValueKeyed` that accept a base name, a discriminant `Readable<K>`, a key-suffix function `Function<K, String>`, and a default value, returning a `ContextualConfigValue<T, K>`. The storage key for each slot SHALL be derived as `resolveKey(baseName + "." + suffix.apply(discriminantValue))`.

#### Scenario: Key derivation uses ConfigGroup namespace
- **WHEN** `configGroup.floatValueKeyed("x", isPortrait, p -> p ? "portrait" : "landscape", 0f)` is called on a group with namespace `mindustrytool.features.chat.collapsed`
- **THEN** portrait values are stored at `mindustrytool.features.chat.collapsed.x.portrait` and landscape values at `mindustrytool.features.chat.collapsed.x.landscape`

**Source: orientation-signal**

Provides a framework-level reactive signal tracking screen orientation (portrait vs landscape) via Arc's `ResizeEvent`.

### Requirement: Signals provides reactive isPortrait
`Signals` SHALL provide a static `Readable<Boolean> isPortrait()` method returning a shared `Signal<Boolean>` that reflects `Core.graphics.isPortrait()`. Static initialization SHALL automatically initialize the signal's current value and install a `ResizeEvent` listener so the signal fires whenever the screen orientation changes.

#### Scenario: Signal reflects initial orientation on init
- **WHEN** `Signals` class is loaded
- **THEN** `Signals.isPortrait().peek()` equals `Core.graphics.isPortrait()` at that moment

#### Scenario: Signal fires on orientation change
- **WHEN** a `ResizeEvent` is fired and `Core.graphics.isPortrait()` returns a different value than before
- **THEN** `Signals.isPortrait()` emits the new boolean value

#### Scenario: Signal does not fire when orientation is unchanged
- **WHEN** a `ResizeEvent` is fired but `Core.graphics.isPortrait()` returns the same value as before
- **THEN** `Signals.isPortrait()` does NOT emit (Signal deduplicates equal values)

#### Scenario: Multiple subscribers receive orientation changes
- **WHEN** two features subscribe to `Signals.isPortrait()`
- **THEN** both receive the new value when orientation changes

**Source: signal-callback-cleanup**

TBD - created by archiving change solim-architecture-refactor. Update Purpose after archive.

### Requirement: createSignal registrar returns a cleanup handle
The `createSignal` method on `BaseComponent` SHALL accept a `Function<Runnable, Disposable>` as the registrar, where the returned `Disposable` represents the callback subscription. The returned disposable SHALL be owned by the component.

#### Scenario: Registrar disposable is owned
- **WHEN** `createSignal(registrar, supplier)` is called inside a component
- **THEN** `registrar.apply(callback)` is called and its returned `Disposable` is added to the component's ownership list

#### Scenario: Component disposal unregisters callback
- **WHEN** the component is disposed
- **THEN** the registrar's returned `Disposable.dispose()` is called, unregistering the callback

### Requirement: No false automatic cleanup claims
APIs that cannot actually unregister a callback SHALL NOT claim automatic cleanup. If an Arc event API does not support unsubscription, the caller MUST be informed via documentation or a runtime assertion.

#### Scenario: No-op cleanup is explicit
- **WHEN** a registrar cannot provide real cleanup and returns `() -> {}`
- **THEN** this is documented at the call site and does not silently claim cleanup capability

**Source: solim-binding**

TBD - created by archiving change create-solim-core. Update Purpose after archive.

### Requirement: Widgets support static and reactive values
Widget factory methods (e.g., `text(...)`, `button(...)`, `visible(...)`, `enabled(...)`) SHALL accept both plain values (`String`, `boolean`) and reactive values (`Signal<T>`, `Computed<T>`). Reactive overloads SHALL apply current value immediately and update on change.

#### Scenario: Static text
- **WHEN** `text("Hello")` is called inside a parent
- **THEN** a `Label` with "Hello" is added and no subscription is created

#### Scenario: Reactive text immediate apply
- **WHEN** `Signal<String> username = Signal.of("Alice")` and `text(username)` is called
- **THEN** resulting `Label` text is "Alice" immediately after construction

#### Scenario: Reactive text future updates
- **WHEN** `username.set("Bob")` after binding
- **THEN** `Label` text automatically updates to "Bob" without rebuilding the widget

### Requirement: Binding updates Arc element directly, no rebuild
Reactive bindings SHALL update the underlying Arc `Element` property via setter (e.g., `label.setText(...)`, `button.setDisabled(...)`) and SHALL NOT rebuild the widget or recreate the `Element`.

#### Scenario: No Element recreation on change
- **WHEN** reactive `text` binding updates due to signal change
- **THEN** `label` instance identity remains same (`==` check passes) and only its property changed

### Requirement: Binding is disposable and tracks subscription
Each reactive binding SHALL create an internal `Subscription`/`Effect` that is disposable. Widget wrappers SHALL expose or internally hold this subscription and dispose on `Component.dispose()` or when element is removed.

#### Scenario: Binding dispose stops updates
- **WHEN** `Binding<String> b = Binding.of(label::setText, username)` then `b.dispose()` then `username.set("Charlie")`
- **THEN** `label` text remains previous value

#### Scenario: Component dispose disposes bindings
- **WHEN** component creates `text(username)` binding and then `component.dispose()` is called
- **THEN** the binding subscription is disposed and no longer reacts

### Requirement: Common reactive properties
Widgets SHALL support reactive bindings for at least: `text` (String), `visible` (boolean), `enabled`/`disabled` (boolean), `style` (Style), and `checked` (for Checkbox/Switch). Additional widget-specific bindings (e.g., `value` for TextField/Slider, `progress` for ProgressBar) follow same pattern.

#### Scenario: Visible binding
- **WHEN** `button("Save").visible(isLoggedIn)` where `isLoggedIn = Signal.of(false)` and later `isLoggedIn.set(true)`
- **THEN** button visibility toggles via `element.setVisible(...)` or Arc equivalent

#### Scenario: Enabled binding
- **WHEN** `button(saveText, onClick).enabled(dirty)` where `dirty = Signal.of(false)` then `dirty.set(true)`
- **THEN** button enabled state updates via `button.setDisabled(!dirty.get())`

### Requirement: Binding helper and Effect integration
`Binding` utility SHALL be implementable via `Effect.of(() -> target.set(prop.get()))` or direct `Subscription`; both are valid. Implementation SHALL prefer `Effect` where multi-dependency computed is involved, or `Subscription` for single signal.

#### Scenario: Computed text binding
- **WHEN** `Computed<String> saveText = dirty.map(v -> v ? "● Save" : "Save")` and `button(saveText, ...)` is bound
- **THEN** binding correctly tracks `dirty` through `saveText` computed and updates on dirty change

### Requirement: No string concatenation for dynamic text
Dynamic text SHALL use bundle formatting or `Computed` mapping, not manual `Core.bundle.get(...) + value` concatenation inside binding.

#### Scenario: Bundle formatted binding
- **WHEN** display needs `Core.bundle.format("message.player", name.get())`
- **THEN** it is expressed as `Signal.computed(() -> Core.bundle.format("message.player", name.get()))` and bound as reactive text

**Source: solim-property-bindings**

TBD - created by archiving change clean-up-solim-refactor. Update Purpose after archive.

### Requirement: Direct Reactive Element Property Bindings
Solim SHALL provide direct property binding mechanisms that mutate existing Arc scene elements when reactive signals or computeds change without requiring standalone `Effect` definitions in user code.

#### Scenario: Binding element width
- **WHEN** an element's width is bound to a reactive `Readable<Float>`
- **THEN** changes to the reactive width immediately update the element's width and invalidate its layout hierarchy without requiring manual Effect management

#### Scenario: Binding element color
- **WHEN** an element's color is bound to a reactive `Readable<Color>`
- **THEN** changes to the reactive color update the element's color in place on the existing element instance

#### Scenario: Binding label text
- **WHEN** an Arc label's text is bound to a reactive `Readable<String>`
- **THEN** changes to the reactive string update the label text directly without reconstructing the label element

### Requirement: Isolation of Arc Layout Lifecycle from Reactive Graph
Arc layout lifecycle methods (`getPrefWidth`, `getPrefHeight`, `layout`, `draw`, `act`) SHALL operate purely on conventional element properties without performing reactive `.get()` reads.

#### Scenario: Arc layout pass execution
- **WHEN** Arc invokes layout passes on Solim-backed elements
- **THEN** the layout passes execute without registering reactive dependencies or triggering unintended effect re-runs

**Source: solim-signal-dispatcher**

One-flush-per-frame signal dispatching in Solim, providing lightweight batching and deduplication so reactive effects execute at most once per Mindustry frame even when multiple dependencies change during the same frame.

### Requirement: Signal effect batching and deduplication
The reactive system SHALL queue invalidated `Effect`s and execute them at most once per Mindustry frame upon `flush()`, regardless of how many dependency signals were modified in that frame.

#### Scenario: Multiple updates to a single signal within a frame
- **WHEN** a signal is updated multiple times in the same frame before `flush()`
- **THEN** an observing effect executes exactly once during the subsequent `flush()`

#### Scenario: Multiple dependency signals modified within a frame
- **WHEN** multiple signals observed by a single effect are modified in the same frame before `flush()`
- **THEN** the observing effect executes exactly once during the subsequent `flush()`

#### Scenario: Signal updates across separate frames
- **WHEN** a signal is updated, followed by `flush()`, and then updated again followed by another `flush()`
- **THEN** the observing effect executes once in each flush (twice in total)

### Requirement: Cascading effect processing during flush
The dispatcher SHALL continue processing effects that become dirty while another effect is executing during `flush()` until all pending effects are exhausted, using generation-based batch passes up to an iteration safety limit. A single flush pass SHALL drain all currently queued effects without incrementing the cascading generation limit.

#### Scenario: Effect dirtied during execution of another effect
- **WHEN** Effect A modifies Signal B during `flush()`, which invalidates Effect B
- **THEN** Effect B is enqueued and executed within a subsequent cascade pass of the same `flush()` cycle

#### Scenario: Large batch of independent effects does not trip cycle limit
- **WHEN** a single flush cycle contains more than 100 queued effects that do not produce recursive cascade loops
- **THEN** all effects execute completely without triggering an infinite reactive loop warning

#### Scenario: Cycle detection limits infinite execution
- **WHEN** effects cause a circular dependency that repeatedly enqueues effects exceeding the cascade depth threshold
- **THEN** the dispatcher terminates the flush loop, logs an error, and clears the queue

### Requirement: Disposed effect skipping
The dispatcher SHALL not execute any effect that has been disposed, even if it was scheduled prior to disposal.

#### Scenario: Effect disposed while queued
- **WHEN** an effect is invalidated and enqueued, and subsequently disposed before `flush()` runs
- **THEN** the effect is skipped during `flush()` and its logic does not run

### Requirement: Lazy computed values are not eagerly scheduled
The reactive system SHALL keep `Computed` evaluations lazy and SHALL not enqueue `Computed` observers into the frame dispatcher.

#### Scenario: Computed value marked dirty without eager evaluation
- **WHEN** a signal dependency of a `Computed` is modified
- **THEN** the `Computed` is marked dirty and notifies downstream observers, but does not recompute until `.get()` or `.peek()` is called

### Requirement: Idempotent frame lifecycle registration
The reactive system SHALL register its frame hook with Mindustry's `Trigger.update` at most once, even if initialization is invoked multiple times.

#### Scenario: Multiple initialization calls
- **WHEN** `SignalDispatcher.register()` or `UI.init()` is called multiple times
- **THEN** the update listener is registered exactly once with the event bus

**Source: two-way-binding**

TBD - created by archiving change solim-architecture-refactor. Update Purpose after archive.

### Requirement: Shared TwoWayBinding utility
A `TwoWayBinding<T>` utility SHALL exist in `solim.input` (package-private is acceptable) that implements the signal ↔ widget synchronization pattern with feedback loop prevention. It SHALL implement `Disposable`.

#### Scenario: Signal change updates widget
- **WHEN** the bound `Signal<T>` value changes
- **THEN** the widget is updated via the widget setter, with equality check to prevent unnecessary updates

#### Scenario: Widget change updates signal
- **WHEN** the widget fires a change event
- **THEN** the `Signal<T>` is updated via `signal.set(widgetGetter.get())`

#### Scenario: Programmatic signal update does not loop back
- **WHEN** signal changes and triggers widget setter
- **THEN** the widget setter does NOT cause another signal update (feedback loop is prevented)

#### Scenario: TwoWayBinding is Disposable
- **WHEN** `TwoWayBinding.dispose()` is called
- **THEN** the Effect and widget listener are both unregistered

### Requirement: All input components use TwoWayBinding
`Checkbox`, `SolimTextField`, `SolimSlider`, `SolimSelect`, and `Switch` SHALL delegate their two-way binding logic to `TwoWayBinding` and SHALL NOT independently maintain a `boolean updating` flag.

#### Scenario: Checkbox uses TwoWayBinding
- **WHEN** `Checkbox` is constructed with a `Signal<Boolean>`
- **THEN** its synchronization behavior is handled by `TwoWayBinding`

#### Scenario: SolimTextField uses TwoWayBinding
- **WHEN** `SolimTextField` is constructed with a `Signal<String>`
- **THEN** its synchronization behavior is handled by `TwoWayBinding`

**Source: rewrite-time-control**

Shared reactive net-state signals in `Signals.java` so game features gate on one source of truth instead of ad-hoc `Vars.net` checks.

### Requirement: Signals provides reactive net state
`Signals` SHALL provide shared reactive net-state accessors (`active()`, `server()`, `client()`, `singlePlayer()`, `localHosting()`, `clientPlaying()`) following the same contract as `isPortrait()`: static initialization of current values, event-driven updates posted to the app thread, and set-only-on-change so equal values do not emit. Full predicates and drivers are specified in the `net-signals` capability; this requirement only extends the ambient-state contract to cover net state.

#### Scenario: Net signals follow the isPortrait contract
- **WHEN** two features subscribe to `Signals.server()`
- **THEN** both receive new values on change and neither is notified when a refresh yields an equal value

#### Scenario: Net signals initialize without a game running
- **WHEN** `Signals` class is loaded in the menu with no session
- **THEN** `active()`, `server()`, and `client()` peek as false and `singlePlayer()` peeks as true

### Requirement: ConfigValue Reset
`ConfigValue<T>` SHALL provide a `reset()` method that restores its value to its configured `defaultValue`. Invoking `reset()` SHALL persist the default value to underlying storage and emit the default value on its reactive `signal()`.

#### Scenario: Calling reset restores defaultValue and updates storage and signal
- **WHEN** a `ConfigValue<T>` has been modified to a non-default value via `set()` or `signal().set()`
- **THEN** calling `reset()` sets the value back to `defaultValue`, updates preference storage, and causes `signal()` to emit the `defaultValue`.

### Requirement: ConfigValue Modification Detection
`ConfigValue<T>` SHALL provide an `isModified()` method that returns `true` if its current value differs from `defaultValue` (via `Objects.equals`), and `false` otherwise.

#### Scenario: Value matches defaultValue
- **WHEN** a `ConfigValue<T>` holds a value equal to `defaultValue`
- **THEN** `isModified()` returns `false`.

#### Scenario: Value differs from defaultValue
- **WHEN** a `ConfigValue<T>` holds a value not equal to `defaultValue`
- **THEN** `isModified()` returns `true`.

