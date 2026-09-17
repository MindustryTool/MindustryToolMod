## 1. SolimCollapser Implementation & Tests

- [x] 1.1 Implement `SolimCollapser` in `solim-core/src/solim/layout/SolimCollapser.java` wrapping Arc `Collapser` with reactive `expanded`/`collapsed` bindings and `duration`
- [x] 1.2 Expose `collapser()` helper overloads in `solim/src/solim/UI.java`
- [x] 1.3 Create behavioral unit tests in `solim-core/src/test/java/solim/layout/SolimCollapserTest.java` verifying zero prefHeight when collapsed, reactive transitions, and clean disposal

## 2. Repair Task Behavioral Fixes

- [ ] 2.1 Update `RepairTask.java` to remove `canBuild()` as a healing capability so units without heal weapons or repair fields yield immediately
- [ ] 2.2 Update `RepairAI` in `RepairTask.java` to support passive aura healing for `RepairFieldAbility` units without heal weapons, suppressing non-healing weapon firing
- [ ] 2.3 Update `AutoplayFeatureTest.java` to verify `RepairTask` yields when player unit lacks healing weapons or abilities

## 3. Autoplay Settings View UI Overhaul

- [ ] 3.1 Wrap each `TaskRow` in a rounded `Card` with `WebStyles.Colors.SECTION_BORDER` and `WebStyles.Colors.SECTION_BG`
- [ ] 3.2 Restructure `TaskRow` into a multi-row column layout with action buttons in row 1 and status text in row 2 to eliminate overlap
- [ ] 3.3 Replace `.visible(expanded)` in task settings with `collapser(expanded, ...)` to eliminate empty whitespace when collapsed

## 4. Verification & Polish

- [ ] 4.1 Run `./gradlew test` across all subprojects to ensure all tests pass
- [ ] 4.2 Verify compliance with `AGENTS.md` rules (Java 8 APIs, clean imports, no `signal.get()` in build, module isolation)
