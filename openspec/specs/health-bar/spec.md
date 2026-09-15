# health-bar Specification

## Purpose

World-space unit and building health/shield bar rendering with independently toggleable friendly/enemy layers, persisted appearance settings, and a Solim settings dialog. Created by archiving change rewrite-healthbar.

## Requirements

**Source: rewrite-healthbar**

### Requirement: Gated world-space health rendering
The feature SHALL draw on `Trigger.draw` with early-out gates: reject when disabled, when not in-game, when the HUD fragment is hidden, and when camera zoom is below the persisted threshold (threshold ≤ 0.01 disables zoom gating entirely). Rendering SHALL iterate units intersecting the camera bounds when the unit layer is enabled and buildings in camera range when the block layer is enabled, skipping units that are neither damaged nor shielded and buildings that are not damaged.

#### Scenario: Bars render during valid gameplay
- **WHEN** the feature is enabled during active gameplay with the HUD shown and zoom above threshold
- **THEN** health bars appear over damaged or shielded units in view

#### Scenario: Bars hidden when HUD hidden
- **WHEN** the HUD fragment is hidden or the game is not in progress
- **THEN** no bars are drawn

#### Scenario: Zoom gate hides bars when far out
- **WHEN** camera zoom falls below a positive threshold
- **THEN** no bars are drawn

#### Scenario: Off threshold disables zoom gating
- **WHEN** the zoom threshold is at or below 0.01
- **THEN** bars draw at any zoom, subject only to the remaining gates

### Requirement: HP bar rendering
For each damaged unit the feature SHALL draw a black backdrop bar above the unit plus a team-colored fill proportional to `health / maxHealth`, with geometry (offset by hit size, width by hit size and width setting, height by scale setting) and opacity matching the legacy layout at default settings. NaN and negative health inputs SHALL be sanitized before use.

#### Scenario: Damaged unit shows proportional fill
- **WHEN** a unit in view has health below max
- **THEN** a backdrop bar and a team-colored fill of matching proportion render above it

#### Scenario: Healthy unshielded unit shows nothing
- **WHEN** a unit is at full health with no shield
- **THEN** no bar is drawn for it

#### Scenario: Identical geometry at defaults
- **WHEN** opacity, scale, and width are 1.0
- **THEN** bar offset, width, height, colors, and layer equal the legacy fixed layout exactly

### Requirement: Single shield bar with whole-bar count
For each shielded unit the feature SHALL draw exactly one shield bar above the HP bar showing the fractional part of `shield / maxHealth` (exact positive multiples render a full bar), plus a `Fonts.outline` world-space count `xN` where `N` is the floored whole-bar count, positioned right of the bar and scaled with the bar scale. The count SHALL be hidden when the whole-bar count is 0; no shield-bar loop SHALL exist. NaN shield input SHALL be treated as 0.

#### Scenario: Partial shield shows fraction plus count
- **WHEN** a unit has shield equal to 3.5 times max health
- **THEN** one bar at half fill renders with a `x3` count beside it

#### Scenario: Exact multiple shows full bar
- **WHEN** a unit has shield equal to exactly 2 times max health
- **THEN** one full bar renders with a `x2` count beside it

#### Scenario: Single whole bar shows count
- **WHEN** a unit has shield equal to 1.7 times max health
- **THEN** one bar at 70% fill renders with a `x1` count beside it

#### Scenario: Lone partial bar shows no count
- **WHEN** a unit has shield below one times max health
- **THEN** only the fractional bar renders with no count text

#### Scenario: Unshielded unit shows no shield bar
- **WHEN** a unit has no shield
- **THEN** no shield bar or count renders for it

### Requirement: Independent friendly and enemy layers
Unit and block rendering SHALL each be split into friendly and enemy layers, toggleable via persisted booleans: show-friendly-units (default true), show-enemy-units (default true), show-friendly-blocks (default false), and show-enemy-blocks (default false). Friendly SHALL mean same team as the player; every other team SHALL count as enemy, and a null player SHALL fall back to all-enemy. Each iteration loop SHALL be skipped entirely when both of its toggles are off.

