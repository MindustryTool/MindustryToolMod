## Why

Solim components currently suffer from redundant ownership calls, an obsolete `reparentTo` escape hatch, eager element realization triggered by modifier interfaces, and manual `.element()` invocations across declarative DSL methods. These issues create conflicting attachment mechanisms, premature UI tree instantiation before modifier chaining finishes, and unnecessary boilerplate across mixins.

## What Changes

- **Remove `reparentTo`**: Delete `reparentTo` from `Component` and `ElementResolver`. Components belong strictly to the owner/scope that instantiates them and are recreated if a different parent requires them.
- **Enforce Package-Private Ownership**: Restrict `adopt()` and `own()` to internal Solim lifecycle mechanisms so external code cannot manually corrupt component ownership trees.
- **Eliminate Redundant `.own()` in Modifiers**: Since `Effect.of(...)` automatically registers with `OwnershipContext.current()`, eliminate manual `((Component) this).own(e)` calls in `CellConfig`, `ElementConfig`, and `TableConfig`.
- **Pure Pending `CellConfig`**: Remove eager `((Component) this).element()` and `this instanceof Component` checks across `CellConfig`. `CellConfig` only records configuration into `PendingCellConfig`, deferring element and cell application to parent attachment time.
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
