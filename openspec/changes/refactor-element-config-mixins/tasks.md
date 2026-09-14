## 1. Rename SizeConstraints → PendingCellConfig

- [x] 1.1 Rename `SizeConstraints.java` to `PendingCellConfig.java` and update class name
- [x] 1.2 Move from `solim.layout` to `solim.modifier` package (or keep in `solim.layout` — decide)
- [x] 1.3 Remove `applySizeToParentCell` method (no longer needed)
- [x] 1.4 Update all imports and references across solim-core (~20 files)

## 2. Create ElementConfig mixin interface

- [x] 2.1 Create `ElementConfig.java` interface in `solim.modifier` with `<SELF>` generic
- [x] 2.2 Add `Element element()` abstract method
- [x] 2.3 Add default methods with static + Readable overloads: `width`, `height`, `size`
- [x] 2.4 Add default methods with static + Readable overloads: `x`, `y`, `position`
- [x] 2.5 Add default methods: `visible(boolean)`, `visible(Readable<Boolean>)`
- [x] 2.6 Add default methods: `opacity(float)`, `opacity(Readable<Float>)`, `alpha(float)`, `alpha(Readable<Float>)`
- [x] 2.7 Add default method: `name(String)`
- [x] 2.8 Add default methods with static + Readable overloads: `rounded`, `border`, `background`
- [x] 2.9 Move `ColorDrawable` and `getOrCreateRounded` as package-private helpers in `solim.modifier`

## 3. Create TableConfig mixin interface

- [x] 3.1 Create `TableConfig.java` interface in `solim.modifier` with `<SELF>` generic
- [x] 3.2 Add `Table table()` abstract method
- [x] 3.3 Add default methods: `align(int)`, `top`, `bottom`, `left`, `right`, `center`
- [x] 3.4 Add default methods with static + Readable overloads: `margin*`, `padding*`, `marginX`, `marginY`, `paddingX`, `paddingY`
- [x] 3.5 Add default methods: `gap(float)`, `gap(Readable<Float>)`, `respace()`
- [x] 3.6 Move `GenericGapContainer` as package-private in `solim.modifier`

## 4. Slim down CellConfig — remove methods now provided by ElementConfig/TableConfig

- [x] 4.1 Remove `width`, `height`, `size` from CellConfig (components get from ElementConfig)
- [x] 4.2 Remove `opacity`, `alpha` from CellConfig (components get from ElementConfig)
- [x] 4.3 Remove `rounded`, `border`, `background` from CellConfig (components get from ElementConfig)
- [x] 4.4 Remove `top`, `bottom`, `left`, `right`, `center` from CellConfig (components get from TableConfig)
- [x] 4.5 CellConfig keeps ONLY with static + Readable overloads: `growX`, `growY`, `grow`, `minWidth`, `minHeight`, `maxWidth`, `maxHeight`, `cellPadding*`

## 5. Remove Element-overload dispatch methods

- [x] 5.1 Delete `margin(Element, ...)` from static ElementConfig
- [x] 5.2 Delete `padding(Element, ...)` from static ElementConfig
- [x] 5.3 Delete `paddingX/Y(Element, ...)` and `marginX/Y(Element, ...)` from static ElementConfig
- [x] 5.4 Delete `gap(Element, ...)` from static ElementConfig
- [x] 5.5 Re-point remaining callers to TableConfig (for Tables) or CellConfig.cellPadding (for parent cells)

## 6. Update component implementations — add mixin interfaces

- [x] 6.1 Row: `implements ElementConfig<Row>, TableConfig<Row>, CellConfig<Row>`, add `element()` and `table()`, remove width/height/opacity/rounded/border/background/alignment from CellConfig overrides
- [x] 6.2 Column: same pattern as Row
- [x] 6.3 Card: same pattern
- [x] 6.4 Grid: add `implements TableConfig<Grid>` (Grid doesn't extend BaseComponent currently)
- [x] 6.5 Scroll: add `implements ElementConfig<Scroll>, TableConfig<Scroll>, CellConfig<Scroll>`
- [x] 6.6 Wrap: add `implements ElementConfig<Wrap>, TableConfig<Wrap>, CellConfig<Wrap>`
- [x] 6.7 Other components (Badge, Hud, Divider, SolimStack, SolimImage, Text, etc.): add `implements ElementConfig<SELF>` where they currently call ElementConfig static methods
- [x] 6.8 Button: add `implements ElementConfig<Button>, TableConfig<Button>` (Button has its own margin methods — evaluate if they conflict)

## 7. Update internal callers — static → mixin

- [x] 7.1 All `ElementConfig.width(element, v)` calls → `element.width(v)` (via mixin)
- [x] 7.2 All `ElementConfig.margin(table, v)` calls → `table.margin(v)` (via mixin)
- [x] 7.3 All `ElementConfig.top/bottom/left/right/center(table)` calls → `table.top()` etc. (via mixin)
- [x] 7.4 All `ElementConfig.gap(table, v)` calls → `table.gap(v)` (via mixin)
- [x] 7.5 Delete static ElementConfig class entirely

## 8. Verify

- [x] 8.1 Run `.\gradlew :solim-core:compileJava` — verify clean compile
- [x] 8.2 Run `.\gradlew :mod:compileJava` — verify mod compiles
- [x] 8.3 Run `.\gradlew :solim-core:test :mod:test` — verify all tests pass
- [x] 8.4 Search for any remaining `SizeConstraints` references — must be zero
- [x] 8.5 Search for any remaining static `ElementConfig.` calls — must be zero
