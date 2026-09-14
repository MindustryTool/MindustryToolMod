## 1. Top-left defaults in containers

- [x] 1.1 Set `Row` ctor `defaults().top().left()` and attacher `cell.top().left()`.
- [x] 1.2 Set `Column` ctor `defaults().top().left()` and attacher `cell.top().left()`.
- [x] 1.3 Set `Card` inner container `defaults().top().left()` and attacher `cell.top().left()`.
- [x] 1.4 Set `Grid` and `ReactiveGrid` child cells to `top().left()` by default.
- [x] 1.5 Add default-alignment unit tests (bare row/column/card stack from top-left; explicit `.center()` still centers).

## 2. Remove Justify

- [x] 2.1 Migrate `SettingsPanel` `row().justify(Justify.END)` to `row().right()` and drop the import.
- [x] 2.2 Delete `solim.layout.Justify` and `Row.justify()`.
- [x] 2.3 Update `LayoutTest` (drop justify scenarios/enum assertions, keep `align()` coverage).

## 3. Remove Container

- [x] 3.1 Delete `solim.layout.Container` and `ContainerTest`.
- [x] 3.2 Remove `Container` references from `LayoutTest`, `ElementModifiersTest`, `ComponentDefaultNameTest`, `AxisSpacingTest`, `RoundedGraphicsTest`.

## 4. Audit bare callsites for intentional centering

- [x] 4.1 List bare `row()`/`column()` callsites with no alignment modifier and triage content (keep top-left) versus dialog/loader/empty-state (needs `.center()`).
- [x] 4.2 Add explicit `.center()` where centering was intentional.
- [x] 4.3 Verify with build plus MCP screenshot pass of dialogs and browser views; confirm no hardcoded user-visible text and Java 8 compatibility per project rules.
