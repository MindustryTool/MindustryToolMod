# solim-styling Specification

## Purpose

Mechanical merge of 8 specs per change `spec-domain-merge` (stage 4 framework-ui, concat-then-dedupe). Sources: solim-styling, pure-button-component, pure-solim-components, solim-badge, solim-card, solim-rounded, unified-border-background, web-styles. Each source below appears under a `**Source:` marker with its purpose body and requirement blocks verbatim; per-source `## Purpose` / `## Requirements` header lines are removed so all requirements parse inside the single `## Requirements` section. TBD purposes carried forward; requirement dedupe is follow-up work.

## Requirements

**Source: solim-styling**

TBD - created by archiving change create-solim-core. Update Purpose after archive.
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

**Source: pure-button-component**

TBD - created by archiving change pure-solim-button-refactor. Update Purpose after archive.
### Requirement: Solim provides a pure composable button component
The Solim UI framework SHALL provide a single `button()` container component that accepts an optional click listener, centers its child elements by default, and relies on `.children()` blocks for content layout and composition.

#### Scenario: Constructing a button with children
- **WHEN** a developer calls `button(onClick).children(() -> { ... })`
- **THEN** a button component is created with default center alignment for its children, with the click listener and inner child components attached to the button container

### Requirement: Deprecation of specialized button facades
The system SHALL NOT provide `IconButton` or specialized text/icon factory overloads on `solim.ui.Ui`.

#### Scenario: Attempting to use removed button facades
- **WHEN** building UI components via `solim.ui.Ui`
- **THEN** only `button()` and `button(onClick)` are available to instantiate buttons

**Source: pure-solim-components**

TBD - created by archiving change pure-solim-button-refactor. Update Purpose after archive.
### Requirement: Solim components are unstyled by default
Solim UI layout and input components SHALL NOT automatically inject or resolve default UI styles or skins (`ButtonStyle`, `ImageButtonStyle`, skin fallbacks).

#### Scenario: Instantiating a pure Solim component
- **WHEN** a Solim component such as `Card` or `Button` is instantiated without an explicit style
- **THEN** the component initializes unstyled without fallback skin resolution

### Requirement: Solim components only wrap Arc elements without changing behavior
Solim components (`Button`, `Text`, `SolimImage`, `SolimTextField`, `Checkbox`, `Card`, etc.) SHALL wrap standard Arc widgets directly without creating custom subclasses that override Arc's native layout or measurement methods (`getPrefWidth`, `getPrefHeight`, `getMinWidth`, `getMinHeight`, etc.) or implementing synthetic constraint marker interfaces (`ConstrainedElement`).

#### Scenario: Instantiating standard Arc widgets directly
- **WHEN** a Solim component such as `Button`, `Text`, `Checkbox`, or `SolimTextField` is instantiated
- **THEN** its underlying element is a standard Arc widget instance (e.g. `arc.scene.ui.Button`, `Label`, `CheckBox`, `TextField`) without layout calculation overrides

#### Scenario: No ConstrainedElement marker implementation
- **WHEN** any Solim component's underlying Arc element is inspected
- **THEN** it does not implement `ConstrainedElement`

**Source: solim-badge**

Provides a declarative, pill-shaped Badge component for counter indicators and status tags in Solim UI.

### Requirement: Declarative Badge Component
The system SHALL provide a Badge component (solim.display.Badge, Ui.badge) for compact tag and counter indicators.

#### Scenario: Rendering text badge
- **WHEN** adge("5") or adge(unreadSignal) is added to a layout
- **THEN** a pill-shaped badge element is rendered with formatted label content.

#### Scenario: Hiding badge on zero count
- **WHEN** a badge is configured with an unread integer signal that reaches 0
- **THEN** the badge automatically hides itself if hideOnZero is enabled.

**Source: solim-card**

Clickable, styled card container component with declarative child composition, fluent chained reactive property bindings, and click event bubbling control.
### Requirement: Declarative Card Container Component
The framework SHALL provide a `Card` component wrapping a clickable, styled container element. It SHALL support declarative child composition via `ParentStack`, custom click handling, and event bubbling control.

#### Scenario: Building card with children
- **WHEN** `card(() -> { text("Title"); text("Subtitle"); })` is executed
- **THEN** a `Card` instance is created and attached to the current parent, with the child elements added inside the card container.

#### Scenario: Card click action
- **WHEN** a card is configured with `.onClick(Runnable)` and clicked directly
- **THEN** the provided click handler is executed.

