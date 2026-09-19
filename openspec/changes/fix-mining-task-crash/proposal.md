## Why

In Mindustry release build 160.4 on Android, running Autoplay with the Mining task crashes the entire game with a `NullPointerException` inside `mindustry.ai.BlockIndexer.findClosestOre`. This occurs because Mindustry's internal indexer contains unguarded tile dereferences (`world.tile(arr.first()).block()`) when tile coordinates in the indexer become invalid or out-of-bounds, and `MiningTask` unconditionally calls `findClosestOre` even for units and items that only mine wall ores. Furthermore, `MiningTask` queries all items every frame and fails to validate wall ores properly.

## What Changes

- **Engine Exception Guarding**: Safely invoke Mindustry's `BlockIndexer` methods (`findClosestOre`, `findClosestWallOre`) with defensive exception handling, returning `null` silently if an engine-level `NullPointerException` or other error occurs.
- **Strict Floor vs. Wall Separation**: Align `findOreTile` with vanilla `MinerAI` by querying `findClosestOre` only when `unit.type.mineFloor && Vars.indexer.hasOre(item)`, and querying `findClosestWallOre` only when `unit.type.mineWalls && Vars.indexer.hasWallOre(item)`.
- **Wall Ore Validation Support**: Update `isValidOreTile` to support both floor ores (`tile.drop() == item && tile.block() == Blocks.air`) and wall ores (`tile.wallDrop() == item || (tile.block() != null && tile.block().itemDrop == item)`).
- **Throttled Target Evaluation**: Throttle candidate ore searches in `MiningTask` so expensive multi-item indexer lookups occur on an interval (e.g. every 30-60 ticks or when the target item changes/completes) rather than querying every game item on every frame tick.

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

- `autonomous-gameplay-ai`: Update Task 8 (Mining) and Safe Evaluation requirements to mandate defensive indexer error handling, strict separation of floor vs. wall ore searches, wall ore validity checks, and throttled evaluation.

## Impact

- **Modified Files**: `mindustrytool.features.autoplay.tasks.MiningTask.java`
- **Dependencies**: No external dependency changes. Compatible with Mindustry Java 8 runtime.
- **Behavioral Impact**: Fixes fatal game crash during autoplay mining on both Android and desktop, restores proper mining support for wall ores (such as Beryllium on Erekir), and reduces per-tick CPU overhead.
