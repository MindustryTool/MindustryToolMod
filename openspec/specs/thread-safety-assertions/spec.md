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
