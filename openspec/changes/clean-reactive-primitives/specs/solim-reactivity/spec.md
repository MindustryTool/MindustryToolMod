## MODIFIED Requirements

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

## REMOVED Requirements

### Requirement: Signal.map convenience
**Reason**: Replaced by the MODIFIED Signal primitive requirement above which already includes `map()`. This standalone requirement entry is superseded.
**Migration**: No action needed — `Signal.map()` is retained; only the listener/subscribe surface is removed.
