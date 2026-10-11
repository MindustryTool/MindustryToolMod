## 1. Feature Core & Models

- [ ] 1.1 Create `ControlGroup` model managing group ID, unit IDs (`IntSeq`), and composition summary
- [ ] 1.2 Implement `ArmyControlFeature` extending `BaseFeature` with settings (display mode, max groups)
- [ ] 1.3 Add selection logic in `ArmyControlFeature` for map selection, screen selection, and unit type deselection
- [ ] 1.4 Implement control group save, recall, dead-unit pruning, and deletion logic

## 2. Localization & QuickAccess Registration

- [ ] 2.1 Add translation keys with descriptive comments to `assets/bundles/bundle.properties`
- [ ] 2.2 Register `ArmyControlFeature` into `QuickAccessFeature` and toolbar items

## 3. UI Components (Solim UI)

- [ ] 3.1 Build Active Selection Bar with interactive unit type chips (tap to deselect/exclude)
- [ ] 3.2 Build Control Group item rows/pills (recall, create/assign, delete)
- [ ] 3.3 Build `ArmyControlPopup` anchored to the QuickAccess button with persistent dismissal logic
- [ ] 3.4 Build `ArmyControlHudView` for optional persistent HUD mode
- [ ] 3.5 Build `ArmyControlSettingsView` and `ArmyControlSettingsDialog` following setting conventions

## 4. Testing & Verification

- [ ] 4.1 Write unit tests for `ControlGroup` and `ArmyControlFeature` selection / pruning logic
- [ ] 4.2 Write Solim UI component tests for the Active Selection Bar and chip deselection
- [ ] 4.3 Verify build passes (`./gradlew test` and `./gradlew build`)
