## 1. Localization

- [ ] 1.1 Add translation keys and descriptions for disabled ore reasons in `assets/bundles/bundle.properties` (`feature.autoplay.settings.mining.unit-cannot-mine`, `feature.autoplay.settings.mining.tier-too-low`, `feature.autoplay.settings.mining.no-wall-mining`, `feature.autoplay.settings.mining.no-floor-mining`)

## 2. Ore Filter & Unit Capability Logic

- [ ] 2.1 Implement helper methods to check map ore presence (`hasFloor`, `hasWall`, `onMap`) and evaluate unit mining capability with reason messages in `MiningTask.java`
- [ ] 2.2 Update `MiningTask.buildSettings` to filter displayed items by map presence and disable chips for unmineable items with tooltips
- [ ] 2.3 Verify fallback when outside active game session (`!Vars.state.isGame()`)

## 3. Verification

- [ ] 3.1 Verify Java 8 compatibility and Solim UI conventions
- [ ] 3.2 Run Gradle build and tests to verify clean compilation
