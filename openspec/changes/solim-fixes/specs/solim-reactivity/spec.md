## ADDED Requirements

### Requirement: Effect pending state is explicit and cannot latch to death
`Effect` SHALL represent its scheduling state explicitly rather than with a single boolean flag. A re-entrant attempt to run an effect that is already executing SHALL leave the effect in a state from which it will be re-scheduled, and SHALL NOT leave it permanently un-invokable. The dispatcher dedupe obligation SHALL be preserved: an effect SHALL NOT be enqueued more than once for the same invalidation.

#### Scenario: Re-entrant run does not wedge the effect
- **WHEN** `runPending()` is invoked on an effect that is already running
- **THEN** the effect returns to a queued state and is re-enqueued with the dispatcher, so a subsequent dependency change still causes it to run

#### Scenario: Single enqueue per invalidation
- **WHEN** a dependency of a queued effect changes repeatedly before the next flush
- **THEN** the effect appears in the dispatcher queue exactly once

#### Scenario: Normal invalidation still queues exactly one run
- **WHEN** an idle effect's dependency changes
- **THEN** the effect transitions to queued and is enqueued for the next flush

## MODIFIED Requirements

### Requirement: Computed with lazy recomputation and dynamic dependencies
`Computed<T>` SHALL implement `Readable<T>`, `ReactiveSource`, `ReactiveObserver`, and `Disposable`. It SHALL be always and only lazy pull: it marks itself dirty when any dependency invalidates it, and recomputes only when `get()` or `peek()` is called and `dirty` is true. `Computed` SHALL NOT expose a `subscribe()` method and SHALL NOT maintain a listener list. `Computed.invalidate()` SHALL set `dirty = true`, propagate invalidation to downstream observers, and return immediately without recomputing. `Computed` SHALL auto-register with the ambient `OwnershipContext` when constructed. `get()` and `peek()` SHALL behave identically with respect to a disposed instance: both SHALL return the cached value without invoking the supplier, and neither SHALL re-establish dependency subscriptions.

#### Scenario: Lazy recomputation only on get
- **WHEN** a dependency signal changes and `computed.get()` is not yet called
- **THEN** the computed supplier is NOT invoked until `computed.get()` or `computed.peek()` is called

#### Scenario: Invalidate does not recompute
- **WHEN** a dependency signal changes and the computed has downstream observers
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

#### Scenario: Disposed computed is inert for both accessors
- **WHEN** a `Computed` has been disposed and either `get()` or `peek()` is called
- **THEN** the cached value is returned, the supplier is NOT invoked, and no dependency subscription is re-established

#### Scenario: Live computed peek still recomputes lazily
- **WHEN** a live `Computed` is dirty and `peek()` is called
- **THEN** the supplier is invoked and the freshly computed value is returned, preserving lazy-getter semantics

### Requirement: Signal primitive
`Signal<T>` SHALL be a write-read reactive state holder implementing `Readable<T>` and `ReactiveSource`. `Signal.set(T)` SHALL notify `ReactiveObserver`s via the reactive graph. `Signal` SHALL NOT expose a `subscribe()` method and SHALL NOT maintain a listener list. `Signal.get()` SHALL track the caller as a reactive dependency via `ReactiveContext`. `Signal.peek()` SHALL return the current value without tracking. `Signal.map(Func)` SHALL create a derived `Computed<R>`. The equality guard in `set()` remains: if the new value equals the current value, no notification is issued. `set()` SHALL notify observers by iterating an immutable observer snapshot and SHALL NOT allocate a per-write observer collection.

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

#### Scenario: Repeated writes allocate no per-write observer collection
- **WHEN** a signal with a stable observer set is written N times
- **THEN** no new observer collection object is allocated for those writes

#### Scenario: Observer added during notification is deferred
- **WHEN** an observer adds itself as an observer of the signal from inside `invalidate()`
- **THEN** it is not invoked for the in-flight notification and receives subsequent changes only

### Requirement: Unified AttachmentStack entry encapsulation
`AttachmentStack` SHALL bundle per-parent state, including its `Table`, `Attacher`, and `pendingComponents`, into a single `Entry` object. `AttachmentStack.isolate()` SHALL suspend the attachment stack for the duration of the supplied block and restore the stack to its entry depth on exit, discarding any entries the block pushed without popping. Restoration SHALL be achieved by truncation to the recorded depth rather than by copying or snapshotting the stack, so that isolation costs work proportional to the entries pushed by the block rather than to the depth at which it was entered. `isolate()` SHALL assert main-thread execution. This contract SHALL match the truncation semantics already specified for `ReactiveContext.untracked`.

#### Scenario: Isolation preserves complete parent state
- **WHEN** code executes inside `AttachmentStack.isolate(...)`
- **THEN** no outer pending components or attachers are accessible or mutated, and the outer state is restored upon exit

#### Scenario: Unbalanced block does not leak stack entries
- **WHEN** a block inside `isolate()` pushes an attachment entry and does not pop it
- **THEN** after `isolate()` returns, the stack depth equals the depth recorded on entry and the leaked entry is discarded rather than reinstated

#### Scenario: Isolate enforces main-thread execution
- **WHEN** `isolate()` is invoked from a thread other than the registered main thread
- **THEN** an `IllegalStateException` is raised

### Requirement: Isolated component factory construction
A component created inside an isolated factory scope SHALL have every resource registered during its construction adopted into that component, and any resource left unadopted when the scope closes SHALL be disposed. This protocol SHALL be available as a single shared helper so that all factory call sites apply the same protections.

#### Scenario: Factory resources are adopted into the created component
- **WHEN** a factory creates a component and, around it, a `Computed`, an `Effect`, or another resource
- **THEN** those resources are adopted into the returned component and are disposed with it

#### Scenario: Unadopted residue is disposed on scope close
- **WHEN** the factory throws before its resources are adopted
- **THEN** the unadopted resources are disposed and the exception propagates

#### Scenario: Factory output does not attach to the enclosing container
- **WHEN** a factory runs inside an isolated scope while an enclosing declarative `children()` block is open
- **THEN** the created component attaches only where the factory explicitly places it

#### Scenario: Multiple call sites share one protocol
- **WHEN** `StructuralReconciler`, `Fragment.capture`, `QueryView`, `Popup.render`, and `SolimStack.layer` each construct a component from a user factory
- **THEN** all of them apply untracked execution, attachment isolation, element forcing, and residue rollback through the same shared helper

### Requirement: Declarative tab content and empty-state views have explicit ownership
Content constructed by a `Tabs` tab builder or by a `ReactiveGrid` empty-state callback SHALL execute inside an explicitly opened ownership scope, so that resources it creates are owned by the component rather than depending on whichever ambient scope happens to be open at call time.

#### Scenario: Tab content resources are owned by the tab component
- **WHEN** a tab's content builder creates an `Effect` or `Computed` and the `Tabs` component is later disposed
- **THEN** those resources are disposed with the `Tabs` component

#### Scenario: Empty-state resources are owned by the grid
- **WHEN** a `ReactiveGrid` empty-state callback creates a resource and the grid is later disposed
- **THEN** that resource is disposed with the grid

#### Scenario: Ownership does not depend on the calling context
- **WHEN** the same tab content builder is invoked outside any effect body
- **THEN** the resources it creates are still owned by the tab component