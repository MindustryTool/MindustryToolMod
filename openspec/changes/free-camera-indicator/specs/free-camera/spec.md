## ADDED Requirements

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
