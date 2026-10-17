## MODIFIED Requirements

### Requirement: Layout and alignment declared exclusively via props

Configuration on every Solim host — layout, spacing, alignment, sizing, styling, and state/config values — SHALL be declared exclusively through `.props(...)`. Chained configuration methods on component classes (`.gap(...)`, `.top()`, `.bottom()`, `.left()`, `.right()`, `.center()`, `.align(...)`, `.color(...)`, `.style(...)`, `.wrap(...)`, `.fontScale(...)`, `.checked(...)`, `.rounded(...)`, `.border(...)`, `.background(...)`, `.padding(...)`, `.margin(...)`, `.collapsed(...)`, `.autoplay(...)`, `.controls(...)`, `.columns(...)` on `Grid`, and the rest) SHALL be removed. Content (appends a child), behaviour (a callback or an imperative action), structure (`children`/`items`/`tab`/`show`/`hide`), and accessors SHALL remain fluent.

#### Scenario: Gap declared in props

- **WHEN** configuring a container with item spacing
- **THEN** the spacing is declared as a prop argument `.props(gap(unit(2)))` instead of a chained `.gap(...)` method

#### Scenario: Alignment declared in props

- **WHEN** configuring a container or element with alignment
- **THEN** alignment is declared as a prop argument `.props(center(), top())` or `.props(align(...))` instead of chained alignment methods

#### Scenario: State and style declared in props

- **WHEN** a host has a state or style value such as `checked`, `collapsed`, `muted`, `progress`, or `style`
- **THEN** it is declared through `.props(...)` rather than a chained setter

#### Scenario: Content and behaviour remain fluent

- **WHEN** `button().text("Save").onClick(this::save)` is declared
- **THEN** both calls compile, because they are content and behaviour, not configuration

### Requirement: Button fluent content composition

`Button` SHALL provide fluent content methods `text(String)`, `text(Readable<String>)`, `icon(Drawable)`, and `icon(Readable<Drawable>)` that append the corresponding child content to the button. `icon(...)` SHALL apply the standard Solim icon treatment: scalable drawable, `Scaling.fit`, and `unit(6)` sizing. Content SHALL append in call order, and reactive content bindings SHALL be owned by and disposed with the button. These methods SHALL be retained as content: they are distinguished from the removed content-shaped setters, such as `Text.text(String)`, which set a value rather than append a child.

#### Scenario: Fluent text button

- **WHEN** `button().text("Save").onClick(this::save)` is declared
- **THEN** the button contains a text label and invokes `save()` on click

#### Scenario: Fluent icon button

- **WHEN** `button().props(style(WebStyles.ghost()), size(unit(11))).icon(Icon.add).onClick(r)` is declared
- **THEN** the button contains an icon child sized at `unit(6)` with no `children(() -> ...)` lambda required, and its style and size come from `props(...)`

#### Scenario: Reactive icon updates without recreation

- **WHEN** `button().icon(playIcon)` is declared with `Readable<Drawable> playIcon` and the icon value changes
- **THEN** the button's icon drawable updates in place

#### Scenario: Reactive content disposed with the button

- **WHEN** a button built with `text(readable)` is disposed
- **THEN** the reactive binding's effect is disposed and no subscription survives
