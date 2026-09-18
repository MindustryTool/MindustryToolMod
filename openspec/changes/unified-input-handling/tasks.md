## 1. Unified Input Infrastructure

- [x] 1.1 Create `mindustrytool.input.ModDesktopInput` extending `DesktopInput` that queries `FreeCameraFeature.isFreeCam()` and `JoystickFeature` move vectors.
- [x] 1.2 Create `mindustrytool.input.ModMobileInput` extending `MobileInput` that supports joystick steering, gesture listening, and FreeCamera camera lerp suppression.
- [x] 1.3 Implement faithful vanilla fallback in `ModMobileInput` (targeting `Core.camera.position` when joystick is inactive).
- [x] 1.4 Create `mindustrytool.input.ModInputManager` to install custom input handlers on `ClientLoadEvent` and re-verify on `WorldLoadEvent`.

## 2. Feature Decoupling & Cleanup

- [x] 2.1 Remove `ensureDesktopInput()`, `originalDesktopInput`, and `Joystick*` dependencies from `FreeCameraFeature.java`.
- [x] 2.2 Remove `swapInput()`, `restoreInput()`, and `originalInput` from `JoystickFeature.java`.
- [x] 2.3 Delete legacy `JoystickDesktopInput.java` and `JoystickMobileInput.java` from `mindustrytool.features.joystick`.
- [x] 2.4 Initialize `ModInputManager` in `Main.java`.

## 3. Verification & Testing

- [x] 3.1 Update existing tests in `FreeCameraFeatureTest.java`.
- [x] 3.2 Add unit tests in `mod/src/test/java/mindustrytool/input/` verifying `ModInputManager`, `ModDesktopInput`, and fallback behavior.
- [x] 3.3 Run `./gradlew check` to ensure all tests pass and Checkstyle passes cleanly.
