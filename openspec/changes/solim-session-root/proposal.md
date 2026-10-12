## Why

Following `solim-ownership-unification`, all components inherit a unified ownership tree and auto-owning reactive modifiers. However, two architectural loose ends remain: top-level components constructed outside any active container or scope have no root owner, and isolated factory wrappers (`ForEach.applyToCell`, `ReactiveGrid.empty(Runnable)`) drop bindings or throwaway elements. Installing a permanent, memory-safe session root and closing the remaining factory drops ensures 100% of Solim nodes and bindings resolve to an owner without memory leaks.

## What Changes

- **Memory-Safe Session Root**: Install a process-lifetime session root (`SessionRoot`) in `OwnershipContext` that acts as the top-level owner for unparented components. Ephemeral nodes (tooltips, popups, detached menus) automatically unregister or weak-link upon closure/disposal so retention does not cause silent leaks.
- **Node Ownership Resolution Assertion**: Enforce and verify that every registered component and reactive resource resolves to an active owner.
- **Close Factory Drop Gaps**:
  - `ForEach.reconcile`: Store and adopt the `Disposable` list returned by `PendingCellConfig.applyToCell(cell)` so reactive cell constraints are properly disposed when items unmount.
  - `ReactiveGrid.empty(Runnable)`: Run the empty runnable inside an owned container/scope so temporary tables and inner bindings are cleanly disposed when data arrives.
  - Wrap bare factory isolate boundaries (`QueryView`, `SolimStack`, `Popup`) in explicit `OwnershipScope` containers.

## Capabilities

### New Capabilities
- `solim-session-root`: Top-level session root ownership, memory-safe ephemeral unregistration, and full ownership-resolution assertion.

### Modified Capabilities
- `solim-component-ownership`: Every component and reactive modifier resolves to an owner, and collection factory constraints/empty states are owned.

## Impact

- `:solim-api`: `OwnershipContext` supports session root resolution and owner querying.
- `:solim-core`: `ForEach` and `ReactiveGrid` adopt cell and empty-state bindings; `SolimDialog`, `Popup`, and `Hud` hook safely into root lifecycles.
- `:solim-runtime`: `StructuralReconciler` and `OwnershipScope` enforce owner resolution.
