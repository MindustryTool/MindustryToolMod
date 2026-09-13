## MODIFIED Requirements

### Requirement: Modifier categories are non-overlapping and clearly defined
Every fluent modifier SHALL target exactly one of: (A) the component's own Arc Element via `ElementConfig`, (B) the component's Table container via `TableConfig`, or (C) the component's cell in its parent layout via `CellConfig`. Each mixin SHALL provide only methods for its domain. `CellConfig` SHALL provide only parent-cell methods (`grow*`, `min/max*`, `cellPadding*`). Element-targeted margin/padding overloads that dispatch via `instanceof Table` SHALL NOT exist.

#### Scenario: Self modifiers affect the Element only
- **WHEN** `.visible(false)`, `.opacity(0.5f)`, or `.rounded(8)` is called via `ElementConfig`
- **THEN** only the component's own Arc Element is modified; no parent cell is touched

#### Scenario: Table modifiers affect the container only
- **WHEN** `.top()`, `.margin(8f)`, or `.gap(4f)` is called via `TableConfig`
- **THEN** only the component's Table content defaults are modified

#### Scenario: Parent-layout modifiers affect the cell only
- **WHEN** `.growX()` or `.cellPadding(8f)` is called via `CellConfig`
- **THEN** only the component's cell in its parent layout is modified

#### Scenario: CellConfig has only parent-cell methods
- **WHEN** searching CellConfig for methods
- **THEN** only `grow*`, `min/max*`, `cellPadding*` exist; no width/height/opacity/rounded/border/background

### Requirement: Duplicate modifiers between ElementConfig and CellConfig are removed
Any modifier that targets the Element (width/height/opacity/rounded/border/background) SHALL exist only in `ElementConfig`. `CellConfig` SHALL NOT have those methods.

#### Scenario: No duplicate width implementation
- **WHEN** searching the Solim source for `width` modifier implementations
- **THEN** exactly one implementation exists in `ElementConfig`; CellConfig does not have `width`

### Requirement: Element overloads of margin/padding that dispatch are removed
Element-targeted overloads of `margin`/`padding` that check `instanceof Table` to dispatch between Table margin and Cell padding SHALL NOT exist. Callers SHALL use `TableConfig` for Table margin or `CellConfig.cellPadding` for parent-cell padding.

#### Scenario: No instanceof Table dispatch in margin
- **WHEN** searching for `margin(Element` in ElementConfig/TableConfig
- **THEN** no methods with `instanceof Table` dispatch exist
