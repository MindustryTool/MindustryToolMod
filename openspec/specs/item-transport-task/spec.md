# Item Transport Task

## Purpose

Autonomously transports items between configured source buildings (targets) and destination buildings using player unit inventory capacity, guided by world-anchored floating selection UI and bounded single-slot map persistence.

## Requirements

### Requirement: Target and Destination Selection Mode
The Item Transport task (`TransportTask`) MUST provide controls for the player to enter continuous selection mode for Target (source) buildings and Destination (sink) buildings, as well as an action to clear all selected buildings.
- Selection controls (`Select Targets`, `Select Destinations`, and `Clear All`) MUST be rendered both in the Autoplay HUD/Popup (when `TransportTask` is enabled) and in the `TransportTask` settings section inside `AutoplaySettingsView`.
- Toggling `Select Targets` MUST arm target selection mode (and disarm destination selection mode if active).
- Toggling `Select Destinations` MUST arm destination selection mode (and disarm target selection mode if active).
- While selection mode is armed, tapping a valid allied building with item capacity (`build.block.hasItems`) MUST add or enable that role (`Target` or `Destination`) on the building and remain in selection mode so multiple buildings can be tapped consecutively.
- Selection mode MUST disarm when the player toggles the active selection button off, presses `Escape` or `Back`, or disables `TransportTask` or `AutoplayFeature`.
- Clicking `Clear All` MUST remove all configured targets and destinations and clear persisted data for the current map.

#### Scenario: Consecutive building selection in target mode
- **WHEN** the player activates `Select Targets` and taps two allied storage or production buildings in the world
- **THEN** both buildings are registered as transport targets and target selection mode remains active until toggled off or cancelled

#### Scenario: Switching from target selection to destination selection
- **WHEN** `Select Targets` is active and the player clicks `Select Destinations`
- **THEN** target selection mode disarms, destination selection mode arms, and subsequent building taps register destinations

#### Scenario: Clearing all transport nodes
- **WHEN** the player clicks `Clear All` in the HUD or task settings
- **THEN** all configured targets and destinations are removed and their floating UI badges disappear

### Requirement: World-Anchored Floating UI and Zoom Threshold
Selected Target and Destination buildings MUST render a world-anchored floating Solim UI badge positioned at the building's projected screen coordinates (`Core.camera.project`).
- The floating UI MUST be visible only when `AutoplayFeature` is enabled, `TransportTask` is enabled, the game HUD is shown (`Vars.state.isGame() && Vars.ui.hudfrag.shown`), and a valid allied building exists at the node's tile coordinates.
- WHEN the camera zoom scale is less than `0.5` (`Vars.renderer.getScale() < 0.5f`) THEN all transport floating UI badges MUST be hidden.
- Each node's floating UI MUST display a compact badge showing its active role(s) (`Transfer` for Targets, `Receive` for Destinations), icons of currently selected items, an expand/edit button to toggle the item picker, and a remove button to delete that role or node.
- If a building is selected as both a Target and a Destination, its floating UI MUST display both the `Transfer` and `Receive` sections.
- Clicking the remove button on a role MUST remove that role from the building, and if neither Target nor Destination remains on that building, remove the floating UI completely.

#### Scenario: Floating badge hides when zoomed out below 0.5
- **WHEN** the player zooms the camera out such that `Vars.renderer.getScale() < 0.5f`
- **THEN** all target and destination floating UI badges are hidden until the camera zooms back in to `>= 0.5f`

#### Scenario: Compact badge expands item picker on edit click
- **WHEN** a target or destination badge is in compact mode and the player clicks its edit/expand button
- **THEN** the floating UI expands to show selectable item chips for that building

#### Scenario: Removing a target or destination via floating UI
- **WHEN** the player clicks the remove button on a building's floating UI
- **THEN** the building is removed from the transport configuration and its floating UI is disposed

### Requirement: Smart Item Filtering and Default Selection
The floating UI item picker MUST filter selectable items to those valid for the building's block type and apply role-appropriate defaults when a building is newly added.
- Multiple items MUST be selectable per Target (`transferItems`) and per Destination (`receiveItems`).
- For specialized blocks (such as turrets, crafters/factories, unit factories, reconstructors, drills, and generators) that only produce or consume specific items, the item picker MUST display only those valid items and MUST auto-select them by default when the building is added.
- For general storage blocks (such as Cores, Vaults, Containers, and Unloaders) that can hold arbitrary items, the item picker MUST display all unlocked items (`item.unlockedNow()`), MUST start with no items selected by default when newly added, and MUST automatically expand the item picker so the player can choose which item(s) to transfer or receive.

