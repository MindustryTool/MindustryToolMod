## MODIFIED Requirements

### Requirement: Enabled-State Status Indicator
The system SHALL display a persistent, non-interactive status label reading the translated `status.free-camera.enabled` label while `FreeCameraFeature` is enabled during gameplay. The label SHALL be frame-drawn (no scene-graph element) horizontally centered on the visible top-center, SHALL render its text in `Pal.accent` on a dark translucent backdrop, SHALL never consume game input, and SHALL hide whenever the feature is disabled, the HUD is hidden, or the player is not in-game.

#### Scenario: Indicator visible while free camera enabled in game
- **WHEN** `FreeCameraFeature` is enabled, `Vars.ui.hudfrag.shown` is true, and `Vars.state.isGame()` is true
- **THEN** a top-center label showing the `status.free-camera.enabled` text in `Pal.accent` is visible, exactly horizontally centered, and does not block map panning or clicks

#### Scenario: Indicator hidden when free camera disabled
- **WHEN** the player disables `FreeCameraFeature` via QuickAccess tap, settings, or keybind
- **THEN** the indicator is removed or hidden without manual refresh

#### Scenario: Indicator hidden outside gameplay
- **WHEN** the player leaves the game, closes the HUD (`hudfrag.shown` false), or opens a non-game screen while free camera is enabled
- **THEN** the indicator is not visible until gameplay HUD returns

#### Scenario: Indicator stays centered at any size, zoom, or locale
- **WHEN** the screen is resized or rotated, the camera zoom changes, or the locale changes the label width while the indicator is visible
- **THEN** the label remains exactly horizontally centered near the top-center with no manual repositioning

#### Scenario: Indicator label is translatable
- **WHEN** the indicator is rendered under any locale
- **THEN** its text resolves from `bundle.properties` key `status.free-camera.enabled` with no hardcoded English in code
