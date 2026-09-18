## MODIFIED Requirements

### Requirement: Unit path filtering
The system SHALL display paths for enemy units, display paths for ally units only when the ally toggle is on, and SHALL NOT display a path for any unit possessed by a human player (local or remote), on any team.

#### Scenario: Enemy unit path shown
- **WHEN** an enemy unit has a traceable path
- **THEN** its path is drawn.

#### Scenario: Ally unit respects toggle
- **WHEN** the ally-unit toggle is on and an ally unit has a traceable path
- **THEN** its path is drawn, and when the toggle is off no ally path is drawn.

#### Scenario: Player controlled unit hidden
- **WHEN** a unit is possessed by a human player (local or remote)
- **THEN** no path is drawn for it and no unit-path cache entry is filled for it.

#### Scenario: RTS-commanded unit still shown
- **WHEN** a unit is driven by `CommandAI` with a command target (including player-issued RTS orders)
- **THEN** its commanded path is drawn as before.

#### Scenario: No stale path after player takes control
- **WHEN** a human takes control of a unit after a wave/commanded path was cached for it
- **THEN** the draw pass hides it immediately rather than rendering the stale cached path until expiry.
