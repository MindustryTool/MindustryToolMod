# Autonomous Gameplay AI

## Purpose

Automates player unit behaviors (self-heal, flee, attack, repair, follow/assist, self-build, rebuild, mining) through a priority-based autonomous decision tree and Solim UI controls.
## Requirements
### Requirement: Task Hierarchy & Evaluation
- The AI MUST evaluate tasks in descending order of user-configured priority.
- The first enabled task whose trigger condition evaluates to true MUST be executed as the active task for the player unit.
- When no tasks trigger or the feature is disabled, control MUST return to the player.

#### Scenario: Highest priority valid task executes
- **WHEN** multiple enabled tasks have valid trigger conditions
- **THEN** Autoplay executes the task with the highest priority in the configured task order

### Requirement: Task 1 - Self-Heal
- MUST trigger when unit health falls below a configurable threshold (slider 20% - 80%, default 60%), healing until 100%.
- MUST navigate to the nearest Repair Point (`BlockFlag.repair`).
- If no repair points exist on the map, it MUST yield rather than blocking other tasks.

#### Scenario: Low health triggers self-heal
- **WHEN** unit health falls below the configured self-heal threshold and a repair point is available
- **THEN** SelfHealTask activates and navigates the unit to the repair point

### Requirement: Task 2 - Flee
- MUST trigger when unit health is below a configurable threshold (slider, default 40%) OR when the unit has no offensive weapons.
- MUST evaluate threats from both enemy units and enemy turrets.
- MUST retreat toward the nearest friendly core or base structures away from the threat.

#### Scenario: Critical health or weaponless unit flees
- **WHEN** unit health falls below the flee threshold or the unit has no weapons while enemies are nearby
- **THEN** FleeTask activates and retreats toward friendly core or structures

### Requirement: Task 3 - Attack
The Attack task MUST evaluate offensive capabilities and engage hostile targets appropriately according to unit combat type.
- The task MUST automatically skip/yield if the player unit has no attack weapons.
- The task MUST target enemy units or enemy buildings within `max(unit.range * 1.2f, 400f)`.
- WHEN the unit is a melee or suicide unit (e.g., suicide weapons or weapon range <= 25f) THEN it MUST move directly into contact (`moveTo(target, 0f)`).
- WHEN the unit has standard ranged weapons THEN it MUST maintain maximum weapon range / kite (`moveTo(target, kiteDistance, 40f, true, null)`).

#### Scenario: Ranged unit engages enemy
- **WHEN** the player unit has ranged weapons and an enemy is within detection range
- **THEN** AttackTask navigates while maintaining distance and fires weapons at the target

#### Scenario: Suicide or melee unit engages enemy
- **WHEN** the player unit is a melee or suicide unit (such as Crawler) and an enemy is detected
- **THEN** AttackTask commands the unit to rush directly into the target without kiting

### Requirement: Task 4 - Repair
The Repair task MUST detect healing capabilities and repair damaged structures and allied units without stalling on in-progress constructions.
- Units lacking healing weapons and repair field abilities MUST yield immediately.
- The task MUST search for damaged friendly buildings across the map, filtering out `ConstructBuild` instances without prematurely aborting the search for other damaged buildings.
- The task MUST heal damaged allied units when within heal range.

#### Scenario: Damaged buildings exist alongside construction sites
- **WHEN** incomplete construction sites and damaged completed buildings exist on the map
- **THEN** RepairTask bypasses the construction sites and navigates to repair the damaged completed buildings

### Requirement: Task 5 - Follow & Assist
The Follow & Assist task MUST follow designated teammates in multiplayer and mirror their actions without polluting the unit build queue.
- In singleplayer, the task MUST yield cleanly.
- In multiplayer, the task MUST track the designated player or active builder.
- WHEN assisting an ally's construction THEN the task MUST verify that the build plan is not already present in the unit's build queue before appending.

#### Scenario: Assisting an active builder
- **WHEN** the followed teammate is actively placing or constructing a structure
- **THEN** FollowAssistTask navigates to assist without adding duplicate build plans to `unit.plans` every frame

