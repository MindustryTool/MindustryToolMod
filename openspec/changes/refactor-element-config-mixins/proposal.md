## Why

`ElementConfig` is an 858-line static utility class that conflates three distinct responsibilities: mutating an Element's own properties, configuring a Table's content defaults, and bridging Element-targeted calls to parent Cell padding. This makes the codebase hard to navigate and creates duplicated implementations between `ElementConfig` and `CellConfig`. The Element overloads of `margin`/`padding` (which check `instanceof Table` to dispatch) are particularly confusing — they mix "set my own margin" with "configure my parent cell" in one method.

Additionally, `SizeConstraints` is misnamed — it's not constraining size, it's a pending buffer of configuration values waiting to be applied to a parent Cell when the element is attached.

## What Changes

- **ElementConfig** becomes a mixin interface (`ElementConfig<SELF>`) with default methods for Element-targeted operations: `width`, `height`, `size`, `x`, `y`, `position`, `visible`, `opacity`, `alpha`, `name`, `rounded`, `border`, `background` (Drawable/Color), `backgroundColor` (Color). Each component provides `Element element()`.
- **TableConfig** becomes a mixin interface (`TableConfig<SELF>`) with default methods for Table-targeted operations: `align`, `top`, `bottom`, `left`, `right`, `center`, `margin*`, `padding*`, `gap`, `respace`. Each component provides `Table table()`.
- **CellConfig** stays as a mixin interface but **removes** methods now provided by ElementConfig/TableConfig (width/height/size, opacity, rounded/border/background, alignment). CellConfig keeps only parent-cell concepts: `grow*`, `min/max*`, `cellPadding*`.
- **SizeConstraints** is renamed to **`PendingCellConfig`** — it's pending configuration for the parent Cell, not size constraints.
- **Element overloads** of `margin`/`padding` that dispatch via `instanceof Table` are **removed**. Callers use `TableConfig` for Tables or `CellConfig.cellPadding` for parent-cell padding.
- **GenericGapContainer** moves from `ElementConfig` inner class to `TableConfig` (or stays as package-private in `solim.modifier`).

## Capabilities

### New Capabilities
- `element-config-mixin`: Specifies the `ElementConfig<SELF>` mixin interface for Element-targeted operations.
- `table-config-mixin`: Specifies the `TableConfig<SELF>` mixin interface for Table-targeted operations.
- `pending-cell-config`: Specifies the renamed `PendingCellConfig` (formerly `SizeConstraints`) as the deferred parent-cell configuration buffer.

### Modified Capabilities
- `modifier-clarity`: Requirements updated to reflect new class names (`ElementConfig`, `TableConfig`, `CellConfig`) and the removal of Element-overload dispatch.
- `solim-shared-modifiers`: Requirements updated to reference `ElementConfig` mixin instead of `ElementModifiers` static utility. Table operations reference `TableConfig`.
- `solim-layout`: Requirements updated to reference `CellConfig` (already correct) and `PendingCellConfig` (renamed from `SizeConstraints`).

## Impact

- **Files renamed/created**: `ElementConfig.java` (mixin, new), `TableConfig.java` (new), `PendingCellConfig.java` (renamed from `SizeConstraints.java`)
- **Files deleted**: `ElementModifiers.java` (already deleted), `LayoutModifiers.java` (already deleted), old Element-overload methods in `ElementConfig`
- **Files modified**: All Solim components that implement `CellConfig` (Row, Column, Card, Grid, Scroll, Wrap, etc.) — their `CellConfig` methods now only contain parent-cell methods; width/height/opacity/rounded/border/background come from `ElementConfig` mixin
- **Internal callers**: ~180+ call sites in solim-core that use `ElementConfig` static methods — re-pointed to `ElementConfig` mixin or `TableConfig` as appropriate
- **Mod impact**: `ChatAvatar` simplified to plain `BaseComponent` (removing `CellConfig` and manual cell alignment in favor of parent row `gap()`). `Hud.background` cleanly supports `Readable<Drawable>`, keeping `TeamResourceHudView` aligned.
