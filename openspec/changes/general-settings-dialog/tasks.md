## 1. Configuration & Localization

- [ ] 1.1 Create ModSettings.java with persistent ConfigGroup and etaParticipate ConfigValue<Boolean>
- [ ] 1.2 Add localization bundle strings and comments in ssets/bundles/bundle.properties for general settings dialog and beta toggle

## 2. UI Components

- [ ] 2.1 Implement GeneralSettingsDialog.java extending SolimDialog with Solim declarative layout and beta checkbox binding
- [ ] 2.2 Update FeatureSettingDialog.java action bar to add a settings button opening GeneralSettingsDialog

## 3. Update Service Integration

- [ ] 3.1 Update UpdateService.java release retrieval and filtering to consider pre-releases when etaParticipate is enabled

## 4. Verification & Polish

- [ ] 4.1 Write unit tests for ModSettings config persistence and UpdateService release filtering logic
- [ ] 4.2 Verify build, Java 8 compatibility, Solim declarative rules, and i18n requirements
