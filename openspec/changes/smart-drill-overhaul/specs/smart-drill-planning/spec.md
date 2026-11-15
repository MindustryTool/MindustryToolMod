# smart-drill-planning Specification

## ADDED Requirements

### Requirement: Target ore resolution

The system SHALL resolve the target ore from the tapped tile. A tile whose floor or overlay carries an item drop SHALL target that floor ore; a tile whose wall carries an item drop SHALL target that wall ore. The two kinds SHALL NOT be mixed.

#### Scenario: Tapping floor ore

- **WHEN** the player taps a tile whose floor or overlay has an item drop
- **THEN** the target ore is that floor ore and only floor drills are offered

#### Scenario: Tapping wall ore

- **WHEN** the player taps a tile whose wall has an item drop
- **THEN** the target ore is that wall ore and only beam drills are offered

#### Scenario: Tapping a tile with no ore

- **WHEN** the player taps a tile that carries neither a floor drop nor a wall drop
- **THEN** no plan is produced and no panel is opened

### Requirement: Floor ore flood is bounded and does not cross rock

The system SHALL build the floor-ore region by flooding from the tapped tile through 4-connected tiles carrying the target floor ore, SHALL stop after 100 ore tiles, and SHALL clamp the resulting bounding box to at most 10x10 tiles. The flood SHALL NOT traverse tiles that do not carry the target floor ore.

#### Scenario: Two patches separated by rock

- **WHEN** two deposits of the target ore are separated by tiles that do not carry the target ore
- **THEN** only the deposit containing the tapped tile is included in the region

#### Scenario: Region larger than the cap

- **WHEN** the connected target ore exceeds 100 tiles
- **THEN** the flood stops at 100 tiles and the bounding box is clamped to 10x10 tiles

#### Scenario: Diagonal vein

- **WHEN** the target ore forms a diagonal line whose bounding box exceeds 10x10 tiles
- **THEN** the bounding box is clamped to 10x10 tiles rather than left at the diagonal extent

### Requirement: Wall ore flood is bounded and matches the floor cap

The system SHALL build the wall-ore region by flooding from the tapped wall through 8-connected tiles carrying the target wall ore, SHALL stop after 100 ore tiles, and SHALL clamp the resulting bounding box to at most 10x10 tiles.

#### Scenario: Wall seam longer than the cap

- **WHEN** the connected target wall ore exceeds 100 tiles
- **THEN** the flood stops at 100 tiles and the bounding box is clamped to 10x10 tiles

### Requirement: Strict ore dominance rule

A drill position SHALL be valid only when its footprint contains at least one tile carrying the target ore and contains no tile carrying any other ore. Non-ore tiles inside the footprint SHALL NOT invalidate the position.

#### Scenario: Footprint of pure target ore

- **WHEN** every ore tile in the footprint carries the target ore
- **THEN** the position is valid

#### Scenario: Footprint straddling two ore types

- **WHEN** the footprint contains the target ore and at least one tile carrying a different ore
- **THEN** the position is rejected

#### Scenario: Footprint with no target ore

- **WHEN** the footprint contains no tile carrying the target ore
- **THEN** the position is rejected

### Requirement: Coverage rectangle

The system SHALL treat the target rectangle as the ore coverage area. For floor drills every footprint SHALL lie inside the rectangle. For beam drills the rectangle SHALL bound the wall ore to be drained and the drill bodies SHALL be placed in open space facing the wall. For bound schematics only the drills SHALL be required to lie inside the rectangle.

#### Scenario: Floor drill near the rectangle edge

- **WHEN** a floor drill footprint would extend outside the rectangle
- **THEN** the position is rejected

#### Scenario: Schematic rig larger than the rectangle

- **WHEN** a bound schematic's drills fit inside the rectangle but its conveyors or nodes extend beyond it
- **THEN** the schematic is still a valid candidate

### Requirement: 2x2 floor drill lattice

The system SHALL place 2x2 floor drills on a lattice anchored to the world origin with a 6x6 period, computed with sign-safe modulo so the pattern is identical in every quadrant. The system SHALL NOT add greedy fill on top of the lattice.

#### Scenario: Pattern is translation invariant

- **WHEN** the same local tile offset is evaluated at a positive and at a negative world coordinate
- **THEN** the lattice predicate returns the same result

#### Scenario: No greedy fill

- **WHEN** valid 2x2 positions remain that are not lattice anchors
- **THEN** no additional drill is placed there

### Requirement: Greedy placement for other floor drill sizes

