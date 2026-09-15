# solim-layout Specification

## Purpose

Mechanical merge of 9 specs per change `spec-domain-merge` (stage 4 framework-ui, concat-then-dedupe). Sources: solim-layout, element-config-mixin, grid-item-context, modifier-clarity, pending-cell-config, solim-shared-modifiers, solim-units, table-config-mixin, wrap-flow-layout. Each source below appears under a `**Source:` marker with its purpose body and requirement blocks verbatim; per-source `## Purpose` / `## Requirements` header lines are removed so all requirements parse inside the single `## Requirements` section. TBD purposes carried forward; requirement dedupe is follow-up work.

## Requirements

**Source: solim-layout**

TBD - created by archiving change create-solim-core. Update Purpose after archive.
### Requirement: Column and Row with flex-like modifiers
`Column` and `Row` SHALL be vertical/horizontal layout containers supporting fluent configuration modifiers `gap(int/float)`, `gap(Readable<Float>)`, `align(Align)` (START/CENTER/END/STRETCH), `padding(int)`, and `grow()`, followed by `.children(Runnable)` for declaring children. `Row` and `Column` SHALL apply gap as directional sibling padding along their primary axis (horizontal `padLeft` for `Row`, vertical `padTop` for `Column`) strictly to subsequent visible siblings, with zero gap padding on the leading child, zero gap padding on trailing edges, and zero gap padding on the cross-axis. `Row` and `Column` SHALL default children to top-left alignment (see Top-left default child alignment).

#### Scenario: Row align and gap
- **WHEN** `row().align(Align.CENTER).gap(8).children(() -> { button("A"); button("B"); })` is called
- **THEN** row configuration is applied before children are declared, button A has 0px gap padding and button B has 8px left gap padding with 0px top/bottom gap padding

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
The `Wrap` component SHALL implement `CellConfig<Wrap>`, `ElementConfig<Wrap>`, and `TableConfig<Wrap>`. CellConfig provides `growX()`, `grow()`, `margin()`. ElementConfig provides `width()`, `height()`, `size()`. TableConfig provides `top()`, `gap()`, `padding()`, `rounded()`, `border()`. Wrap SHALL support `children(Runnable)` for declarative child attachment via `ParentStack`.

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

### Requirement: Divider, SplitPane
Framework SHALL provide `Divider` (horizontal/vertical line) and `SplitPane` (if Arc provides `SplitPane` primitive) as thin wrappers.

#### Scenario: Divider
- **WHEN** `column(() -> { text("Above"); divider(); text("Below"); })` is rendered
- **THEN** a horizontal line (e.g., `Image` with `Tex.whiteui` tinted) separates sections

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

### Requirement: Use Arc-native layout mechanisms
All layout primitives and components SHALL delegate to Arc's existing `Table`, `Stack`, `ScrollPane`, and `Cell` APIs and SHALL NOT reimplement or alter Arc's layout engine. Layout containers and components SHALL NOT implement `ConstrainedElement`, SHALL NOT subclass Arc widgets to override `getPrefWidth`, `getPrefHeight`, `getMinWidth`, `getMinHeight`, `getMaxWidth`, or `getMaxHeight`, and SHALL NOT alter Arc's native layout calculations. Layout sizing, expansion, padding, and alignment SHALL be configured through native Arc `Cell` and `Element` properties.

#### Scenario: No custom layout engine or measurement overrides
- **WHEN** layout containers (`Row`, `Column`, `Grid`, `Scroll`, `Dynamic`, `ForEach`, `ReactiveGrid`) and Solim widgets are inspected
- **THEN** they wrap standard Arc widgets directly without subclassing to override layout measurement methods or implementing `ConstrainedElement`

### Requirement: Align enum
`Align` SHALL have START, CENTER, END, STRETCH.

#### Scenario: Align values exist
- **WHEN** `Align.values()` is inspected
- **THEN** it contains exactly START, CENTER, END, STRETCH

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

**Source: element-config-mixin**

