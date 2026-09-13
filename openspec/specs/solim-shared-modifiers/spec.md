# solim-shared-modifiers Specification

## Purpose
Provides shared element modifier utilities and fluent component chaining for sizing, positioning, padding, and styling.
## Requirements
### Requirement: Centralized ElementModifiers utility for element sizing and positioning
The Solim framework SHALL provide an `ElementModifiers` static utility class in package `solim.modifier` to handle sizing (`width`, `height`, `size`), positioning (`x`, `y`, `position`), alignment, and visibility mutations on Arc `Element` and `Table` instances.

#### Scenario: Delegating element size and position mutations
- **WHEN** a component invokes `ElementModifiers.width(element, width)` or `ElementModifiers.size(element, w, h)`
- **THEN** `ElementModifiers` safely applies lower-bounded dimensions to the element and updates preferred size if applicable

#### Scenario: Delegating table alignment mutations
- **WHEN** a component invokes `ElementModifiers.align(table, align)` or `ElementModifiers.center(table)`
- **THEN** `ElementModifiers` updates the table's alignment

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
The `ElementModifiers` static utility class SHALL provide `padding` and `margin` methods for Arc `Element` instances (`padding(@Nullable Element, float)`, `padding(@Nullable Element, float, float, float, float)`, directional `paddingTop/Bottom/Left/Right`, `margin(@Nullable Element, float)`, `margin(@Nullable Element, float, float, float, float)`, directional `marginTop/Bottom/Left/Right`). When the element is a `Table`, they SHALL apply to table margins. When the element is contained within a parent `Table`, they SHALL apply to the element's enclosing `Cell` padding.

#### Scenario: Applying padding to an Element in a Table
- **WHEN** an element is placed inside an Arc `Table` and `ElementModifiers.padding(element, 10f)` is called
- **THEN** the parent table cell for that element has its padding updated to 10f

#### Scenario: Null-safe element padding and margin
- **WHEN** `ElementModifiers.padding(null, 10f)` or `ElementModifiers.margin(null, 10f)` is called
- **THEN** no exception is thrown

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