### Requirement: Chained Reactive Property Bindings on Card
The `Card` component SHALL provide fluent modifier methods for reactive and static properties including `.width(Readable<Float>)`, `.width(float)`, `.height(Readable<Float>)`, `.height(float)`, `.prefHeight(float)`, `.color(Readable<Color>)`, `.color(Color)`, `.style(ButtonStyle)`, `.name(String)`, and `.padding(float)`. The reactive bindings SHALL be automatically managed and disposed by the component lifecycle.

#### Scenario: Reactive width and color binding
- **WHEN** `card(...).width(cardWidth).color(cardColor)` is rendered and the underlying signals update
- **THEN** the card element's width and color are updated in place and layout is invalidated without reconstructing the card.

### Requirement: Click Event Bubbling Control in Card
The `Card` component and child widgets SHALL support event bubbling control such that interactive child controls can consume click events without triggering the card's click handler.

#### Scenario: Clicking child button inside card
- **WHEN** an interactive control inside a clickable card is clicked and stops event propagation
- **THEN** the child control's handler executes and the card's `onClick` handler is not triggered.

### Requirement: Card children default to top-left
The `Card` inner container SHALL default children to top-left alignment via both `defaults().top().left()` and per-cell `cell.top().left()` at attach time, matching `Row`/`Column`. Explicit `.top()`, `.left()`, `.right()`, `.bottom()`, or `.center()` modifiers SHALL continue to override the default.

#### Scenario: Bare card stacks from top-left
- **WHEN** `card(() -> { text("Title"); text("Subtitle"); })` is rendered in a larger area without alignment modifiers
- **THEN** child cells carry top-left alignment and children stack from the top-left of the card

**Source: solim-rounded**

TBD - created by archiving change add-squircle-background-and-border. Update Purpose after archive.
### Requirement: Procedural continuous-curvature rounded 9-patch generation
The system SHALL generate pure white 9-patch drawables for continuous-curvature rounded shapes using the L4 superellipse norm (x^4 + y^4 <= r^4) with sub-pixel anti-aliasing.

#### Scenario: Solid rounded generation
- **WHEN** a rounded drawable with radius 8 is requested
- **THEN** a NinePatchDrawable is produced with dimensions matching the radius and sub-pixel smoothed boundary alphas

#### Scenario: Hollow rounded border generation
- **WHEN** a rounded border with radius 12 and stroke width 2 is requested
- **THEN** a NinePatchDrawable is produced with an anti-aliased border stroke and transparent center

### Requirement: Rounded drawable caching
The system SHALL cache generated rounded NinePatchDrawables by integer radius and stroke thickness, reusing existing textures for matching parameters.

#### Scenario: Cache hit on repeated requests
- **WHEN** rounded drawable with radius 10 is requested multiple times
- **THEN** subsequent calls return the cached NinePatchDrawable instance without creating new textures

### Requirement: Composite RoundedDrawable with fill and border
The system SHALL provide a RoundedDrawable that composites a rounded background fill and an optional rounded border stroke in a single Drawable instance with independent colors. By default, the fill color SHALL be transparent (`Color.clear`) so that adding a border or corner radius alone does not inject an opaque background fill.

#### Scenario: Composite drawing
- **WHEN** a RoundedDrawable configured with fill color darkGray and border color accent is drawn
- **THEN** the fill 9-patch is rendered with darkGray followed by the border 9-patch rendered with accent in the current batch

#### Scenario: Default transparent background
- **WHEN** a RoundedDrawable is created without specifying a fill color or when border is applied to an element
- **THEN** the fill color SHALL default to Color.clear and no solid fill is rendered

### Requirement: Universal LayoutModifiers rounded and border support
All Solim layout containers implementing LayoutModifiers (including Card, Column, Row, Grid, SolimStack, and Scroll) SHALL provide .rounded(...) and .border(...) modifiers.

#### Scenario: Layout container rounded styling
- **WHEN** column().rounded(12, Pal.darkMetal).border(1.5f, Pal.accent) is declared
- **THEN** the column's underlying table receives a background RoundedDrawable configured with radius 12 and border stroke 1.5

#### Scenario: Reactive color updates on container
- **WHEN** a reactive Readable<Color> is supplied to .rounded(radius, colorSignal) and the signal changes value
- **THEN** the container's RoundedDrawable updates its fill color without recreating the container or underlying textures

