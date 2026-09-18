## 1. Feature Definition & Metadata

- [ ] 1.1 Create `FreeCameraFeature` extending `Feature` in package `mindustrytool.features.freecamera` with metadata (`id: "free-camera"`, icon: `camera.png`, order: 6, `enabledByDefault(true)`, `quickAccessByDefault(true)`).
- [ ] 1.2 Implement `snapToPlayer()` method and bindable hotkey on `FreeCameraFeature`.
- [ ] 1.3 Implement `onQuickAccessClick()` (toggling enabled) and `onQuickAccessLongClick()` (calling `snapToPlayer()`).
- [ ] 1.4 Register `FreeCameraFeature` in `Main.java` and expose helper accessor `FreeCameraFeature.isFreeCam()`.

## 2. Desktop Controls Decoupling

- [ ] 2.1 Implement desktop camera decoupling in `DesktopInput` handling (preventing WASD movement from snapping camera back to player while Free Camera is enabled).
- [ ] 2.2 Verify pan keys and mouse drag panning continue to work seamlessly on Desktop.

## 3. Feature Interactions & Settings Cleanup

- [ ] 3.1 Update `JoystickMobileInput.java` to check `FreeCameraFeature.isFreeCam()` instead of `ModSettings.freeCamera.get()`.
- [ ] 3.2 Update `JoystickSettingsView.java` to bind the Free Camera checkbox reactively to `FreeCameraFeature.get().enabled()`.
- [ ] 3.3 Update `AutoplayFeature.java` to suppress camera lerping when `FreeCameraFeature.isFreeCam()` is active.
- [ ] 3.4 Remove the redundant `freeCamera` checkbox row from `GeneralSettingsView.java`.
- [ ] 3.5 Deprecate and synchronize `ModSettings.freeCamera` with `FreeCameraFeature.get().enabled()`.

## 4. Localization & Verification

- [ ] 4.1 Add translation keys to `assets/bundles/bundle.properties` (`feature.free-camera.name`, `feature.free-camera.description`, `feature.free-camera.help`, etc.) with descriptive comments.
- [ ] 4.2 Add unit tests in `mod/src/test/java` verifying `FreeCameraFeature` metadata, default state, snapping behavior, and settings synchronization.
- [ ] 4.3 Run `./gradlew check` and tests to verify Java 8 compatibility and passing builds.
