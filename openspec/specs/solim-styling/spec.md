# solim-styling Specification

## Purpose
TBD - created by archiving change create-solim-core. Update Purpose after archive.
## Requirements
### Requirement: Style immutable value object
`Style` SHALL be an immutable object holding Arc-relevant styling data (e.g., `Drawable background`, `Color`, `font`, `pad`/`margin` variants relevant to widget, or a reference to Arc `TextButtonStyle`/`LabelStyle` wrapper) and SHALL NOT be mutated after creation. Predefined instances SHALL be provided via `Styles` constants (`PRIMARY`, `GHOST`, etc.).

#### Scenario: Immutable style
- **WHEN** `Style s = Style.builder().background(Styles.PRIMARY).pad(8).build()` then `s` is used on two buttons
- **THEN** modifying builder after `build()` does not affect `s`, and `s` has no setters

#### Scenario: Styles constants exist
- **WHEN** `Styles.PRIMARY` and `Styles.GHOST` are referenced
- **THEN** they are non-null `Style` singletons usable via `.style(Styles.PRIMARY)`

### Requirement: Static style application
`widget.style(Style)` SHALL apply the entire `Style` to the underlying `Element` immediately (e.g., `button.style.background = style.background` or `element.setStyle(...)` depending on widget). No CSS parsing SHALL be involved.

#### Scenario: Static style apply
- **WHEN** `button("Save").style(Styles.PRIMARY)` is called
- **THEN** underlying Arc element has primary style applied and `getStyle()` reflects it

### Requirement: Reactive style binding
`widget.style(Signal<Style>)` and `widget.style(Computed<Style>)` SHALL apply current style immediately, subscribe to future changes, and on each change re-apply the entire new `Style` to the `Element` without diffing.

#### Scenario: Reactive style toggle
- **WHEN** `Signal<Boolean> darkMode = Signal.of(false)` and `button("Toggle").style(darkMode.map(v -> v ? Styles.PRIMARY : Styles.GHOST))` and `darkMode.set(true)`
- **THEN** button style switches from GHOST to PRIMARY via full re-apply

#### Scenario: Reactive style is disposable
- **WHEN** style binding is disposed then signal changes
- **THEN** element style remains previous value

### Requirement: Style application without diffing or CSS
Framework SHALL NOT implement CSS parsing, CSS Grid/Flexbox recreation, or style diffing beyond straightforward full re-apply. Do not attempt to diff individual style fields unless Arc internals require it.

#### Scenario: No CSS engine
- **WHEN** `Style.java` is inspected
- **THEN** it contains no CSS parser, no `selector`, no `stylesheet`, and no `flex`/`grid` property strings

#### Scenario: Full re-apply on change
- **WHEN** reactive style changes from `A` to `B`
- **THEN** implementation calls `applyStyle(element, B)` that overwrites all relevant fields, not a field-by-field diff

### Requirement: Style builder and composition
`Style` SHALL provide a builder or factory that allows composing from existing `Styles` constants plus overrides, and SHALL remain thin and Arc-native.

#### Scenario: Compose from base
- **WHEN** `Style custom = Styles.PRIMARY.withPad(16).withBackground(otherDrawable)`
- **THEN** `custom` has primary fields plus overrides without mutating `PRIMARY`

### Requirement: Fluent Button Style Builder
The system SHALL provide `SolimButtonStyleBuilder` supporting chained method configuration of root styling properties (such as `rounded(int)`, `border(float, Color)`, `font(Font)`) and state-specific sub-builders (`up`, `over`, `down`, `disabled`, `checked`). Root properties SHALL be inherited by all states unless explicitly overridden by a state builder.

#### Scenario: State builder inheritance and overrides
- **WHEN** `builder.rounded(8).border(1.5f, Color.white).up(u -> u.background(Color.blue))` is executed
- **THEN** all states inherit 8px radius and 1.5px border stroke, and the `up` state uses `Color.blue` background

#### Scenario: Automatic hover and pressed tints
- **WHEN** only the `up` state background is defined without explicit `over` or `down` states
- **THEN** the builder SHALL automatically derive lightened `over` and darkened `down` states

### Requirement: Button Scoped Lambda Styling
`Button.style(Consumer<SolimButtonStyleBuilder>)` SHALL execute the styling lambda against a new or pooled builder, resolve the style, and apply it to the underlying button without breaking the `Button` method chain.

#### Scenario: Scoped lambda application
- **WHEN** `button("Test").style(s -> s.rounded(4).up(u -> u.background(Color.red))).width(100f)` is called
- **THEN** the button is styled with the red rounded background and the `.width(100f)` method executes on the same `Button` instance

### Requirement: Style Layout Properties
`SolimButtonStyleBuilder` SHALL support configuring layout properties (`padding`, `margin`, `gap`) with both static numeric values and `Readable<Float>` reactive signals.

#### Scenario: Static padding and gap applied to button
- **WHEN** a style with `.padding(8f).gap(4f)` is applied to a `Button`
- **THEN** the button's internal padding is set to 8px and cell gap is set to 4px

#### Scenario: Reactive margin updates
- **WHEN** a style with `.margin(marginSignal)` is applied to a `Button` and `marginSignal` updates
- **THEN** the button's margin updates reactively to match the new signal value

### Requirement: Reactive Property Binding and Ownership in Styles
When style properties (such as background color, border color, radius, or padding) are provided as `Readable<?>` signals, the system SHALL establish reactive bindings that are automatically owned by the host `Button` and disposed when the button is disposed.

#### Scenario: Color signal updates drawable
- **WHEN** `style.up(u -> u.background(colorSignal))` is applied to a button and `colorSignal.set(Color.green)` is called
- **THEN** the button's `up` drawable color updates to `Color.green` without rebuilding the button widget

#### Scenario: Reactive style bindings disposed with button
- **WHEN** a button with signal-bound style properties is disposed via `button.dispose()`
- **THEN** all underlying style effects and signal subscriptions are disposed

### Requirement: Static Style Flyweight Caching
The system SHALL maintain a thread-safe flyweight cache for static style configurations. When a style builder containing only static values is built, the system SHALL return a cached `ButtonStyle` and layout metadata instance matching the configuration key, avoiding redundant object allocations.

#### Scenario: Identical static styles reuse instance
- **WHEN** two separate buttons are configured with identical static properties via the style builder
- **THEN** both buttons receive the exact same underlying `ButtonStyle` instance

