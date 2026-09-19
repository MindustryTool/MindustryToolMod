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
The system SHALL support selecting world map coordinates by temporarily hiding the active dialog, capturing a world tile tap, and re-opening the dialog with the selected coordinates. The picker SHALL track at most one active listener at any time, cancel prior registrations before adding a new one, and defer event unregistration so `Events.remove` is never invoked synchronously during `Events.fire`.

#### Scenario: Select coordinate on map
- **WHEN** the player taps "Select on Map" in the unit spawner or core placement dialog
- **THEN** the dialog closes, the next map tile tap records world coordinates (X, Y), and the dialog re-opens with those coordinates filled in.

#### Scenario: Repeated picker requests do not accumulate duplicate listeners
- **WHEN** the player activates the coordinate picker multiple times without tapping a map tile
- **THEN** any previously registered picker listener is cancelled and removed, leaving exactly one active listener.

#### Scenario: Picker cancellation and deferred unregistration
- **WHEN** a tile tap is captured or `cancel()` is invoked
- **THEN** the picker listener is deactivated immediately and unregistration from `Events` is deferred to `Core.app.post`, ensuring `Events.fire` iteration is never corrupted.

### Requirement: Dual Execution Providers
The system SHALL automatically route god mode actions through InternalGodModeProvider in singleplayer/hosting, or through JSGodModeProvider using Rhino-compatible /js chat commands when connected as an admin on a remote server.

#### Scenario: Remote admin execution
- **WHEN** the player is an admin on a multiplayer server and executes a god mode action
- **THEN** the action is formatted into valid Rhino JavaScript and sent via /js ... through the chat pipeline.

### Requirement: GodMode placeholder shell

The system SHALL register GodMode as a real (non-development) feature with QuickAccess membership, an enable signal, and a display-mode option defaulting to HUD, exposing no cheat controls and performing no game logic in any state.

#### Scenario: GodMode registers as non-development

- **WHEN** the mod starts and features register
- **THEN** `god-mode` reports `development=false`, appears in the settings grid without the In Development badge, and is toggleable like other real features

#### Scenario: Shell performs no logic

- **WHEN** GodMode is enabled in any display mode
- **THEN** no game state is modified and no overlay beyond the shell surfaces is shown

### Requirement: GodMode QuickAccess presence

The system SHALL list GodMode among QuickAccess HUD items (subject to existing visibility rules) with tap opening its popup in popup mode and toggling in HUD mode, and long-press opening its settings entry.

#### Scenario: GodMode QuickAccess button renders

- **WHEN** QuickAccess renders its item grid with GodMode visible
- **THEN** a GodMode button is present alongside other quick-access features

### Requirement: GodMode display mode and popup shell

The system SHALL persist a GodMode display-mode option (`ConfigValue<String>`, HUD/popup values, default HUD) and toggle a popup shell from its QuickAccess button (opening if closed, hiding if already showing or clicked again) under the same placement, suppression, fallback, and styling rules as the TimeControl popup, reusing the shared icon-only horizontal row layout from `GodModeHudView` (excluding drag handle, icon-only with tooltips, disabled state governed by `canEdit`) with no title header, wrapped in a container with a black background, `rounded(unit(2))`, `padding(unit(1))`, and `gap(unit(1))`.

#### Scenario: GodMode popup shell opens

- **WHEN** the player taps the GodMode QuickAccess button in popup mode with QuickAccess on while the popup is not showing
- **THEN** a popup shell anchored above or below the QuickAccess bar opens and the standalone path stays suppressed

#### Scenario: Clicking button again hides popup

- **WHEN** the player taps the GodMode QuickAccess button in popup mode with QuickAccess on while the GodMode popup is already showing
- **THEN** the popup closes and the feature enabled state is unchanged

#### Scenario: GodMode falls back silently

- **WHEN** GodMode display mode is popup and QuickAccess is off
- **THEN** the HUD-path behavior applies with no message and the setting stays at popup

