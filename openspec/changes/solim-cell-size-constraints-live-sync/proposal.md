## Why

In Solim UI, components like `Button` attach to their parent layout immediately upon instantiation via `ParentStack.attachToParent()`. When parent-cell constraint modifiers such as `minWidth()`, `minHeight()`, `maxWidth()`, and `maxHeight()` are chained after component instantiation, `CellConfig` only stores the constraint inside `PendingCellConfig` without calling `applySizeToParentCell()`. As a result, components that are already attached to a parent Table never transfer their min/max size constraints to the live Arc `Cell`, causing constraints like `.minWidth(unit(14f))` to be silently ignored during layout.

## What Changes

- Update `CellConfig.minWidth(float)`, `CellConfig.minWidth(Readable<Float>)`, `CellConfig.minHeight(float)`, `CellConfig.minHeight(Readable<Float>)`, `CellConfig.maxWidth(float)`, `CellConfig.maxWidth(Readable<Float>)`, `CellConfig.maxHeight(float)`, and `CellConfig.maxHeight(Readable<Float>)` to immediately invoke `applySizeToParentCell()` on their element when already attached to a parent Table.
- Ensure reactive bindings registered when chaining size modifiers on already-attached components bind to the live cell and handle dynamic signal updates.
- Add unit tests in `solim-core` verifying that calling `minWidth()`, `minHeight()`, `maxWidth()`, and `maxHeight()` on an already-attached component updates the parent cell dimensions immediately.

## Capabilities

### Modified Capabilities
- `solim-layout`: Update requirements for `PendingCellConfig provides apply helpers for immediate application` and `CellConfig` size constraint methods to guarantee immediate synchronization to the parent Cell when already attached.

## Impact

- `solim.modifier.CellConfig`: Dispatch `cellConfig().applySizeToParentCell(...)` in `minWidth`, `minHeight`, `maxWidth`, and `maxHeight`.
- `solim.modifier.PendingCellConfig`: Ensure effects are registered for reactive size constraints applied to already-attached cells.
- Any Solim component implementing `CellConfig` (e.g. `Button`, `Text`, `Card`, `Wrap`, etc.) chained with min/max dimensions inside layout containers.