TBD - created by archiving change refactor-element-config-mixins. Update Purpose after archive.
### Requirement: ElementConfig is a mixin interface with default methods
`ElementConfig<SELF>` SHALL be a public interface in `solim.modifier` with generic parameter `<SELF extends ElementConfig<SELF>>`. Implementing components SHALL provide `Element element()` to supply their root Arc Element.

#### Scenario: Component implements ElementConfig
- **WHEN** a Solim component `implements ElementConfig<MyComponent>` and provides `Element element()`
- **THEN** it inherits all ElementConfig default methods for free

### Requirement: ElementConfig provides size mutations
`ElementConfig` SHALL provide `width(float)`, `width(Readable<Float>)`, `height(float)`, `height(Readable<Float>)`, `size(float)`, `size(float, float)`, `size(Readable<Float>)`, `size(Readable<Float>, Readable<Float>)` that set the element's dimensions and update the parent cell if attached. All Readable overloads SHALL install reactive Effects.

#### Scenario: Setting element width
- **WHEN** `width(100f)` is called on an ElementConfig component
- **THEN** the element's width is set to 100f and the parent cell width is updated if attached

#### Scenario: Setting reactive element width
- **WHEN** `width(signal)` is called and signal changes from 100f to 200f
- **THEN** the element's width updates to 200f automatically

### Requirement: ElementConfig provides position mutations
`ElementConfig` SHALL provide `x(float)`, `x(Readable<Float>)`, `y(float)`, `y(Readable<Float>)`, `position(float, float)`, `position(Readable<Float>, Readable<Float>)` that set the element's local position.

#### Scenario: Setting element position
- **WHEN** `position(10f, 20f)` is called
- **THEN** the element's x is 10f and y is 20f

### Requirement: ElementConfig provides visibility
`ElementConfig` SHALL provide `visible(boolean)` and `visible(Readable<Boolean>)` that set element visibility and trigger gap re-spacing on parent Table if applicable.

#### Scenario: Hiding an element
- **WHEN** `visible(false)` is called
- **THEN** the element is hidden and parent Table re-spaces siblings

### Requirement: ElementConfig provides opacity
`ElementConfig` SHALL provide `opacity(float)`, `opacity(Readable<Float>)`, `alpha(float)`, `alpha(Readable<Float>)` that set element color alpha.

#### Scenario: Setting opacity
- **WHEN** `opacity(0.5f)` is called
- **THEN** the element's color alpha is 0.5f

### Requirement: ElementConfig provides naming
`ElementConfig` SHALL provide `name(String)` that sets the element's debug name.

#### Scenario: Setting element name
- **WHEN** `name("my-element")` is called
- **THEN** `element.name` equals `"my-element"`

**Source: grid-item-context**

Exposes contextual metrics from `ReactiveGrid` to child item component factories.


### Requirement: ReactiveGrid Item Context
`ReactiveGrid` SHALL provide a `GridItemContext` interface to child item factories, exposing reactive layout metrics including the computed usable item width and current column count.

#### Scenario: Item width matches rendered cell width
- **WHEN** `ReactiveGrid` is rendered and its backing table width changes
- **THEN** `GridItemContext.itemWidth()` SHALL reactively reflect the usable cell content width (`(gridWidth / columnCount) - gap`)

#### Scenario: Column count updates reactively
- **WHEN** the grid's column count signal emits a new value
- **THEN** `GridItemContext.columnCount()` SHALL emit the updated column count

### Requirement: ReactiveGrid Overloaded Item Factory
`ReactiveGrid` and the `solim.UI` facade SHALL support overloaded factory signatures accepting a `BiFunction<T, GridItemContext, Component>` while maintaining full backward compatibility for existing single-argument `Function<T, Component>` factories.

#### Scenario: Item factory receives context
- **WHEN** a `ReactiveGrid` is constructed with a `BiFunction<T, GridItemContext, Component>` item factory
- **THEN** each reconciled item component SHALL be instantiated with the item data and the grid's `GridItemContext`

