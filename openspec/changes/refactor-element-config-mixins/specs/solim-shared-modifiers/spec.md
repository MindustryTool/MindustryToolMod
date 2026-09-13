## MODIFIED Requirements

### Requirement: Centralized ElementConfig mixin for element sizing and positioning
The Solim framework SHALL provide an `ElementConfig<SELF>` mixin interface in package `solim.modifier` to handle sizing (`width`, `height`, `size`), positioning (`x`, `y`, `position`), visibility, opacity, naming, and visual styling (rounded, border, background) on Arc `Element` instances. Implementing components SHALL provide `Element element()`.

#### Scenario: Delegating element size mutations
- **WHEN** a component calls `width(100f)` inherited from `ElementConfig`
- **THEN** the element's width is set to 100f and the parent cell is updated if attached

### Requirement: TableConfig mixin for table alignment and spacing
The Solim framework SHALL provide a `TableConfig<SELF>` mixin interface in package `solim.modifier` to handle alignment (`top`, `bottom`, `left`, `right`, `center`, `align`), margin, padding, and gap on Arc `Table` instances. Implementing components SHALL provide `Table table()`.

#### Scenario: Table alignment via mixin
- **WHEN** a component calls `top()` inherited from `TableConfig`
- **THEN** the table's alignment is set to top

### Requirement: Element overloads of margin/padding are removed
The `ElementConfig` and `TableConfig` interfaces SHALL NOT provide `margin(Element, ...)` or `padding(Element, ...)` overloads that dispatch via `instanceof Table`. Table-specific operations SHALL only accept `Table` parameters.

#### Scenario: No Element-targeted margin dispatch
- **WHEN** `margin(element, 8f)` is called with an Element parameter
- **THEN** the method does not exist; callers use `TableConfig.margin(table, 8f)` or `CellConfig.cellPadding(8f)`
