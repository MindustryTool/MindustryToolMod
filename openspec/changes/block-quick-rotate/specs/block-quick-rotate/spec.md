## ADDED Requirements

### Requirement: In-world quick-rotate overlay display
The system SHALL display an in-world floating rotation overlay when a player selects or taps an eligible rotatable block belonging to their team.

#### Scenario: Tapping an eligible rotatable block
- **WHEN** the player taps or selects an eligible building with `block.rotate == true` on their team
- **THEN** the in-world quick-rotate overlay SHALL be displayed adjacent to the building

#### Scenario: Tapping a non-rotatable block or empty space
- **WHEN** the player taps empty ground or a building where `block.rotate == false`
- **THEN** the quick-rotate overlay SHALL be dismissed

### Requirement: Step rotation controls
The quick-rotate overlay SHALL provide interactive buttons to rotate the building clockwise and counter-clockwise.

#### Scenario: Clicking rotate clockwise
- **WHEN** the player clicks the clockwise rotate button (`↻`)
- **THEN** the system SHALL issue `Call.rotateBlock(player, building, true)` to advance the building's rotation clockwise

#### Scenario: Clicking rotate counter-clockwise
- **WHEN** the player clicks the counter-clockwise rotate button (`↺`)
- **THEN** the system SHALL issue `Call.rotateBlock(player, building, false)` to rotate the building counter-clockwise

### Requirement: Overlay lifecycle and screen tracking
The system SHALL update the screen coordinates of the overlay as the camera moves or zooms, and automatically hide it when the target building is destroyed, out of bounds, or deselected.

#### Scenario: Camera pan and zoom
- **WHEN** the camera moves or zooms while the overlay is visible
- **THEN** the overlay's screen coordinates SHALL update dynamically to remain anchored to the building's world position

#### Scenario: Target building destroyed
- **WHEN** the targeted building is broken or removed
- **THEN** the overlay SHALL automatically hide

### Requirement: Translatable UI and settings
The feature SHALL provide configuration options in the settings dialog and translatable bundle strings for all tooltips and settings.

#### Scenario: Viewing settings and tooltips
- **WHEN** the user opens settings or hovers over the rotate controls
- **THEN** all displayed text and tooltips SHALL use localized strings from `bundle.properties`