#### Scenario: Backward compatible construction
- **WHEN** a `ReactiveGrid` is constructed with a single-parameter `Function<T, Component>`
- **THEN** existing items SHALL be reconciled and rendered without errors

**Source: modifier-clarity**

TBD - created by archiving change solim-architecture-refactor. Update Purpose after archive.
### Requirement: Modifier categories are non-overlapping and clearly defined
Every fluent modifier SHALL target exactly one of: (A) the component's own Arc Element via `ElementConfig`, (B) the component's Table container via `TableConfig`, or (C) the component's cell in its parent layout via `CellConfig`. Each mixin SHALL provide only methods for its domain. `CellConfig` SHALL provide only parent-cell methods (`grow*`, `min/max*`, `margin*`). `TableConfig` SHALL provide inner container insets via `padding*`. `cellPadding` and `TableConfig.margin` SHALL NOT exist. Element-targeted margin/padding overloads that dispatch via `instanceof Table` SHALL NOT exist.

#### Scenario: Self modifiers affect the Element only
- **WHEN** `.visible(false)`, `.opacity(0.5f)`, or `.rounded(8)` is called via `ElementConfig`
- **THEN** only the component's own Arc Element is modified; no parent cell is touched

#### Scenario: Table modifiers affect the container only
- **WHEN** `.top()`, `.padding(8f)`, or `.gap(4f)` is called via `TableConfig`
- **THEN** only the component's Table content insets and defaults are modified

#### Scenario: Parent-layout modifiers affect the cell only
- **WHEN** `.growX()` or `.margin(8f)` is called via `CellConfig`
- **THEN** only the component's cell in its parent layout is modified

#### Scenario: CellConfig has only parent-cell methods
- **WHEN** searching CellConfig for methods
- **THEN** only `grow*`, `min/max*`, `margin*` exist; no width/height/opacity/rounded/border/background and no `cellPadding*`

### Requirement: Fluent ordering convention is consistent
All Solim container components SHALL support the following fluent ordering convention:
1. Container/self configuration (before `children()`)
2. `children()` — declares children
3. Parent-layout configuration (after `children()`)

#### Scenario: Post-children parent-layout modifiers work
- **WHEN** `.grow()` is called after `.children(() -> { ... })`
- **THEN** the growth modifier is applied to the container's parent cell and does not affect children

### Requirement: Duplicate modifiers between ElementModifiers and LayoutModifiers are removed
Any modifier that targets the Element (width/height/opacity/rounded/border/background) SHALL exist only in `ElementConfig` or `TableConfig`. `CellConfig` SHALL NOT have those methods.

#### Scenario: No duplicate width implementation
- **WHEN** searching the Solim source for `width` modifier implementations
- **THEN** exactly one implementation exists in `ElementConfig`; CellConfig does not have `width`

### Requirement: Element overloads of margin/padding that dispatch are removed
Element-targeted overloads of `margin`/`padding` that check `instanceof Table` to dispatch between Table margin and Cell padding SHALL NOT exist. Callers SHALL use `TableConfig` for Table padding or `CellConfig.margin` for parent-cell padding.

#### Scenario: No instanceof Table dispatch in margin
- **WHEN** searching for `margin(Element` in ElementConfig/TableConfig
- **THEN** no methods with `instanceof Table` dispatch exist

**Source: pending-cell-config**

TBD - created by archiving change refactor-element-config-mixins. Update Purpose after archive.
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

**Source: solim-shared-modifiers**

Provides shared element modifier utilities and fluent component chaining for sizing, positioning, padding, and styling.
### Requirement: Centralized ElementModifiers utility for element sizing and positioning
The Solim framework SHALL provide an `ElementConfig<SELF>` mixin interface in package `solim.modifier` to handle sizing (`width`, `height`, `size`), positioning (`x`, `y`, `position`), visibility, opacity, and naming on Arc `Element` instances. Implementing components SHALL provide `Element element()`.

