## Context

The Solim framework is a custom reactive UI engine layered on Mindustry's Arc Scene2D runtime. It features:
- Fine-grained reactive primitives (`Signal`, `Computed`, `Effect`).
- Batching frame scheduler (`SignalDispatcher`).
- Thread-local-free ambient context stacks (`ReactiveContext`, `ComponentContext`, `ParentStack`).
- Ambient lifecycle ownership and reverse-order LIFO disposal.
- Keyed transactional diffing and element reconciliation (`StructuralReconciler`).
- Declarative layout containers, modifiers, dynamic spacing (`GapContainer`), and two-way bindings.

While previous updates introduced encapsulation boundaries and bug fixes, test coverage remains fragmented across modules. Some tests rely on shallow non-null checks, while complex failure modes—such as reconciler factory crashes, nested context corruption during unhandled exceptions, circular effect cascades, and ambient state leaks across repeated mount cycles—lack systematic behavioral verification.

This design establishes a comprehensive, deterministic test architecture and reusable test infrastructure to guarantee regression prevention across all Solim subsystems.

## Goals / Non-Goals

**Goals:**
- Provide a unified `SolimTestHarness` with strict teardown assertions that verify zero ambient context leakage (`ParentStack`, `ComponentContext`, `ReactiveContext`, `SignalDispatcher`).
- Provide deterministic scheduler control (`TestScheduler`) for manual frame flushes, cascade stepping, and queue inspection without real-time delays.
- Implement exhaustive behavioral test suites across all 24 phases defined in the specification.
- Test failure-injection scenarios: reconciler transactional rollback, partial component build errors, and disposal exception containment.
- Implement stress, leak, and randomized property tests ensuring zero residual listeners across 100+ lifecycle iterations.
- Maintain strict Java 8 runtime compatibility.

**Non-Goals:**
- Testing Mindustry mod-level game views (gameplay HUDs, mod screens), which run in the game client; only Solim framework engine and components are tested here.
- Introducing external heavy mocking frameworks or altering the core Gradle multi-project dependency DAG.
- Rewriting working production components without behavioral necessity.

## Decisions

### 1. Unified Test Fixture & Ambient Context Teardown (`SolimTestHarness`)
- **Decision**: Create `SolimTestHarness` as an abstract base class or test extension for all Solim unit tests.
- **Teardown Invariants**:
  1. `ParentStack.current() == null` and stack depth is 0.
  2. `ComponentContext.current() == null` and stack depth is 0.
  3. `ReactiveContext.current() == null` and observer stack depth is 0.
  4. `SignalDispatcher.pendingCount() == 0` and dispatcher is not mid-flush.
- **Rationale**: Any unpopped context in one test can cause subsequent unrelated tests to fail or record erroneous dependencies. Automatic validation catches context corruptions at the exact source.

### 2. Deterministic Scheduler Architecture (`TestScheduler`)
- **Decision**: Provide a test scheduler hook in `SignalDispatcher` that permits substituting or controlling the flush loop directly in test mode.
- **Capabilities**:
  - `flush()`: Synchronously executes all pending dirty effects.
  - `flushStep()`: Executes one generation of cascading effects.
  - `pendingCount()`: Returns count of currently enqueued effects.
  - `clear()`: Empties pending queue during test cleanup.
- **Alternative Considered**: Relying on `Trigger.update` and thread sleeps. Rejected because real-time timing introduces test flakiness and fails in headless test runners.

### 3. Module Placement of Test Doubles and Infrastructure
- **Decision**:
  - Infrastructure for public contracts (`TestDisposable`) lives in `solim-api` test source set.
  - Runtime harness and scheduler control (`SolimTestHarness`, `TestScheduler`, `TestObserver`) live in `solim-runtime` test source set.
  - Component, layout, and binding fixtures live in `solim-core` test source set.
- **Rationale**: Respects the physical dependency hierarchy (`:solim-api` ← `:solim-runtime` ← `:solim-core` ← `:solim`) without cyclic test dependencies.

### 4. Transactional Reconciler Verification Strategy
- **Decision**: Formulate explicit failure-injection tests for `StructuralReconciler`.
- **Test Scenarios**:
  1. *Duplicate Keys*: Provide duplicate keys `["a", "b", "a"]`; assert `IllegalArgumentException` is thrown before any component is built or existing child mutated.
  2. *Factory Crash*: When reconciling `[A, B]` to `[A, B, C]`, cause factory for `C` to throw a `RuntimeException`. Assert `A` and `B` remain mounted and undisposed, partially created items are disposed, and reconciler state remains at `[A, B]`.

### 5. Arc Headless Mocking Isolation
- **Decision**: Reuse the existing `HeadlessApplication` and Arc mock harness configured in `solim-core/src/test`.
- **Rationale**: Mindustry's Arc Scene2D requires `Core.app`, `Core.graphics`, and `Core.settings` initialized. Reusing the headless harness ensures zero graphics device requirements while supporting `Element`, `Table`, `Cell`, and event dispatching.

### 6. Verification Matrix Across 24 Subsystems
- **Decision**: Maintain a comprehensive verification matrix mapping all 24 phases to concrete test suites:
  1. Audit existing test cases.
  2. Build test infrastructure (`SolimTestHarness`, `TestScheduler`, test doubles).
  3. Component lifecycle & ownership.
  4. Disposable contract.
  5. Signal semantics.
  6. Computed lazy memoization & dynamic branch pruning.
  7. Effect auto-tracking, cleanup & disposal.
  8. Scheduler queueing, deduplication & cascade protection.
  9. ReactiveContext isolation & exception safety.
  10. ParentStack nesting, attachers & cell isolation.
  11. StructuralReconciler keyed diffing.
  12. Duplicate key detection.
  13. Transactional rollback under factory failure.
  14. Modifier pipeline & pending cell configuration.
  15. GapContainer dynamic spacing & visibility toggles.
  16. Layout containers (`Column`, `Row`, `Grid`, `ReactiveGrid`, `VirtualList`, `Card`, `Wrap`).
  17. Two-Way Binding loop prevention & rapid updates.
  18. Concrete components (`Button`, `Text`, `Badge`, `SolimTextField`, `Checkbox`, `Dialog`, `HUD`).
  19. Async operations & unmount safety.
  20. Multi-subsystem integration trees.
  21. Leak & stress testing (100+ cycles).
  22. Randomized property-based state transitions.
  23. Java 8 bytecode and standard library compliance.
  24. Historical regression suite mapping.

## Risks / Trade-offs

- **[Risk]** Package-private internal runtime APIs may be needed for context inspection in tests.
  - **Mitigation**: Place test classes in matching packages (`solim.runtime`, `solim.core`, `solim.reactive`) so package-private state can be verified without opening public API surface.
- **[Risk]** Large stress/leak tests could slow down build execution.
  - **Mitigation**: Keep stress tests tightly focused (e.g. 100-500 iterations of lightweight micro-components), running entirely in memory in sub-second time.
- **[Risk]** Java 8 API regressions during test development.
  - **Mitigation**: CI and Gradle compiler compliance checks enforce Java 8 source/target compatibility and flag forbidden Java 9+ APIs (`List.of`, `stream.toList()`).
