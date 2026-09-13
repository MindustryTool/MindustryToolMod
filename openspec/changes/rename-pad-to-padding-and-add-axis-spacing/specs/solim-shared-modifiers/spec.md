## MODIFIED Requirements

### Requirement: ElementModifiers padding and margin utilities for elements
The `ElementModifiers` static utility class SHALL provide canonical `padding` and `margin` methods for Arc `Element` and `Table` instances (`padding`, `paddingTop`, `paddingBottom`, `paddingLeft`, `paddingRight`, `paddingX`, `paddingY`, `margin`, `marginTop`, `marginBottom`, `marginLeft`, `marginRight`, `marginX`, `marginY`). Abbreviated `pad*` methods SHALL NOT be provided. When the element is a `Table`, padding/margin methods SHALL apply to table margins. When the element is contained within a parent `Table`, they SHALL apply to the element's enclosing `Cell` padding.

#### Scenario: Applying padding to an Element in a Table
- **WHEN** an element is placed inside an Arc `Table` and `ElementModifiers.padding(element, 10f)` is called
- **THEN** the parent table cell for that element has its padding updated to 10f

#### Scenario: Null-safe element padding and margin
- **WHEN** `ElementModifiers.padding(null, 10f)` or `ElementModifiers.margin(null, 10f)` is called
- **THEN** no exception is thrown

#### Scenario: Applying two-axis padding and margin via ElementModifiers
- **WHEN** `ElementModifiers.paddingX(table, 12f)` and `ElementModifiers.paddingY(table, 6f)` are called
- **THEN** the table's left and right margins are set to 12f, and top and bottom margins are set to 6f

## ADDED Requirements

### Requirement: Two-axis spacing modifiers on Solim components
Solim layout and display components (`Column`, `Row`, `Text`, `SolimImage`, `NetworkImage`, `Container`, `Card`, `Button`) SHALL expose fluent `paddingX(float)`, `paddingY(float)`, `marginX(float)`, and `marginY(float)` methods (with `Readable<Float>` reactive overloads where supported) for configuring horizontal (left and right) and vertical (top and bottom) spacing symmetrically.

#### Scenario: Setting horizontal and vertical padding on Column
- **WHEN** `column().paddingX(16f).paddingY(8f)` is declared
- **THEN** the column's underlying table has left and right padding set to 16f, and top and bottom padding set to 8f

#### Scenario: Setting horizontal and vertical margin on Text
- **WHEN** `text("hello").marginX(10f).marginY(4f)` is declared
- **THEN** the text's parent cell padding reflects 10f on left and right, and 4f on top and bottom

#### Scenario: Setting horizontal and vertical margin on Button
- **WHEN** `button("OK").marginX(12f).marginY(6f)` is declared
- **THEN** the button's margins are set to 12f horizontally and 6f vertically