#### Scenario: Delegating element size mutations
- **WHEN** a component calls `width(100f)` inherited from `ElementConfig`
- **THEN** the element's width is set to 100f and the parent cell is updated if attached

### Requirement: ElementModifiers gap utility for table spacing
The `ElementModifiers` static utility class SHALL provide `gap(@Nullable Table table, float gap)` and `gap(@Nullable Element element, float gap)` static methods to apply directional item gap spacing to Arc `Table` instances without mutating `table.defaults().pad(g / 2f)` across all four edges. For recognized Solim layout containers (`Row`, `Column`, `Grid`, `Wrap`), `gap` SHALL configure primary-axis sibling spacing. For generic `Table` or `Button` instances, `gap` SHALL configure horizontal sibling padding between adjacent child cells.

#### Scenario: Applying gap spacing via ElementModifiers
- **WHEN** `ElementModifiers.gap(table, gap)` is invoked with a non-null Table and float value `g`
- **THEN** the table configures directional inter-sibling cell spacing of `g` along its layout axis, leaving outer container edges and cross-axis padding unaffected

#### Scenario: Element overload delegates safely
- **WHEN** `ElementModifiers.gap((Element) table, 20f)` is invoked
- **THEN** directional gap spacing of 20f is applied to the table's cells, and invoking with a non-Table element or null does not throw

### Requirement: Components delegate modifier methods to ElementModifiers
Solim components SHALL expose semantic fluent modifier methods (`width`, `height`, `size`, `position`, `visible`, `gap`, etc.) that return `this` for chaining while delegating implementation execution directly to `ElementModifiers`.

#### Scenario: Chaining modifier methods on UI components
- **WHEN** a developer calls `.width(100f).height(40f).gap(8f).visible(true)` on a Solim component (such as `Button` or `Row`)
- **THEN** internal element mutations including gap spacing are executed via `ElementModifiers` and the component instance is returned for chaining

### Requirement: ElementModifiers name utility for element naming
The `ElementModifiers` static utility class SHALL provide `name(@Nullable Element element, @Nullable String name)` to set the `name` field on the given Arc `Element`.

#### Scenario: Setting element name via ElementModifiers
- **WHEN** `ElementModifiers.name(element, "test-name")` is invoked with a non-null Element
- **THEN** `element.name` equals `"test-name"`

#### Scenario: Null-safe element naming
- **WHEN** `ElementModifiers.name(null, "test-name")` is invoked
- **THEN** no exception is thrown

### Requirement: Fluent name modifier on Solim components
All Solim components SHALL expose a fluent `.name(String name)` method that updates the underlying Arc `Element.name` and returns the component instance for method chaining. Invoking `.name(String name)` SHALL completely overwrite any default name assigned to the component.

#### Scenario: Chaining name modifier on Row
- **WHEN** `row().name("my-row").gap(8f)` is declared
- **THEN** the underlying table has `name` set to `"my-row"` and the `Row` instance is returned for subsequent chaining

#### Scenario: Chaining name modifier on Button
- **WHEN** `button("OK").name("confirm-button")` is declared
- **THEN** the underlying button has `name` set to `"confirm-button"`, replacing its default name

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

### Requirement: ElementModifiers opacity and alpha utilities
The `ElementModifiers` static utility class SHALL provide `opacity(@Nullable Element element, float opacity)` and `opacity(@Nullable Element element, Readable<Float> opacity)` (with `alpha` as an alias) to adjust element color alpha transparency, supporting both static values and reactive signals.

#### Scenario: Setting static element opacity
- **WHEN** `ElementModifiers.opacity(element, 0.6f)` is called
- **THEN** the element's color alpha is set to 0.6f

#### Scenario: Binding reactive element opacity
- **WHEN** `ElementModifiers.opacity(element, opacitySignal)` is called and `opacitySignal` changes from 1.0f to 0.5f
- **THEN** the element's color alpha updates to 0.5f via an internal effect

