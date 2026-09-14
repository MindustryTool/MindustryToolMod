# solim-layout Specification

## Purpose
TBD - created by archiving change create-solim-core. Update Purpose after archive.
## Requirements
### Requirement: Column and Row with flex-like modifiers
`Column` and `Row` SHALL be vertical/horizontal layout containers supporting fluent configuration modifiers `gap(int/float)`, `gap(Readable<Float>)`, `justify(Justify)` (START/CENTER/END/BETWEEN/AROUND/EVENLY), `align(Align)` (START/CENTER/END/STRETCH), `padding(int)`, and `grow()`, followed by `.children(Runnable)` for declaring children. `Row` and `Column` SHALL apply gap as directional sibling padding along their primary axis (horizontal `padLeft` for `Row`, vertical `padTop` for `Column`) strictly to subsequent visible siblings, with zero gap padding on the leading child, zero gap padding on trailing edges, and zero gap padding on the cross-axis.

#### Scenario: Row justify and align
- **WHEN** `row().justify(Justify.BETWEEN).align(Align.CENTER).gap(8).children(() -> { button("A"); button("B"); })` is called
- **THEN** row configuration is applied before children are declared, distributing children with space-between and centered vertically, button A has 0px gap padding and button B has 8px left gap padding with 0px top/bottom gap padding

#### Scenario: Column gap and padding
- **WHEN** `column().gap(16).padding(24).children(() -> { text("Title"); divider(); text("Body"); })` is called
- **THEN** column configuration is set before children execution, applying 24px container padding via table margins, 0px top gap padding to "Title", 16px top gap padding to "divider" and "Body", and 0px left/right gap padding

### Requirement: Grow semantics
Layouts SHALL support `growX()`, `growY()`, `grow()` on cells and widgets (e.g., `textField(input).growX()` or `button("Action").growX()`). Components in layout containers (`Column`, `Row`, `Card`, `Scroll`) and structural components (`ForEach`, `Dynamic`) SHALL NOT grow by default. Sizing SHALL adhere to the child's natural content size or explicit constraints unless `grow()`, `growX()`, or `growY()` is explicitly invoked, or the element is an expanding spacer. Grow SHALL map to Arc `cell.growX()`/`grow()`.

#### Scenario: Components do not grow by default
- **WHEN** a component without `growX()`, `growY()`, or `grow()` is attached to a `Column`, `Row`, `Card`, `Scroll`, or `ForEach`
- **THEN** the parent cell does NOT set `growX` or `growY`, and the component retains its natural or explicit dimensions

#### Scenario: Explicit growX on text field inside row
- **WHEN** `row(() -> { textField(input).growX(); button("Send"); })` inside a column
- **THEN** text field cell has `growX` set and expands to fill remaining width, while the button does not grow

#### Scenario: Grow via cell wrapper
- **WHEN** `cell(textField(input)).growX()` is used as alternative API
- **THEN** same Arc `growX` behavior occurs

### Requirement: Spacer consumes remaining space
`spacer()` SHALL create an element that grows to consume remaining space in `Row`/`Column`, typically via `add(new Spacer()).grow()`. The spacer element SHALL be recognized by `Ui.isExpanding(child)` and cause `Row.ATTACHER` to apply `cell.growX()` and `Column.ATTACHER` to apply `cell.growY()`.

#### Scenario: Spacer between buttons in Row
- **WHEN** `row(() -> { button("Back"); spacer(); button("Save"); })` is rendered
- **THEN** spacer expands horizontally with `growX > 0` and "Back" is left-aligned while "Save" is right-aligned

#### Scenario: Spacer in Column
- **WHEN** `column(() -> { text("Top"); spacer(); text("Bottom"); })` is rendered
- **THEN** spacer expands vertically with `growY > 0`, pushing "Bottom" to the end of the column

### Requirement: Grid with columns and gap
`grid(int columns)` and `grid(columns).gap(g)` SHALL provide a grid layout container supporting `.children(Runnable)` after column count and gap configuration. The grid SHALL apply horizontal gap between adjacent columns (`col > 0`) and vertical gap between adjacent rows (`row > 0`), with zero gap padding on the outer container borders.

#### Scenario: Grid 3 columns
- **WHEN** `grid(3).gap(8f).children(() -> { button("One"); button("Two"); button("Three"); button("Four"); })` is rendered
- **THEN** grid arranges 3 buttons in the first row and wraps the fourth, with button "One" having 0px left/top gap, button "Two" having 8px left gap, and button "Four" having 8px top gap

### Requirement: Wrap children wrapping
`wrap(() -> { ... })` SHALL layout children horizontally and wrap to next line when exceeding container width, using Arc wrapping container or `Table` with wrap enabled.

#### Scenario: Wrap tags
- **WHEN** `wrap(() -> { text("Java"); text("Mindustry"); text("Arc"); })` with narrow width
- **THEN** texts flow to next line when out of horizontal space

### Requirement: Wrap with LayoutModifiers and declarative children
The `Wrap` component SHALL implement `CellConfig<Wrap>`, `ElementConfig<Wrap>`, and `TableConfig<Wrap>`. CellConfig provides `growX()`, `grow()`, `cellPadding()`. ElementConfig provides `width()`, `height()`, `size()`. TableConfig provides `top()`, `gap()`, `padding()`, `rounded()`, `border()`. Wrap SHALL support `children(Runnable)` for declarative child attachment via `ParentStack`.