The system SHALL place 1x1 and 3x3-or-larger floor drills by repeatedly choosing the valid position whose footprint covers the most currently-uncovered target ore tiles, then removing those tiles from consideration.

#### Scenario: Densest position first

- **WHEN** two valid positions are available and one covers more uncovered target ore than the other
- **THEN** the position covering more uncovered target ore is placed first

#### Scenario: Exhausted candidates

- **WHEN** no valid position covers any uncovered target ore
- **THEN** placement stops

### Requirement: Minimal bridge chain

The system SHALL place item bridges on the 3-periodic lattice (tiles where both coordinates are multiples of 3), restricted to the minimal connected subtree that spans the output root and every lattice node orthogonally adjacent to a planned drill. Each placed bridge SHALL be configured with the delta to its parent, and the root SHALL be configured with the stub.

#### Scenario: Unused bridge pruned

- **WHEN** a lattice node is not required to connect any drill to the output
- **THEN** no bridge is placed there

#### Scenario: Required bridge kept

- **WHEN** a lattice node is orthogonally adjacent to a planned drill
- **THEN** a bridge is placed there and configured toward the root

#### Scenario: Chain remains connected

- **WHEN** the bridge set is produced
- **THEN** every kept bridge has a path to the root through kept bridges

### Requirement: Stub at the chain root

The system SHALL place a single duct tile at the chain root, facing away from the ore field, and SHALL skip the stub when that tile is not placeable.

#### Scenario: Stub tile is free

- **WHEN** the tile beyond the root is placeable
- **THEN** one duct is placed there facing away from the field

#### Scenario: Stub tile is blocked

- **WHEN** the tile beyond the root is not placeable
- **THEN** no stub is placed and the root bridge is left unconfigured

### Requirement: Beam drill unit

The system SHALL place a beam drill as a unit consisting of a body facing the wall, a duct directly behind the body, and a beam node two tiles further back along the same axis. A position SHALL be valid only when every lane resolves to the target wall ore and at least one lane resolves.

#### Scenario: Duct is adjacent to the body

- **WHEN** a beam drill is placed
- **THEN** the duct tile is orthogonally adjacent to the body for every supported drill size

#### Scenario: A lane resolves to other ore

- **WHEN** one lane hits a wall tile carrying a different ore
- **THEN** the position is rejected

#### Scenario: No lane resolves

- **WHEN** no lane hits a wall tile carrying the target ore
- **THEN** the position is rejected

### Requirement: Schematic binding and placement

The system SHALL allow many schematics to be bound to a single drill block and SHALL place the selected schematic exactly once, at the highest-scoring position. A schematic SHALL be a candidate only when every drill in it satisfies the strict ore dominance rule and every non-air tile is placeable at its transformed position.

#### Scenario: Multiple bound schematics

- **WHEN** a drill block has more than one bound schematic
- **THEN** each is offered and selecting one places only that schematic

#### Scenario: Drills outside the rectangle

- **WHEN** a candidate placement would put a drill footprint outside the rectangle
- **THEN** the candidate is rejected

#### Scenario: Blocked tile inside the rig

- **WHEN** any non-air tile of the schematic is not placeable at its transformed position
- **THEN** the candidate is rejected

#### Scenario: Rotation is applied to configs

- **WHEN** a schematic containing a `Point2` config is rotated
- **THEN** the config is rotated with the schematic

### Requirement: Efficiency metric

The system SHALL score a schematic candidate by total mineable target-ore tiles under its drills, then by fewer blocks placed, then by shorter distance from the rectangle centre, in that order.

#### Scenario: More ore under drills wins

- **WHEN** two candidate positions differ in the total target ore covered by their drills
- **THEN** the position covering more target ore is chosen

#### Scenario: Equal coverage prefers fewer blocks

- **WHEN** two candidate positions cover the same amount of target ore
- **THEN** the position placing fewer blocks is chosen

### Requirement: Planner is pure

The planning core SHALL NOT reference `Vars`, `Core`, or any Arc rendering type. It SHALL operate over a world-view interface that exposes tile lookup, solidity, ore kind and buildability, and SHALL return plain planned-build data.

#### Scenario: Planning without a game

- **WHEN** a plan is produced against an in-memory world view in a test
- **THEN** no Mindustry game state is required and no rendering type is touched

#### Scenario: Engine fidelity for buildability

- **WHEN** the planning core runs inside the game
- **THEN** buildability is answered by the engine's own placement check rather than a reimplementation