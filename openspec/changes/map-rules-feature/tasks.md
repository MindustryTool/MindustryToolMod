## 1. Bundle Translations & Assets

- [ ] 1.1 Add all translation keys for map rules viewer, categories, rule names, descriptions, and permission status messages into `assets/bundles/bundle.properties` with explanatory comments.
- [ ] 1.2 Verify or provide icon asset for Map Rules feature in `assets/icons/`.

## 2. Feature Core & Permission Logic

- [ ] 2.1 Create `RuleEditMode` enum and authority resolver (`SINGLE_PLAYER`, `HOST`, `CLIENT_ADMIN`, `CLIENT_READONLY`).
- [ ] 2.2 Create `MapRulesFeature` extending `Feature` with `quickAccess(true)` and initial rule snapshot capture.
- [ ] 2.3 Implement Rule diff engine comparing current `Vars.state.rules` against default baseline `new Rules()`.
- [ ] 2.4 Implement rule mutation dispatcher (direct mutation, `Call.setRules` host sync, `/js` chat RPC for client admin, and local visual overrides).

## 3. Pure Solim UI Implementation

- [ ] 3.1 Create `MapRulesView` with header status badge, reactive search input, and category filter chips (`All`, `Modified Only`, `Multipliers`, `Waves`, `Bans`, `Environment`, `Mechanics`).
- [ ] 3.2 Implement category sections for Multipliers (build/unit/block speeds), Waves (wave spacing, win wave), Banned content (block/unit icons and tags), Environment (fog, lighting, borders), and Mechanics (reactors, fire, possession).
- [ ] 3.3 Create `MapRulesDialog` extending `SolimDialog` with constrained width, centered layout, and reset to defaults action.
- [ ] 3.4 Create `MapRulesSettingsDialog` and `MapRulesSettingsView` for feature settings.

## 4. Main Registration & Verification

- [ ] 4.1 Register `MapRulesFeature` in `FeatureManager.register(...)` within `mindustrytool.Main`.
- [ ] 4.2 Verify compilation and build via `./gradlew classes`.
- [ ] 4.3 Verify Java 8 compatibility, no forbidden post-Java 8 APIs, and Checkstyle compliance.