#### Scenario: Default layers match legacy behavior
- **WHEN** the game starts with untouched settings
- **THEN** damaged and shielded units of all teams show bars while buildings show none

#### Scenario: Friendly-unit toggle hides friendly bars only
- **WHEN** the user disables show-friendly-units
- **THEN** friendly unit bars disappear while enemy unit bars follow their own toggle

#### Scenario: Enemy-block toggle shows enemy building bars
- **WHEN** the user enables show-enemy-blocks
- **THEN** damaged enemy buildings in range show HP bars while friendly buildings follow their own toggle

#### Scenario: Disabled layers cost nothing
- **WHEN** both toggles of a layer are off
- **THEN** its iteration loop does not run

### Requirement: Block HP bar rendering
For each damaged building in camera range the feature SHALL draw a black backdrop bar above the building plus a team-colored fill proportional to `health / maxHealth`, with width derived from `block.size * tilesize` and the width setting, the shared `2*scale` bar thickness, and the shared opacity. Buildings SHALL never render shield bars or counts.

#### Scenario: Damaged building shows proportional fill
- **WHEN** show-enemy-blocks is enabled and an enemy building in range has health below max
- **THEN** a backdrop bar and a team-colored fill of matching proportion render above it

#### Scenario: Healthy building shows nothing
- **WHEN** a building is at full health
- **THEN** no bar is drawn for it

### Requirement: Persisted appearance settings
Show-friendly-units (default true), show-enemy-units (default true), show-friendly-blocks (default false), show-enemy-blocks (default false), zoom threshold (0–2, step 0.1), opacity (0–1, step 0.05), scale (0.5–1.5, step 0.1), and width multiplier (0.5–2.0, step 0.1) SHALL persist via `ConfigGroup`/`ConfigValue` with legacy defaults for the four appearance values. Draw code SHALL read each value once per frame via untracked `peek()` into locals; no subscription-synced cache fields SHALL be maintained.

#### Scenario: Settings survive restart
- **WHEN** the user changes appearance settings and restarts the game
- **THEN** the values are restored

#### Scenario: Reset restores defaults
- **WHEN** the user activates reset-to-defaults
- **THEN** all eight settings return to their defaults

### Requirement: Solim settings dialog
The feature SHALL expose a Solim settings dialog with four layer checkboxes (friendly units, enemy units, friendly buildings, enemy buildings) above four slider rows (zoom with Off state, opacity, scale, width), each bound directly to its config signal with a reactive value label, plus a reset-to-defaults action. All rows SHALL bind declaratively with no manual subscriptions for ordinary state.

#### Scenario: Adjusting a slider
- **WHEN** the user drags any appearance slider
- **THEN** the configuration persists immediately and world rendering reflects it

#### Scenario: Toggling a layer
- **WHEN** the user toggles any layer checkbox
- **THEN** the configuration persists immediately and the corresponding rendering layer appears or disappears

### Requirement: Translated strings and rewritten help
All user-visible health-bar text SHALL resolve from `assets/bundles/bundle.properties` under `feature.health-bar.*` keys with per-key translator comments, including new `feature.health-bar.settings.*` keys for the dialog title, four layer checkboxes, four slider labels, reset action, and Off state. The help text SHALL describe shipped unit and building bar behavior with no "cannot be enabled yet" wording. No hardcoded display strings SHALL be introduced.

#### Scenario: Settings labels are translated
- **WHEN** the settings dialog renders
- **THEN** every label resolves from the bundle with no hardcoded fallback

#### Scenario: Help describes shipped behavior
- **WHEN** the help dialog renders for health-bar
- **THEN** the text describes unit health/shield bars, building bars, and zoom-gated availability