### Requirement: Widget and dialog rounded support
Solim interactive widgets (Button, SolimTextField) and overlay components (SolimDialog, Popup, Badge) SHALL provide rounded and border configuration.

#### Scenario: Button rounded styling
- **WHEN** utton().rounded(8, Pal.gray).border(1f, Pal.accent) is declared
- **THEN** the button background style reflects the rounded fill and border

#### Scenario: Dialog rounded panel
- **WHEN** dialog().rounded(16, Pal.darkMetal).border(2f, Pal.accent) is declared
- **THEN** the dialog main window panel renders with the rounded background and border

### Requirement: Static ElementModifiers for arbitrary Arc elements
The system SHALL provide static utility methods in ElementModifiers to apply rounded backgrounds and borders to any Arc Table or Element.

#### Scenario: Direct element modification
- **WHEN** ElementModifiers.rounded(table, 10, Color.darkGray) is called on an Arc Table
- **THEN** the table's background is updated with a rounded drawable matching radius 10 and darkGray

**Source: unified-border-background**

Order-independent composition of background drawables/colors, continuous-curvature rounded corners, and border strokes on Solim elements with automatic geometry safety clamping.

### Requirement: Order-Independent Background and Border Composition
The system SHALL preserve and compose background drawables, background fill colors, corner radius, and border strokes regardless of the order in which .background(...), .rounded(...), and .border(...) are called on a Solim container or element.

#### Scenario: Background called after border
- **WHEN** developer calls .border(1f, Color.gray) followed by .background(Styles.black6) on a Solim container
- **THEN** the element renders the Styles.black6 background with the 1px gray border stroke overlay intact

#### Scenario: Border called after background
- **WHEN** developer calls .background(Styles.black6) followed by .border(1f, Color.gray) on a Solim container
- **THEN** the element renders the Styles.black6 background with the 1px gray border stroke overlay intact

### Requirement: Fluent Background Color Overloads
The system SHALL support setting background color directly on Solim layout containers via .background(Color) and .background(Readable<Color>) overloads on LayoutModifiers and ElementModifiers.

#### Scenario: Set static background color on Column
- **WHEN** developer calls column().background(Color.royal)
- **THEN** the column renders with a royal blue background fill

#### Scenario: Set reactive background color on Row
- **WHEN** developer binds row().background(colorSignal)
- **THEN** the row background updates automatically when colorSignal changes value

### Requirement: Dynamic Corner Radius Geometry Clamping
The system SHALL automatically clamp the rendered corner radius in RoundedDrawable so that it never exceeds half the rendered width or height (min(width, height) / 2), preventing NinePatch negative slice height inversions and line artifacts.

#### Scenario: Oversized radius on small element
- **WHEN** developer specifies rounded(40) on an element with height of 36px
- **THEN** the rendered radius is clamped to 18px, forming a smooth pill shape without inverted lines or overlapping crossover artifacts

**Source: web-styles**

Provides shadcn/ui-inspired semantic design-system button styles with a centralized color sheet, five shared variants (primary/secondary/outline/ghost/danger), continuous rounded borders and multi-state visuals with built-in padding for web and browser components without mutating Arc global styles.


### Requirement: Semantic Color Sheet (WebStyles.Colors)
The system SHALL provide a nested `WebStyles.Colors` static class containing semantic color tokens derived from the shadcn/ui dark theme:
- Primary brand: `PRIMARY` (shadcn `primary`, deep indigo `(0.215f, 0.163f, 0.674f, 1.0f)`), `PRIMARY_HOVER` (shadcn `sidebar-primary`, brighter indigo), `PRIMARY_DOWN`, `PRIMARY_FG` (shadcn `primary-foreground`, lavender-white).
- Primary wash: `PRIMARY_BG` (primary at 15% alpha), `PRIMARY_BG_HOVER` (35%), `PRIMARY_BG_DOWN` (60%).
- Secondary: `SECONDARY` (shadcn `secondary`, dark slate `(0.153f, 0.153f, 0.166f, 0.70f)`), `SECONDARY_HOVER`, `SECONDARY_DOWN`, `SECONDARY_FG` (shadcn `secondary-foreground`).
- Ghost: `GHOST_HOVER` (white 10%, shadcn `border`), `GHOST_DOWN`, `GHOST_FG` (shadcn `muted-foreground`).
- Danger: `DANGER` (shadcn `destructive`, coral red `(1.0f, 0.391f, 0.404f, 1.0f)`), `DANGER_HOVER`, `DANGER_DOWN`, `DANGER_FG`.
- Border and disabled: `BORDER` (shadcn `border`, white 10%), `BORDER_INPUT` (shadcn `input`, white 15%, used for the outline button border), `DISABLED_BG`, `DISABLED_BORDER`, `DISABLED_FG`.

