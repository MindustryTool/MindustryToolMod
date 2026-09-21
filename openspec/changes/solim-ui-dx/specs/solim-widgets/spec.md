## MODIFIED Requirements

### Requirement: Reusable virtual list layout component
The Solim layout system SHALL provide a declarative `VirtualList` component (accessible via `solim.UI.virtualList(...)`) that accepts a fundamental data pair on the factory — the item collection (`Readable<? extends List<T>>` or `List<T>`) and the item height provider — and mounts only elements that intersect the visible viewport plus an overscan buffer. The key selector SHALL be optional fluent configuration (`.key(Function<T, ?>)`, defaulting to identity), and the item component builder SHALL be supplied through a terminal `void` `children(Function<T, Component>)`. Component generics SHALL be `<T>` only.

#### Scenario: Mounting only visible items in viewport
- **WHEN** a list of 100 items is supplied to `VirtualList` inside a scroll viewport that can physically fit 10 items
- **THEN** only the visible items plus configured overscan buffer items are instantiated and added to the Scene2D hierarchy, while off-screen items are not mounted

#### Scenario: Overscan buffer preservation
- **WHEN** items are rendered in `VirtualList`
- **THEN** an overscan buffer (default 3 items above and below the visible viewport) is mounted to prevent visual blanking during scrolling

#### Scenario: Fluent virtual list declaration
- **WHEN** `virtualList(groupedMessages, heightCalculator).key(MessageGroup::getKey).overscan(3).onReachTop(50f, load).children(item -> new MessageGroupView(item))` is declared
- **THEN** the virtual list mounts visible items with the supplied factory, keeps the height provider as fundamental factory data, and `.children(...)` completes the definition

### Requirement: Text widget with static and reactive content
`Text` SHALL display string content via `text(String)` and `text(Readable<String>)` (reactive). It SHALL wrap `arc.scene.ui.Label` and provide fluent chained property modifiers including `.color(Color)`, `.color(Readable<Color>)`, `.style(LabelStyle)`, `.wrap(boolean)`, `.ellipsis(boolean)`, `.fontScale(float)`, text alignment (`.left()`, `.center()`, `.right()`), and padding/margin modifiers (`.padding(float)`, `.padding(float, float, float, float)`, `.paddingTop(float)`, `.paddingBottom(float)`, `.paddingLeft(float)`, `.paddingRight(float)`, `.margin(float)`, `.margin(float, float, float, float)`, `.marginTop(float)`, `.marginBottom(float)`, `.marginLeft(float)`, `.marginRight(float)`). Separate `text(Signal<String>)` and `text(Computed<String>)` overloads SHALL NOT exist; reactive sources bind through `Readable<String>` because `Signal` and `Computed` implement it. Reactive bindings SHALL be managed internally by the component lifecycle.

#### Scenario: Static Text
- **WHEN** `text("Settings")` is called
- **THEN** a `Label` with "Settings" is created and added to current parent

#### Scenario: Reactive Text
- **WHEN** `Computed<String> t = count.map(v -> "Count: " + v)` and `text(t)` then `count.set(5)`
- **THEN** label text updates to "Count: 5" via binding without recreation

#### Scenario: Signal binds through the Readable overload
- **WHEN** `Signal<String> name = Signal.of("Alice")` and `text(name)` is declared
- **THEN** it resolves to `text(Readable<String>)` and updates when `name` changes

#### Scenario: Chained text styling and layout modifiers
- **WHEN** `text("Description").wrap(true).ellipsis(true).color(Color.lightGray).fontScale(0.9f)` is declared
- **THEN** the underlying `Label` has word-wrapping, ellipsis truncation, light gray color, and 0.9 font scale configured directly

### Requirement: Image and Icon widgets
`Image` SHALL display `Drawable`/`TextureRegion` with `image(Drawable)` and `image(Readable<Drawable>)`. `Icon` SHALL display icon drawable with `icon(Drawable)` and `icon(Readable<Drawable>)`, applying the standard treatment (scalable drawable, `Scaling.fit`, `unit(6)` sizing). A no-argument `icon()` facade SHALL NOT exist; empty images use `image()`/`image(Readable)`. Both SHALL support size/style bindings, as well as fluent padding and margin modifiers on `SolimImage` (`.padding(float)`, `.padding(float, float, float, float)`, `.paddingTop(float)`, `.paddingBottom(float)`, `.paddingLeft(float)`, `.paddingRight(float)`, `.margin(float)`, `.margin(float, float, float, float)`, `.marginTop(float)`, `.marginBottom(float)`, `.marginLeft(float)`, `.marginRight(float)`).

