## 1. Remove Cooldown State and Logic

- [x] 1.1 Remove `overrideCooldown` field and its `config.floatValue("override-cooldown", 2.0f)` registration in `AutoplayFeature.java`
- [x] 1.2 Remove `resumeTime` field, the cooldown-arming lines in the input block, and the entire `if (Time.time < resumeTime)` wait block in `AutoplayFeature.update()`, keeping instant yield (controller reset + state cleanup)
- [x] 1.3 Remove the now-unused `arc.util.Time` import in `AutoplayFeature.java` if no other usage remains

## 2. Remove Cooldown Settings UI and Strings

- [x] 2.1 Remove the cooldown label and slider from `globalSection()` in `AutoplaySettingsView.java`, keeping the follow-unit checkbox; remove the now-unused `Strings` import if applicable
- [x] 2.2 Remove `feature.autoplay.settings.override-cooldown` and `feature.autoplay.settings.override-cooldown.description` (plus their comment lines) from `assets/bundles/bundle.properties`

## 3. Verification

- [x] 3.1 Remove the `overrideCooldown` default assertion in `AutoplayFeatureTest.configDefaults`
- [x] 3.2 Grep for remaining `overrideCooldown` / `override-cooldown` / `resumeTime` references and confirm zero hits outside the archived change
- [x] 3.3 Run `./gradlew test` and `:mod:checkstyleMain` to ensure all tests pass with no regressions
