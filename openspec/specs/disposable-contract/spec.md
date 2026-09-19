# disposable-contract Specification

## Purpose

Defines the `Disposable` and `DisposableAction` contracts for deterministic, idempotent lifecycle cleanup throughout Solim.

## Requirements

### Requirement: Disposable isDisposed contract
The `Disposable` interface SHALL declare `boolean isDisposed()` as an abstract method without a default implementation. All implementors of `Disposable` SHALL track and report their disposal status accurately.

#### Scenario: Abstract isDisposed reports accurate status
- **WHEN** a concrete `Disposable` is disposed via `dispose()`
- **THEN** subsequent calls to `isDisposed()` return `true`

### Requirement: DisposableAction utility
The framework SHALL provide a `DisposableAction` class implementing `Disposable` that executes an underlying `Runnable` action at most once upon `dispose()` and maintains idempotent disposal status.

#### Scenario: DisposableAction runs action only once
- **WHEN** `DisposableAction.of(runnable)` is created and `dispose()` is called multiple times
- **THEN** the `runnable` executes exactly once on the first call, and `isDisposed()` returns `true` on and after the first call

#### Scenario: DisposableAction isDisposed initial state
- **WHEN** `DisposableAction.of(runnable)` is created and has not been disposed
- **THEN** `isDisposed()` returns `false`
