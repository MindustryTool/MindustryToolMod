# Display Features (Healthbar, Pathfinding, TeamResource, Range, Progress, QuickAccess, ToggleRendering, WavePreview, ItemVisualizer) Audit

## HealthBar

### Anti-Patterns
- **MEDIUM** `HealthBarVisualizer.java:55` — Shield bar loop: `while (shieldValue > 0)` with `shieldValue -= 1` capped at 20 — still unbounded if shieldValue is NaN (already checked but defensive)
- **LOW** `HealthBarVisualizer.java:47` — `barRegion` loaded lazily on first draw; if atlas fails, falls back to `Core.atlas.white()` — good defense
- **MEDIUM** `HealthBarConfig.java:5-8` — Static mutable fields with `load()`/`save()` pattern; not thread-safe if settings changed during gameplay

## Pathfinding

### Performance
- **HIGH** `PathfindingDisplay.java:73-90` — Draw and update run every frame; re-calculates culling bounds, iterates all enemy units (potentially thousands), allocates `Rect` and `Tmp.r1` each frame
- **HIGH** `PathfindingDisplay.java:145-155` — Pathfinding recalculation uses `Pathfinder.getTargetTile` per step; for 250 steps per unit × many units, this is very expensive per frame
- **MEDIUM** `PathfindingDisplay.java:272-281` — Spawn path cache data array starts at 2048 and doubles on overflow; but the initial 2048 is wasted for most entries

### Concurrency
- **MEDIUM** `PathfindingDisplay.java:96-101` — `currentFrameUpdates` is instance field modified in update loop; but update and draw may run in same frame on same thread (Mindustry single-threaded)

### Anti-Patterns
- **MEDIUM** `PathfindingDisplay.java:53-55` — Numerous magic constants for cache update intervals; should be configurable or derived
- **MEDIUM** `PathfindingDisplay.java` — `@SuppressWarnings("unchecked")` raw type casts for path cache

## TeamResource

### Performance
- **MEDIUM** `TeamResourceFeature.java:113-116` — Timer fires every 30 frames; `Vars.content.items().contains(...)` called inside update loop that iterates all items
- **MEDIUM** `TeamResourceFeature.java:198-206` — Rebuilds entire UI on every item discovery; if many items discovered, causes frequent full rebuilds
- **MEDIUM** `TeamResourceFeature.java:224-247` — Power stats iterate `Groups.build` every 30 frames; for large builds with many power graphs, this is expensive

### Anti-Patterns
- **MEDIUM** `TeamResourceFeature.java:84` — `Events.run(ResetEvent.class, ...)` — clears `usedItems` but doesn't reset other state; partial state reset
- **MEDIUM** `TeamResourceFeature.java:342-350` — `formatItem` uses string color codes hardcoded; mix of presentation and logic
- **MEDIUM** `TeamResourceFeature.java:68-72` — `teamGraphs` Seq.equals used for change detection — `PowerGraph` may not implement equals properly

## Range

### Performance
- **LOW** `RangeDisplay.java:108` — `Vars.indexer.eachBlock(null, ...)` iterates all blocks in range every draw frame; could be expensive with many blocks

### Anti-Patterns
- **MEDIUM** `RangeDisplay.java:47-54` — Uses `instanceof` chains for range calculation; adding new block types requires modifying this switch-like logic
- **LOW** `RangeDisplay.java:61` — `MAX_RANGE = 169 * Vars.tilesize` — hardcoded based on chunk size but depends on internal game constant

## Progress

### Findings
- **LOW** `ProgressDisplay.java` — Relies on Trigger.draw; may have z-ordering issues with other overlay features

## QuickAccess

### Findings
- **MEDIUM** `QuickAccessFeature.java` — Hardcodes feature references in quick access toolbar; adding new quick-access features requires code change, not configurable at runtime

## ToggleRendering

### Findings
- **LOW** `ToggleRenderingFeature.java` — Toggles rendering of game elements; uses reflection to access private rendering fields — fragile per Mindustry updates

## WavePreview

### Findings
- **LOW** `WavePreviewFeature.java` — Reads incoming wave data for preview; may not work correctly with modded units/types

## ItemVisualizer
- **MEDIUM** `ItemVisualizerFeature.java` — Commented out in Main.java registration; dead code maintained but never loaded
