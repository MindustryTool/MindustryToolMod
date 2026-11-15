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
The system SHALL allow spawning units of any valid, spawnable type at chosen world coordinates, and killing all alive units of a given type and team. Internal, hidden, and block-bound unit entities SHALL NOT be selectable for spawning in the units dialog.

#### Scenario: Spawn units at map coordinates
- **WHEN** the player chooses a unit type, count, target team, selects a map coordinate, and clicks Spawn
- **THEN** the specified number of units are spawned at that map location.

#### Scenario: Filter unspawnable and internal units
- **WHEN** the player opens the God Mode Units Dialog
- **THEN** units that are internal, hidden, or construct `BlockUnitc` instances are excluded from the selectable unit list and initial selection.

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

### Requirement: Universal Block Placement
The system SHALL allow placing any synthetic or sandbox block on the map for any team with configurable rotation, overwriting existing tiles at the target location.

#### Scenario: Place block with rotation and overwrite
- **WHEN** the player selects a block, team, rotation, and target map tile
- **THEN** the block is placed at the specified tile for that team with the selected orientation, overwriting any prior tile contents.

#### Scenario: Reject placement when tile is out of world bounds
- **WHEN** the chosen world coordinates fall outside the map boundary or tile is null
- **THEN** the placement is rejected and returns false.

### Requirement: Block Category and Search Filtering
The system SHALL organize selectable blocks by their Mindustry Category and allow real-time text query filtering.

#### Scenario: Filter blocks by category
- **WHEN** the player selects a category filter chip (e.g. All, Turret, Production, Distribution, Liquid, Power, Defense, Crafting, Units, Effect, Logic)
- **THEN** the visible block list updates to display only blocks matching that category.

#### Scenario: Search blocks by name
- **WHEN** the player enters a search query in the block search field
- **THEN** the block list filters to show blocks whose localized name contains the search text case-insensitively.

### Requirement: Dynamic Rotation Controls
The system SHALL display 4-way rotation controls (0°, 90°, 180°, 270°) when the selected block is rotatable, and hide them when the block does not support rotation.

#### Scenario: Show rotation controls for rotatable block
- **WHEN** the selected block has `rotate == true`
- **THEN** the 4-way rotation selector is displayed in the dialog.

#### Scenario: Hide rotation controls for non-rotatable block
- **WHEN** the selected block has `rotate == false`
- **THEN** the rotation controls are hidden and rotation defaults to 0.

