## 1. Reusable Setting UI Components

- [x] 1.1 Create `SettingCard` component in `mindustrytool.components` supporting standard card styling (`padding(unit(3))`, `gap(unit(3))`, `rounded(unit(2))`, `border(1f, Colors.SECTION_BORDER)`), `SettingCard.header(...)`, and `SettingCard.section(...)`
- [x] 1.2 Create `SettingSlider` component in `mindustrytool.components` providing above-track label and clean background-less value readout with full-width `.growX()` slider track
- [x] 1.3 Create `SettingScaffold` component in `mindustrytool.components` providing top-aligned Y, centered X, `maxWidth(500f)`, and `gap(unit(6))` exponential inter-card spacing

## 2. Guideline Enforcement in AGENTS.md

- [x] 2.1 Update `AGENTS.md` settings dialog conventions with the mandatory top-alignment, 1.5/3/6 exponential spacing scale, header card format, section card format, and above-track background-less slider rules for all future setting UIs

## 3. Migration - Core & Tactical Views

- [x] 3.1 Migrate `ScreenshotSettingsView` and `ScreenshotSettingsDialog` to use `SettingScaffold`, `SettingCard`, and `SettingSlider`
- [x] 3.2 Migrate `SmartUpgradeSettingsView` and `SmartUpgradeSettingsDialog`
- [x] 3.3 Migrate `SmartDrillSettingsView` and `SmartDrillSettingsDialog`
- [x] 3.4 Migrate `RangeDisplaySettingsView` and `RangeDisplaySettingsDialog`
- [x] 3.5 Migrate `VisualizerSettingsView` and `VisualizerSettingsDialog`

## 4. Migration - HUD & Simulation Views

- [x] 4.1 Migrate `HealthBarSettingsView` and `HealthBarSettingsDialog`
- [x] 4.2 Migrate `ProgressDisplaySettingsView` and `ProgressDisplaySettingsDialog`
- [x] 4.3 Migrate `TeamResourceSettingsView` and `TeamResourceSettingsDialog`
- [x] 4.4 Migrate `WavePreviewSettingsView` and `WavePreviewSettingsDialog`
- [x] 4.5 Migrate `TimeControlSettingsView` and `TimeControlSettingsDialog`

## 5. Migration - Graphics, Input & Navigation Views

- [x] 5.1 Migrate `ToggleRenderingSettingsView` and `ToggleRenderingSettingsDialog`
- [x] 5.2 Migrate `CameraZoomSettingsView` and `CameraZoomSettingsDialog`
- [x] 5.3 Migrate `JoystickSettingsView` and `JoystickSettingsDialog`
- [x] 5.4 Migrate `QuickRotateSettingsView` and `QuickRotateSettingsDialog`
- [x] 5.5 Migrate `QuickSchematicGridSettingsView` and `QuickSchematicGridSettingsDialog`

## 6. Migration - Communication & Utility Views

- [x] 6.1 Migrate `ChatSettingsView` and `ChatSettingsDialog`
- [x] 6.2 Migrate `PrettyChatSettingsView` and `PrettyChatSettingsDialog`
- [x] 6.3 Migrate `TranslationSettingsView` and `TranslationSettingsDialog`
- [x] 6.4 Migrate `GodModeSettingsView` and `GodModeSettingsDialog`
- [x] 6.5 Migrate `GriefAlertSettingsView` and `GriefAlertSettingsDialog`
- [x] 6.6 Migrate `PathfindingSettingsView` and `PathfindingSettingsDialog`
- [x] 6.7 Migrate `MapRulesSettingsView` and `MapRulesSettingsDialog`
- [x] 6.8 Migrate `BackgroundSettingsView` and `BackgroundSettingsDialog`
- [x] 6.9 Migrate `AutoplaySettingsView` and `AutoplaySettingsDialog`
- [x] 6.10 Migrate `GeneralSettingsView` and `FeatureSettingsView`

## 7. Verification

- [x] 7.1 Verify compilation and Solim test suite with `./gradlew test`
- [x] 7.2 Verify localization bundle checks with `./gradlew checkBundles`