#### Scenario: Image display
- **WHEN** `image(backgroundDrawable)` is called
- **THEN** an `Image` with that drawable is added

#### Scenario: Icon reactive
- **WHEN** `icon(darkMode.map(v -> v ? Icon.moon : Icon.sun))` and `darkMode` toggles
- **THEN** icon drawable updates via binding

#### Scenario: Icon factory applies standard sizing
- **WHEN** `icon(Icon.add)` is declared
- **THEN** the resulting `SolimImage` uses a scalable drawable, `Scaling.fit`, and `unit(6)` size

#### Scenario: Image padding and margin modifiers
- **WHEN** `image(icon).padding(8f).margin(2f)` is declared inside a parent table
- **THEN** the parent cell padding around the image reflects the combined padding and margin

### Requirement: Badge and Avatar lightweight components
`Badge` SHALL be a lightweight label/container for counts/status. Count badges SHALL be created through `badge(int)` or `badge(Readable<String>)` (with caller-side formatting such as `"99+"`); a separate `badgeCount(...)` facade SHALL NOT exist. `Avatar` SHALL display user image with fallback.

#### Scenario: Badge count
- **WHEN** `badge(count.map(v -> v > 99 ? "99+" : String.valueOf(v)))` is used
- **THEN** badge text updates reactively

#### Scenario: Static integer badge
- **WHEN** `badge(3)` is used
- **THEN** a count badge showing "3" is created and hidden when the count is zero

#### Scenario: badgeCount facade is absent
- **WHEN** the public facade is inspected
- **THEN** `badgeCount(...)` is not declared

### Requirement: Button and IconButton with click handler and reactive props
`Button` SHALL support the constructor/factory forms `button()`, `button(Runnable onClick)`, `button(String text, Runnable onClick)`, and `button(Readable<String> text, Runnable onClick)`. `Button` SHALL additionally provide fluent content methods `.text(String)`, `.text(Readable<String>)`, `.icon(Drawable)`, and `.icon(Readable<Drawable>)`, where `.icon(...)` applies the standard icon treatment (scalable, `Scaling.fit`, `unit(6)`). The overloads `button(Drawable)`, `button(Drawable, Runnable)`, `button(String, Drawable, Runnable)`, and both `button(text, onClick, Consumer<SolimButtonStyleBuilder>)` variants SHALL be removed. `Button` SHALL support chained modifiers `.enabled(Readable<Boolean>)` (null-tolerant), `.visible(Readable<Boolean>)`, `.size(float)`, `.tooltip(String)`, `.tooltip(Readable<String>)`, `.onLongClick(Runnable)`, `.onLongClick(long, Runnable)`, `.style(...)`, and `.stopClickPropagation()`. When a long click triggers, regular `onClick` SHALL be suppressed. Reactive bindings SHALL be managed internally by the component and disposed with it.

#### Scenario: Button click handler
- **WHEN** `button("Save", () -> save())` is clicked
- **THEN** `save()` is invoked

#### Scenario: Fluent icon button replaces Drawable overloads
- **WHEN** an icon-only button is needed
- **THEN** it is declared as `button(onClick).icon(icon)` rather than `button(icon, onClick)`

#### Scenario: Null-tolerant enabled chaining
- **WHEN** `button(...).enabled(canEdit)` is declared with `@Nullable Readable<Boolean> canEdit`
- **THEN** the binding is installed when non-null and the chain continues without an intermediate variable when null

#### Scenario: Button reactive text and style
- **WHEN** `Computed<String> saveText = dirty.map(v -> v ? "● Save" : "Save")` and `button(saveText, onSave).enabled(dirty).style(Styles.PRIMARY)` then `dirty.set(true)`
- **THEN** button text, enabled, and style update via bindings; click only enabled when dirty true

#### Scenario: Button long click handler
- **WHEN** a user holds a button configured with `.onLongClick(onLongPressAction)` for >= 300ms
- **THEN** `onLongPressAction` is executed and regular `onClick` is suppressed upon release

#### Scenario: Button short click does not trigger long press
- **WHEN** a user taps and releases a button configured with both `.onClick(clickAction)` and `.onLongClick(longAction)` in < 300ms
- **THEN** `clickAction` is executed and `longAction` is not triggered

#### Scenario: Reactive tooltip on Button
- **WHEN** `button("Action", () -> {}).tooltip(tooltipSignal)` is declared and `tooltipSignal` emits a new string
- **THEN** the button's tooltip text updates to reflect the new string value

### Requirement: Card is a single-Table layout element
The `Card` component SHALL be a single-Table layout element consistent with `Column` and `Row`, where child composition, sizing, and background all target the same Arc `Table` instead of a Button-wrapping-Table pair. The `Card.of(Runnable)` static factory SHALL be removed because it discarded its argument and is superseded by `UI.card(Runnable)`.

