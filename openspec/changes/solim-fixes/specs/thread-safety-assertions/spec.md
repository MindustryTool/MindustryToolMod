## ADDED Requirements

### Requirement: Main-thread binding is explicit at bootstrap
The reactive and ownership stacks SHALL be bound to a specific main thread explicitly during UI initialization, before any reactive work begins. The main thread SHALL NOT be inferred from whichever thread happens to be the first to create or dispatch a reactive primitive. Main-thread assertions SHALL remain dormant only until the thread is explicitly bound.

#### Scenario: UI initialization binds the calling thread
- **WHEN** `UI.init()` is invoked
- **THEN** the main thread is set to the calling thread and the frame hook is registered

#### Scenario: Main thread is never inferred from reactive work
- **WHEN** the first reactive primitive in the process is created on a worker thread before any UI initialization
- **THEN** the worker thread is not recorded as the main thread, and a later main-thread assertion still rejects that worker thread

#### Scenario: Off-thread mutation is rejected after binding
- **WHEN** a signal write or an ownership-stack push is attempted from a thread other than the bound main thread after binding
- **THEN** an `IllegalStateException` naming both threads is raised

#### Scenario: Re-initialization is safe
- **WHEN** `UI.init()` is invoked more than once
- **THEN** the frame hook is registered exactly once and the main-thread binding is idempotent

### Requirement: Ambient stack assertions report violations without redundant work
A main-thread violation SHALL be reported by throwing an exception carrying the offending thread and the expected main thread. The assertion SHALL NOT additionally dump the current stack trace, because the exception it raises already captures one.

#### Scenario: Violation carries thread identity
- **WHEN** a main-thread assertion fails
- **THEN** the raised exception names both the expected main thread and the offending thread

#### Scenario: No redundant stack dump
- **WHEN** a main-thread assertion fails
- **THEN** the runtime does not perform a separate stack dump in addition to capturing the exception's stack trace

## MODIFIED Requirements

### Requirement: Reactive context tracking asserts main-thread execution
Push and pop operations on the reactive tracking stack SHALL assert main-thread execution. Reads of the currently-tracked observer SHALL NOT assert, because reads may legitimately occur during frames that are not themselves mutations.

#### Scenario: Push from a non-main thread is rejected
- **WHEN** an observer is pushed onto the tracking stack from a non-main thread
- **THEN** an `IllegalStateException` is raised

#### Scenario: Pop from a non-main thread is rejected
- **WHEN** an observer is popped from the tracking stack from a non-main thread
- **THEN** an `IllegalStateException` is raised

#### Scenario: Reading the current observer is not restricted
- **WHEN** the current observer is read from a thread that is not the bound main thread
- **THEN** no assertion is raised and the current stack state is returned