### Requirement: Fluent opacity modifier on Solim layout containers
Solim layout containers (`Hud`, `Column`, `Row`, `Card`) SHALL expose fluent `.opacity(float)` and `.opacity(Readable<Float>)` (and `.alpha(...)` alias) modifiers that delegate to `ElementModifiers` and return the component instance.

#### Scenario: Chaining opacity on a layout component
- **WHEN** `hud().opacity(feature.opacityConfig.signal())` is declared
- **THEN** the HUD container's transparency is bound to the opacity signal

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

### Requirement: TableConfig mixin for table alignment and spacing
The Solim framework SHALL provide a `TableConfig<SELF>` mixin interface in package `solim.modifier` to handle alignment (`top`, `bottom`, `left`, `right`, `center`, `align`), margin, padding, and gap on Arc `Table` instances. Implementing components SHALL provide `Table table()`.

#### Scenario: Table alignment via mixin
- **WHEN** a component calls `top()` inherited from `TableConfig`
- **THEN** the table's alignment is set to top

### Requirement: Element overloads of margin/padding are removed
The `ElementConfig` and `TableConfig` interfaces SHALL NOT provide `margin(Element, ...)` or `padding(Element, ...)` overloads that dispatch via `instanceof Table`. Table-specific operations SHALL only accept `Table` parameters.

#### Scenario: No Element-targeted margin dispatch
- **WHEN** `margin(element, 8f)` is called with an Element parameter
- **THEN** the method does not exist; callers use `TableConfig.padding(table, 8f)` or `CellConfig.margin(8f)`

**Source: solim-units**

Reactive viewport unit signals (`dvw`, `dvh`) and percentage calculation utilities that automatically synchronize with window resize events.


### Requirement: Reactive Viewport Signals
The `Units` class SHALL expose reactive signals `dvw` and `dvh` representing 1% of the dynamic viewport width and 1% of the dynamic viewport height in Arc scene coordinates (`(Core.graphics.getWidth() / Scl.scl()) / 100f` and `(Core.graphics.getHeight() / Scl.scl()) / 100f`).

#### Scenario: Reading initial dvw and dvh
- **WHEN** `Units.dvw` and `Units.dvh` signals are queried
- **THEN** they return 1% of current screen width and height in scene coordinates

### Requirement: Auto Update on ResizeEvent
The `Units` class SHALL automatically listen to Mindustry's `EventType.ResizeEvent` and update both `dvw` and `dvh` signals when the event fires.

#### Scenario: Screen size changes
- **WHEN** Mindustry fires `EventType.ResizeEvent` after the screen dimensions change
- **THEN** `Units.dvw` and `Units.dvh` signals update to reflect the new viewport dimensions
- **THEN** all reactive components and computed values bound to `Units.dvw` or `Units.dvh` are notified and updated

### Requirement: Viewport Percentage Helper Methods
The `Units` class SHALL provide helper methods `dvw(float percentage)` and `dvh(float percentage)` returning `Computed<Float>` representing the given percentage of the dynamic viewport width and height.

#### Scenario: Computing percentage of viewport
- **WHEN** `Units.dvw(50f)` or `Units.dvh(80f)` is evaluated
- **THEN** it returns a computed value equal to 50% of the viewport width or 80% of the viewport height respectively

#### Scenario: Dynamic percentage computation
- **WHEN** `Units.dvw(Readable<Float> percentage)` is evaluated with a reactive signal
- **THEN** the returned computed value updates when either the percentage signal or the viewport width changes

### Requirement: Full Viewport Dimensions Access
The `Units` class SHALL provide access to full viewport dimensions in scene coordinates.

#### Scenario: Reading full width and height
- **WHEN** full viewport dimensions are requested
- **THEN** `Units.width()` and `Units.height()` return computed signals equal to 100% of dynamic viewport width and height

**Source: table-config-mixin**

TBD - created by archiving change refactor-element-config-mixins. Update Purpose after archive.
### Requirement: TableConfig is a mixin interface with default methods
`TableConfig<SELF>` SHALL be a public interface in `solim.modifier` with generic parameter `<SELF extends TableConfig<SELF>>`. Implementing components SHALL provide `Table table()` to supply their underlying Arc Table.

