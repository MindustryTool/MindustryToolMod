## Why

In CSS and modern UI frameworks, `margin` represents outer spacing outside an element's border/background (pushing siblings and container boundaries away), while `padding` represents inner spacing inside the border/background (inset between container edges and its child elements).

In Arc/Scene2D, these concepts are confusingly inverted:
- Arc's `Table.margin()` actually sets the *inner* padding inside the table around its children.
- Arc's `Cell.pad()` sets the *outer* spacing around the element inside the parent layout cell (i.e. true CSS margin).

Solim currently exposes `TableConfig.margin(...)` as an alias for `padding(...)`, while parent-cell outer spacing is named `cellPadding(...)`. This causes developers to reach for `.margin()` when attempting to add outer breathing room around bordered or rounded cards and components, which fails because `Table.margin()` only expands the inner area inside the background.

Aligning Solim's API with CSS box-model conventions provides an intuitive, standard developer experience without breaking existing layouts.

## What Changes

- **Add `margin(...)` to `CellConfig`**: Expose `.margin(float)`, `.margin(top, left, bottom, right)`, `.marginX(float)`, `.marginY(float)` and their reactive `Readable<Float>` overloads on `CellConfig` (implemented by `Button`, `Row`, `Column`, `Card`, `Grid`, etc.), mapping directly to parent-cell padding (`PendingCellConfig.padTop`, `padLeft`, `padBottom`, `padRight`).
- **Deprecate `cellPadding(...)`**: Keep `cellPadding(...)` on `CellConfig` as a deprecated backward-compatible alias delegating directly to `margin(...)`.
- **Clarify `TableConfig` padding vs margin**: Ensure `TableConfig` focuses on `padding(...)` for inner table insets. Deprecate `TableConfig.margin(...)` in favor of `padding(...)` to eliminate the ambiguity where `margin` was treated as inner table padding.
- **Ensure root popup content attaches with cell config**: Update `Popup.render()` to route through `ParentStack` or explicitly apply `PendingCellConfig` so `margin(...)` on root popup content takes effect.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `solim-layout`: Update requirement and modifier specifications to align `padding(...)` with inner container insets and `margin(...)` with outer parent-cell spacing.

## Impact

- `solim-core`: `solim.layout.CellConfig`, `solim.modifier.TableConfig`, `solim.modifier.PendingCellConfig`, `solim.overlay.Popup`.
- Existing usage of `cellPadding(...)` remains valid via deprecated forwarders.
- Existing usage of `padding(...)` on containers (`row()`, `column()`, `card()`) remains completely intact and unchanged.
