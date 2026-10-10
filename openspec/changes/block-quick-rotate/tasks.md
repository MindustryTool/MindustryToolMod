## 1. Feature Architecture & Configuration

- [x] 1.1 Create `QuickRotateFeature` class in `mindustrytool.features.quickrotate` implementing `Feature`
- [x] 1.2 Add configuration toggles and default settings for `QuickRotateFeature`
- [x] 1.3 Register `QuickRotateFeature` in `FeatureManager`

## 2. In-World Floating Overlay Component

- [x] 2.1 Create `QuickRotateOverlayView` building a raw Arc `Table` floating menu (SmartDrill/SmartUpgrade pattern) with Lucide icon buttons
- [x] 2.2 Add Step-Rotate buttons for counter-clockwise (`↺`) and clockwise (`↻`) actions with tooltips
- [x] 2.3 Implement world-to-screen coordinate projection and dynamic camera anchoring for the overlay

## 3. Interaction & Networking

- [x] 3.1 Hook into tap and selection events to track the target rotatable building on the player's team
- [x] 3.2 Dispatch multiplayer-safe rotation via `Call.rotateBlock(player, building, direction)`
- [x] 3.3 Implement auto-dismissal on deselect, clicking empty space, or target building destruction

## 4. Localization & Settings UI

- [x] 4.1 Add translation keys with descriptive comments to `assets/bundles/bundle.properties`
- [x] 4.2 Create `QuickRotateSettingsView` / dialog for configuring the feature
- [x] 4.3 Verify bundle integrity using the Gradle check task
