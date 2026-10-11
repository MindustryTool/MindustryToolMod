## 1. Feature Package & Class Renaming

- [ ] 1.1 Create new package `mindustrytool.features.blockstats` and move/rename `ProgressDisplayFeature` to `BlockStatsDisplayFeature`
- [ ] 1.2 Rename settings dialog to `BlockStatsDisplaySettingsDialog` and settings view to `BlockStatsDisplaySettingsView`
- [ ] 1.3 Update feature ID to `blockstats-display` and block setting prefix to `mindustrytool.features.blockstats-display.block.`
- [ ] 1.4 Update feature registration in `Main.java` and remove old `mindustrytool.features.progressdisplay` directory

## 2. Localization & Bundle Updates

- [ ] 2.1 Update `assets/bundles/bundle.properties` renaming all `feature.progress-display.*` keys to `feature.blockstats-display.*` with "Block Stats Display" title
- [ ] 2.2 Update other bundle properties files (`bundle_vi.properties`, `bundle_zh_CN.properties`, etc.) to match the renamed key prefix

## 3. Pre-cached Strings & Efficiency Overlay

- [ ] 3.1 Implement static pre-cached `PERCENT_STRINGS` array (`0%` through `300%`) in `BlockStatsDisplayFeature`
- [ ] 3.2 Update building drawer to calculate efficiency percentage clamped to `[0, 300]` and pick color (`Pal.power`/`Pal.accent`, `Color.orange`, `Pal.scarlet`)
- [ ] 3.3 Adjust text drawing layout to center the two-line stack vertically at `(build.x, build.y)` with countdown on top and efficiency % below

## 4. Verification & Testing

- [ ] 4.1 Compile the project with `./gradlew compileJava` to ensure zero compilation or syntax errors
- [ ] 4.2 Verify zero string allocations in the efficiency render path
