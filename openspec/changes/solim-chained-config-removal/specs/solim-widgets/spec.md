## MODIFIED Requirements

### Requirement: Text widget with static and reactive content

`Text` SHALL display string content via `text(String)` and `text(Readable<String>)` (reactive). It SHALL wrap `arc.scene.ui.Label`, and its configuration SHALL be declared through `.props(...)`: `.props(color(...))`, `.props(style(LabelStyle))`, `.props(wrap(boolean))`, `.props(ellipsis(boolean))`, `.props(fontScale(float))`, alignment via `.props(left(), center(), right())`, and padding/margin via `.props(padding(...), margin(...))`. The chained configuration setters `.color(...)`, `.style(...)`, `.wrap(boolean)`, `.ellipsis(boolean)`, `.fontScale(float)`, the alignment setters, and the padding/margin setters SHALL NOT exist. Separate `text(Signal<String>)` and `text(Computed<String>)` overloads SHALL NOT exist; reactive sources bind through `Readable<String>` because `Signal` and `Computed` implement it. Reactive bindings SHALL be managed internally by the component lifecycle.

#### Scenario: Static Text

- **WHEN** `text("Settings")` is called
- **THEN** a `Label` with "Settings" is created and added to current parent

#### Scenario: Reactive Text

- **WHEN** `Computed<String> t = count.map(v -> "Count: " + v)` and `text(t)` then `count.set(5)`
- **THEN** label text updates to "Count: 5" via binding without recreation

#### Scenario: Signal binds through the Readable overload

- **WHEN** `Signal<String> name = Signal.of("Alice")` and `text(name)` is declared
- **THEN** it resolves to `text(Readable<String>)` and updates when `name` changes

#### Scenario: Configuration declared in props

- **WHEN** `text("Description").props(wrap(true), ellipsis(true), color(Color.lightGray), fontScale(0.9f))` is declared
- **THEN** the underlying `Label` has word-wrapping, ellipsis truncation, light gray color, and 0.9 font scale configured, and no chained setter is available for them

### Requirement: Button and IconButton with click handler and reactive props

`Button` SHALL support the constructor/factory forms `button()`, `button(Runnable onClick)`, `button(String text, Runnable onClick)`, and `button(Readable<String> text, Runnable onClick)`. `Button` SHALL provide fluent content methods `.text(String)`, `.text(Readable<String>)`, `.icon(Drawable)`, and `.icon(Readable<Drawable>)`, where `.icon(...)` applies the standard icon treatment (scalable, `Scaling.fit`, `unit(6)`), and these SHALL remain the only chained content methods. `Button` SHALL declare its configuration through `.props(...)`: `.props(enabled(...))`, `.props(visible(...))`, `.props(size(...))`, `.props(tooltip(...))`, `.props(style(...))`, and `.props(checked(...))`. The chained configuration setters `.enabled(Readable<Boolean>)`, `.visible(Readable<Boolean>)`, `.size(float)`, `.tooltip(...)`, `.style(...)`, `.checked(...)`, and `.stopClickPropagation()` SHALL NOT exist; click-stop control SHALL be declared through its property factory. `.onLongClick(Runnable)` and `.onLongClick(long, Runnable)` SHALL remain fluent as behaviour. The overloads `button(Drawable)`, `button(Drawable, Runnable)`, `button(String, Drawable, Runnable)`, and both `button(text, onClick, Consumer<SolimButtonStyleBuilder>)` variants SHALL be removed. When a long click triggers, regular `onClick` SHALL be suppressed. Reactive bindings SHALL be managed internally by the component and disposed with it.

#### Scenario: Fluent content and prop configuration

- **WHEN** `button().props(style(WebStyles.ghost()), size(unit(11)), tooltip(tip)).icon(Icon.add).onClick(r)` is declared
- **THEN** the button has ghost styling at `unit(11)`, a `unit(6)` icon child, the tooltip, and the click handler, with no chained configuration setters

#### Scenario: Long click suppresses click

- **WHEN** a long click triggers on a button with both handlers
- **THEN** the regular `onClick` handler is not invoked

### Requirement: NetworkImage Layout Methods and Parent Cell Constraints

`NetworkImage` SHALL declare its layout geometry and parent cell constraints through `.props(...)`, providing sizing (`size`, `width`, `height` and their reactive forms), the parent cell constraints (`minWidth`, `maxWidth`, `minHeight`, `maxHeight`, `growX`, `growY`, `grow`), and reactive variants as property factories. Parent cell constraints SHALL be recorded in `PendingCellConfig` and automatically bound to the parent `Cell` when attached via `AttachmentStack`, or applied immediately if already attached. The chained sizing and constraint methods SHALL NOT exist. `NetworkImage` SHALL NOT provide inner `padding()` or `gap()` and SHALL NOT implement `SpacingAware`.

#### Scenario: Declarative sizing

- **WHEN** `networkImage(url).props(size(width, height))` is declared
- **THEN** the sizing is recorded and applied on attachment with no chained sizing method

#### Scenario: Parent cell constraint

- **WHEN** `networkImage(url).props(growX())` is declared
- **THEN** the grow constraint is buffered in its `PendingCellConfig` and applied to the parent cell on attachment

### Requirement: Fluent chainable configuration

The `Popup` component SHALL return its own instance from every remaining control method (`children`, `show`, `hide`) so a menu is declared as a single chain. Its chrome SHALL be configured through `.props(...)`: `.props(rounded(...), border(...))`. The chained configuration methods `rounded` and `border` SHALL NOT exist.

#### Scenario: Chained control yields one instance

- **WHEN** a menu is configured through `children(provider)`, `show()`, and `hide()` on a typed instance
- **THEN** each call returns the same single menu instance in order

#### Scenario: Chrome declared in props

- **WHEN** a menu's chrome is configured
- **THEN** it is declared as `.props(rounded(...), border(...))` rather than chained calls

### Requirement: Default visible menu chrome

The `Popup` component SHALL render with a default dark rounded menu background so an unstyled menu is visible, while `rounded(...)` and `border(...)` declared through `.props(...)` override the chrome.

#### Scenario: Unstyled menu is visible

- **WHEN** a menu is shown without explicit chrome configuration
- **THEN** the menu renders with the default dark rounded background

#### Scenario: Explicit chrome overrides default

- **WHEN** `props(rounded(...))` or `props(border(...))` is declared before showing
- **THEN** the menu renders with the caller-specified chrome instead of the default
