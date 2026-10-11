## 1. Configuration & Core Classification

- [ ] 1.1 Add new ConfigValue definitions to `RangeDisplayFeature`: `drawOverdriveRangeConfig`, `drawMenderRangeConfig`, `drawPointDefenseRangeConfig`, and `useTeamColorForSpecialConfig`.
- [ ] 1.2 Wire signal subscriptions and frame-cached fields for the new configs in `RangeDisplayFeature`.
- [ ] 1.3 Implement helper methods in `RangeDisplayFeature` to classify blocks: `isOverdriveBlock(Block)`, `isMenderBlock(Block)`, and `isPointDefenseBlock(Block)`.

## 2. Rendering & Color Logic

- [ ] 2.1 Update `drawBuildingRange` in `RangeDisplayFeature` to filter out blocks when their respective category toggle (`draw-overdrive-range`, `draw-mender-range`, `draw-point-defense-range`) is disabled.
- [ ] 2.2 Update color selection logic in `drawBuildingRange`: assign Orange (`Pal.accent`) for overdrives, Green (`Pal.heal`) for menders, White (`Color.white`) for point defense, falling back to `build.team.color` if `useTeamColorForSpecial` is true, while preserving `Color.gray` for turrets unable to shoot.

## 3. UI & Localization

- [ ] 3.1 Add new user-facing translation keys with descriptive comments to `assets/bundles/bundle.properties`.
- [ ] 3.2 Update `RangeDisplaySettingsView` to add checkboxes for Overdrive range, Mender range, Point Defense range, and Team Color override in the settings cards.
- [ ] 3.3 Update `RangeDisplayFeature.resetToDefaults()` to reset the new config values.

## 4. Verification & Testing

- [ ] 4.1 Update or add unit tests in `RangeDisplayFeatureTest` verifying default values, config subscriptions, block classification, and color resolution.
- [ ] 4.2 Run test suite `./gradlew test` to ensure all tests pass cleanly.
