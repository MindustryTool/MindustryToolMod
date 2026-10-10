## 1. Feature Architecture & Configuration

- [ ] 1.1 Create `QuickRotateFeature` class in `mindustrytool.features.quickrotate` implementing `Feature`
- [ ] 1.2 Add configuration toggles and default settings for `QuickRotateFeature`
- [ ] 1.3 Register `QuickRotateFeature` in `FeatureManager`

## 2. In-World Floating Overlay Component

- [ ] 2.1 Create `QuickRotateOverlayView` using Solim declarative components (`row()`, `button()`, `WebStyles.ghost()`)
- [ ] 2.2 Add Step-Rotate buttons for counter-clockwise (`↺`) and clockwise (`↻`) actions with tooltips
- [ ] 2.3 Implement world-to-screen coordinate projection and dynamic camera anchoring for the overlay

## 3. Interaction & Networking

- [ ] 3.1 Hook into tap and selection events to track the target rotatable building on the player's team
- [ ] 3.2 Dispatch multiplayer-safe rotation via `Call.rotateBlock(player, building, direction)`
- [ ] 3.3 Implement auto-dismissal on deselect, clicking empty space, or target building destruction

## 4. Localization & Settings UI

- [ ] 4.1 Add translation keys with descriptive comments to `assets/bundles/bundle.properties`
- [ ] 4.2 Create `QuickRotateSettingsView` / dialog for configuring the feature
- [ ] 4.3 Verify bundle integrity using the Gradle check task
