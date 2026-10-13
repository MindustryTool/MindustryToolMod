# solim-reactivity Specification

## Purpose

Mechanical merge of 10 specs per change `spec-domain-merge` (stage 3 core, concat-then-dedupe). Sources: solim-reactivity, automatic-effect-ownership, config-value-signal, contextual-config-value, orientation-signal, signal-callback-cleanup, solim-binding, solim-property-bindings, solim-signal-dispatcher, two-way-binding. Each source below appears under a `**Source:` marker with its purpose body and requirement blocks verbatim; per-source `## Purpose` / `## Requirements` header lines are removed so all requirements parse inside the single `## Requirements` section. TBD purposes carried forward; requirement dedupe is follow-up work.
## Requirements

**Source: solim-reactivity**

### Requirement: Signal primitive
`Signal<T>` SHALL be a write-read reactive state holder implementing `Readable<T>` and `ReactiveSource`. `Signal.set(T)` SHALL notify `ReactiveObserver`s via the reactive graph. `Signal` SHALL NOT expose a `subscribe()` method and SHALL NOT maintain a listener list. `Signal.get()` SHALL track the caller as a reactive dependency via `ReactiveContext`. `Signal.peek()` SHALL return the current value without tracking. `Signal.map(Func)` SHALL create a derived `Computed<R>`. The equality guard in `set()` remains: if the new value equals the current value, no notification is issued.

#### Scenario: Equality-guarded notification
- **WHEN** `signal.set(value)` is called with a value equal to the current value
- **THEN** no observer is notified and `observerCount()` observers remain unchanged

#### Scenario: Map creates derived Computed
- **WHEN** `signal.map(v -> v + "!")` is called
- **THEN** a `Computed<String>` is returned whose value updates reactively when the signal changes

#### Scenario: Signal get tracks dependency in Computed and Effect
- **WHEN** `signal.get()` is called inside a running `Computed` or `Effect`
- **THEN** the signal is recorded as a dependency and the observer is invalidated on the next `signal.set()`

#### Scenario: No subscribe method exists
- **WHEN** a caller attempts to call `signal.subscribe(...)`
- **THEN** a compile-time error is raised because the method does not exist

### Requirement: Computed with lazy recomputation and dynamic dependencies
`Computed<T>` SHALL implement `Readable<T>`, `ReactiveSource`, `ReactiveObserver`, and `Disposable`. It SHALL be always and only lazy pull: it marks itself dirty when any dependency invalidates it, and recomputes only when `get()` or `peek()` is called and `dirty` is true. `Computed` SHALL NOT expose a `subscribe()` method and SHALL NOT maintain a listener list. `Computed.invalidate()` SHALL set `dirty = true`, propagate invalidation to downstream observers, and return immediately without recomputing. `Computed` SHALL auto-register with the ambient `OwnershipContext` when constructed.

#### Scenario: Lazy recomputation only on get
- **WHEN** a dependency signal changes and `computed.get()` is not yet called
- **THEN** the computed supplier is NOT invoked until `computed.get()` or `computed.peek()` is called

#### Scenario: Invalidate does not recompute
- **WHEN** a dependency signal changes and the computed has downstream observers but no listeners
- **THEN** `invalidate()` sets dirty, propagates to downstream observers, and does NOT call the supplier

#### Scenario: Dynamic dependencies cleanup
- **WHEN** a computed conditionally reads signal A or signal B based on a boolean and the boolean changes
- **THEN** the computed unregisters from the signal it no longer reads and registers with the newly read signal

#### Scenario: Cycle detection does not infinite loop
- **WHEN** a computed's supplier reads itself
- **THEN** an `IllegalStateException` is thrown rather than infinite recursion

#### Scenario: Invalidation propagates to dependent Computeds
- **WHEN** a signal changes and a chain of computeds depends on it
- **THEN** all dependent computeds are marked dirty in order without recomputing

#### Scenario: Computed is ownable as a Disposable
- **WHEN** a `Computed` is constructed inside a component `build()`
- **THEN** it is auto-registered with the ambient owner and disposed when the component is disposed

#### Scenario: No subscribe method exists on Computed
- **WHEN** a caller attempts to call `computed.subscribe(...)`
- **THEN** a compile-time error is raised because the method does not exist

