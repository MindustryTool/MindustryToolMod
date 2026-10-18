# mining-task-ore-filter Specification

## Requirements

### Requirement: Active Map Ore Visibility
The Mining Task settings UI SHALL only display item selection chips for items that exist as natural ore deposits (floor ores or wall ores) on the active map when a game is in progress.

#### Scenario: Item exists on the map
- **WHEN** the player opens the Mining Task settings during a match and an item has floor ores (`Vars.indexer.hasOre(item)`) or wall ores (`Vars.indexer.hasWallOre(item)`)
- **THEN** the item chip is displayed in the filter list

#### Scenario: Item does not exist on the map
- **WHEN** the player opens the Mining Task settings during a match and an item does not exist as floor or wall ores on the map
- **THEN** the item chip is hidden from the filter list

#### Scenario: Outside active game session
- **WHEN** the player opens the Mining Task settings outside an active game session (`!Vars.state.isGame()`)
- **THEN** all unlocked items are displayed in the filter list

### Requirement: Unit Capability Check and Disabling
The Mining Task settings UI SHALL disable item selection chips for items that the player's active unit cannot mine, while allowing enabled chips for items the unit can mine.

#### Scenario: Unit can mine item
- **WHEN** the player's active unit has mining capability and meets the tier/deposit requirements for the item (`unit.canMine(item)` and matching floor/wall capability)
- **THEN** the item chip is enabled and can be toggled by the player

#### Scenario: Unit cannot mine item due to tier or lack of mining capability
- **WHEN** the player's active unit cannot mine the item (e.g., `!unit.canMine(item)`)
- **THEN** the item chip is disabled, preventing toggling, and visually styled as disabled

#### Scenario: Unit lacks deposit type capability
- **WHEN** an item is only available in walls on the map but the unit cannot mine walls (`!unit.type.mineWalls`), or only on floors and `!unit.type.mineFloor`
- **THEN** the item chip is disabled, preventing toggling

#### Scenario: Tooltip on disabled item
- **WHEN** a player hovers over a disabled item chip
- **THEN** a tooltip is shown indicating why the unit cannot mine the item (e.g. tier requirement or unmineable deposit)

### Requirement: Persistent Selection State
The system SHALL preserve the configured selection state of an item even when disabled by current unit limitations.

#### Scenario: Disabled item selection persists
- **WHEN** an item was previously selected and the player switches to a unit that cannot mine it
- **THEN** the selection remains stored in configuration, and the chip retains its checked indicator while being disabled
