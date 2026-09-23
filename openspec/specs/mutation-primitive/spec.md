# mutation-primitive Specification

## Purpose

Async action primitive `Mutation<T,R>` for write operations (send, upload, delete) with pending/error/result tracking and lifecycle hooks for optimistic updates. Established by change `solim-query`.

## Requirements

### Requirement: Mutation Creation
`Mutation<T, R>` SHALL be created via `Mutation.of(Function<T, CompletableFuture<R>> mutator)` where `T` is the input type and `R` is the result type. It SHALL automatically register with the active `OwnershipContext` for lifecycle ownership.

#### Scenario: Mutation created in component build
- **WHEN** `Mutation.of(mutator)` is called inside a component's `build()` method
- **THEN** the Mutation SHALL be registered with the component's lifecycle

#### Scenario: Mutation does not execute on creation
- **WHEN** a Mutation is created
- **THEN** no async operation SHALL be triggered until `mutate(input)` is called

### Requirement: Mutation Execution
`Mutation` SHALL expose a `mutate(T input)` method that triggers the async operation.

#### Scenario: Mutate triggers async operation
- **WHEN** `mutation.mutate(input)` is called
- **THEN** the mutator function SHALL be invoked with the input, and the resulting `CompletableFuture` SHALL be tracked

#### Scenario: Concurrent mutation replaces previous
- **WHEN** `mutate()` is called while a previous mutation is still in-flight
- **THEN** the previous result SHALL be discarded (generation counter) and only the latest mutation's result SHALL be applied

### Requirement: Mutation State
`Mutation<T, R>` SHALL expose its state via three `Readable<>` accessors:
- `isPending()` — `Readable<Boolean>` true while the mutation is in-flight
- `error()` — `Readable<Throwable>` containing the last error (or null)
- `result()` — `Readable<R>` containing the last successful result (or null)

#### Scenario: Initial state
- **WHEN** a Mutation is created but not yet invoked
- **THEN** `isPending()` SHALL return false, `error()` SHALL return null, `result()` SHALL return null

#### Scenario: Pending state during mutation
- **WHEN** `mutate(input)` is called and the async operation is in-flight
- **THEN** `isPending()` SHALL return true

#### Scenario: Successful mutation
- **WHEN** the mutation completes successfully with result `R`
- **THEN** `isPending()` SHALL return false, `result()` SHALL return `R`, `error()` SHALL return null

#### Scenario: Failed mutation
- **WHEN** the mutation fails with error `E`
- **THEN** `isPending()` SHALL return false, `error()` SHALL return `E`, `result()` SHALL retain its previous value

### Requirement: Main Thread Marshaling
All signal mutations resulting from async completion SHALL be executed on the main thread via `Core.app.post()`.

#### Scenario: Background thread completion
- **WHEN** a mutation's `CompletableFuture` resolves on a background thread
- **THEN** the signal updates SHALL be marshaled to the main thread

### Requirement: Lifecycle Hooks
`Mutation` SHALL support optional lifecycle hooks for optimistic update patterns.

#### Scenario: onMutate hook for optimistic update
- **WHEN** a Mutation is configured with `.onMutate(input -> { /* optimistic update */ return rollbackContext; })` and `mutate(input)` is called
- **THEN** the `onMutate` hook SHALL be called synchronously on the main thread BEFORE the async operation starts, and its return value SHALL be passed to subsequent hooks

#### Scenario: onSuccess hook
- **WHEN** a Mutation is configured with `.onSuccess((result, context) -> { /* confirm */ })` and the mutation succeeds
- **THEN** the `onSuccess` hook SHALL be called on the main thread with the result and the context from `onMutate`

#### Scenario: onError hook for rollback
- **WHEN** a Mutation is configured with `.onError((error, context) -> { /* rollback */ })` and the mutation fails
- **THEN** the `onError` hook SHALL be called on the main thread with the error and the context from `onMutate`

#### Scenario: Hooks without onMutate
- **WHEN** `onSuccess` or `onError` hooks are configured without `onMutate`
- **THEN** the context parameter SHALL be null

### Requirement: Mutation Reset
`Mutation` SHALL expose a `reset()` method that clears `isPending`, `error`, and `result` back to initial state.

#### Scenario: Reset clears state
- **WHEN** `mutation.reset()` is called after a completed mutation
- **THEN** `isPending()` SHALL return false, `error()` SHALL return null, `result()` SHALL return null

### Requirement: Disposal
`Mutation` SHALL implement `Disposable`. On disposal, it SHALL discard any in-flight mutation result (via generation counter).

#### Scenario: Dispose discards in-flight
- **WHEN** a Mutation is disposed while a mutation is in-flight
- **THEN** the in-flight result SHALL be discarded

#### Scenario: Auto-dispose on component dispose
- **WHEN** the parent Solim component is disposed
- **THEN** the Mutation SHALL be automatically disposed
