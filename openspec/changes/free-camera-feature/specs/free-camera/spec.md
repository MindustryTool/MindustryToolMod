# Free Camera

## Purpose
Provides an unconstrained, decoupled camera mode that allows players to inspect the map freely while moving on both mobile and desktop platforms, with 1-tap toggling and instant recentering via the QuickAccess HUD.

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

### Requirement: Desktop Movement Decoupling
On Desktop platforms, while `FreeCameraFeature` is enabled, moving the player unit with directional controls (WASD) SHALL NOT forcibly snap the camera back to the player unit. When `FreeCameraFeature` is disabled, standard vanilla camera tracking (WASD resetting pan state and centering on player) SHALL be restored.

#### Scenario: WASD movement with Free Camera enabled
- **WHEN** the player pans the camera away from the unit and moves using WASD while Free Camera is enabled
- **THEN** the player unit moves according to WASD input while the camera remains at its panned position

#### Scenario: WASD movement with Free Camera disabled
- **WHEN** the player moves using WASD while Free Camera is disabled
- **THEN** the camera smoothly lerps and centers onto the player unit

### Requirement: Snapping to Player Unit
The system SHALL provide a `snapToPlayer()` method and bindable hotkey that immediately sets `Core.camera.position` to the player unit's coordinates `(Vars.player.x, Vars.player.y)` whenever the player is alive.

#### Scenario: Snapping centers camera immediately
- **WHEN** `snapToPlayer()` is invoked while the player unit is alive
- **THEN** `Core.camera.position.x` equals `Vars.player.x` and `Core.camera.position.y` equals `Vars.player.y`

#### Scenario: Snapping while player dead
- **WHEN** `snapToPlayer()` is invoked while the player is dead or does not exist
- **THEN** the camera position remains unchanged without throwing an exception

### Requirement: Internationalization
All user-facing strings for Free Camera (name, description, help, keybinds, and tooltips) SHALL be resolved from `bundle.properties`.

#### Scenario: Free Camera bundle keys resolve
- **WHEN** `getName()` and `getDescription()` are called on `FreeCameraFeature`
- **THEN** the text resolves from `feature.free-camera.name` and `feature.free-camera.description`
