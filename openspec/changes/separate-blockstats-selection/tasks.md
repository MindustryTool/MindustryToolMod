## 1. Feature Core & BitSets

- [ ] 1.1 Implement separate BitSets (`progressBlockEnabled` and `efficiencyBlockEnabled`) and config key resolvers in `BlockStatsDisplayFeature`
- [ ] 1.2 Implement helper queries `isProgressEligible(Block)` and `isEfficiencyEligible(Block)` covering discrete production blocks, drills, pumps, and power generators
- [ ] 1.3 Update countdown time calculation in `drawStats` for `GenericCrafter`, `Separator`, `UnitFactory`, `Reconstructor`, `UnitAssembler`, `Constructor`/`BlockProducer`, and `PayloadDeconstructor`
- [ ] 1.4 Update efficiency calculation in `drawStats` for all eligible buildings (including drills, pumps, and power generators)
- [ ] 1.5 Implement adaptive layout positioning: dual-line stacked when both active, single-line centered when only progress or only efficiency active

## 2. Localization

- [ ] 2.1 Add bundle keys and comments in `assets/bundles/bundle.properties` for the Progress section, Efficiency section, and category filter chips (All, Crafters, Units, Drills, Pumps, Power)

## 3. Settings View UI

- [ ] 3.1 Refactor `BlockStatsDisplaySettingsView` to render two consecutive cards: Progress Display Blocks section and Efficiency Display Blocks section
- [ ] 3.2 Add category filter chips and select-all/deselect-all controls for both sections
- [ ] 3.3 Bind block toggles in each section to their respective `progressBlockEnabled` and `efficiencyBlockEnabled` settings

## 4. Verification & Testing

- [ ] 4.1 Run Gradle build and tests to verify clean compilation and absence of regressions
- [ ] 4.2 Verify zero-allocation performance and accurate rendering across test scenarios