#### Scenario: Accessing semantic tokens
- **WHEN** `WebStyles.Colors.PRIMARY` or `WebStyles.Colors.DANGER` is referenced
- **THEN** it returns the corresponding non-null `Color` token

### Requirement: Shared Button Variants and Text Counterparts
`WebStyles` SHALL provide standard button style variants and matching text button variants:
- `primary()` / `primaryText()`
- `secondary()` / `secondaryText()`
- `outline()` / `outlineText()`
- `ghost()` / `ghostText()`
- `danger()` / `dangerText()`
Each variant SHALL be pre-built and cached as a reusable singleton with zero runtime allocations.

#### Scenario: Retrieving cached button variants
- **WHEN** `WebStyles.primary()`, `WebStyles.outlineText()`, or any variant method is called repeatedly
- **THEN** it returns the cached singleton instance with zero new object allocations

#### Scenario: Text button styling
- **WHEN** `WebStyles.outlineText()` is inspected
- **THEN** it contains `Fonts.def` with matching state-dependent font colors and outline drawables

### Requirement: Built-in Button Padding
Shared button variants SHALL configure built-in default padding (`unit(2)`) so that buttons styled with these variants automatically receive standard internal spacing around children.

#### Scenario: Button padding applied from style
- **WHEN** a button is styled with a `WebStyles` shared variant via `button.style(...)`
- **THEN** the button's internal padding is set to `unit(2)`

### Requirement: WebStyles Multi-State Rounded Button Styling
The system SHALL provide isolated `TextButtonStyle` and `ButtonStyle` instances for each shared variant styled with the `WebStyles.Colors` semantic tokens, distinct `up`, `down`, `over`, and `disabled` visual states, and continuous rounded borders using `RoundedDrawable`.

#### Scenario: Normal and interactive states
- **WHEN** a button styled with any `WebStyles` shared variant is rendered in normal, hovered, or pressed state
- **THEN** it SHALL render with a rounded border and the corresponding variant tint from `WebStyles.Colors` without mutating `arc.scene.ui.Button.ButtonStyle` or `mindustry.ui.Styles` globals

#### Scenario: Disabled state styling
- **WHEN** a WebStyles button has its enabled state set to false
- **THEN** it SHALL render with a muted dark background, dark gray rounded border, and disabled font/image color

### Requirement: Direct Value Inlining
`WebStyles` SHALL define styles by inlining dimensional values directly (e.g. `unit(2)`, `1.5f`) into builder method calls without declaring disposable intermediate variables.

#### Scenario: Static initializer simplicity
- **WHEN** `WebStyles.java` source is inspected
- **THEN** no temporary single-use local variables (e.g. `int radius = ...`, `float stroke = ...`) are declared in the static initializer

### Requirement: No Legacy Aliases
`WebStyles` SHALL NOT define legacy aliases (`webButton`, `webTextButton`, `CHANNEL_BLUE`, `CHANNEL_BLUE_*`). All consuming components across the codebase SHALL reference the new standard methods (`outlineText()`, `primaryText()`, `Colors.PRIMARY`, etc.) directly.

#### Scenario: No legacy aliases exist
- **WHEN** `WebStyles.java` is inspected
- **THEN** it contains no deprecated `webButton`, `webTextButton`, or `CHANNEL_BLUE` fields

### Requirement: Clear Input Preset (WebStyles.clearInput)
`WebStyles` SHALL provide a shared, cached `clearInput()` preset returning an `InputStyle` with a blank drawable (`Tex.clear`) in all four background slots (`background`, `focusedBackground`, `disabledBackground`, `invalidBackground`) and everything else unset (inheriting the field base style).

#### Scenario: Retrieving the cached preset
- **WHEN** `WebStyles.clearInput()` is called repeatedly
- **THEN** it returns the same shared instance with zero new allocations

#### Scenario: Preset blanks all chrome states
- **WHEN** the preset is inspected
- **THEN** all four background slots hold the blank drawable, so no underline appears in normal, focused, disabled, or invalid states

