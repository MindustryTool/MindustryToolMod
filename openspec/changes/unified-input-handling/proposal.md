## Why

Currently, features requiring custom input behaviors (`FreeCameraFeature` and `JoystickFeature`) dynamically swap and restore `Vars.control.input` at runtime. This causes several issues:
1. **Fragile State Management**: Both features maintain local references to the original input handler and must coordinate who restores what when one feature is toggled off while the other remains active.
2. **Gameplay Glitches**: Calling `Vars.control.setInput()` during active gameplay unregisters and re-registers input processors (`Core.input.removeProcessor()`), dropping active touch gestures, key chords, and drag operations.
3. **Tight Architectural Coupling**: `FreeCameraFeature` directly depends on `JoystickDesktopInput` and `JoystickFeature`, violating modular feature separation.

By replacing `Vars.control.input` permanently with unified `ModDesktopInput` and `ModMobileInput` handlers, we eliminate input swaps, prevent touch/key drop glitches, and decouple feature implementations.

## What Changes

- **Add Unified Input Handlers**: Create `ModDesktopInput` (extending `DesktopInput`) and `ModMobileInput` (extending `MobileInput`) in a dedicated package `mindustrytool.input`.
- **Add ModInputManager**: Create an input manager that installs `ModDesktopInput` or `ModMobileInput` during `ClientLoadEvent` and `WorldLoadEvent`, re-wrapping if Mindustry settings switch between mouse and touch control.
- **Remove Dynamic Swapping in JoystickFeature**: Remove `swapInput()`, `restoreInput()`, and `originalInput` tracking from `JoystickFeature`. The feature simply updates its `moveVector`.
- **Remove Input Swapping in FreeCameraFeature**: Remove `ensureDesktopInput()`, `originalDesktopInput`, and direct dependencies on `Joystick*` classes from `FreeCameraFeature`.
- **Faithful Vanilla Fallback**: Ensure that when features are disabled, `ModDesktopInput` and `ModMobileInput` pass through to 100% vanilla behavior (including falling back to `Core.camera.position` for target tracking on mobile).
- **Deprecate / Remove Old Handlers**: Replace `JoystickDesktopInput` and `JoystickMobileInput` with the new unified classes.

## Capabilities

### New Capabilities
- `unified-input-handling`: Persistent input interception layer that routes movement, camera, and gestures to active features with zero-glitch passive fallback to vanilla behavior.

### Modified Capabilities
<!-- No requirement changes to existing functional user-facing specs; existing virtual-joystick and free-camera user behaviors are preserved. -->

## Impact

- **Packages Affected**:
  - `mindustrytool.input` (new package containing `ModDesktopInput`, `ModMobileInput`, `ModInputManager`)
  - `mindustrytool.features.joystick` (removal of input swap state machine and `JoystickDesktopInput`/`JoystickMobileInput`)
  - `mindustrytool.features.freecamera` (removal of input swap state machine and joystick coupling)
  - `mindustrytool.Main` (registration/initialization of `ModInputManager`)
- **Compatibility**: No breaking changes to Mindustry APIs or user settings. Operates seamlessly on both Desktop and Mobile (Android).
