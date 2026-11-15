## REMOVED Requirements

### Requirement: Core Placement with Area Validation
**Reason**: Replaced by generalized Universal Block Placement supporting all synthetic and sandbox blocks, category filtering, rotation controls, and force overwrite.
**Migration**: Use `GodModeProvider.placeBlock(Block, Team, float, float, int)` and `GodModeBlockDialog`.

## ADDED Requirements

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
