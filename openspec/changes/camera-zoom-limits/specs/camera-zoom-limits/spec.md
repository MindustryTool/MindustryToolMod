## ADDED Requirements

### Requirement: Persisted shared zoom range with vanilla defaults

The system SHALL persist a single shared `min-zoom` and `max-zoom` range applied on all platforms, defaulting to vanilla limits so a fresh install does not change zoom behavior.

#### Scenario: Fresh install is a no-op

- **WHEN** the feature is enabled with default settings
- **THEN** the camera zooms exactly as vanilla with no observable clamping.

#### Scenario: Settings survive restart

- **WHEN** the user sets min/max values and restarts the game
- **THEN** the previously saved min/max values are restored.

### Requirement: Per-frame zoom clamping including beyond-vanilla values

The system SHALL clamp the live renderer scale into `[min-zoom, max-zoom]` every frame while playing, supporting limits beyond vanilla in both directions and covering desktop and mobile zoom inputs uniformly.

#### Scenario: Zoom out past max is pulled back

- **WHEN** the live scale exceeds max-zoom during play
- **THEN** the system sets the scale back to max-zoom before render.

#### Scenario: Zoom in past min is pulled back

- **WHEN** the live scale drops below min-zoom during play
- **THEN** the system sets the scale back to min-zoom before render.

#### Scenario: In-range zoom untouched

- **WHEN** the live scale is within `[min-zoom, max-zoom]`
- **THEN** the system performs no write.

#### Scenario: Clamp inactive outside gameplay

- **WHEN** the feature is disabled, no game is active, or the HUD is hidden
- **THEN** the system performs no clamping.

### Requirement: Min-max invariant by push

The system SHALL enforce `min-zoom <= max-zoom` by pushing the crossed value whenever sliders cross.

#### Scenario: Min dragged past max pushes max

- **WHEN** the user sets min-zoom above the current max-zoom
- **THEN** max-zoom is raised to equal min-zoom.

#### Scenario: Max dragged below min pushes min

- **WHEN** the user sets max-zoom below the current min-zoom
- **THEN** min-zoom is lowered to equal max-zoom.

### Requirement: Zoom settings UI and localization

The system SHALL expose a Solim settings dialog with min and max slider rows, reactive value labels, and a reset-to-defaults action, with fully localized strings for the title, labels, values, reset, and feature name, description, and help.

#### Scenario: Dialog adjusts live range

- **WHEN** the user moves the min or max slider
- **THEN** the persisted range updates and subsequent frames clamp to the new range.

#### Scenario: Reset restores vanilla limits

- **WHEN** the user invokes reset-to-defaults
- **THEN** min-zoom and max-zoom return to vanilla defaults.

#### Scenario: Feature entry is localized

- **WHEN** the user views the Camera Zoom feature card or help
- **THEN** the feature name, description, and help text come from the locale bundle.