#### Scenario: Card children attach to the element table
- **WHEN** `card(() -> { text("Hi"); })` is rendered
- **THEN** the text label is a direct child cell of the table returned by both `element()` and `table()`.

#### Scenario: Card click isolation from inner buttons
- **WHEN** an inner button stops event propagation inside a clickable card
- **THEN** the inner button handler runs and the card `onClick` handler does not run.

#### Scenario: Card.of is absent
- **WHEN** the `Card` API is inspected
- **THEN** no `Card.of(...)` factory is declared

### Requirement: Layout facades return layout component
Each declarative helper SHALL return the created fluent component so callers can chain modifiers or store references. `spacer()` SHALL return the `Spacer` component rather than a raw `Element`.

#### Scenario: Chained modifiers
- **WHEN** `column().padding(24).gap(16)` is declared
- **THEN** the returned `Column` has padding/gap applied and supports further chaining

#### Scenario: Spacer returns its component
- **WHEN** `spacer().name("gap")` is declared
- **THEN** a `Spacer` is returned and its name is applied

### Requirement: Implicit parent stack with lambda scopes
The framework SHALL provide `ParentStack` with static helpers `column()`, `row()`, `stack()`, `grid(int columns)`, `wrap()`, `scroll()`, `card()`, `collapser()`, and `dialog(String title)` returning fluent builder instances supporting `.children(Runnable)`. A `container()` alias SHALL NOT exist; `column()` is the canonical vertical layout. Calling `.children(Runnable)` pushes the layout Element onto `ParentStack`, executes the lambda, pops with try/finally, attaches the layout to the outer active parent container, and returns the container instance. Every child created inside the `.children(Runnable)` lambda SHALL auto-attach to current parent, including `BaseComponent` subclasses. The `stack()` helper SHALL create a `solim.layout.SolimStack` overlay container attached to `ParentStack`. SolimStack SHALL expose Component-returning `layer(Supplier<Component>)` and `layer(Function<Element, Component>)` methods that attach directly to the underlying Stack with no intermediate Row, own each returned layer component, and dispose owned layers exactly once on stack disposal; SolimStack SHALL NOT declare a `children(...)` overload, and a `stack(Runnable)` facade SHALL NOT exist.

#### Scenario: Push/pop with try/finally and configuration before children
- **WHEN** column().grow().children(() -> { text("Settings"); row().growX().children(() -> { button("Cancel"); button("Save"); }); }) executes
- **THEN** internally: configuration .grow() is applied to column first, then column table is pushed to ParentStack, text is added to column, row table is pushed with .growX(), buttons are added to row, row is popped and attached to column, column is popped; if lambda throws, finally still pops

#### Scenario: Auto-attach children including BaseComponent subclasses
- **WHEN** inside column(() -> { text("Chat"); new ChatMessageListView(store, service); row().children(() -> { textField(input); button("Send", this::send); }); })
- **THEN** textField and button are children of inner row; text and ChatMessageListView element are children of outer column — all without explicit add or component() calls

#### Scenario: container alias is absent
- **WHEN** the public facade is inspected
- **THEN** `container()` and `container(Runnable)` are not declared

#### Scenario: Stack helper creates SolimStack overlay
- **WHEN** stack().grow().layer(() -> icon(Icon.chat)).layer(() -> icon(Icon.warning)) is executed inside an active parent
- **THEN** a SolimStack is instantiated, its Stack element is attached to the parent, and both layers are overlaid on top of each other

#### Scenario: No start/end API
- **WHEN** the public facade is inspected
- **THEN** it does NOT expose startColumn()/endColumn() or startComponent()/endComponent() — only configuration-before-children fluent methods exist

## REMOVED Requirements

### Requirement: Icon button declarative facades
**Reason**: The requirement described `Ui.iconButton(Drawable, Runnable)` and `Ui.iconButton(Drawable, ImageButtonStyle, Runnable)` on the legacy `solim.core.Ui` facade, which no longer exists. Icon buttons are now composed from the canonical `button().icon(...)` fluent content API, with project-specific styling supplied by the mod's `WebStyles.iconButton(...)` helpers.
**Migration**: Replace `iconButton(icon, onClick)` with `button(onClick).icon(icon)`; replace styled variants with `button(onClick).icon(icon).style(SolimButtonStyle)`. Mod code uses the new `WebStyles.iconButton(icon, [tooltip,] onClick)` helper which applies the standard ghost style and `unit(11)`/`unit(6)` sizing.