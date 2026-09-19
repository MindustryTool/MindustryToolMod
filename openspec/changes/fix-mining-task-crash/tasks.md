## 1. Safe Indexer Queries & Floor/Wall Separation

- [ ] 1.1 Add defensive helper methods (`safeFindClosestOre`, `safeFindClosestWallOre`) in `MiningTask` catching `Throwable` silently and returning null
- [ ] 1.2 Refactor `findOreTile` in `MiningTask` to query floor ores only when `unit.type.mineFloor && Vars.indexer.hasOre(item)` and wall ores only when `unit.type.mineWalls && Vars.indexer.hasWallOre(item)`
- [ ] 1.3 Update fallback nearby scan (`findNearbyUncoveredOre`) to only execute for floor tiles, avoiding spurious scans for wall blocks

## 2. Wall Ore Validation & Candidate Throttling

- [ ] 2.1 Update `isValidOreTile` in `MiningTask` to recognize valid wall ores using `tile.wallDrop()` and `tile.block().itemDrop`
- [ ] 2.2 Implement timer throttling for multi-item candidate evaluation in `MiningTask.update` while retaining immediate switching when core accepts 0 or target becomes invalid

## 3. Verification & Build Validation

- [ ] 3.1 Verify Java 8 runtime compatibility and ensure no forbidden imports or post-Java-8 standard library methods are introduced
- [ ] 3.2 Run `./gradlew :mod:check` and `./gradlew :mod:compileJava` to verify compilation and checkstyle compliance