### Requirement: Subscription and Disposable contracts
`Subscription` as a distinct interface SHALL NOT exist. Reactive observation and reactive change reactions SHALL be expressed exclusively through `Effect`. The `Disposable` contract remains unchanged.

#### Scenario: Subscription type does not exist
- **WHEN** code references the type `Subscription`
- **THEN** a compile-time error is raised because the type has been removed

#### Scenario: Reactions expressed through Effect
- **WHEN** a caller wants to react to signal changes
- **THEN** they create an `Effect.of(() -> signal.get())` which auto-owns and disposes with its parent

### Requirement: Binding updates Arc element directly, no rebuild
An `Effect.of(Readable<T>, Cons<T>)` SHALL update the bound Arc element property directly without creating or replacing any Element or Component. The Effect SHALL re-run on the next game frame after the source changes, calling the consumer with the new value.

#### Scenario: No Element recreation on change
- **WHEN** a signal bound to a label's text changes
- **THEN** only `label.setText(newValue)` is called and the label element instance remains the same

### Requirement: Binding is disposable and tracks subscription
An `Effect` created via `Effect.of(Readable<T>, Cons<T>)` SHALL be a `Disposable`. Disposing it SHALL detach it from its reactive dependencies and stop all future consumer invocations. This replaces the prior `Subscription`-based disposal model.

#### Scenario: Binding dispose stops updates
- **WHEN** the owning component is disposed (which disposes the effect)
- **THEN** subsequent source changes do not invoke the consumer

### Requirement: Untracked Execution in ReactiveContext
`ReactiveContext` SHALL provide an `untracked(Supplier<T>)` and `untracked(Runnable)` mechanism that temporarily suspends active dependency tracking so that any signal or computed reads occurring inside the block are not recorded as dependencies of the active `ReactiveObserver`. Suspension SHALL be implemented by recording the current stack depth and restoring to that depth on exit, rather than by copying the observer stack. Restoration SHALL occur even when the block pushes an observer without popping it.

#### Scenario: Reading signals inside untracked block
- **WHEN** an `Effect` is executing and invokes `ReactiveContext.untracked(() -> signal.get())`
- **THEN** `signal` is not added as a dependency to the running `Effect`, and changes to `signal` do not trigger the effect to re-run

#### Scenario: Dynamic component factory isolation
- **WHEN** `Dynamic` executes its child component factory
- **THEN** the factory is invoked within an untracked scope, preventing child signal evaluations during component construction from leaking into the `Dynamic` switcher effect

#### Scenario: Observer stack is restored after a balanced block
- **WHEN** a block pushed onto and popped from the observer stack runs inside `untracked`
- **THEN** the surrounding observer stack is exactly as it was before the call

#### Scenario: Observer stack is restored after an unbalanced block
- **WHEN** a block inside `untracked` pushes an observer and does not pop it
- **THEN** after `untracked` returns, the stack depth equals the depth recorded on entry

**Source: automatic-effect-ownership**

### Requirement: Effects auto-register inside component scope
When `Effect.of(...)` is called while a component's `build()` is executing (i.e., `OwnershipContext` has an active component), the Effect SHALL be registered with that component's ownership list before its initial execution.

#### Scenario: Effect created during build() is auto-owned
- **WHEN** `Effect.of(() -> ...)` is called inside `build()`
- **THEN** the Effect is added to the component's disposable list before `runEffect()` is called

#### Scenario: Effect is disposed when owning component is disposed
- **WHEN** the owning component is disposed
- **THEN** the Effect is disposed and unsubscribes from all its signal dependencies

### Requirement: Effects outside component scope are caller-managed
When `Effect.of(...)` is called outside any active component context, it SHALL establish an `OwnershipScope` for the duration of its body, adopt every resource registered by that body into the Effect itself, and dispose any resource that was not adopted if the body throws. It SHALL NOT attach those resources to an ambient enclosing component.

#### Scenario: Effect outside component scope has no implicit owner
- **WHEN** `Effect.of(...)` is called outside a `build()` execution
- **THEN** no enclosing component owns the Effect

#### Scenario: Effect owns resources its body creates
- **WHEN** an effect body creates a component or a reactive primitive and the effect is later disposed
- **THEN** that resource is disposed with the effect and not left attached to the session root

