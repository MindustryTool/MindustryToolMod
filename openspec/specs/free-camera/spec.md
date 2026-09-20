# free-camera Specification

## Purpose
TBD - created by archiving change free-camera-feature. Update Purpose after archive.
## Requirements
### Requirement: Free Camera Feature Lifecycle & Metadata
The system SHALL register a dedicated `FreeCameraFeature` under `mindustrytool.features.freecamera` extending `Feature`. The feature SHALL have metadata ID `"free-camera"`, icon `FileIcon.of("camera.png")`, order 6, `enabledByDefault(true)`, and `quickAccessByDefault(true)`.

#### Scenario: Feature registered and enabled by default
- **WHEN** the mod is loaded on a clean installation
- **THEN** `FreeCameraFeature` is registered in `FeatureManager` and `isEnabled()` returns `true`

#### Scenario: Present on QuickAccess HUD by default
- **WHEN** the player enters gameplay with default settings
- **THEN** the Free Camera icon button appears on the QuickAccess HUD bar in the active state

### Requirement: QuickAccess HUD Interaction
The `FreeCameraFeature` SHALL handle QuickAccess HUD interactions such that clicking or tapping the icon toggles the feature's enabled state, and long-pressing the icon instantly snaps the camera position onto the player unit.

#### Scenario: Single click or tap toggles Free Camera
- **WHEN** the player clicks or taps the Free Camera icon in the QuickAccess HUD
- **THEN** `isEnabled()` toggles between `true` and `false` and the QuickAccess icon button reflects the new state

#### Scenario: Long-press snaps camera to player unit
- **WHEN** the player long-presses the Free Camera icon in the QuickAccess HUD during active gameplay
- **THEN** the camera position immediately centers onto `Vars.player.x, Vars.player.y`

### Requirement: Desktop WASD Camera Panning
On Desktop platforms, while `FreeCameraFeature` is enabled, directional controls (WASD) SHALL pan the camera position across the map instead of steering the player unit. When `FreeCameraFeature` is disabled, standard vanilla camera tracking (WASD steering unit and camera following player) SHALL be restored.

#### Scenario: WASD input with Free Camera enabled
- **WHEN** the player presses directional controls (WASD) while Free Camera is enabled on Desktop
- **THEN** the camera position shifts across the map in the indicated direction at camera pan speed, and the player unit's movement is not driven by the keyboard

#### Scenario: WASD input with Free Camera disabled
- **WHEN** the player moves using WASD while Free Camera is disabled on Desktop
- **THEN** the player unit moves according to WASD input and the camera smoothly follows the player unit

### Requirement: Mobile Free Camera Movement Decoupling
On Mobile platforms, while `FreeCameraFeature` is enabled and `JoystickFeature` is disabled, the player unit SHALL remain stationary instead of moving toward the panned camera position. When `AutoplayFeature` is active, manual unit controls (movement, auto-aim, and weapon controls) in `ModMobileInput` SHALL yield to the autonomous bot AI unless the virtual joystick knob is actively dragged.

#### Scenario: Mobile Free Camera panning without Virtual Joystick
- **WHEN** the player pans the camera away while Free Camera is enabled and Virtual Joystick is disabled
- **THEN** the camera pans to inspect the map and the player unit remains stationary without chasing the camera

#### Scenario: Mobile Free Camera with Autoplay active
- **WHEN** Autoplay and Free Camera are active on mobile and the joystick knob is not being dragged
- **THEN** `ModMobileInput` does not override unit movement, aiming, or weapon controls, allowing the bot AI to operate uninterrupted

### Requirement: Snapping to Player Unit
The system SHALL provide a `snapToPlayer()` method and bindable hotkey that immediately sets `Core.camera.position` to the player unit's coordinates `(Vars.player.x, Vars.player.y)` whenever the player is alive, resets camera panning state, and clears the vanilla `spectating` target so no stale target pulls the camera away afterwards.

#### Scenario: Snapping centers camera immediately
- **WHEN** `snapToPlayer()` is invoked while the player unit is alive
- **THEN** `Core.camera.position.x` equals `Vars.player.x` and `Core.camera.position.y` equals `Vars.player.y`

#### Scenario: Snapping clears spectate target
- **WHEN** `snapToPlayer()` is invoked while a vanilla `spectating` target is set
- **THEN** the `spectating` target is cleared and the camera remains on the player unit on subsequent frames

#### Scenario: Snapping while player dead
- **WHEN** `snapToPlayer()` is invoked while the player is dead or does not exist
- **THEN** the camera position remains unchanged without throwing an exception

### Requirement: Internationalization
All user-facing strings for Free Camera (name, description, help, keybinds, and tooltips) SHALL be resolved from `bundle.properties`.

#### Scenario: Free Camera bundle keys resolve
- **WHEN** `getName()` and `getDescription()` are called on `FreeCameraFeature`
- **THEN** the text resolves from `feature.free-camera.name` and `feature.free-camera.description`

### Requirement: Enabled-State Status Indicator
The system SHALL display a persistent, non-interactive status pill reading the translated `status.free-camera.enabled` label while `FreeCameraFeature` is enabled during gameplay. The pill SHALL be anchored top-center (`Units.screenWidth() / 2`, near the top edge), SHALL render its label in `Pal.accent` on a dark translucent background, SHALL never consume game input, and SHALL hide whenever the feature is disabled, the HUD is hidden, or the player is not in-game.

#### Scenario: Indicator visible while free camera enabled in game
- **WHEN** `FreeCameraFeature` is enabled, `Vars.ui.hudfrag.shown` is true, and `Vars.state.isGame()` is true
- **THEN** a top-center pill showing the `status.free-camera.enabled` text in `Pal.accent` is visible and does not block map panning or clicks

#### Scenario: Indicator hidden when free camera disabled
- **WHEN** the player disables `FreeCameraFeature` via QuickAccess tap, settings, or keybind
- **THEN** the indicator is removed or hidden without manual refresh

#### Scenario: Indicator hidden outside gameplay
- **WHEN** the player leaves the game, closes the HUD (`hudfrag.shown` false), or opens a non-game screen while free camera is enabled
- **THEN** the indicator is not visible until gameplay HUD returns

#### Scenario: Indicator survives resize and stays on screen
- **WHEN** the screen is resized or rotated while the indicator is visible
- **THEN** the pill remains fully within screen bounds near the top-center

#### Scenario: Indicator label is translatable
- **WHEN** the indicator is rendered under any locale
- **THEN** its text resolves from `bundle.properties` key `status.free-camera.enabled` with no hardcoded English in code

