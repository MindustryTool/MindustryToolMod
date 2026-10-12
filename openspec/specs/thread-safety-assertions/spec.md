# thread-safety-assertions Specification

## Purpose

Enforces single-threaded Arc Scene2D invariants across Solim runtime mutations and state transitions.

## Requirements

### Requirement: Always-on main-thread assertions
The Solim runtime SHALL provide thread verification checks (`SolimAssert.checkMainThread()`) guarding state mutations and lifecycle stack operations. If an operation is invoked on a thread other than the designated main thread, an `IllegalStateException` SHALL be thrown immediately.

#### Scenario: Mutation on background thread throws
- **WHEN** `Signal.set()` or `OwnershipContext.push()` is invoked from an off-main background thread while a main thread is registered
- **THEN** an `IllegalStateException` is thrown detailing the offending thread name

#### Scenario: Execution on main thread succeeds
- **WHEN** operations are invoked on the registered main thread
- **THEN** execution proceeds normally without exception

#### Scenario: Unset main thread allows test execution
- **WHEN** no main thread is registered (such as during standalone unit tests)
- **THEN** `SolimAssert.checkMainThread()` is a no-op and does not throw

### Requirement: Thread checks are unconditional
`SolimAssert.checkMainThread()` SHALL throw `IllegalStateException` whenever a main thread has been registered and the call originates from a different thread. It SHALL NOT offer a mode in which an off-main-thread access is logged and permitted. The `throww` configuration flag SHALL be removed.

#### Scenario: Off-main access always throws
- **WHEN** any guarded operation is invoked off the registered main thread
- **THEN** `IllegalStateException` is thrown, with no configuration that downgrades this to a log

#### Scenario: No opt-out exists for a production build
- **WHEN** the codebase is searched for `throww`
- **THEN** no reference remains outside archived change history and the legacy `old/` folder

### Requirement: Dependency tracking is thread-guarded
`ReactiveContext.push` and `ReactiveContext.track` SHALL verify main-thread affinity. `track` SHALL perform the check only on the branch where an active observer exists and a dependency is about to be recorded against it, so that ordinary reads with no active observer are not rejected.

#### Scenario: Registering a dependency off-thread throws
- **WHEN** `ReactiveContext.track(source)` is called off the main thread while an observer is active on the tracking stack
- **THEN** `IllegalStateException` is thrown before the dependency is recorded

#### Scenario: Reading with no active observer is permitted off-thread
- **WHEN** `signal.get()` is called off the main thread and no observer is evaluating
- **THEN** the read proceeds normally, because no dependency is being recorded

#### Scenario: Track only pushes during evaluation
- **WHEN** `track` is called with no current observer and a non-null source
- **THEN** the method returns without recording anything and without raising a thread violation
