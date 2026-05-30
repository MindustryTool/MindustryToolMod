# Gameplay Features (Time Control, Smart Drill, Smart Upgrade) Audit

## Time Control

### Security
- **HIGH** `TimeControlFeature.java:76` — `Time.setDeltaProvider(() -> ...)` overrides the global delta time provider — affects ALL game systems including physics, AI, networking. Speed multiplier of 16x could desync multiplayer games.
- **MEDIUM** `TimeControlFeature.java:62` — Only hidden when `Vars.net.client()` — but as client, `Time.setDeltaProvider` still overrides local clock

### Anti-Patterns
- **MEDIUM** `TimeControlFeature.java:29-32` — `SPEEDS` array + `selected` + `doubleSpeed` — three mutable variables to track one state; `doubleSpeed` toggling creates non-obvious speed values
- **LOW** `TimeControlFeature.java:50-58` — Drag listener creates `lastX`/`lastY` as local variables captured by closure on each rebuild; if the dialog is rebuilt while dragging, these are reset

### Dead Code
- **MEDIUM** `TimeControlConfig.java` — Separate x/y for portrait/landscape but never updates when orientation changes; orientation is checked only at read time, not at event time

## Smart Drill

### Performance
- **MEDIUM** `SmartDrillFeature.java:249-261` — `findAllConnectedOreTiles` uses BFS with sorting on each iteration (`queue.sort(...)` in while loop) — O(n log n) per expansion step; for large ore patches, this is very expensive
- **MEDIUM** `SmartDrillFeature.java:298-300` — `expandTiles` called 3 times, each calling `expandTiles` which iterates all tiles and their 4 neighbors — O(n*m) where n grows each iteration

### Concurrency
- **MEDIUM** `SmartDrillFeature.java:59-62` — `TapEvent` listener and `TapListener` hold listener both fire for same tile; ordering between the two is non-deterministic

### Anti-Patterns
- **HIGH** `SmartDrillFeature.java:310-330` — `isDrillTile` uses hardcoded modulo arithmetic `(tile.x % 6 == 0 || ...)` — only works for specific drill grid patterns; fragile and not documented
- **HIGH** `SmartDrillFeature.java:177-213` — `placeBeamDrill` method is extremely complex (50+ lines) with nested loops and hardcoded offsets; many magic numbers for drill positioning
- **MEDIUM** `SmartDrillFeature.java:239-243` — `place2x2Drill` uses `expandTiles(tiles, 3)` which modifies the tiles sequence; `retainAll` after expansion may remove tiles that were added during expansion
- **MEDIUM** `SmartDrillFeature.java:159` — `Direction` enum has `mul`, `offset`, `withOrigin`, `opposite`, `rotate90Degrees` methods — some unused or over-engineered
- **MEDIUM** `SmartDrillFeature.java:231-240` — `findAllConnectedOreTiles` sorts by distance from center on every iteration — O(n² log n) worst case

## Smart Upgrade

### Performance
- **HIGH** `SmartUpgradeFeature.java:145-147` — `upgradeChain` iterates up to 500 tiles, checking `Build.validPlace` and creating `BuildPlan` per tile; for large conveyor networks, this places many build plans at once
- **MEDIUM** `SmartUpgradeFeature.java:180-182` — `checkAndAdd` called per neighbor per building; on dense networks with routers/junctions, explosion of queued tiles
- **MEDIUM** `SmartUpgradeFeature.java:96-98` — Iterates `Vars.content.blocks()` every time menu is shown — thousands of block types checked each time; slow on mobile

### Anti-Patterns
- **MEDIUM** `SmartUpgradeFeature.java:118-120` — `upgradeChain` dispatches via `Core.app.post` — defers build plan addition; user may toggle menu while plans are still queued
- **HIGH** `SmartUpgradeFeature.java:153-178` — Bridge/liquid-bridge traversal logic mixed with conveyor logic; tightly coupled to specific subtype detection
- **MEDIUM** `SmartUpgradeFeature.java:105-108` — `Timer.schedule(() -> toFront(), 5f)` — 5-second delay to bring menu to front; if user interacts before 5s, menu may be behind other elements

### Dead Code
- **LOW** `SmartUpgradeFeature.java:179-180` — DuctRouter and OverflowDuct appear in `checkAndAdd` but Duct conveyor type is also in `getGroup` — potential double-handling
