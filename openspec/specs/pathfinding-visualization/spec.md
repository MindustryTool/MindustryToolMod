# pathfinding-visualization Specification

## Purpose
Visualize unit pathfinding paths for wave and commanded units on host and client with accurate native sources, team filtering, solid team-color rendering, Solim settings UI, and full i18n, filtering out misleading paths for logic-controlled units.

## Requirements

**Source: rewrite-pathfinding-feature**

### Requirement: Host and client accurate rendering
The system SHALL accurately display unit paths regardless of whether the user is hosting or connected as a client.

#### Scenario: Host wave paths render
- **WHEN** the user hosts a game with active wave units
- **THEN** paths are drawn for those units from their positions toward their targets.

#### Scenario: Client paths match server behavior
- **WHEN** the user plays as a client on a server with active wave and commanded units
- **THEN** locally drawn paths match the server-computed routes for the same units and targets.

### Requirement: Host-mode path sources
In host mode (not a net client) the system SHALL trace wave units via `Vars.pathfinder.getField(team, costType, fieldCore)` and commanded units via `Vars.controlPath.getPathPosition(unit, targetPos)`.

#### Scenario: Host wave unit uses flow field
- **WHEN** a wave unit is traced on the host
- **THEN** the path follows the flow field for its team, cost type, and core.

#### Scenario: Host commanded unit uses control path
- **WHEN** a commanded unit with a target position is traced on the host
- **THEN** the path follows the control-path trajectory from the unit to its target position.

### Requirement: Client-mode local path computation
In client mode the system SHALL trace paths through a separate `ClientPathfinder` utility that copies server pathfinding logic: wave units via a locally computed BFS flowfield from the enemy core, commanded units via a locally computed A* path from the unit to its `targetPos`, reproducing game cost and passability logic without mutating client game state.

#### Scenario: Client wave unit uses local BFS flowfield
- **WHEN** a wave unit is traced on a client
- **THEN** the path follows the locally computed BFS flowfield rooted at the enemy core using the server cost-type arrays.

#### Scenario: Client commanded unit uses local A-star
- **WHEN** a commanded unit with a target position is traced on a client
- **THEN** the path follows the locally computed A* route from the unit to its target position.

#### Scenario: Client computation causes no desync
- **WHEN** client pathfinding runs
- **THEN** game pathfinder state is not mutated and no desync or side effect occurs.

### Requirement: Unit path filtering
The system SHALL display paths for enemy units, display paths for ally units only when the ally toggle is on, and SHALL NOT display a path for the player's actively controlled unit.

#### Scenario: Enemy unit path shown
- **WHEN** an enemy unit has a traceable path
- **THEN** its path is drawn.

#### Scenario: Ally unit respects toggle
- **WHEN** the ally-unit toggle is on and an ally unit has a traceable path
- **THEN** its path is drawn, and when the toggle is off no ally path is drawn.

#### Scenario: Player controlled unit hidden
- **WHEN** a unit is the player's actively controlled unit
- **THEN** no path is drawn for it.

### Requirement: Solid team-color visuals
The system SHALL draw all unit and spawn-point paths in solid team colors with no fade effect along the path.

#### Scenario: Paths use solid team color
- **WHEN** paths for different teams render in the same frame
- **THEN** each path renders in a solid color identifying its team.

#### Scenario: No fade along path length
- **WHEN** a long path renders
- **THEN** opacity and color remain uniform from start to end.

### Requirement: Solim settings UI and disabled by default
The system SHALL provide configuration through the Solim UI framework with toggles for unit paths, spawn-point paths, and specific cost types, and the feature SHALL have `enabledByDefault` false.

#### Scenario: Toggles control rendered layers
- **WHEN** the user disables unit paths, spawn-point paths, or a cost type
- **THEN** the corresponding paths stop rendering while other enabled layers continue.

#### Scenario: Disabled by default on fresh install
- **WHEN** the mod is installed with no prior pathfinding-visualization state
- **THEN** the feature starts disabled.

### Requirement: Translated UI strings
All user-visible pathfinding-visualization text SHALL resolve from `assets/bundles/bundle.properties` with per-key translator comments, and no hardcoded display strings SHALL be introduced.

#### Scenario: Settings labels are translated
- **WHEN** the settings dialog renders
- **THEN** every label, tooltip, and toggle resolves from the bundle with no hardcoded fallback.

### Requirement: Logic-controlled unit filtering

The system SHALL skip path rendering for units controlled by a logic processor (`controller instanceof LogicAI`), regardless of team, except when the logic controller is in `pathfind` or `autoPathfind` mode. The skip SHALL apply in both the update pass (no cache fill) and the draw pass (no stale render), require no setting or toggle, and leave spawn-point paths unchanged.

#### Scenario: Logic move unit hidden
- **WHEN** a unit's controller is `LogicAI` with control `move` (or `idle`, `stop`, `approach`, or any mode other than `pathfind` / `autoPathfind`)
- **THEN** the system draws no unit path for it and fills no unit-path cache entry for it.

#### Scenario: Logic pathfind unit shown via commanded path
- **WHEN** a unit's controller is `LogicAI` with control `pathfind`
- **THEN** the system traces it through the existing commanded-unit path (control-path based), same as a `CommandAI` unit.

#### Scenario: Logic autoPathfind unit shown via wave path
- **WHEN** a unit's controller is `LogicAI` with control `autoPathfind`
- **THEN** the system traces it through the existing wave-unit flowfield path, same as a non-logic wave unit.

#### Scenario: Applies to all teams with no setting
- **WHEN** logic-controlled units exist on any team (enemy, ally, or player team)
- **THEN** the filtering rule applies uniformly with no configuration option.

#### Scenario: No stale path after ubind
- **WHEN** a unit becomes logic-controlled (e.g. `ubind`) after a wave/commanded path was cached for it
- **THEN** the draw pass hides it immediately rather than rendering the stale cached path until expiry.
