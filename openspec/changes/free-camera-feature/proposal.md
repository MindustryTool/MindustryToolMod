## Why

Currently, Free Camera exists only as a boolean toggle in `ModSettings` and is buried inside General Settings and Virtual Joystick settings. Players cannot quickly toggle it on or off during live combat or construction, nor can they instantly recenter onto their unit. Furthermore, on desktop platforms, moving the unit with WASD always forcibly snaps the camera back to the player, preventing independent map scouting while maneuvering.

Promoting Free Camera into a dedicated, first-class feature (`FreeCameraFeature`) enabled by default and integrated into the QuickAccess HUD bar provides 1-tap toggling, instant recentering via long-press, desktop WASD decoupling, and coordinated behavior with existing features like Virtual Joystick and Autoplay.

## What Changes

- **New `FreeCameraFeature`**: Registers a dedicated feature (`id: "free-camera"`, order: 6, icon: `FileIcon.of("camera.png")`) with `enabledByDefault(true)` and `quickAccessByDefault(true)`.
- **QuickAccess HUD Integration**: Adds an active camera toggle button to the QuickAccess bar by default. Clicking toggles Free Camera; long-pressing snaps the camera directly onto the player unit.
- **Desktop Decoupling**: On Desktop platforms, moving the unit via WASD no longer forcibly snaps the camera to the player unit when Free Camera is active. The camera remains where panned, and Space/recenter hotkey snaps back when needed.
- **Virtual Joystick Coordination**: `JoystickMobileInput` checks `FreeCameraFeature.isEnabled()` to decide whether to auto-track the player unit. Double-tapping the joystick knob continues to snap the camera to the player unit.
- **Autoplay Coordination**: `AutoplayFeature` respects `FreeCameraFeature.isEnabled()`, bypassing forced camera lerping when Free Camera is enabled so the player can inspect the map while the bot operates.
- **Settings UI Cleanup**:
  - `JoystickSettingsView`: Retains the Free Camera checkbox as a convenient 2-way binding to `FreeCameraFeature.enabled()`.
  - `GeneralSettingsView`: Removes the redundant `freeCamera` checkbox row, centralizing feature management in `FeatureSettingsView`.
  - `ModSettings.freeCamera`: Deprecated / synchronized with `FreeCameraFeature.enabled()`.

## Capabilities

### New Capabilities
- `free-camera`: Dedicated Free Camera feature providing camera decoupling from unit movement (mobile and desktop), QuickAccess HUD tap-toggle and long-press snap actions, keybindings, and reactive enabled state.

### Modified Capabilities
- `virtual-joystick`: Delegates camera tracking and decoupling checks to `FreeCameraFeature` instead of reading `ModSettings.freeCamera`.
- `app-settings`: Removes `freeCamera` checkbox from `GeneralSettingsView` and updates `ModSettings.freeCamera` specification.
- `autonomous-gameplay-ai`: Skips camera lerp when `FreeCameraFeature` is active.

## Impact

- **Classes Added**: `FreeCameraFeature` in `mindustrytool.features.freecamera`.
- **Classes Modified**: `Main.java`, `JoystickMobileInput.java`, `JoystickSettingsView.java`, `GeneralSettingsView.java`, `AutoplayFeature.java`, `DesktopInput` / desktop movement handling.
- **Bundle Keys**: Adds `feature.free-camera.name`, `feature.free-camera.description`, `feature.free-camera.help`, `feature.free-camera.keybind.snap`, etc.
- **APIs**: Provides public `FreeCameraFeature.snapToPlayer()` and `FreeCameraFeature.isFreeCam()`.
