## MODIFIED Requirements

### Requirement: Two-axis spacing modifiers on Solim components
Solim layout and display components (`Column`, `Row`, `Text`, `SolimImage`, `NetworkImage`, `Card`, `Button`) SHALL expose fluent `paddingX(float)`, `paddingY(float)`, `marginX(float)`, and `marginY(float)` methods (with `Readable<Float>` reactive overloads where supported) for configuring horizontal (left and right) and vertical (top and bottom) spacing symmetrically.

#### Scenario: Setting horizontal and vertical padding on Column
- **WHEN** `column().paddingX(16f).paddingY(8f)` is declared
- **THEN** the column's underlying table has left and right padding set to 16f, and top and bottom padding set to 8f

#### Scenario: Setting horizontal and vertical margin on Text
- **WHEN** `text("hello").marginX(10f).marginY(4f)` is declared
- **THEN** the text's parent cell padding reflects 10f on left and right, and 4f on top and bottom

#### Scenario: Setting horizontal and vertical margin on Button
- **WHEN** `button("OK").marginX(12f).marginY(6f)` is declared
- **THEN** the button's margins are set to 12f horizontally and 6f vertically
