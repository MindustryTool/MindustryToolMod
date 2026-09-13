## MODIFIED Requirements

### Requirement: Wrap with CellConfig, ElementConfig, and declarative children
The `Wrap` component SHALL implement `CellConfig<Wrap>`, `ElementConfig<Wrap>`, and `TableConfig<Wrap>`. CellConfig provides `growX()`, `grow()`, `cellPadding()`. ElementConfig provides `rounded()`, `border()`, `width()`, `height()`. TableConfig provides `top()`, `gap()`, `padding()`. Wrap SHALL support `children(Runnable)` for declarative child attachment via `ParentStack`.

#### Scenario: Wrap with rounded and border
- **WHEN** `wrap().rounded(4).border(1f, Color.gray).children(() -> { ... })` is called
- **THEN** the wrap element SHALL have rounded corners and a 1px gray border via ElementConfig

### Requirement: PendingCellConfig replaces SizeConstraints
The deferred parent-cell configuration buffer SHALL be named `PendingCellConfig` instead of `SizeConstraints`. All references to `SizeConstraints` SHALL be renamed.

#### Scenario: PendingCellConfig used by ParentStack
- **WHEN** ParentStack adds a child element to a parent Table
- **THEN** `PendingCellConfig.find(child)` resolves the pending config and `applyToCell(cell)` applies stored values