#### Scenario: Component implements TableConfig
- **WHEN** a Solim component `implements TableConfig<MyComponent>` and provides `Table table()`
- **THEN** it inherits all TableConfig default methods for free

### Requirement: TableConfig provides alignment methods
`TableConfig` SHALL provide `align(int)`, `top()`, `bottom()`, `left()`, `right()`, `center()` that configure the Table's content alignment.

#### Scenario: Aligning table content
- **WHEN** `top()` is called on a TableConfig component
- **THEN** the table's alignment is set to top

### Requirement: TableConfig provides margin methods
`TableConfig` SHALL provide `margin(float)`, `margin(Readable<Float>)`, `margin(float, float, float, float)`, `margin(Readable<Float>, Readable<Float>, Readable<Float>, Readable<Float>)`, `marginTop(float)`, `marginTop(Readable<Float>)`, `marginBottom(float)`, `marginBottom(Readable<Float>)`, `marginLeft(float)`, `marginLeft(Readable<Float>)`, `marginRight(float)`, `marginRight(Readable<Float>)` that set the Table's margins. All Readable overloads SHALL install reactive Effects.

#### Scenario: Setting table margin
- **WHEN** `margin(8f)` is called
- **THEN** the table has 8f margin on all sides

### Requirement: TableConfig provides padding methods
`TableConfig` SHALL provide `padding(float)`, `padding(Readable<Float>)`, `padding(float, float, float, float)`, `padding(Readable<Float>, Readable<Float>, Readable<Float>, Readable<Float>)`, `paddingTop(float)`, `paddingTop(Readable<Float>)`, `paddingBottom(float)`, `paddingBottom(Readable<Float>)`, `paddingLeft(float)`, `paddingLeft(Readable<Float>)`, `paddingRight(float)`, `paddingRight(Readable<Float>)`, `paddingX(float)`, `paddingX(Readable<Float>)`, `paddingY(float)`, `paddingY(Readable<Float>)` that set the Table's margins (padding on a Table IS its margins). All Readable overloads SHALL install reactive Effects.

#### Scenario: Setting table padding
- **WHEN** `padding(12f)` is called
- **THEN** the table has 12f margin on all sides (Table padding = margin)

### Requirement: TableConfig provides gap and respace
`TableConfig` SHALL provide `gap(float)`, `gap(Readable<Float>)`, and `respace()` for configuring inter-sibling spacing. Readable overloads SHALL install reactive Effects.

#### Scenario: Setting gap
- **WHEN** `gap(8f)` is called on a Row
- **THEN** adjacent children have 8f spacing along the primary axis

### Requirement: TableConfig provides visual styling
`TableConfig` SHALL provide `rounded(int)`, `rounded(int, Color)`, `rounded(int, Readable<Color>)`, `border(float, Color)`, `border(float, Readable<Color>)`, `background(Drawable)`, `background(Readable<Drawable>)`, `background(Color)`, `backgroundColor(Color)`, `backgroundColor(Readable<Color>)` that configure the Table's background and RoundedDrawable. All Readable overloads SHALL install reactive Effects.

#### Scenario: Setting rounded corners
- **WHEN** `rounded(8)` is called on a Table-backed element
- **THEN** the Table's background is wrapped in a RoundedDrawable with radius 8

#### Scenario: Setting reactive background drawable
- **WHEN** `background(drawableSignal)` is called and signal changes
- **THEN** the Table's background drawable updates automatically

#### Scenario: Setting reactive background color
- **WHEN** `backgroundColor(colorSignal)` is called and signal changes
- **THEN** the Table's background color updates automatically

**Source: wrap-flow-layout**

Auto-wrapping flow container wrapping Arc.scene.ui.layout.WrapTable with 2D gap spacing, default container expansion, and Solim alignment support.

