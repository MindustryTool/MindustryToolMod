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

### Requirement: Mobile Free Camera and Joystick Integration
`ModMobileInput` SHALL drive unit movement via `JoystickFeature` when active, smoothly follow the player unit only during idle gameplay while `JoystickFeature` is active and `FreeCameraFeature` is inactive, and fall back to 100% vanilla `MobileInput` behavior without camera auto-following when `JoystickFeature` is disabled.

#### Scenario: Joystick active on mobile
- **WHEN** `JoystickFeature` is enabled
- **THEN** player unit movement is driven by the virtual joystick vector and does not follow the camera center

#### Scenario: Joystick inactive on mobile
- **WHEN** `JoystickFeature` is disabled
- **THEN** player unit movement targets `Core.camera.position` matching vanilla mobile behavior, and the camera does not automatically lerp toward the player unit

#### Scenario: Camera follow active only during idle with joystick
- **WHEN** `JoystickFeature` is enabled, `FreeCameraFeature.isFreeCam()` is false, the player is not actively panning, not in line placement mode (`!lineMode`), not shifting plans (`!selecting`), and not in placement mode (`mode == none`)
- **THEN** the camera smoothly lerps toward the player unit

#### Scenario: Camera follow suppressed during building with joystick
- **WHEN** `JoystickFeature` is enabled and the player is in line mode (`lineMode`), shifting plans (`selecting`), or in an active placement mode (`mode != none`)
- **THEN** the camera does not auto-lerp toward the player unit, keeping screen coordinates stable

### Requirement: Mobile Line Placement Gesture Timing
`ModMobileInput` SHALL configure its gesture detector with a 0.3-second long-press threshold matching vanilla Mindustry to support responsive dragging of block and conveyor lines.

#### Scenario: Long-press triggers line mode
- **WHEN** the player touches down on a buildable tile and holds without moving beyond the tap square for 0.3 seconds
- **THEN** `longPress` triggers, activating `lineMode` and allowing the player to drag a connected placement line

### Requirement: Feature Decoupling and State Cleanup
Neither `FreeCameraFeature` nor `JoystickFeature` SHALL track original input handlers or invoke `Vars.control.setInput()` during their enable, disable, or toggle lifecycles.

#### Scenario: Toggling features does not swap input
- **WHEN** `FreeCameraFeature` or `JoystickFeature` is enabled or disabled
- **THEN** `Vars.control.input` remains the exact same instance and active touch or key input is not dropped

### Requirement: Fault-Tolerant Mobile Gesture Processing
`ModMobileInput` SHALL intercept touch gestures and isolate downstream event dispatch and game logic errors from crashing the mobile render thread.

#### Scenario: Uncaught exception in mobile tap does not crash
- **WHEN** an uncaught exception or runtime error occurs during `super.tap()` execution or downstream `Call.tileTap` / `Events.fire`
- **THEN** `ModMobileInput.tap` catches the exception, logs the error, and returns `false` without terminating the game thread

#### Scenario: Normal tap behavior preserved
- **WHEN** a tap gesture is performed on a valid tile and no exception occurs
- **THEN** `ModMobileInput.tap` returns the result of vanilla `MobileInput.tap()`


