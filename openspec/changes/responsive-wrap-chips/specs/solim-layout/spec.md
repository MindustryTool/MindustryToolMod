## ADDED Requirements

### Requirement: Wrap with LayoutModifiers and declarative children
The `Wrap` component SHALL implement `LayoutModifiers<Wrap>`, providing `rounded()`, `border()`, `growX()`, `grow()`, `width()`, `height()`, `padding()`, `align()`, and all other modifier capabilities. Wrap SHALL support `children(Runnable)` for declarative child attachment via `ParentStack`, matching the pattern used by `Column` and `Grid`. Wrap SHALL also expose `background(Drawable)` for setting a background on its underlying table.

#### Scenario: Wrap with rounded and border
- **WHEN** `wrap().rounded(4).border(1f, Color.gray).children(() -> { ... })` is called
- **THEN** the wrap element SHALL have rounded corners and a 1px gray border, and children SHALL be attached via ParentStack

#### Scenario: Wrap with growX in parent column
- **WHEN** `wrap().growX().children(() -> { ... })` is placed inside a `column()`
- **THEN** the wrap SHALL expand horizontally to fill the parent column's width, and child chips SHALL wrap to the next row when their cumulative widths exceed the wrap's width

#### Scenario: Wrap with padding
- **WHEN** `wrap().padding(8).children(() -> { ... })` is called
- **THEN** the wrap's underlying table SHALL have 8px padding on all sides

#### Scenario: Imperative add still works
- **WHEN** `new Wrap()` is used with `add(element)` (without `children()`)
- **THEN** elements SHALL be added to the underlying table as before, preserving backward compatibility

### Requirement: Flow layout via Arc Table wrapping
The `Wrap` component SHALL use Arc's native Table wrapping mechanism. When child cells exceed the table's available width, they SHALL wrap to the next row. The wrap table's width SHALL be determined by the parent cell constraints (e.g., `growX()`).

#### Scenario: Chips wrap when row is full
- **WHEN** chips with widths [60f, 55f, 100f, 70f, 50f] are added to a wrap with 300f width
- **THEN** chips SHALL flow as: row 1 = [60f, 55f, 100f, 70f] (285f), row 2 = [50f]

#### Scenario: Wrap adapts to viewport width
- **WHEN** the parent column's width changes (e.g., window resize)
- **THEN** the wrap table SHALL reflow children to fit the new width
