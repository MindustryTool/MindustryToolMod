## 1. Core Framework

- [ ] 1.1 Create `FeatureKeybind` descriptor class in `mindustrytool.features` holding `KeyBind`, `Runnable action`, and `boolean requireEnabled`
- [ ] 1.2 Add keybind registration helper methods (`bindToggle`, `bindAction`, `bindDialog`, `getKeybinds`) to `Feature`
- [ ] 1.3 Create `FeatureKeybindManager` with centralized `Trigger.update` listener, `!Core.scene.hasField()` guard, and `requireEnabled` checks
- [ ] 1.4 Wire `FeatureKeybindManager.init()` in `FeatureManager.init()`

## 2. Feature Migration & Adoption

- [ ] 2.1 Migrate `SchematicBrowserFeature` to use `bindDialog` and remove its manual `Events.run(Trigger.update)` loop
- [ ] 2.2 Migrate `MapBrowserFeature` to use `bindDialog` and remove its manual `Events.run(Trigger.update)` loop
- [ ] 2.3 Add toggle keybind to `AutoplayFeature` using `bindToggle`
- [ ] 2.4 Add overlay toggle keybind to `ChatFeature` using `bindAction`
- [ ] 2.5 Add pause, speed up, and speed down keybinds to `TimeControlFeature` using `bindAction`

## 3. Localization & Verification

- [ ] 3.1 Add translatable section name and keybind descriptions to `assets/bundles/bundle.properties` with required comments
- [ ] 3.2 Write behavioral unit tests for `FeatureKeybind`, `FeatureKeybindManager`, precondition enforcement, and focus suppression
- [ ] 3.3 Verify build and tests pass via `./gradlew test`
