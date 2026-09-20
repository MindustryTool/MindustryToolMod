## MODIFIED Requirements

### Requirement: Desktop Free Camera and Joystick Integration
`ModDesktopInput` SHALL delegate to `FreeCameraFeature` and `JoystickFeature` when active, reproduce stock detach-camera unit behavior when the mod's free camera is inactive but vanilla `detach-camera` is enabled, and fall back to vanilla `DesktopInput` behavior otherwise.

#### Scenario: Free camera active on desktop
- **WHEN** `FreeCameraFeature.isFreeCam()` is true
- **THEN** WASD unit movement does not snap or lerp the camera back to the player unit

#### Scenario: Free camera disabled on desktop
- **WHEN** `FreeCameraFeature.isFreeCam()` is false
- **THEN** WASD unit movement resets camera panning and centers on the player unit according to vanilla logic

#### Scenario: Stock detach-camera honored when mod free camera is off
- **WHEN** `FreeCameraFeature.isFreeCam()` is false and vanilla `detach-camera` is enabled
- **THEN** the player unit moves toward `Core.camera.position` and stops within 15f, matching stock vanilla behavior

#### Scenario: Joystick active on desktop
- **WHEN** `JoystickFeature` has an active non-zero movement vector
- **THEN** the joystick vector is blended with the keyboard axis movement