#### Scenario: Adding a specialized turret or factory as destination
- **WHEN** the player taps an allied turret or factory while in `Select Destinations` mode
- **THEN** the building is added as a destination with its valid input/ammo items automatically selected and displayed in compact badge mode

#### Scenario: Adding a Core or Vault as target or destination
- **WHEN** the player taps a Core, Vault, or Container while in selection mode
- **THEN** the building is added with no items pre-selected and its floating item picker expanded to show unlocked items

### Requirement: Demand-Driven Item Transport AI
`TransportTask` and `TransportAI` MUST autonomously ferry items from configured Targets to configured Destinations using the player unit's inventory capacity.
- If the player unit cannot carry items (`unit.type.itemCapacity <= 0`), the task MUST yield immediately.
- WHEN the unit is empty (`unit.stack.amount == 0`) THEN the task MUST search for a valid `(Target, Destination, Item)` match where `Target != Destination`, `Item` is selected on `Target` (`transferItems`) and on `Destination` (`receiveItems`), `Target.items.get(Item) > 0`, and `Destination.acceptStack(Item, 1, unit) > 0`.
- When multiple destinations need items, the task MUST prioritize the destination with the lowest current stock of the needed item (`Destination.items.get(Item)`), breaking ties by closest distance to the unit.
- WHEN a match is found and the unit is empty THEN `TransportAI` MUST navigate within range of `Target` and invoke `Call.requestItem(Vars.player, Target, Item, amount)` subject to a transfer cooldown timer.
- WHEN the unit is carrying an item (`unit.stack.amount > 0`) and at least one configured Destination accepts that item THEN `TransportAI` MUST navigate within range of the highest-priority Destination (lowest current stock of that item) and invoke `Call.transferInventory(Vars.player, Destination)` subject to a transfer cooldown timer.
- WHEN the unit is carrying an item (`unit.stack.amount > 0`) that no configured Destination can currently accept THEN `TransportAI` MUST navigate to the closest allied Core to deposit the item via `Call.transferInventory`, or clear the unit's held item if no Core can accept it.
- WHEN no valid transport match exists and the unit is not holding transportable cargo THEN `TransportTask` MUST yield (`return false`) to lower-priority autoplay tasks.

#### Scenario: Unit transports item from target to lowest-stock destination
- **WHEN** a Target Vault has Silicon enabled with stock available, and two Destination Turrets both accept Silicon with stocks of 5 and 20 respectively
- **THEN** `TransportTask` activates, moves the unit to the Target Vault to request Silicon, and delivers it to the Destination Turret with 5 Silicon first

#### Scenario: Destination fills up while unit is carrying item
- **WHEN** the unit is carrying Titanium toward a Destination and that Destination becomes full before delivery
- **THEN** `TransportTask` redirects the unit to deposit the carried Titanium into the closest allied Core (or another Destination that accepts Titanium) so the unit inventory does not deadlock

### Requirement: Single-Slot Map-Keyed Persistence
Transport configurations MUST be persisted in settings using a single storage slot keyed by a map identifier so disk usage remains bounded to one map instance at a time.
- The persistence payload MUST include a composite `mapKey` (derived from map name, world dimensions, and sector ID) along with the serialized list of configured nodes (`tileX`, `tileY`, target/destination flags, and selected transfer/receive item names).
- WHEN a world loads (`WorldLoadEvent`) and its computed `mapKey` matches the persisted `mapKey` THEN the saved targets and destinations MUST be restored.
- WHEN a world loads and its computed `mapKey` does not match the persisted `mapKey` THEN the active transport node list MUST start empty in memory, and modifying or adding nodes on the new map MUST overwrite the single persisted slot with the new `mapKey` and node list.

#### Scenario: Reloading the same map restores transport nodes
- **WHEN** the player configures targets and destinations on a map, exits to menu, and reloads the same map
- **THEN** the persisted `mapKey` matches and all targets, destinations, and selected items are restored at their tile coordinates

#### Scenario: Loading a different map overwrites single storage slot on edit
- **WHEN** the player loads a different map with a new `mapKey` and selects a new target building
- **THEN** the single persisted settings entry is overwritten with the new map's `mapKey` and node configuration without retaining old map entries
