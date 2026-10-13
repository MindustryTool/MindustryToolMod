## Why

Solim components currently suffer from redundant ownership calls, an obsolete `reparentTo` escape hatch, eager element realization triggered by modifier interfaces, and manual `.element()` invocations across declarative DSL methods. These issues create conflicting attachment mechanisms, premature UI tree instantiation before modifier chaining finishes, and unnecessary boilerplate across mixins.

## What Changes

- **Remove `reparentTo`**: Delete `reparentTo` from `Component` and `ElementResolver`. Components belong strictly to the owner/scope that instantiates them and are recreated if a different parent requires them.
- **Strict Single Ownership for Disposables**: Enforce that a `Disposable` can only be owned by at most one `DisposableOwner` at a time. Adopting an already-owned `Disposable` is an illegal operation or automatically unbinds from previous owner, preventing premature disposal and use-after-free bugs.
- **Enforce Package-Private Ownership**: Restrict `adopt()` and `own()` to internal Solim lifecycle mechanisms so external code cannot manually corrupt component ownership trees.
- **Modifier Configuration Records (`CellConfig`, `ElementConfig`, `TableConfig`)**: 
  - Stop eagerly calling `element()` / `table()` and creating `Effect`s inside modifier chains while the parent is still active on `OwnershipContext`.
  - Drop the redundant `Pending` prefix from class names (`CellConfig` data record rather than `PendingCellConfig`). In declarative UI, configurations are inherently deferred/pending until realization.
  - Modifiers simply record configuration state. Effect registration and Arc widget/cell application happen at realization time when `OwnershipContext.push(this)` has the component itself active as the owner.
  - Eliminate all `((Component) this).own(e)` hacks and premature widget realization.
- **Remove Manual `.element()` in Declarative DSL**: Remove redundant `AttachmentStack.attachToParent(x.element())` calls from factory methods in `solim.UI`. `AttachmentStack.attachPendingComponents()` is the sole mechanism that invokes `comp.element()` when the parent scope closes.

## Capabilities

### Modified Capabilities
- `solim-lifecycle`: Disallow manual component reparenting, restrict ownership adoption methods, and guarantee lazy single realization of `element()` on parent attachment.
- `solim-component-ownership`: Enforce ambient-only registration of effects and component resources, eliminating external manual `.own()` calls.

## Impact

- `solim-api`: `Component.java`, `DisposableOwner.java`, `SessionRoot.java`.
- `solim-core`: `CellConfig.java`, `ElementConfig.java`, `TableConfig.java`.
- `solim-runtime`: `ElementResolver.java`, `AttachmentStack.java`.
- `solim`: `UI.java`.
- Tests verifying ownership, cell configurations, and attachment behavior.
