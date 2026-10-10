## Context

`SelfBuildTask` in `AutoplayFeature` manages personal construction and deconstruction plans queued on the player's unit (`unit.plans`). Its expandable settings section in `AutoplaySettingsView` currently contains a single one-shot action button, **Deconstruct All Derelicts** (`deconstructDerelicts()`), which iterates over `Vars.state.teams.get(Team.derelict).buildings` and appends breaking `BuildPlan` entries to `unit.plans`.

In Mindustry (`InputHandler.tryRepairDerelict`, `BuilderComp`, `Build.beginPlace`), derelict structures belonging to `Team.derelict` can be reclaimed for the player's team by queuing a non-breaking `BuildPlan(b.tileX(), b.tileY(), b.rotation, b.block, b.config())`. When a unit executes a derelict repair plan (`plan.isDerelictRepair()`), Mindustry's build engine skips core material consumption and directly transfers ownership (`b.changeTeam(team)`).

However, `SelfBuildTask.canAffordPlan(core, plan)` currently checks core item inventory for all non-breaking `BuildPlan` entries. Without accounting for `plan.isDerelictRepair()`, `SelfBuildTask` falsely marks derelict reclaim plans as lacking materials whenever the core does not hold enough items to build those blocks from scratch.

## Goals / Non-Goals

**Goals:**
- Add a one-shot **Reclaim All Derelicts** button stacked vertically in `SelfBuildTask.buildSettings(AutoplayFeature)` alongside **Deconstruct All Derelicts**.
- Queue valid non-breaking `BuildPlan` entries for all reclaimable `Team.derelict` buildings on the map when clicked.
- Replace any existing `BuildPlan` at the same `(tileX, tileY)` coordinate when queuing either reclaim or deconstruct plans so repeated clicks or switching between Deconstruct and Reclaim do not create duplicate or conflicting plans.
- Update `SelfBuildTask.canAffordPlan` so derelict repair plans (`plan.isDerelictRepair()`) and rotation plans (`plan.isRotation(unit.team)`) evaluate as affordable (`true`) with zero core resource requirements.

**Non-Goals:**
- Adding a persistent background toggle that continuously scans for derelict blocks every tick.
- Modifying `RebuildTask` or team ghost `BlockPlan` queues.

## Decisions

### 1. Vertical Button Layout in `SelfBuildTask.buildSettings`
- Keep the existing `column().growX().gap(unit(1))` container in `SelfBuildTask.buildSettings`.
- Render **Reclaim All Derelicts** (`feature.autoplay.settings.self-build.reclaim-derelict`) and **Deconstruct All Derelicts** (`feature.autoplay.settings.self-build.annihilate-derelict`) as full-width (`growX()`) `WebStyles.secondary()` buttons with `height(unit(10))` and descriptive tooltips.

### 2. Eligibility Filtering for Reclaimable Derelict Buildings
- In `reclaimDerelicts()`, verify:
  1. `Unit unit = Vars.player.unit()` is non-null, valid, `unit.canBuild()`, and `unit.team != Team.derelict`.
  2. `Vars.state.rules.derelictRepair` is `true`.
  3. For each building `b` in `Vars.state.teams.get(Team.derelict).buildings`:
     - `b.block != null && b.block.allowDerelictRepair`
     - `Build.validPlaceIgnoreUnits(b.block, unit.team, b.tileX(), b.tileY(), b.rotation, true, true)` is `true` (ensuring the block is placeable/unlocked, not inside an enemy core protection zone, and not hidden by static fog, while ignoring temporary unit overlap).

### 3. Coordinate-Based Plan Deduplication & Replacement
- When queuing derelict plans (both in `reclaimDerelicts()` and `deconstructDerelicts()`), replace any existing non-rebuild `BuildPlan` in `unit.plans` at `(b.tileX(), b.tileY())` before appending the new `BuildPlan`.
- Using an `IntSet` of packed coordinates (`Point2.pack(x, y)`) or removing matching plans from `unit.plans` in a single pass avoids $O(N^2)$ scans when maps contain hundreds of derelict structures, and allows the player to switch cleanly between **Deconstruct All Derelicts** and **Reclaim All Derelicts**.

### 4. Zero-Cost Affordability Check in `canAffordPlan`
- In `SelfBuildTask.canAffordPlan(Building core, BuildPlan plan)` (or `canAffordPlan(Unit unit, Building core, BuildPlan plan)`):
  - Return `true` immediately when `plan.breaking`, `plan.isDerelictRepair()`, `plan.isRotation(unit.team)`, `Vars.state.rules.infiniteResources`, or `unit.team.rules().infiniteResources`.
  - This aligns `SelfBuildTask`'s affordability check with Mindustry's `BuilderComp.shouldSkip` contract so derelict blocks are reclaimed even when the core has zero resources.

## Risks / Trade-offs

- **[Risk] Large number of derelict blocks on campaign/custom maps** → Batch-removing existing plans at target coordinates via `IntSet` (`Point2.pack(x, y)`) keeps plan replacement $O(N + M)$ rather than $O(N \times M)$.
- **[Risk] Map rules disable derelict repair (`Vars.state.rules.derelictRepair == false`)** → Guarding `reclaimDerelicts()` on `Vars.state.rules.derelictRepair` prevents queuing unbuildable plans when the map rule forbids derelict repair.
