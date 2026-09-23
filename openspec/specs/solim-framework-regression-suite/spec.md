# solim-framework-regression-suite Specification

## Purpose

Comprehensive multi-phase behavioral regression test suite verifying Solim reactive runtime, lifecycle, reconciler, contexts, layout, and component integration. Created by archiving change solim-comprehensive-test-suite.

## Requirements

### Requirement: Lifecycle and Ownership Regression Suite
The test suite SHALL verify the complete component lifecycle contract: lazy build execution exactly once, immutable cached `element()`, nested ownership hierarchy, strict LIFO disposal order, exception containment when an owned resource fails during disposal, and partial build failure cleanup.

#### Scenario: Build executes lazily and exactly once
- **WHEN** a component is instantiated
- **THEN** its `build()` method is not invoked until `element()` is called, and repeated `element()` calls return the identical element instance without re-executing `build()`

#### Scenario: LIFO disposal order
- **WHEN** a component registers owned resources A, B, and C in order
- **THEN** on `dispose()`, C is disposed first, followed by B, then A

#### Scenario: Disposal error isolation
- **WHEN** an owned resource throws a `RuntimeException` during `dispose()`
- **THEN** the error is caught/logged and remaining owned resources in the component are still cleanly disposed

#### Scenario: Partial build cleanup on exception
- **WHEN** `build()` registers several owned resources and subsequently throws an exception
- **THEN** all resources registered up to that point are automatically disposed and `OwnershipContext` is popped cleanly

---

### Requirement: Disposable Contract Compliance Suite
The test suite SHALL verify that every class implementing `Disposable` across `solim-api`, `solim-runtime`, and `solim-core` adheres to the contract: `isDisposed()` returns false initially, returns true after `dispose()`, repeated calls to `dispose()` are safe no-ops, and disposed instances reject or safely ignore post-disposal operations.

#### Scenario: Abstract isDisposed tracking
- **WHEN** any framework `Disposable` implementation is queried
- **THEN** `isDisposed()` accurately reflects whether `dispose()` has been executed

#### Scenario: Double disposal idempotency
- **WHEN** `dispose()` is invoked repeatedly on a `Disposable` instance
- **THEN** subsequent calls execute without error and no side effects or repeat cleanups are triggered

---

### Requirement: Reactive Signal Correctness Suite
The test suite SHALL verify the full behavioral contract of `Signal<T>`: initial value retrieval, untracked `peek()`, equality suppression (preventing notifications when new value equals current value), subscription disposal, nested mutations, and mutation during effect runs.

#### Scenario: Equality suppression
- **WHEN** `Signal.of("value")` has `set("value")` called with an equal string
- **THEN** registered subscribers and dependent computeds/effects receive zero notifications

#### Scenario: Mutation during effect execution
- **WHEN** an executing effect mutates another signal B
- **THEN** dependent observers of signal B are invalidated and scheduled for subsequent execution without corrupting active effect context

#### Scenario: Post-disposal subscription mutation
- **WHEN** a subscription to a signal is disposed and the signal updates
- **THEN** the disposed subscription callback is never invoked

---

### Requirement: Computed Memoization and Dynamic Branch Suite
The test suite SHALL verify that `Computed<T>` evaluates lazily on `get()`, caches computed values until dependency invalidation, dynamically updates subscriptions when conditional branches change (`if (cond) A.get() else B.get()`), detects recursive self-computation cycles, and restores reactive context if supplier throws.

#### Scenario: Lazy memoization and invalidation
- **WHEN** a `Computed` dependency changes
- **THEN** the supplier function does not re-run until `get()` or `peek()` is invoked

#### Scenario: Inactive dependency pruning on branch switch
- **WHEN** a conditional `Computed` toggles from branch A to branch B
- **THEN** changes to signal A no longer invalidate or trigger re-evaluations of the `Computed`

#### Scenario: Cycle detection halts recursion
- **WHEN** a `Computed` directly or transitively accesses its own `get()` during evaluation
- **THEN** a recursive computation cycle is detected and handled without causing a stack overflow

#### Scenario: Supplier exception restores ReactiveContext
- **WHEN** a `Computed` evaluation throws an uncaught exception
- **THEN** `ReactiveContext` is popped back to its prior state and ambient tracking remains uncorrupted

---

### Requirement: Effect Lifecycle and Cleanup Suite
The test suite SHALL verify that `Effect` executes immediately upon construction, tracks dynamic dependencies, invalidates into the scheduler, executes registered cleanups before each re-run and upon disposal, and guarantees that a disposed effect never executes even if queued.

