# unified-input-handling Specification

## Purpose
TBD - created by archiving change unified-input-handling. Update Purpose after archive.
## Requirements
### Requirement: Persistent Mod Input Handlers
The system SHALL install and maintain custom input handlers (`ModDesktopInput` on desktop and `ModMobileInput` on mobile) as the active `Vars.control.input` without requiring dynamic runtime swap or restore cycles when features are toggled.

#### Scenario: Input installed on client load
- **WHEN** the game fires `ClientLoadEvent`
- **THEN** `Vars.control.input` is wrapped with `ModDesktopInput` (on desktop) or `ModMobileInput` (on mobile)

#### Scenario: Input verified on world load
- **WHEN** the game fires `WorldLoadEvent`
- **THEN** if `Vars.control.input` is not an instance of `ModDesktopInput` or `ModMobileInput`, it is immediately wrapped with the appropriate mod input handler

### Requirement: Desktop Free Camera and Joystick Integration
`ModDesktopInput` SHALL delegate to `FreeCameraFeature` and `JoystickFeature` when active, and fall back to vanilla `DesktopInput` behavior when inactive.

#### Scenario: Free camera active on desktop
- **WHEN** `FreeCameraFeature.isFreeCam()` is true
- **THEN** WASD unit movement does not snap or lerp the camera back to the player unit

#### Scenario: Free camera disabled on desktop
- **WHEN** `FreeCameraFeature.isFreeCam()` is false
- **THEN** WASD unit movement resets camera panning and centers on the player unit according to vanilla logic

#### Scenario: Joystick active on desktop
- **WHEN** `JoystickFeature` has an active non-zero movement vector
- **THEN** the joystick vector is blended with the keyboard axis movement

### Requirement: Mobile Free Camera and Joystick Integration
`ModMobileInput` SHALL drive unit movement via `JoystickFeature` when active, suppress camera lerping when `FreeCameraFeature` is active, and fall back to 100% vanilla `MobileInput` behavior when both features are disabled.

#### Scenario: Joystick active on mobile
- **WHEN** `JoystickFeature` is enabled
- **THEN** player unit movement is driven by the virtual joystick vector and does not follow the camera center

#### Scenario: Joystick inactive on mobile
- **WHEN** `JoystickFeature` is disabled
- **THEN** player unit movement targets `Core.camera.position` matching vanilla mobile behavior

#### Scenario: Free camera disabled on mobile
- **WHEN** `FreeCameraFeature.isFreeCam()` is false and the player is not actively panning
- **THEN** the camera smoothly lerps toward the player unit

### Requirement: Feature Decoupling and State Cleanup
Neither `FreeCameraFeature` nor `JoystickFeature` SHALL track original input handlers or invoke `Vars.control.setInput()` during their enable, disable, or toggle lifecycles.

#### Scenario: Toggling features does not swap input
- **WHEN** `FreeCameraFeature` or `JoystickFeature` is enabled or disabled
- **THEN** `Vars.control.input` remains the exact same instance and active touch or key input is not dropped

