## ADDED Requirements

### Requirement: Ambient Context Guard at Teardown
The test infrastructure SHALL provide a base test fixture (`SolimTestHarness`) that executes after each test to verify that Solim runtime ambient contexts are completely balanced and empty, preventing context leakage between test runs. Specifically, it MUST assert that `ParentStack.current()` has no dangling elements, `ComponentContext.current()` is null, `ReactiveContext.current()` is null, and `SignalDispatcher` has no pending dirty effects or uncompleted flush flags.

#### Scenario: Clean state passes teardown assertion
- **WHEN** a test completes normally having pushed and popped contexts symmetrically
- **THEN** teardown validation succeeds without raising an exception

#### Scenario: Corrupted ParentStack fails teardown
- **WHEN** a test leaks an unpopped parent onto `ParentStack`
- **THEN** teardown validation catches the non-empty stack, resets ambient state to clean, and throws `AssertionError`

#### Scenario: Corrupted ReactiveContext fails teardown
- **WHEN** an exception or faulty test leaves an active observer on `ReactiveContext`
- **THEN** teardown catches the non-empty context stack, resets it, and throws `AssertionError`

#### Scenario: Pending effects in SignalDispatcher queue fail teardown
- **WHEN** a test invalidates effects without flushing or disposing them
- **THEN** teardown reports unhandled queued effects and resets dispatcher state

---

### Requirement: Deterministic Test Scheduler
The test infrastructure SHALL provide a deterministic scheduler implementation (`TestScheduler`) that enables tests to trigger discrete execution flushes, inspect queued effects, control cascading depth, and step through effect processing without depending on Mindustry frame loops or real-time thread sleeps.

#### Scenario: Synchronous flush execution
- **WHEN** multiple effects are invalidated and `TestScheduler.flush()` is invoked
- **THEN** all pending effects execute in deterministic FIFO order within the current thread

#### Scenario: Queue inspection prior to flush
- **WHEN** three distinct effects are invalidated
- **THEN** `TestScheduler.pendingCount()` reports 3, and individual effects can be queried for pending status

#### Scenario: Cascade pass stepping
- **WHEN** an effect mutates a signal during flush that enqueues another effect
- **THEN** `TestScheduler.flushNextGeneration()` executes only the current generation and leaves cascade generations pending for explicit verification

#### Scenario: Disposal while queued is skipped deterministically
- **WHEN** an effect is queued in `TestScheduler` and subsequently disposed prior to `flush()`
- **THEN** `TestScheduler.flush()` executes without invoking the disposed effect

---

### Requirement: Test Doubles for Verification
The test infrastructure SHALL provide standardized test doubles: `TestDisposable`, `TestComponent`, `TestObserver`, and `TestAttacher` to record lifecycle invocations, assertion checkpoints, and error simulation.

#### Scenario: TestDisposable tracks disposal count and order
- **WHEN** a `TestDisposable` is passed into an ownership container and disposed
- **THEN** `disposed()` returns true, `disposeCount()` equals 1, and its order index in global disposal sequence is recorded

#### Scenario: TestDisposable error simulation
- **WHEN** `TestDisposable.failing()` is configured to throw on disposal
- **THEN** `dispose()` throws a defined `RuntimeException` while recording the attempt

#### Scenario: TestComponent lifecycle tracking
- **WHEN** `TestComponent` is created and element is requested
- **THEN** `buildCount()` is exactly 1, and on `dispose()` its owned resources are disposed and `isDisposed()` becomes true

#### Scenario: TestObserver dependency recording
- **WHEN** `TestObserver` is executed within `ReactiveContext.track()`
- **THEN** all accessed signals are recorded in `trackedDependencies()` and subsequent notifications increment `updateCount()`

---

### Requirement: Leak Detection and Allocation Sweeper
The test infrastructure SHALL provide leak assertion helpers that execute repeated allocation-and-disposal cycles (e.g. 100+ components or reconciliations) and verify that no subscriptions, observers, or listener callbacks remain registered on target signals or dispatchers.

#### Scenario: Repeated component mount and dispose leaves zero listeners
- **WHEN** 100 `TestComponent` instances observing a shared `Signal` are created and disposed
- **THEN** `signal.subscriberCount()` returns 0 and no strong references remain
