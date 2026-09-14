## MODIFIED Requirements

### Requirement: Column and Row with flex-like modifiers
`Column` and `Row` SHALL be vertical/horizontal layout containers supporting fluent configuration modifiers `gap(int/float)`, `gap(Readable<Float>)`, `align(Align)` (START/CENTER/END/STRETCH), `padding(int)`, and `grow()`, followed by `.children(Runnable)` for declaring children. `Row` and `Column` SHALL apply gap as directional sibling padding along their primary axis (horizontal `padLeft` for `Row`, vertical `padTop` for `Column`) strictly to subsequent visible siblings, with zero gap padding on the leading child, zero gap padding on trailing edges, and zero gap padding on the cross-axis. `Row` and `Column` SHALL default children to top-left alignment (see Top-left default child alignment).

#### Scenario: Row align and gap
- **WHEN** `row().align(Align.CENTER).gap(8).children(() -> { button("A"); button("B"); })` is called
- **THEN** row configuration is applied before children are declared, button A has 0px gap padding and button B has 8px left gap padding with 0px top/bottom gap padding

#### Scenario: Column gap and padding
- **WHEN** `column().gap(16).padding(24).children(() -> { text("Title"); divider(); text("Body"); })` is called
- **THEN** column configuration is set before children execution, applying 24px container padding via table margins, 0px top gap padding to "Title", 16px top gap padding to "divider" and "Body", and 0px left/right gap padding

### Requirement: Container, Divider, SplitPane
Framework SHALL provide `Divider` (horizontal/vertical line) and `SplitPane` (if Arc provides `SplitPane` primitive) as thin wrappers.

#### Scenario: Divider
- **WHEN** `column(() -> { text("Above"); divider(); text("Below"); })` is rendered
- **THEN** a horizontal line (e.g., `Image` with `Tex.whiteui` tinted) separates sections

## ADDED Requirements

### Requirement: Top-left default child alignment
`Row`, `Column`, `Card` (inner container), `Grid`, and `ReactiveGrid` SHALL default children to top-left alignment: each new child cell is aligned top-left via both the container's `table.defaults().top().left()` and per-cell `cell.top().left()` at attach time (covering `children(Runnable)` and direct `add(Element)` paths). `Scroll` already behaves this way and SHALL remain unchanged as the reference. Explicit `.top()`, `.left()`, `.right()`, `.bottom()`, or `.center()` modifiers SHALL continue to override the default for the table, its defaults, and existing cells.

#### Scenario: Bare row defaults to top-left
- **WHEN** `row().children(() -> { button("A"); button("B"); })` is rendered in a larger area without alignment modifiers
- **THEN** child cells carry top-left alignment and children sit at the top-left of the row

#### Scenario: Bare column defaults to top-left
- **WHEN** `column().children(() -> { text("A"); text("B"); })` is rendered in a larger area without alignment modifiers
- **THEN** child cells carry top-left alignment and children stack from the top-left of the column

#### Scenario: Explicit center still centers
- **WHEN** `column().grow().center().children(() -> { text("Loading"); })` is rendered
- **THEN** the content is centered exactly as before the default change

### Requirement: Align enum
`Align` SHALL have START, CENTER, END, STRETCH.

#### Scenario: Align values exist
- **WHEN** `Align.values()` is inspected
- **THEN** it contains exactly START, CENTER, END, STRETCH

## REMOVED Requirements

### Requirement: Justify and Align enums
**Reason**: `Justify` duplicates `left()/center()/right()` for START/CENTER/END while BETWEEN/AROUND/EVENLY never took effect in `Row.justify()`; one explicit alignment vocabulary removes dead API surface.
**Migration**: Replace `justify(Justify.START)` with `left()`, `justify(Justify.CENTER)` with `center()`, `justify(Justify.END)` with `right()` (e.g. `SettingsPanel`); drop `justify(BETWEEN/AROUND/EVENLY)` calls. `Align` continues under the Align enum requirement.
