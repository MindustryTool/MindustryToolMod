## Why

The `props(...)` DSL is now value-typed (Phase 1), target-typed (Phase 2), and semantic-typed (Phase 3), but the chained configuration methods it was meant to replace still exist. Two ways to say the same thing means:

- the compile-time guarantees only hold for call sites that chose `props(...)`; a chained `.padding(4f)` on a non-table host still compiles;
- `solim-config-dsl` still carries the transitional "Chained and Props modifiers have a defined precedence" requirement, which exists only because both styles are live;
- `mod` has ~1000 chained configuration call sites (`.color` ≈477, `.style` ≈433, `.wrap` ≈103, `.fontScale` ≈87, `.checked` ≈71, `.background` ≈61, `.border` ≈52, `.placeholder` ≈26), so the DSL is not actually declarative in practice.

## What Changes

- **Remove every chained configuration method**: the setter forms that configure a rendered/layout value, a state value, or a style. `Text.text(String)`, `Text.color(...)`, `Text.wrap(...)`, `Text.fontScale(...)`, `Text.style(...)`, `SolimImage.padding(...)`, `Button.checked(...)`, `SolimVideo.autoplay(...)`, `SolimCollapser.collapsed(...)`, `Grid.columns(...)`, and the rest are deleted, together with the `padding*`/`margin*`/`rounded`/`border`/`background*` setters on containers.
- **Keep what is not configuration**: constructors and factories keep their content (`new Text("hi")`, `new Badge("7")`, `Checkbox(label, signal)`), `Button.text(...)`/`Button.icon(...)` remain as **fluent content composition** because they append a child, behaviour and structure stay fluent (`onClick`, `onLongClick`, `items`, `tab`, `show`/`hide`, `mount`, `scrollTo`, `validator`, `onEnter`, `focus`, `onReach*`), and getters stay (`text()`, `checked()`, `columns()`).
- **Add the missing prop vocabulary**: every deleted setter gets a prop factory (`fillParent`, `touchable`-free layout values, per-widget state props such as `activeTab`, `progress`, `hideOnZero`, `placeholder`, `maxLength`, `scrollingDisabled`, `scrollX`, `scrollY`, `pinnedEdge`, `headerGap`, `tabStyle`, `playIcon`, `pauseIcon`, …), and `Grid.columns(...)` becomes a layout prop. Reconciler configuration (`reactiveGrid(items).columns(3).key(k).items(f)`) stays fluent per decision.
- **State props bind in the right direction**: user-togglable state (`collapsed`/`expanded`/`muted`/`checked`/`value`/`selected`) accepts a `Signal<T>` and installs a `TwoWayBinding`, so a header toggle or mute click writes back; pure-display values stay `Readable<T>` one-way. Applied-once values (`autoplay`, `duration`, `animated`, icons) take plain values.
- **Staged removal**: props ship alongside the setters, the setters are `@Deprecated`, `mod` is migrated package by package, and the setters are then deleted so the compiler proves completion.
- **Specs are reconciled**: the transitional precedence requirement is replaced by "configuration is declared only through props", and every spec that mandates a fluent chained configuration method is amended.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `solim-config-dsl`: "Chained and Props modifiers have a defined precedence" is removed and replaced by a "Configuration is declared only through props" requirement that also fixes application order (props apply after fluent content; later props in one call win).
- `solim-ui-dx`: "Layout and alignment declared exclusively via props" is widened from layout-only to all configuration; "Button fluent content composition" is amended to state that content methods are retained while content-shaped setters (`Text.text`) are removed.
- `solim-widgets`: the `Text`, `Button`, and `NetworkImage` requirements drop their chained configuration-modifier lists in favour of the prop vocabulary; the popup-menu "Fluent chainable configuration" requirement is amended to keep behaviour (`show`/`hide`) fluent while moving chrome to props.
- `solim-styling`: "Chained Reactive Property Bindings on Card" is removed; card chrome is declared through props.
- `solim-video`: `autoplay`/`loop`/`volume`/`muted`/`controls` and the overlay icons are declared as props rather than chained setters.

## Impact

- `solim-core`: chained configuration methods deleted across `display`, `input`, `layout`, `feedback`, `overlay`, and the modifiers; new prop factories in `Values`; `Grid.columns` becomes a prop; two-way state props.
- `mod`: every chained configuration call site migrates to `props(...)`; call sites using `.style(...)`, `.color(...)`, `.wrap(...)`, `.fontScale(...)`, `.checked(...)`, `.background(...)`, `.border(...)`, `.placeholder(...)` are the bulk. Some `.color`/`.style`/`.background` calls are Arc (non-Solim) and are left untouched.
- `solim-core` tests: tests that call the deleted setters are migrated to `props(...)`.
- Compatibility: `@Deprecated` setters exist for the migration window and are then deleted; no permanent dual API.

## Non-Goals

- Making reconciler configuration a prop (`columns`/`key`/`items`/`overscan` stay fluent).
- Making behaviour or structure (`onClick`, `items`, `show`, `mount`, `scrollTo`) a prop.
- A new style value object or re-parenting Arc styles.
