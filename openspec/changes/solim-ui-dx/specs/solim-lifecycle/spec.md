## MODIFIED Requirements

### Requirement: Complete Public UI Facade
`solim.UI` in `:solim` SHALL provide all user-permitted static factory methods for Solim UI operations. This SHALL include:
- Layouts: `column()`, `row()`, `grid()`, `reactiveGrid()`, `card()`, `scroll()`, `stack()`, `wrap()`, `collapser()`, `divider()`, `spacer()`
- Widgets: `button()`, `text()`, `textField()`, `slider()`, `checkbox()`, `switchToggle()`, `select()`, `image()`, `icon()`, `networkImage()`, `badge()`
- Overlays: `dialog()`, `hud()`, `popup()`
- Reactivity: `signal()`, `computed()`, `effect()`, `createSignal()`
- Structural & Components: `dynamic()`, `when()`, `forEach()`, `virtualList()`, `arc()`
- Units & Events: `unit()`, `dvw()`, `dvh()`, `listen()`
The keyed-collection factories `reactiveGrid(items)`, `forEach(items)`, and `virtualList(items, heightProvider)` SHALL accept only fundamental data, with configuration applied through fluent methods and the required item factory supplied through terminal `void` `children(...)`. The facade SHALL NOT expose `badgeCount()`, `container()`, `divider(String)`, `divider(char)`, or no-argument `icon()`. The legacy duplicate facade `solim.core.Ui` SHALL NOT exist; `solim.UI` is the sole public facade. Internal helpers such as `isExpanding` (now `SolimToken.isExpandingChild`), `element`, and `add` SHALL NOT be exposed on `solim.UI`.

#### Scenario: UI methods instantiation
- **WHEN** UI elements, signals, layouts, or widgets are created in user code
- **THEN** all methods are accessible statically via `solim.UI`

#### Scenario: Reactivity factory methods on UI
- **WHEN** a user creates a signal or computed value via `UI.signal(value)` or `UI.computed(supplier)`
- **THEN** the corresponding `Signal<T>` or `Computed<T>` instance from `solim-core` is instantiated and returned

#### Scenario: Single facade available
- **WHEN** a consumer inspects the Solim modules for a public UI facade
- **THEN** only `solim.UI` provides factory methods and no `solim.core.Ui` type exists

#### Scenario: Structural facade includes when
- **WHEN** a consumer needs conditional rendering
- **THEN** `UI.when(condition, supplier)` is available alongside `UI.dynamic(...)`

#### Scenario: Removed facade methods unavailable
- **WHEN** a consumer attempts to use `badgeCount`, `container`, string/char `divider`, or no-argument `icon`
- **THEN** the methods do not exist and the consumer uses `badge(Readable<String>)`, `column()`, `divider(Direction)`, and `image()` instead