### Requirement: Task 6 - Self-Build & Task 7 - Rebuild
Self-Build and Rebuild tasks MUST construct structures efficiently while remaining isolated to prevent priority ping-ponging.
- `RebuildTask` MUST identify destroyed structures from team block plans and manage their reconstruction to completion.
- `RebuildTask` build plans MUST be isolated such that `SelfBuildTask` does not hijack execution on subsequent frames.
- `SelfBuildTask` MUST only evaluate and construct plans initiated by the player.

#### Scenario: Rebuilding destroyed structures
- **WHEN** team rebuild plans are present in `unit.team.data().plans`
- **THEN** RebuildTask handles the reconstruction of the block without triggering priority oscillation to SelfBuildTask

#### Scenario: Player places new build plan
- **WHEN** the player manually queues construction plans
- **THEN** SelfBuildTask executes the player's queued plans in order of nearest distance

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

### Requirement: Camera & Visuals
The Autoplay feature MUST render visual status indicators overhead and connect to valid target coordinates, and MUST respect `FreeCameraFeature` when updating camera position.
- MUST provide a cross-platform "Follow Unit" toggle, defaulting to `false`.
- MUST render the active task icon above the player unit.
- WHEN a task has an active, non-null destination target THEN it SHALL render a dashed line connecting the unit to the target.
- WHEN a task is idle or has no target destination THEN no dashed line SHALL be drawn.
- WHEN `FreeCameraFeature` is enabled THEN Autoplay SHALL NOT pull or lerp the camera position toward the unit.

#### Scenario: Active task with destination
- **WHEN** an active task is moving toward a target coordinate
- **THEN** a dashed line is rendered from the unit to the target position

#### Scenario: Task is idle or has no target
- **WHEN** autoplay has no active target or movement destination
- **THEN** no line is drawn to coordinate (0, 0)

#### Scenario: Free camera active suppresses Autoplay camera pull
- **WHEN** Autoplay is running and `FreeCameraFeature` is enabled
- **THEN** Autoplay does not lerp `Core.camera.position` toward the unit, allowing unconstrained map viewing

### Requirement: Settings & Internationalization
The settings view MUST provide intuitive controls for configuring task priorities and toggling individual tasks without interaction conflicts.
- Each task card MUST display an explicit toggle switch/checkbox for enabling or disabling the task.
- Clicking the outer card body MUST NOT toggle the task state.
- Sliders, filter chips, and configuration controls within expanded task settings MUST be freely interactive without risk of toggling the task.

#### Scenario: Adjusting task settings slider
- **WHEN** the user drags a threshold slider inside an expanded task card
- **THEN** the slider value updates without toggling the task's enabled/disabled state

### Requirement: Base AI Null Safety and Boundary Protection
- The `BaseAutoplayAI` controller SHALL strictly guard against unassigned units (`unit == null`) and null target positions (`target == null` or `pos == null`) in all movement and steering delegations.
- WHEN an AI movement method (`moveTo`, `circle`) is invoked while `unit` is null THEN the method SHALL safely return immediately without invoking Mindustry's `AIController` or throwing `NullPointerException`.
- WHEN `moveTo` is invoked with a null `Position` THEN the method SHALL safely return immediately without attempting coordinate reads or throwing `NullPointerException`.

#### Scenario: Null unit or position safely ignored
- **WHEN** AI movement methods are called with an unassigned unit or null destination position
- **THEN** the method returns immediately without throwing NullPointerException

### Requirement: Task Transition and State Cleanup
- The Autoplay system SHALL manage transient unit action states during task switching and feature state transitions.
- WHEN Autoplay transitions the active task from `AttackTask` to another task (or to idle/disabled) THEN it SHALL reset shooting state via `unit.isShooting(false)`.
- WHEN Autoplay transitions away from `MiningTask` THEN it SHALL clear the active mining tile target `unit.mineTile = null`.
- WHEN user input overrides autoplay or the feature is disabled THEN Autoplay SHALL yield control to `Vars.player`, reset transient states, and clear `currentTask`.

#### Scenario: Transient states reset on task change
- **WHEN** Autoplay transitions away from AttackTask or MiningTask
- **THEN** shooting or mining tiles are cleared to prevent state bleed

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

