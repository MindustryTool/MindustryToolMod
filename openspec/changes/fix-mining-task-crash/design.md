## Context

`MiningTask` drives autonomous mining behavior when `AutoplayFeature` is active. During gameplay, `MiningTask.update(Unit unit)` scans all available items in the game, identifies the lowest-stock item accepted by the core, and resolves the closest matching ore tile using Mindustry's `Vars.indexer`.

In Mindustry release build 160.4, an unhandled engine flaw in `BlockIndexer.findClosestOre` / `findClosestWallOre` can return a `null` tile from `world.tile(arr.first())` (e.g. during map transitions, world bounds changes, or stale quadrant indices) and immediately attempts to call `.block()`, throwing a fatal `NullPointerException`. In addition, `MiningTask` currently calls `findClosestOre` even when `mineFloor` is false, fails to support wall ores in `isValidOreTile`, and queries the indexer for every game item every single tick.

## Goals / Non-Goals

**Goals:**
- Eliminate all game crashes caused by `BlockIndexer` NPEs in `MiningTask`.
- Align floor vs. wall ore queries with unit capabilities and map indexer presence.
- Support wall ore mining by updating ore tile validity checks to accept wall drops and solid wall blocks.
- Throttle candidate item and tile resolution to eliminate unnecessary per-tick CPU load.

**Non-Goals:**
- Modifying engine classes or bytecode outside the mod.
- Altering user configuration options or Solim UI for autoplay mining.
- Changing core item prioritization and hysteresis transfer logic.

## Decisions

### Decision 1: Defensive Indexer Invocation (Silent Try/Catch)
Wrap calls to `Vars.indexer.findClosestOre` and `Vars.indexer.findClosestWallOre` in private helper methods that catch all `Throwable` errors and return `null`.
- *Rationale*: Since Mindustry's internal indexer is outside the mod's direct control, defensive catches prevent engine-level indexing anomalies from crashing the game client. Silent handling was chosen to prevent console log spam during fast frame updates.
- *Alternatives considered*: Logging warnings with `Log.warn`/`Log.debug` (rejected per user preference to keep logs clean).

### Decision 2: Distinct Floor and Wall Ore Query Paths
Evaluate floor and wall queries independently based on unit capabilities and indexer contents:
```java
Tile tile = null;
if (unit.type.mineFloor && Vars.indexer.hasOre(item)) {
    tile = safeFindClosestOre(originX, originY, item);
}
if (tile == null && unit.type.mineWalls && Vars.indexer.hasWallOre(item)) {
    tile = safeFindClosestWallOre(originX, originY, item);
}
```
- *Rationale*: Matches vanilla Mindustry `MinerAI` behavior. Prevents requesting floor ores for units that can only mine walls (such as Erekir units) and vice versa.

### Decision 3: Expand `isValidOreTile` to Support Wall Ores
Update `isValidOreTile(@Nullable Tile tile, Item item)` to recognize valid wall ores:
- Floor ore is valid when: `tile.drop() == item && (tile.block() == Blocks.air || tile.block() == null)`
- Wall ore is valid when: `(tile.wallDrop() == item || (tile.block() != null && tile.block().itemDrop == item))`
- *Rationale*: Previously, `isValidOreTile` required `tile.block() == Blocks.air` and `tile.drop() == item`, which made it impossible to validate wall ores returned by `findClosestWallOre`.

### Decision 4: Throttling Candidate Ore Evaluation
Instead of iterating through every item in `Vars.content.items()` and invoking indexer searches on every frame, use a tick interval timer (e.g. 30-60 ticks) to refresh candidate selection, while immediately invalidating when:
- The core cannot accept any more of the target item (`core.acceptStack(targetItem, 1, unit) <= 0`).
- The current target ore tile becomes invalid or mined out.
- The unit's inventory fills up.

## Risks / Trade-offs

- **[Risk] Suppressing engine exceptions could hide map corruptions** → Mitigation: Only wrap `findClosestOre` and `findClosestWallOre`; if `null` is returned, the task yields or selects another valid resource safely.
- **[Risk] Throttling candidate scans could slow down target switching** → Mitigation: Keep immediate checks for core capacity and invalid current target; only throttle the multi-item candidate search loop.
