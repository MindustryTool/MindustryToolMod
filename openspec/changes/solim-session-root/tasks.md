## 1. SessionRoot Implementation

- [x] 1.1 Implement `SessionRoot` in `solim-api/src/solim/core/SessionRoot.java` as a `DisposableOwner` with child tracking, unregistration on disposal, and queryable root lineage
- [x] 1.2 Update `OwnershipContext` to register unparented components with `SessionRoot` when the ambient stack is empty
- [x] 1.3 Ensure `Component.dispose()` unlinks and unregisters the component from its parent or `SessionRoot`
- [x] 1.4 Test: Unparented top-level components resolve to `SessionRoot`, and disposing them removes them from `SessionRoot` without memory retention

## 2. Close Collection Factory Gaps

- [x] 2.1 Update `ForEach.reconcile` in `solim-core/src/solim/reactive/ForEach.java` to adopt the `Disposable` list returned by `PendingCellConfig.applyToCell(cell)` into the reconciled item component
- [x] 2.2 Update `ReactiveGrid.reconcile` in `solim-core/src/solim/layout/ReactiveGrid.java` to manage and dispose empty-state runnable/supplier bindings when transitioning to populated data
- [x] 2.3 Wrap bare factory isolate boundaries in `QueryView`, `SolimStack`, and `Popup` in explicit `OwnershipScope` containers
- [x] 2.4 Test: Verify `ForEach` cell constraints and `ReactiveGrid` empty state bindings are cleaned up when items unmount or data changes

## 3. Node Ownership Resolution Assertion & Verification

- [x] 3.1 Implement ownership resolution assertion in `solim-api/src/solim/core/SolimAssert.java` verifying any registered component or reactive resource resolves to an owner
- [x] 3.2 Add test asserting every registered node resolves to an owner (`solim-reactivity-hardening` task 7.9)
- [x] 3.3 Run `./gradlew checkBundles test` across all modules to ensure zero regressions
