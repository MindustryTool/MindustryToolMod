## MODIFIED Requirements

### Requirement: Task 6 - Self-Build & Task 7 - Rebuild
Self-Build and Rebuild tasks MUST construct structures efficiently while remaining isolated to prevent priority ping-ponging.
- `RebuildTask` MUST identify destroyed structures from team block plans and manage their reconstruction to completion.
- `RebuildTask` build plans MUST be isolated such that `SelfBuildTask` does not hijack execution on subsequent frames.
- `SelfBuildTask` MUST only evaluate and construct plans initiated by the player.
- `SelfBuildTask` settings MUST provide one-shot action buttons stacked vertically to **Reclaim All Derelicts** and **Deconstruct All Derelicts**.
- WHEN the player clicks **Reclaim All Derelicts** while `Vars.state.rules.derelictRepair` is enabled THEN `SelfBuildTask` MUST queue non-breaking `BuildPlan(b.tileX(), b.tileY(), b.rotation, b.block, b.config())` entries for all derelict buildings (`Team.derelict`) that allow derelict repair (`b.block.allowDerelictRepair`) and pass placement validation (`Build.validPlaceIgnoreUnits`).
- WHEN queuing derelict reclaim or deconstruct plans THEN `SelfBuildTask` MUST replace any existing non-rebuild `BuildPlan` in `unit.plans` at the same `(tileX, tileY)` coordinate to prevent duplicate plans and allow switching between reclaim and deconstruct actions.
- WHEN `SelfBuildTask` evaluates whether a `BuildPlan` is affordable THEN it MUST treat derelict repair plans (`plan.isDerelictRepair()`) and rotation plans (`plan.isRotation(unit.team)`) as requiring no core items, allowing the unit to reclaim derelict blocks regardless of core inventory.

#### Scenario: Rebuilding destroyed structures
- **WHEN** team rebuild plans are present in `unit.team.data().plans`
- **THEN** RebuildTask handles the reconstruction of the block without triggering priority oscillation to SelfBuildTask

#### Scenario: Player places new build plan
- **WHEN** the player manually queues construction plans
- **THEN** SelfBuildTask executes the player's queued plans in order of nearest distance

#### Scenario: Reclaiming derelict blocks via settings button
- **WHEN** the player clicks **Reclaim All Derelicts** in `SelfBuildTask` settings on a map with `derelictRepair` enabled
- **THEN** non-breaking `BuildPlan` entries matching each eligible derelict building's block, rotation, and config are queued on `unit.plans` and `SelfBuildTask` navigates to reclaim them even if the core lacks materials to build those blocks from scratch

#### Scenario: Switching between Deconstruct and Reclaim Derelicts replaces existing plans
- **WHEN** derelict tiles already have breaking or non-breaking plans in `unit.plans` and the player clicks **Reclaim All Derelicts** or **Deconstruct All Derelicts**
- **THEN** the existing plans at those derelict tile coordinates are replaced without creating duplicate `BuildPlan` entries
