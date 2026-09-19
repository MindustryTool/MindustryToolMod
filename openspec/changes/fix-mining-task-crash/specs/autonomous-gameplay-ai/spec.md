## MODIFIED Requirements

### Requirement: Task 8 - Mining
The Mining task MUST mine the lowest-stock selected resources safely and deposit them into the core.
- The task MUST search for candidate ore tiles relative to the friendly Core.
- The task MUST query floor ores (`findClosestOre`) only when the unit can mine floors (`unit.type.mineFloor`) and floor ore is present in the indexer (`Vars.indexer.hasOre(item)`).
- The task MUST query wall ores (`findClosestWallOre`) only when the unit can mine wall ores (`unit.type.mineWalls`) and wall ore is present in the indexer (`Vars.indexer.hasWallOre(item)`).
- The task MUST verify that candidate ore tiles are valid to mine before selecting them, accepting uncovered floor ores (`tile.drop() == item && tile.block() == Blocks.air`) and valid wall ores (`tile.wallDrop() == item || (tile.block() != null && tile.block().itemDrop == item)`).
- The task MUST throttle candidate ore evaluation using a tick timer to prevent searching all content items across the indexer every frame.
- WHEN the unit is transporting collected ore to the core (`mining == false`) THEN `unit.mineTile` MUST NOT be set or re-assigned until deposit completes.
- The task MUST use a hysteresis / delta threshold based on core inventory quantities (`Math.max(unit.type.itemCapacity * 2, 60)`) to determine when to switch target resources, ensuring smooth rotation without 1-item ping-pong or infinite lockup when items beam directly into the core.
- The task MUST immediately switch target resources if the friendly core cannot accept any more of the current target resource (`core.acceptStack <= 0`), the resource is deselected by the player, or no valid ore tile remains accessible.

#### Scenario: Ore covered by player structures
- **WHEN** the nearest map ore tile is covered by a conveyor belt or wall
- **THEN** MiningTask skips the covered tile and locates the nearest uncovered valid ore tile

#### Scenario: Hauling mined ore back to core
- **WHEN** the unit item capacity is reached and the unit is flying to the core
- **THEN** unit.mineTile remains null and does not thrash while traveling

#### Scenario: Rotating lowest-stock resources near core
- **WHEN** the player unit is mining an ore tile within core transfer range (`Vars.mineTransferRange`) where items beam directly into the core
- **THEN** MiningTask continues mining the current target item until an alternative selected resource in the core is lower by at least the hysteresis threshold, at which point it switches target items

#### Scenario: Immediate switch when core is full of current resource
- **WHEN** the core reaches maximum capacity for the current target resource (`core.acceptStack <= 0`)
- **THEN** MiningTask immediately switches to the next lowest-stock valid candidate without waiting for the threshold

#### Scenario: Wall ore mining for wall-mining units
- **WHEN** a player unit can only mine wall ores (`mineFloor == false`, `mineWalls == true`)
- **THEN** MiningTask bypasses floor ore searches, queries wall ores via `findClosestWallOre`, and validates wall ore tiles correctly

#### Scenario: Throttled candidate evaluation
- **WHEN** Autoplay updates the Mining task across successive game ticks
- **THEN** comprehensive candidate ore searches across all items are evaluated periodically on an interval rather than on every tick

### Requirement: Safe Evaluation and Coordinate Resolution
- All autoplay tasks SHALL operate without side effects during priority arbitration and MUST use null-safe coordinate lookups.
- WHEN `SelfHealTask.update(unit)` is evaluated during task selection THEN it SHALL evaluate eligibility and status without calling AI movement methods.
- WHEN build plans reference missing or boundary tiles (`tile() == null`) THEN distance calculation and movement targets SHALL compute from plan tile coordinates (`x * Vars.tilesize, y * Vars.tilesize`) rather than throwing `NullPointerException`.
- WHEN a ground unit updates boosting while near or outside map boundaries where `unit.floorOn()` may be null THEN it SHALL null-check the floor before inspecting floor properties (`isDuct`, `damageTaken`, `isDeep`).
- WHEN `MiningTask` queries `Vars.indexer` for closest floor or wall ores THEN calls SHALL be defensively guarded to catch engine-level exceptions silently, returning null instead of throwing unhandled exceptions.

#### Scenario: Boundary tile coordinates resolved safely
- **WHEN** build plans or ground units check boundary positions where tiles or floors may be null
- **THEN** coordinates resolve without throwing NullPointerException

#### Scenario: Defensive indexer error handling
- **WHEN** Mindustry's internal `BlockIndexer` throws a `NullPointerException` or runtime exception during ore lookup
- **THEN** the exception is caught silently and `null` is returned without crashing the game
