## MODIFIED Requirements

### Requirement: PendingCellConfig provides apply helpers for immediate application
`PendingCellConfig` SHALL provide `applyGrowToParentCell(Element)`, `applyMarginToParentCell(Element)`, `applyAlignToParentCell(Element)`, and `applySizeToParentCell(Element)` for immediate application when the element is already attached to a parent Table. `CellConfig` sizing methods (`minWidth`, `minHeight`, `maxWidth`, `maxHeight`) SHALL immediately invoke `applySizeToParentCell` so constraints take effect on the live Arc `Cell` when invoked on an already-attached component.

#### Scenario: Sizing modifier applied to attached element
- **WHEN** `minWidth(float)` or `maxWidth(float)` is called on a component that has already been attached to a parent Table
- **THEN** the parent Table's Cell is immediately updated with the minimum or maximum width, and the parent table's layout hierarchy is invalidated

#### Scenario: Reactive sizing modifier applied to attached element
- **WHEN** `minWidth(Readable<Float>)` is called on an already-attached component
- **THEN** the initial minimum width is applied immediately to the parent Cell, and an Effect is registered to keep the parent Cell in sync whenever the readable value changes