#### Scenario: Effect body throwing disposes what it created
- **WHEN** an effect body registers resources and then throws
- **THEN** the unadopted resources are disposed before the exception propagates

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

### Requirement: Transactional reconciliation and duplicate-key detection
`StructuralReconciler` SHALL validate keys prior to component instantiation, logging a warning and ignoring duplicate items ("first wins" strategy) instead of throwing an `IllegalArgumentException`. If child component creation fails or throws during reconciliation, all components newly instantiated during that failed reconciliation attempt SHALL be disposed immediately, and previously active components SHALL remain untouched. Components removed during a successful reconciliation SHALL be disposed only after the new state is committed.

#### Scenario: Duplicate key logs warning and drops duplicate item
- **WHEN** reconciliation is invoked with an item collection containing duplicate extracted keys
- **THEN** a warning is logged, the first occurrence of the key is retained, duplicate items are ignored without throwing an exception, and active components are updated accordingly

#### Scenario: Component factory failure rolls back newly created components
- **WHEN** the component factory throws an exception on the N-th item during reconciliation
- **THEN** the first N-1 newly created components are disposed, previously active components are preserved, and the exception is propagated

#### Scenario: Removed components are disposed only after successful commit
- **WHEN** reconciliation completes without errors
- **THEN** removed components are disposed after updating the active component map

### Requirement: Unified AttachmentStack entry encapsulation
`AttachmentStack` SHALL bundle per-parent state, including its `Table`, `Attacher`, and `pendingComponents`, into a single `Entry` object. `AttachmentStack.isolate()` SHALL save and restore the complete stack context so that pending attachments and attachers do not leak across isolation boundaries.

#### Scenario: Isolation preserves complete parent state
- **WHEN** code executes inside `AttachmentStack.isolate(...)`
- **THEN** no outer pending components or attachers are accessible or mutated, and the outer state is restored upon exit

### Requirement: Unified reactive error logging
`Signal`, `Computed`, and `Effect` SHALL log evaluation, listener, and observer errors using Arc's `Log.err(...)` rather than standard error streams.

#### Scenario: Observer error logged via Log.err
- **WHEN** an observer throws an unhandled exception during notification
- **THEN** the error is logged through `Log.err` with stack trace and context

### Requirement: Typed dependency dispatch preserves reactivity
The system SHALL wire `Computed` and `Effect` dependencies through the shared reactive-source contract instead of concrete-type chains, preserving equality guards, lazy recomputation, dynamic cleanup, cycle handling, disposal parity, and main-thread assertions for `Signal`, `Computed`, `MapSignal`, and key readables.

#### Scenario: Key-level isolation preserved after typing
- **WHEN** an observer reads `mapSignal.readable(keyA).get()` and `mapSignal.put(keyB, value)` changes a different key
- **THEN** the observer is not invalidated, while observers of `keyB` and whole-map observers are

#### Scenario: Mixed-source effect re-runs on any dependency
- **WHEN** an `Effect` reads a `Signal`, a `Computed`, and a `MapSignal` key readable
- **THEN** a change to any of the three re-runs the effect, and disposing the effect detaches it from all three

**Source: solim-query**

Adds `Query` and `Mutation` as first-class async reactive primitives alongside Signal/Computed/Effect, with `UI.query()`/`UI.mutation()` facades.

### Requirement: Query reactive primitive
`Query<T>` SHALL be a reactive primitive in `solim-core/src/solim/reactive/` alongside `Signal`, `Computed`, and `Effect`. It SHALL implement `Disposable` and `Readable<T>` (where `get()` returns the current data value). It SHALL integrate with `OwnershipContext.register()` for automatic lifecycle ownership, the same as `Computed` and `Effect`.

#### Scenario: Query listed as reactive primitive
- **WHEN** the Solim reactive module is inspected
- **THEN** `Query` SHALL be available alongside `Signal`, `Computed`, `Effect`, and `Mutation` as a first-class reactive primitive

#### Scenario: Query implements Readable
- **WHEN** `query.get()` is called
- **THEN** it SHALL return the current data value (equivalent to `query.data().get()`) and participate in reactive dependency tracking

