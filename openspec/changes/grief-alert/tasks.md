## 1. Rename Feature Package and Classes

- [ ] 1.1 Create `mod/src/mindustrytool/features/griefalert/GriefAlertFeature.java` with feature ID `"grief-alert"` and per-block setting key prefix `"mindustrytool.features.grief-alert.block."`
- [ ] 1.2 Create `mod/src/mindustrytool/features/griefalert/GriefAlertSettingsDialog.java` (`name("griefAlertSettingDialog")`) and `GriefAlertSettingsView.java` using `feature.grief-alert.*` bundle keys
- [ ] 1.3 Update `mod/src/mindustrytool/Main.java` to import and register `GriefAlertFeature` instead of `ReactorAlertFeature`, and remove `mod/src/mindustrytool/features/reactoralert/`

## 2. Target and Core Coordinates in Alert Message

- [ ] 2.1 In `GriefAlertFeature.onBuildBegin`, retain the nearest `CoreBuild` alongside the minimum distance and format `feature.grief-alert.alert` with builder name (`{0}`), block localized name (`{1}`), target tile coordinates (`{2}`, `{3}`), and nearest core tile coordinates (`{4}`, `{5}`)

## 3. Internationalization and Verification

- [ ] 3.1 Rename `feature.reactor-alert.*` keys to `feature.grief-alert.*` and update `feature.grief-alert.alert` across `assets/bundles/bundle.properties` and all localized `bundle_*.properties` files with translator comments
- [ ] 3.2 Run `./gradlew checkBundles compileJava test` to verify bundle keys and compilation
