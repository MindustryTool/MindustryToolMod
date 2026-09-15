## ADDED Requirements

### Requirement: CSS Box Model Margin on CellConfig
`CellConfig` SHALL provide `.margin(...)` methods (`margin(float)`, `margin(float, float, float, float)`, `marginX(float)`, `marginY(float)`, `marginTop(float)`, `marginBottom(float)`, `marginLeft(float)`, `marginRight(float)`) and their reactive `Readable<Float>` overloads, mapping directly to outer spacing in the parent layout cell (`PendingCellConfig.padTop`, `padLeft`, `padBottom`, `padRight`).

#### Scenario: Margin applied to child in Row
- **WHEN** a component calls `.margin(8f)` inside a `Row`
- **THEN** 8px padding is stored in its `PendingCellConfig` and applied as outer cell padding in the parent table cell

#### Scenario: Reactive margin updates dynamically
- **WHEN** a component binds `.margin(Readable<Float>)` to a signal
- **THEN** the parent cell's padding updates reactively whenever the signal emits a new value

### Requirement: Backward-compatible cellPadding alias
`CellConfig` SHALL retain `.cellPadding(...)` methods as `@Deprecated` forwarders to `.margin(...)`.

#### Scenario: Legacy cellPadding calls work identically
- **WHEN** legacy code calls `.cellPadding(4f)`
- **THEN** `.margin(4f)` is invoked, applying 4px outer cell padding to the parent cell

### Requirement: Popup applies PendingCellConfig to root content
`Popup.render()` SHALL inspect `PendingCellConfig` on the root content component and apply its constraints (including margin) to the cell created by `table.add(content.element())`.

#### Scenario: Popup root content with margin
- **WHEN** a popup's content provider returns a component with `.margin(12f)`
- **THEN** the cell inside the popup table receives 12px padding around the content element
