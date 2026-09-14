# solim-shared-modifiers Specification

## Purpose
Provides shared element modifier utilities and fluent component chaining for sizing, positioning, padding, and styling.
## Requirements
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