#### Scenario: Immediate initial run and auto-tracking
- **WHEN** `Effect.of(runnable)` is instantiated
- **THEN** `runnable` executes immediately and registers all read signals as active dependencies

#### Scenario: Cleanup execution order
- **WHEN** an effect registers cleanup callbacks and dependency changes trigger a re-run
- **THEN** the cleanup callback executes before the new run of the effect body

#### Scenario: Disposed effect in queue is skipped
- **WHEN** an effect is queued for execution and `dispose()` is invoked before flush
- **THEN** upon scheduler flush the effect body does not execute

---

### Requirement: Scheduler Semantics and Cascade Protection Suite
The test suite SHALL verify the execution semantics of `SignalDispatcher`: FIFO ordering of independent effects, deduplication of duplicate invalidations within a single frame/flush, cascading passes for effects dirtied during flush, and cycle termination when cascade depth exceeds safety thresholds.

#### Scenario: Batching and deduplication
- **WHEN** an effect's dependency signal is mutated 5 times prior to flush
- **THEN** the effect executes exactly once during the flush

#### Scenario: Cascade pass depth limit
- **WHEN** two effects form an infinite ping-pong mutation loop
- **THEN** the scheduler terminates after the cascade limit, clears the queue, and logs an error without hanging the main thread

---

### Requirement: Ambient Context Restoration and Isolation Suite
The test suite SHALL verify that `ReactiveContext`, `OwnershipContext`, and `AttachmentStack` maintain strict balance, stack integrity, and isolation across nested operations, `withoutAutoOwnership` scopes, and unexpected runtime exceptions.

#### Scenario: Untracked execution does not register dependencies
- **WHEN** a signal is read inside `ReactiveContext.untracked(...)` within an effect
- **THEN** the signal is not added as a dependency of the effect

#### Scenario: Exception inside AttachmentStack children block unwinds stack
- **WHEN** an exception is thrown inside a container's `children()` block
- **THEN** `AttachmentStack` pops the active parent entry and returns to the prior parent level

#### Scenario: Sibling isolation in cell configuration
- **WHEN** parent cell configuration is applied to child A
- **THEN** cell modifiers do not leak or apply to sibling child B

---

### Requirement: Transactional StructuralReconciler Suite
The test suite SHALL verify that `StructuralReconciler` performs keyed diffing (insert, reuse, reorder, remove, dispose), detects duplicate keys prior to modification, and enforces transactional rollback such that a factory failure preserves the previous committed state without leaking components.

#### Scenario: Identity preservation and reordering
- **WHEN** an existing list of keys is reordered
- **THEN** existing component instances are reused without being recreated, and elements are reordered in the parent container

#### Scenario: Duplicate key detection
- **WHEN** a collection with duplicate keys is supplied to the reconciler
- **THEN** an `IllegalArgumentException` is raised, no components are mutated, and previous state is preserved

#### Scenario: Transactional rollback on factory failure
- **WHEN** a new key fails in the component factory during reconciliation of `[A, B]` to `[A, B, C]`
- **THEN** newly created elements are disposed, existing components `A` and `B` remain mounted and undisposed, and previous reconciliation state remains valid

---

### Requirement: Layout and Two-Way Binding Regression Suite
The test suite SHALL verify layout containers (`Column`, `Row`, `Grid`, `ReactiveGrid`, `VirtualList`, `Wrap`, `Card`), `GapContainer` dynamic spacing across child visibility toggles, and `TwoWayBinding` feedback loop prevention.

#### Scenario: Dynamic visibility in GapContainer
- **WHEN** the first visible child in a `GapContainer` is hidden
- **THEN** the gap is dynamically reassigned to the next visible child without layout corruption

#### Scenario: Two-way binding prevents feedback loop
- **WHEN** a signal updates a bound widget setter
- **THEN** the widget's change event does not trigger a re-entrant write back to the signal

---

### Requirement: Multi-Subsystem Integration and Leak Regression Suite
The test suite SHALL verify realistic multi-subsystem trees combining components, signals, computed properties, effects, reconcilers, and modifiers through mount, update, reorder, and unmount cycles, asserting that no listeners or contexts leak over 100+ cycles.

#### Scenario: End-to-end component tree lifecycle
- **WHEN** a full tree with reactive list, header, and computed badges undergoes state mutations, reorders, and final disposal
- **THEN** all elements update correctly, unmounted components dispose cleanly, and zero ambient listeners remain

#### Scenario: 100-cycle stress test
- **WHEN** 100 consecutive mount-and-dispose cycles are performed on a dynamic keyed collection
- **THEN** all observers, components, and effect subscriptions are 100% disposed with zero residual references