#### Scenario: Wrap with rounded and border
- **WHEN** `wrap().rounded(4).border(1f, Color.gray).children(() -> { ... })` is called
- **THEN** the wrap element SHALL have rounded corners and a 1px gray border via TableConfig

### Requirement: Flow layout via Arc Table wrapping
The `Wrap` component SHALL use Arc's native Table wrapping mechanism. When child cells exceed the table's available width, they SHALL wrap to the next row. The wrap table's width SHALL be determined by the parent cell constraints (e.g., `growX()`).

#### Scenario: Chips wrap when row is full
- **WHEN** chips with widths [60f, 55f, 100f, 70f, 50f] are added to a wrap with 300f width
- **THEN** chips SHALL flow as: row 1 = [60f, 55f, 100f, 70f] (285f), row 2 = [50f]

#### Scenario: Wrap adapts to viewport width
- **WHEN** the parent column's width changes (e.g., window resize)
- **THEN** the wrap table SHALL reflow children to fit the new width

### Requirement: Stack overlays children
`stack(() -> { image(bg); text("Loading"); })` SHALL overlay children on top of each other (last on top) using Arc `Stack`.

#### Scenario: Stack overlay
- **WHEN** `stack(() -> { image(background); text("Loading"); })` is rendered
- **THEN** both children occupy same bounds with text drawn over image

### Requirement: Scroll container
`scroll().grow().children(() -> { column().children(...) })` SHALL wrap content in a `ScrollPane` or Arc `Scroll` widget with configuration declared prior to child content.

#### Scenario: Scroll overflow
- **WHEN** `scroll().grow().children(() -> { column().children(() -> { for (i in 0..100) text("Item "+i); }); })` is declared
- **THEN** scroll is configured to grow before child column elements are populated

### Requirement: Container, Divider, SplitPane
Framework SHALL provide `Container` (single child with padding/background), `Divider` (horizontal/vertical line), and `SplitPane` (if Arc provides `SplitPane` primitive) as thin wrappers.

#### Scenario: Divider
- **WHEN** `column(() -> { text("Above"); divider(); text("Below"); })` is rendered
- **THEN** a horizontal line (e.g., `Image` with `Tex.whiteui` tinted) separates sections

#### Scenario: Container padding
- **WHEN** `container(() -> text("Hi")).padding(12).background(Styles.black6)` is used
- **THEN** child has 12px padding and background drawable

### Requirement: Use Arc-native layout mechanisms
All layout primitives and components SHALL delegate to Arc's existing `Table`, `Stack`, `ScrollPane`, and `Cell` APIs and SHALL NOT reimplement or alter Arc's layout engine. Layout containers and components SHALL NOT implement `ConstrainedElement`, SHALL NOT subclass Arc widgets to override `getPrefWidth`, `getPrefHeight`, `getMinWidth`, `getMinHeight`, `getMaxWidth`, or `getMaxHeight`, and SHALL NOT alter Arc's native layout calculations. Layout sizing, expansion, padding, and alignment SHALL be configured through native Arc `Cell` and `Element` properties.

#### Scenario: No custom layout engine or measurement overrides
- **WHEN** layout containers (`Row`, `Column`, `Grid`, `Scroll`, `Dynamic`, `ForEach`, `ReactiveGrid`) and Solim widgets are inspected
- **THEN** they wrap standard Arc widgets directly without subclassing to override layout measurement methods or implementing `ConstrainedElement`

### Requirement: Justify and Align enums
`Justify` SHALL have START, CENTER, END, BETWEEN, AROUND, EVENLY. `Align` SHALL have START, CENTER, END, STRETCH.

#### Scenario: Enum values exist
- **WHEN** `Justify.values()` and `Align.values()` are inspected
- **THEN** they contain exactly the listed constants

### Requirement: Additive combination of child margins and container gap
Layout containers (`Row`, `Column`, `Grid`) SHALL combine child element margins and container gap additively on the primary layout axis. For any child element in a `Row`, the cell's `padLeft` SHALL equal the child's explicit `marginLeft` plus the container's `gap` if the child is preceded by an earlier visible sibling. For any child element in a `Column`, the cell's `padTop` SHALL equal the child's explicit `marginTop` plus the container's `gap` if preceded by an earlier visible sibling.

#### Scenario: Child margin coexists with container gap
- **WHEN** a `Row` with `gap(16)` contains child A and child B with `marginLeft(8)`
- **THEN** child A has `padLeft = 0` and child B has `padLeft = 24` (8px margin + 16px gap), while neither child receives top or bottom padding from gap

### Requirement: Visibility-aware gap re-spacing
When a child element inside a `Row` or `Column` changes visibility or collapses to zero-size via `Dynamic`, the container SHALL re-evaluate adjacent sibling gap spacing so that the first currently visible element receives 0px gap padding along the primary axis, avoiding dead space at the container's leading edge.

#### Scenario: First child collapses in Row
- **WHEN** the first child of a `Row` with `gap(16)` becomes hidden (`visible = false`)
- **THEN** the second child is promoted to the leading visible element and its `padLeft` gap offset is reduced to 0px

### Requirement: PendingCellConfig replaces SizeConstraints
The deferred parent-cell configuration buffer SHALL be named `PendingCellConfig` instead of `SizeConstraints`. All references to `SizeConstraints` SHALL be renamed.

#### Scenario: PendingCellConfig used by ParentStack
- **WHEN** ParentStack adds a child element to a parent Table
- **THEN** `PendingCellConfig.find(child)` resolves the pending config and `applyToCell(cell)` applies stored values

