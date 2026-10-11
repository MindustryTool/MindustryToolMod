## 1. Feature Core & Models

- [x] 1.1 Create `ControlGroup` model managing group ID, unit IDs (`IntSeq`), and composition summary
- [x] 1.2 Implement `ArmyControlFeature` extending `BaseFeature` with settings (display mode, max groups)
- [x] 1.3 Add selection logic in `ArmyControlFeature` for map selection, screen selection, and unit type deselection
- [x] 1.4 Implement control group save, recall, dead-unit pruning, and deletion logic

## 2. Localization & QuickAccess Registration

- [x] 2.1 Add translation keys with descriptive comments to `assets/bundles/bundle.properties`
- [x] 2.2 Register `ArmyControlFeature` into `QuickAccessFeature` and toolbar items

## 3. UI Components (Solim UI)

- [x] 3.1 Build Active Selection Bar with interactive unit type chips (tap to deselect/exclude)
- [x] 3.2 Build Control Group item rows/pills (recall, create/assign, delete)
- [x] 3.3 Build `ArmyControlPopup` anchored to the QuickAccess button with persistent dismissal logic
- [x] 3.4 Build `ArmyControlHudView` for optional persistent HUD mode
- [x] 3.5 Build `ArmyControlSettingsView` and `ArmyControlSettingsDialog` following setting conventions

## 4. Testing & Verification

- [x] 4.1 Write unit tests for `ControlGroup` and `ArmyControlFeature` selection / pruning logic
- [x] 4.2 Write Solim UI component tests for the Active Selection Bar and chip deselection
- [x] 4.3 Verify build passes (`./gradlew test` and `./gradlew build`)
