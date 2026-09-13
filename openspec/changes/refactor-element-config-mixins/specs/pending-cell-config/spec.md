# pending-cell-config Specification

## Purpose
Defines `PendingCellConfig` (renamed from `SizeConstraints`) as the deferred configuration buffer for parent Cell settings.

## Requirements

### Requirement: PendingCellConfig stores pending parent-cell values
`PendingCellConfig` SHALL store pending values for: `prefWidth`, `prefHeight`, `minWidth`, `minHeight`, `maxWidth`, `maxHeight` (all `Readable<Float>`), `growX`, `growY` (boolean), `padTop`, `padLeft`, `padBottom`, `padRight` (all `Readable<Float>`), and `align` (Integer).

#### Scenario: Storing pending width
- **WHEN** `prefWidth` is set to `Readable.of(100f)`
- **THEN** the value is stored until applied to a parent Cell

### Requirement: PendingCellConfig.applyToCell applies all stored values to a Cell
`PendingCellConfig` SHALL provide `applyToCell(Cell<?>)` that applies all stored pending values to the given Cell, installing reactive Effects for Readable values. This method SHALL return a list of `Disposable` effects for the caller to own.

#### Scenario: Applying pending values to a cell
- **WHEN** `applyToCell(cell)` is called with pending `prefWidth=100f` and `growX=true`
- **THEN** `cell.width(100f)` and `cell.growX()` are called

### Requirement: PendingCellConfig provides apply helpers for immediate application
`PendingCellConfig` SHALL provide `applyGrowToParentCell(Element)`, `applyMarginToParentCell(Element)`, and `applyAlignToParentCell(Element)` for immediate application when the element is already attached to a parent Table.

#### Scenario: Applying grow to attached element
- **WHEN** `applyGrowToParentCell(element)` is called and element's parent is a Table
- **THEN** `cell.growX()` or `cell.growY()` is set on the parent cell

### Requirement: PendingCellConfig provides find helper
`PendingCellConfig` SHALL provide a static `find(Object)` method that resolves a `PendingCellConfig` from an object chain: returns the object if it's a `PendingCellConfig`, calls `sizeConstraints()` if it's a `CellConfig`, or traverses `Element.userObject` if it's an `Element`.

#### Scenario: Finding PendingCellConfig from Element
- **WHEN** `PendingCellConfig.find(element)` is called where `element.userObject` implements `CellConfig`
- **THEN** the `CellConfig.sizeConstraints()` result is returned

### Requirement: PendingCellConfig replaces SizeConstraints
All references to `SizeConstraints` in the codebase SHALL be renamed to `PendingCellConfig`. The class SHALL move from `solim.layout` to `solim.modifier` (or stay in `solim.layout` if preferred).

#### Scenario: No SizeConstraints references remain
- **WHEN** the codebase is searched for `SizeConstraints`
- **THEN** zero results are found (all renamed to `PendingCellConfig`)
