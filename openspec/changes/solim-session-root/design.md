## Context

`solim-ownership-unification` unified all components under `solim.core.Component` with automatic ownership and closure-free ambient stacks. During that work, tasks 8.1 and 8.2 (installing a session root and asserting owner resolution) were deliberately deferred because a naive retaining root would hold every top-level component alive for the entire JVM process lifetime, leading to leaks for ephemeral widgets like popups, tooltips, or dynamic transient dialogs. Furthermore, `audit-recheck.md` identified two specific factory sites that still drop resources:
1. `ForEach.reconcile` drops the `Disposable` list returned by `PendingCellConfig.applyToCell(cell)`.
2. `ReactiveGrid.build` builds `empty(Runnable)` into a throwaway table without managing its inner reactive bindings on unmount.

## Goals / Non-Goals

**Goals:**
- Provide a permanent `SessionRoot` in `solim-api` / `solim-core` so unparented root components have a valid queryable root owner.
- Implement an explicit lifecycle and unregistration model for transient top-level components (popups, tooltips, and closed dialogs) so the session root never causes memory retention leaks.
- Assert that every registered component and reactive resource resolves to an active owner.
- Fix resource retention in `ForEach.applyToCell` and `ReactiveGrid.empty(Runnable)`.
- Wrap isolated factory executions (`QueryView`, `SolimStack`, `Popup`) in explicit `OwnershipScope` contexts.

**Non-Goals:**
- Altering the core reactive evaluation loop or scheduling semantics.
- Forcing every non-component object (like global application singletons) to be owned by `SessionRoot`.

## Decisions

### 1. `SessionRoot` Lifecycle and Weak/Unregisterable Membership
- **Decision**: `SessionRoot` acts as a top-level `DisposableOwner`. When a component is constructed with an empty ambient `OwnershipContext` stack, it registers with `SessionRoot`.
- **Memory Safety Rule**: When any top-level component is disposed (or when an overlay like `Popup` or `SolimDialog` hides/closes), it unregisters itself from `SessionRoot` (`SessionRoot.remove(this)`). `SessionRoot` maintains its child list cleanly without indefinitely keeping dead elements referenced.

### 2. Adoption of `ForEach.applyToCell` Disposables
- **Decision**: Store the `List<Disposable>` returned by `applyToCell(cell)` during `ForEach.reconcile` and attach them to the reconciled item `Component` (or an item-scoped `DisposableOwner`), so when the item key is removed, cell constraints are disposed with the item.

### 3. Owned Container for `ReactiveGrid.empty(Runnable)`
- **Decision**: Wrap the empty runnable execution in a dedicated `Component` or `OwnershipScope` that is stored and disposed whenever data becomes non-empty.

### 4. Ownership Resolution Verification
- **Decision**: Provide an assertion helper (e.g. `SolimAssert.assertOwned(node)`) that checks `node.owner() != null` or that it is reachable from `SessionRoot`.

## Risks / Trade-offs

- **[Risk]** Memory leak if a top-level component forgets to dispose.  
  → **Mitigation**: Unit tests with leak detectors verify that creating and disposing top-level dialogs, popups, and components leaves `SessionRoot.children()` empty.