### Requirement: Automatic Line Wrapping
The system SHALL wrap child elements in a Wrap container onto new rows whenever the accumulated cell widths on the current row exceed the container's available width.

#### Scenario: Children wrap when container width exceeded
- **WHEN** multiple buttons are added inside a wrap() container whose combined width exceeds the container width
- **THEN** children that do not fit on the current line are laid out on subsequent rows below

### Requirement: 2D Gap Spacing
The system SHALL support uniform horizontal and vertical spacing between wrapped elements without leaving leading indentations on wrapped rows.

#### Scenario: Uniform gap on wrapped items
- **WHEN** developer calls .gap(unit(1)) on a wrap() container
- **THEN** adjacent items on the same row have horizontal gap and rows have vertical gap spacing

### Requirement: Container Alignment Support
The system SHALL support configuring child flow alignment via .left(), .center(), and .right() methods on Wrap.

#### Scenario: Align wrap flow to left
- **WHEN** developer calls wrap().left()
- **THEN** the child items align to the left side of the container

**Source: complete-component-modifiers**

Complete ElementConfig and TableConfig mixin support across all Solim structural and compound components.

### Requirement: Structural and compound components implement ElementConfig and TableConfig
The Solim structural and compound components `Dynamic`, `ForEach`, `Tabs`, `ReactiveGrid`, and `VirtualList` SHALL implement `ElementConfig` and `TableConfig` in addition to `CellConfig`. Each component SHALL provide `element()` and `table()` methods pointing to its root Arc Table.

#### Scenario: Sizing and visibility on Dynamic
- **WHEN** `.width(200f)`, `.height(100f)`, or `.visible(false)` is invoked on `Dynamic`
- **THEN** the underlying Arc Table and its cell configuration reflect the specified dimensions and visibility

#### Scenario: Sizing and styling on ForEach
- **WHEN** `.width(300f)` or `.padding(8f)` is invoked on `ForEach`
- **THEN** the underlying container Table has width set to 300f and padding set to 8f

#### Scenario: Element and table configuration on Tabs
- **WHEN** `.width(400f)`, `.name("settings-tabs")`, or `.background(color)` is invoked on `Tabs`
- **THEN** the root Table reflects the dimensions, name, and background styling

#### Scenario: Element and table configuration on ReactiveGrid
- **WHEN** `.width(500f)` or `.padding(10f)` is invoked on `ReactiveGrid`
- **THEN** the underlying grid Table reflects the dimensions and padding

#### Scenario: Element and table configuration on VirtualList
- **WHEN** `.height(600f)` or `.padding(4f)` is invoked on `VirtualList`
- **THEN** the outer Table of the virtual list reflects the height and padding

**Source: solim-css-margin-padding**

### Requirement: CSS Box Model Margin on CellConfig

`CellConfig` SHALL provide `.margin(...)` methods (`margin(float)`, `margin(float, float, float, float)`, `marginX(float)`, `marginY(float)`, `marginTop(float)`, `marginBottom(float)`, `marginLeft(float)`, `marginRight(float)`) and their reactive `Readable<Float>` overloads, mapping directly to outer spacing in the parent layout cell (`PendingCellConfig.padTop`, `padLeft`, `padBottom`, `padRight`).

#### Scenario: Margin applied to child in Row

- **WHEN** a component calls `.margin(8f)` inside a `Row`
- **THEN** 8px padding is stored in its `PendingCellConfig` and applied as outer cell padding in the parent table cell

#### Scenario: Reactive margin updates dynamically

- **WHEN** a component binds `.margin(Readable<Float>)` to a signal
- **THEN** the parent cell's padding updates reactively whenever the signal emits a new value

### Requirement: Popup applies PendingCellConfig to root content

`Popup.render()` SHALL inspect `PendingCellConfig` on the root content component and apply its constraints (including margin) to the cell created by `table.add(content.element())`.

#### Scenario: Popup root content with margin

- **WHEN** a popup's content provider returns a component with `.margin(12f)`
- **THEN** the cell inside the popup table receives 12px padding around the content element


