## 1. Feature Definition & Registration

- [ ] 1.1 Create `DisplayGroupFeature` in `mindustrytool.features.displaygroup` with metadata (`id="display-group"`, `Icon.eye`, `order=4`, `quickAccessByDefault=true`) and config values (`colsConfig`, `displayOrderConfig`, `hiddenFeaturesConfig`, `shownFeaturesConfig`)
- [ ] 1.2 Register `DisplayGroupFeature` in `Main.java`
- [ ] 1.3 Add translation keys and descriptive comments for `display-group` in `assets/bundles/bundle.properties`

## 2. Popup UI & Reactive Controls

- [ ] 2.1 Create `DisplayGroupPopup` anchored to QuickAccess bar via `QuickAccessPopupHelper`
- [ ] 2.2 Implement `DisplayGroupPopupView` containing the reactive grid of display feature buttons (`HealthBar`, `RangeDisplay`, `Pathfinding`, `ProgressDisplay`, `TeamResource`, `Visualizer`, `WavePreview`, `ToggleRendering`) with active state reactive highlight, click toggle, and long-press settings delegation
- [ ] 2.3 Connect `DisplayGroupFeature.onQuickAccessClick` to toggle `DisplayGroupPopup` and `onQuickAccessLongClick` to open `DisplayGroupSettingsDialog`

## 3. Settings Dialog

- [ ] 3.1 Implement `DisplayGroupSettingsView` with column count slider (range 1–8, default 4), item reordering, item visibility checkboxes, and reset to defaults action
- [ ] 3.2 Implement `DisplayGroupSettingsDialog` extending `SolimDialog`

## 4. QuickAccess HUD De-cluttering & Verification

- [ ] 4.1 Set `quickAccess(false)` on `HealthBarFeature`, `RangeDisplayFeature`, `PathfindingFeature`, `ProgressDisplayFeature`, `TeamResourceFeature`, `VisualizerFeature`, `WavePreviewFeature`, and `ToggleRenderingFeature`
- [ ] 4.2 Run `./gradlew checkBundles test` and update any affected tests (such as QuickAccess default item list tests)
