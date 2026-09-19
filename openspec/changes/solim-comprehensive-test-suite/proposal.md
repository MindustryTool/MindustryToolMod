## Why

Solim is a fine-grained reactive UI framework for Mindustry with ambient lifecycle ownership, transactional keyed reconciliation, custom execution scheduling, and Arc scene graph integration. Without a comprehensive behavioral test suite that tests observable invariants, failure modes, and lifecycle boundaries, future refactors and performance optimizations risk introducing silent reactive stalls, context corruption, and memory leaks. A comprehensive test suite backed by reusable test harness infrastructure is required to provide confidence that the framework behaves correctly across all subsystems.

## What Changes

- **Test Infrastructure (`solim-test-harness`)**:
  - `SolimTestHarness`: Base test harness providing automatic ambient context validation, verifying balanced `ParentStack`, `ComponentContext`, `ReactiveContext`, and clean `SignalDispatcher` queues at teardown.
  - `TestScheduler`: Deterministic scheduler implementation enabling fine-grained control over frame flushes, cascade passes, and queue inspection without machine timing dependencies.
  - `TestDisposable`, `TestComponent`, `TestObserver`, and lifecycle tracking helpers to assert exact event sequences, allocation counts, and disposal states.
  - Exception-injection and leak-detection helpers for failure recovery and stress verification.
- **Auditing & Upgrading Existing Tests**:
  - Audit existing Solim test classes, eliminating vacuous "does not crash" or non-null assertions and replacing them with observable behavioral and state assertions.
- **Subsystem Test Suites across 24 Phases**:
  - **Lifecycle & Ownership**: Lazy build, single materialization, LIFO disposal, nested ownership, error isolation during disposal, partial build unwinding.
  - **Disposable Contract**: Abstract `isDisposed()` compliance across all implementations, idempotency, post-disposal safety.
  - **Signal**: Initial value, `get()`, `peek()`, `set()`, `update()`, equality suppression, subscription disposal, mutation during effect runs.
  - **Computed**: Lazy evaluation, memoization, dynamic dependency branch switching, cycle detection, invalidation cascading, error isolation and context balance.
  - **Effect**: Immediate initial run, auto-tracking, scheduler dispatch, cleanup-before-rerun, disposal while scheduled, exception containment.
  - **Scheduler**: FIFO queueing, deduplication, cascade pass limits, disposal while queued, nested flush protection.
  - **Ambient Contexts**: `ReactiveContext`, `ComponentContext`, and `ParentStack` balance, untracked isolation, sibling isolation, and exception safety.
  - **StructuralReconciler & Transactional Rollback**: Keyed reconciliation (create, reuse, reorder, remove, dispose), duplicate key detection, transactional rollback on factory failure preserving existing state.
  - **Modifiers & Spacing**: Element/table/cell modifiers, sibling isolation, `GapContainer` layout calculations across dynamic visibility changes.
  - **Layout & Collections**: `Column`, `Row`, `Grid`, `ReactiveGrid`, `VirtualList`, `Card`, `Wrap` layout bounds and reactivity.
  - **Two-Way Binding**: Feedback loop prevention, bidirectional synchronization, rapid updates, disposal.
  - **Components**: Behavioral coverage of `BaseComponent`, `Button`, `Text`, `SolimTextField`, `Checkbox`, `Slider`, `Badge`, `Dialog`, `HUD`.
  - **Async & Lifecycle**: Unmount cancellation, out-of-order resolution, post-disposal callback safety.
  - **Integration & Property Tests**: Realistic component trees, mount/update/reconcile/dispose pipelines, randomized operations, and leak tests (100+ cycles).
  - **Java 8 Runtime Compatibility & Regression Mapping**: Bytecode verification, prohibition of Java 9+ standard library APIs, regression tests for historical bug patterns.

## Capabilities

### New Capabilities
- `solim-test-harness`: Reusable test infrastructure, ambient context invariant guards, deterministic test scheduler, and leak/lifecycle tracking helpers.
- `solim-framework-regression-suite`: Comprehensive multi-phase behavioral regression test suite verifying Solim reactive runtime, lifecycle, reconciler, contexts, layout, and component integration.

### Modified Capabilities
*(None — existing framework requirements are preserved; the test suite verifies existing invariants and prevents regressions.)*

## Impact

- **Affected Code**: `solim-api/src/test`, `solim-runtime/src/test`, `solim-core/src/test`, `solim/src/test`.
- **Production Code**: No breaking changes or public API removals in production code; tests enforce existing contracts.
- **Dependencies**: Uses existing JUnit 5 and Arc test harness dependencies; no external heavy dependencies introduced.
- **CI / Build**: Ensures tests execute quickly and deterministically via `./gradlew test` with Java 8 bytecode verification.
