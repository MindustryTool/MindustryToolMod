# Tasks

## 1. Toggle Button Restyling

- [ ] 1.1 Replace `Styles.togglet` with `WebStyles.ghostText()` on all sort option buttons in `renderSortOptions()` <!-- id: 1.1 -->
- [ ] 1.2 Replace `Styles.togglet` with `WebStyles.ghostText()` on all planet toggle buttons in `renderPlanet()` <!-- id: 1.2 -->
- [ ] 1.3 Replace `Styles.togglet` with `WebStyles.ghostText()` on all tag toggle buttons in `renderTag()` <!-- id: 1.3 -->
- [ ] 1.4 Replace `Styles.togglet` with `WebStyles.ghostText()` on all block toggle buttons in `renderBlocks()` <!-- id: 1.4 -->
- [ ] 1.5 Replace `Styles.defaultb` with `WebStyles.dangerText()` on "Clear all filters" button <!-- id: 1.5 -->

## 2. Static Data Cache

- [ ] 2.1 Add `static Signal<List<TagCategory>> cachedTags` and `static boolean tagsLoaded` fields to `FilterContent` <!-- id: 2.1 -->
- [ ] 2.2 Add `static Signal<List<ModData>> cachedPlanets` and `static boolean planetsLoaded` fields to `FilterContent` <!-- id: 2.2 -->
- [ ] 2.3 Modify `fetchTags()` to check `tagsLoaded` flag — skip fetch if already loaded, set flag after fetch completes <!-- id: 2.3 -->
- [ ] 2.4 Modify `fetchPlanets()` to check `planetsLoaded` flag — skip fetch if already loaded, set flag after fetch completes <!-- id: 2.4 -->
- [ ] 2.5 Wire `renderTagCategories()` and `renderPlanets()` to use static cached signals instead of instance signals <!-- id: 2.5 -->

## 3. Dynamic Column Calculation

- [ ] 3.1 Add helper method `computeColumns(float availableWidth, float longestLabelWidth)` returning `max(1, floor(availableWidth / (longestLabelWidth + 3)))` clamped to reasonable range <!-- id: 3.1 -->
- [ ] 3.2 Replace fixed `tagColumns` computed with dynamic calculation using `computeColumns()` and longest tag label width <!-- id: 3.2 -->
- [ ] 3.3 Replace fixed sort layout with grid using `computeColumns()` and longest sort label width (~15u for "most-download") <!-- id: 3.3 -->
- [ ] 3.4 Replace fixed planet layout with grid using `computeColumns()` and longest planet label width <!-- id: 3.4 -->
- [ ] 3.5 Replace fixed block layout with grid using `computeColumns()` and longest block label width <!-- id: 3.5 -->
- [ ] 3.6 Remove `.growX()` from individual toggle buttons — let grid cells control width <!-- id: 3.6 -->

## 4. Compile and Test

- [ ] 4.1 Run `./gradlew :mod:compileJava` to verify clean compilation <!-- id: 4.1 -->
- [ ] 4.2 Run `./gradlew test` to verify full test suite passes <!-- id: 4.2 -->
