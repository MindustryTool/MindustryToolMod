## 1. ModSettings & Core Utilities

- [ ] 1.1 Add `universalScale` configuration value with mobile-aware default (`Vars.mobile ? 0.8f : 1.0f`) in `ModSettings.java`
- [ ] 1.2 Implement reactive `effectiveScale(Readable<Float>)` and non-reactive `effectiveScale(float)` helper methods in `ModSettings.java`
- [ ] 1.3 Add unit tests verifying `universalScale` defaults and `effectiveScale` calculations in `mod/src/test/java`

## 2. General Settings UI & Localization

- [ ] 2.1 Add translation keys and translator comments for universal scale in `assets/bundles/bundle.properties`
- [ ] 2.2 Add universal scale slider and percentage label row in `GeneralSettingsView.java`
- [ ] 2.3 Run `:checkBundles` to ensure localization bundle integrity

## 3. Connect HUD Views & Indicators

- [ ] 3.1 Update `AutoplayHudView.java` to use `ModSettings.effectiveScale(feature.scaleConfig.signal())`
- [ ] 3.2 Update `GodModeHudView.java` to use `ModSettings.effectiveScale(feature.scaleConfig.signal())`
- [ ] 3.3 Update `QuickAccessHudView.java` to use `ModSettings.effectiveScale(feature.scaleConfig.signal())`
- [ ] 3.4 Update `TeamResourceHudView.java` to use `ModSettings.effectiveScale(feature.scaleConfig.signal())`
- [ ] 3.5 Update `TimeControlHudView.java` to use `ModSettings.effectiveScale(feature.scaleConfig.signal())`
- [ ] 3.6 Update `WavePreviewPanelView.java` to use `ModSettings.effectiveScale(feature.scaleConfig.signal())`
- [ ] 3.7 Update `HealthBarFeature.java` to use `ModSettings.effectiveScale(...)` for in-world bar scaling
- [ ] 3.8 Update `ProgressDisplayFeature.java` to use `ModSettings.effectiveScale(...)` for in-world bar scaling

## 4. Verification

- [ ] 4.1 Run Gradle test suite (`./gradlew test`) to verify existing and new tests pass