### Requirement: Mutation reactive primitive
`Mutation<T, R>` SHALL be a reactive primitive in `solim-core/src/solim/reactive/` alongside `Signal`, `Computed`, `Effect`, and `Query`. It SHALL implement `Disposable` and integrate with `OwnershipContext.register()` for automatic lifecycle ownership.

#### Scenario: Mutation listed as reactive primitive
- **WHEN** the Solim reactive module is inspected
- **THEN** `Mutation` SHALL be available as a first-class reactive primitive

### Requirement: UI facade methods
The `solim.UI` facade SHALL expose static methods for creating `Query` and `Mutation` instances, consistent with the existing pattern for `signal()`, `computed()`, and `effect()`.

#### Scenario: UI.query facade
- **WHEN** `UI.query(key, fetcher)` is called in a declarative Solim component
- **THEN** it SHALL create and return a `Query<T>` instance, equivalent to `Query.of(key, fetcher)`

#### Scenario: UI.mutation facade
- **WHEN** `UI.mutation(mutator)` is called in a declarative Solim component
- **THEN** it SHALL create and return a `Mutation<T, R>` instance, equivalent to `Mutation.of(mutator)`

### Requirement: Reactive pipeline fail-fast error propagation
The reactive framework (`SignalDispatcher`, `Computed`, `Signal`, `MapSignal`, `Mutation`, and `Effect`) SHALL NOT swallow or silently log unhandled exceptions thrown during reactive effect execution, computed value calculation, subscriber notifications, or mutation completion callbacks, and SHALL re-throw them so they terminate the executing thread.

#### Scenario: Reactive effect execution error terminates flush
- **WHEN** a scheduled effect throws a `RuntimeException` during `SignalDispatcher.flush()`
- **THEN** `SignalDispatcher` does NOT swallow the exception in `catch (Throwable t)` and the exception escapes `flush()`

#### Scenario: Effect body errors are not caught by Effect
- **WHEN** an `Effect` body throws during `runPending()`
- **THEN** the exception propagates out of `runPending()` rather than being caught and logged inside `Effect`

#### Scenario: Exception raised during an effect's initial synchronous run
- **WHEN** `Effect.of(...)` is called and the body throws during the initial run inside `build()`
- **THEN** the exception propagates out of `Effect.of` and the owning component's `build()` fails as a result

#### Scenario: Computed supplier error propagates immediately
- **WHEN** a `Computed` evaluation throws an unhandled exception inside its supplier (`Prov.get()`)
- **THEN** the exception is thrown immediately to the caller and is not suppressed with `dirty = false` and stale cached values

#### Scenario: Signal subscriber exception propagates
- **WHEN** a subscriber listener registered on a `Signal` throws a `RuntimeException` when updated
- **THEN** the exception is not caught and swallowed by `Signal.set()`, escaping to the caller

#### Scenario: Mutation callbacks propagate unhandled errors
- **WHEN** an `onSuccess` or `onError` callback in `Mutation` throws an unhandled exception
- **THEN** the exception is not swallowed in an empty `catch (Throwable t)` block and propagates to the main thread

### Requirement: Unprocessed effects survive an aborted flush
When an effect throws during `SignalDispatcher.flush()`, effects that were not polled from the queue SHALL retain their pending flag and remain in the queue, so they execute on the next flush rather than being silently discarded.

#### Scenario: Remaining queue survives a throwing effect
- **WHEN** effects A, B and C are queued and A throws during flush
- **THEN** B and C remain queued with their pending flags set and execute on the next flush

#### Scenario: Dispatcher is not left marked as flushing
- **WHEN** flush terminates because an effect threw
- **THEN** `isFlushing()` reports false so the next flush is permitted

### Requirement: Dependency staging buffer is reused
`Computed` and `Effect` SHALL reuse a single dependency-collection buffer across recomputations rather than allocating a fresh collection per run, clearing and refilling it before each evaluation and diffing it against the previously recorded dependency set.

#### Scenario: Repeated recomputation allocates no staging collection
- **WHEN** the same `Computed` recomputes N times with an unchanged dependency set
- **THEN** no new dependency-collection object is allocated for those runs

#### Scenario: Dependency diff is still correct when the set is unchanged
- **WHEN** a recomputation observes exactly the dependencies recorded previously
- **THEN** no `addObserver` or `removeObserver` call is issued

