## 1. Enhance Wrap Component

- [x] 1.1 Add `LayoutModifiers<Wrap>` interface, `SizeConstraints constraints` field, and `sizeConstraints()` method to `Wrap.java` <!-- id: 1.1 -->
- [x] 1.2 Add `children(Runnable)` method using `ParentStack.push/pop/attachToParent` with a simple ATTACHER that adds cells without forced row breaks <!-- id: 1.2 -->
- [x] 1.3 Add `padding(float)` and `padding(float, float, float, float)` methods delegating to `ElementModifiers.padding(table, ...)` <!-- id: 1.3 -->
- [x] 1.4 Add `background(Drawable)` method delegating to `table.background(bg)` <!-- id: 1.4 -->
- [x] 1.5 Preserve existing `add(Element)` method for backward compatibility <!-- id: 1.5 -->
- [x] 1.6 Compile `solim-core` to verify Wrap enhancement <!-- id: 1.6 -->

## 2. Integrate Wrap in BrowserFilterDialog

- [x] 2.1 Add `chipWidth(String label)` static helper using `GlyphLayout.setText(Fonts.def, label).width + 12f` <!-- id: 2.1 -->
- [x] 2.2 Replace `grid(columns)` in `renderSortOptions()` with `wrap().gap(unit(1)).children(...)` and set `.width(chipWidth(name))` on each button <!-- id: 2.2 -->
- [x] 2.3 Replace `grid(columns)` in `renderPlanets()` with `wrap()` and per-item width <!-- id: 2.3 -->
- [x] 2.4 Replace `grid(columns)` in `renderCategory()` (tag grid) with `wrap()` and per-item width <!-- id: 2.4 -->
- [x] 2.5 Replace `grid(columns)` in `renderBlocks()` with `wrap()` and per-item width <!-- id: 2.5 -->
- [x] 2.6 Remove `computeColumns()` helper method (no longer needed) <!-- id: 2.6 -->
- [x] 2.7 Remove unused `dvw(95f).map(...)` column computations from all render methods <!-- id: 2.7 -->

## 3. Compile and Test

- [x] 3.1 Run `./gradlew :mod:compileJava` to verify clean compilation <!-- id: 3.1 -->
- [x] 3.2 Run `./gradlew test` to verify full test suite passes <!-- id: 3.2 -->
