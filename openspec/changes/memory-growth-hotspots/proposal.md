## Why

A scan of the mod's hot paths (`Trigger.draw`/`Trigger.update` listeners) and long-lived in-memory collections found several sources of fast memory growth that are not captured by the existing `agent-perf-discipline` spec: copy-heavy mutation paths, unbounded caches/maps that retain references across worlds, per-frame boxing/reflection/lambda allocation in features that draw every frame, and missing `isEnabled()` guards. These accumulate GC pressure and, in some cases, retain stale objects (PowerGraph, Item, bridge states) across world loads. This change records the decisions made per hotspot and scopes the fixes that were accepted.

## What Changes

Accepted fixes (implementation to follow):

- `features/teamresource/TeamResourceState.java`: clear `itemHistories` and `graphHistories` maps (not just buffer contents) on `reset()` / `onWorldLoad()` / `setSelectedTeam()`, so stale `PowerGraph`/`Item` references are released.
- `features/togglerendering/ToggleRenderingFeature.java`: refactor `updateVisibility` to avoid per-frame lambda capture, the `tileview.removeAll(...)` scan, and per-unit reflection; cache the draw-index field once and batch restore.
- `features/blockstats/BlockStatsDisplayFeature.java`: cache `zoomThreshold`/`opacity`/`scale` as primitive fields updated via `Effect`, instead of boxing `Float` per frame in `draw()`.
- `features/autoplay/AutoplayFeature.java`: replace the per-frame cooldown label string concat with a cached `String` field updated only when the rounded value changes.
- `features/music/MusicFeature.java`: add `isEnabled()` guard as the first statement of the 0.1s timer callback.
- `features/logicvisualizer/LogicVisualizerFeature.java`: add camera-bounds frustum culling in `draw()` (match `HealthBarFeature` pattern).
- `features/pathfinding/PathfindingFeature.java` + `PathfindingCacheManager.java`: lower cleanup interval to 30 frames and add a hard cap of 4096 entries evicting oldest `lastUsedTime`.
- `features/visualizer/VisualizerFeature.java`: clear `statePool` on world load / reset.

Explicitly deferred / left as-is (recorded decisions, no change):

- `features/chat/state/ChatMessages.java` copy-on-write allocations (leave as-is).
- ~68 constructor-registered listeners: only hot (draw/update) listeners get `isEnabled()` guards; UI/event listeners deferred (D7).
- `TeamResourceState.usedItems` never shrinks (leave as-is).
- Chat message/user trimming (D15) (leave as-is).
- `MusicFeature.trackStates` stale entries (leave as-is).
- `ImageCache` eviction disposal (keep no-dispose).
- solim-reactive defensive copies (leave as-is).
- `FileIcon`/`ReflectUtil` caches (bounded by code surface).

## Capabilities

### New Capabilities
- (none — behavior is already covered by `agent-perf-discipline`)

### Modified Capabilities
- `agent-perf-discipline`: extend the performance-discipline requirements to cover (a) long-lived collection reference release on world load, (b) per-frame zero-allocation for the additional hot features (ToggleRendering, BlockStats, Autoplay, Pathfinding, LogicVisualizer, Visualizer state pool), and (c) hot-listener `isEnabled()` gating scope.

## Impact

Affected code: `mod/src/mindustrytool/features/{teamresource, togglerendering, blockstats, autoplay, music, logicvisualizer, pathfinding, visualizer}/*`. No API or dependency changes. No new user-visible text. Builds on existing `RenderLoopAllocationTest` gate. Implementation is deferred to a follow-up change; this proposal captures decisions only.