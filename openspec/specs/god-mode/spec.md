# god-mode Specification

## Purpose

Comprehensive debug, testing, and sandbox control suite providing team modification, resource injection, unit spawning/mass-clearing, status effect control, core placement, and map fog toggling across local and remote admin environments. Created by archiving change god-mode-rewrite.

## Requirements

**Source: god-mode-rewrite**

### Requirement: Floating Draggable God Mode HUD
The system SHALL provide a floating, draggable Solim HUD for God Mode when the feature is enabled. The HUD SHALL remember its position across sessions and screen orientations (portrait and landscape).

#### Scenario: HUD visibility on gameplay
- **WHEN** the God Mode feature is enabled and a game session is active
- **THEN** the floating God Mode HUD appears on screen with action buttons: Team, Items, Units, Effects, Core, and Fog Toggle.

#### Scenario: Dragging HUD updates configuration
- **WHEN** the player drags the HUD to a new position
- **THEN** the position coordinates are saved in the feature configuration.

### Requirement: Team Selection
The system SHALL allow changing any player's team to any valid target team.

#### Scenario: Change player team
- **WHEN** the player selects a player and a target team from the Team Dialog and confirms
- **THEN** the selected player's team is updated immediately via the active provider (internal API or remote /js).

### Requirement: Core Item Injection
The system SHALL allow injecting items into any team's active core.

#### Scenario: Add items to core with count presets
- **WHEN** the player selects an item, target team, and specifies an amount (using presets +10, +100, +1000, Max or manual input)
- **THEN** the specified quantity of that item is added to the chosen team's core.

### Requirement: Unit Spawner and Mass Destroyer
The system SHALL allow spawning units of any type at chosen world coordinates, and killing all alive units of a given type and team.

#### Scenario: Spawn units at map coordinates
- **WHEN** the player chooses a unit type, count, target team, selects a map coordinate, and clicks Spawn
- **THEN** the specified number of units are spawned at that map location.

#### Scenario: Kill all units of type
- **WHEN** the player chooses a unit type and team and clicks "Kill All"
- **THEN** all units matching the selected type and team are eliminated.

### Requirement: Status Effect Control
The system SHALL allow applying and clearing status effects on the player's current unit.

#### Scenario: Apply status effect with duration
- **WHEN** the player selects a status effect from the searchable list, enters a duration, and clicks Apply
- **THEN** the effect is applied to the player's controlled unit.

#### Scenario: Clear status effect
- **WHEN** the player selects a status effect and clicks Clear
- **THEN** the effect is removed from the player's controlled unit.

### Requirement: Core Placement with Area Validation
The system SHALL allow placing any core block on the map for any team, provided the target area is valid.

#### Scenario: Place core on valid terrain
- **WHEN** the player selects a core block, team, and valid target map tile
- **THEN** the tile is verified for placement suitability and the core block is constructed for that team.

#### Scenario: Reject core placement on invalid terrain
- **WHEN** the target tile is out of bounds or cannot support the core block
- **THEN** the placement is rejected or an error is displayed.

### Requirement: Map Fog Toggle
The system SHALL provide a toggle switch to clear or restore fog of war.

#### Scenario: Disable fog of war
- **WHEN** the player activates the Fog toggle switch to OFF (reveal map)
- **THEN** the fog of war is lifted across the map.

#### Scenario: Re-enable fog of war
- **WHEN** the player toggles Fog back to ON
- **THEN** normal fog of war rules are restored.

### Requirement: Interactive Map Coordinate Picker
The system SHALL support selecting world map coordinates by temporarily hiding the active dialog, capturing a world tile tap, and re-opening the dialog with the selected coordinates.

#### Scenario: Select coordinate on map
- **WHEN** the player taps "Select on Map" in the unit spawner or core placement dialog
- **THEN** the dialog closes, the next map tile tap records world coordinates (X, Y), and the dialog re-opens with those coordinates filled in.

### Requirement: Dual Execution Providers
The system SHALL automatically route god mode actions through InternalGodModeProvider in singleplayer/hosting, or through JSGodModeProvider using Rhino-compatible /js chat commands when connected as an admin on a remote server.

#### Scenario: Remote admin execution
- **WHEN** the player is an admin on a multiplayer server and executes a god mode action
- **THEN** the action is formatted into valid Rhino JavaScript and sent via /js ... through the chat pipeline.