## 1. Setup & Configuration

- [ ] 1.1 Create \PathfindingFeature.java\ stub and add translation keys for config labels, dialogs, and tooltips in \undle.properties\.
- [ ] 1.2 Define \ConfigValue<T>\ fields in \PathfindingFeature\ (drawUnitPath, drawSpawnPath, zoomThreshold, opacity, and cost types).
- [ ] 1.3 Create \PathfindingSettingsDialog.java\ with Solim declarative UI layout for pathfinding settings.
- [ ] 1.4 Create \PathfindingSettingsView.java\ implementing the Solim view that reacts to config signals.

## 2. Pathfinding Logic (Two Systems)

- [ ] 2.1 Implement \ClientPathfinder.java\ copying the BFS flowfield logic for wave units and A* for commanded units.
- [ ] 2.2 Implement \PathfindingCache.java\ and \PathfindingCacheManager.java\ to deduplicate and cache path calculations.
- [ ] 2.3 Add update logic in \PathfindingFeature\ for HOST mode: use \Vars.pathfinder\ and \Vars.controlPath\ to trace paths.
- [ ] 2.4 Add update logic in \PathfindingFeature\ for CLIENT mode: use \ClientPathfinder\ to trace paths.
- [ ] 2.5 Add update logic for spawn point paths using the respective mode's flowfield.

## 3. Rendering & Polish

- [ ] 3.1 Implement drawing logic for wave unit paths (lines between tiles) with solid team color.
- [ ] 3.2 Implement drawing logic for commanded unit paths with solid team color.
- [ ] 3.3 Implement drawing logic for spawn point paths with solid team color.
- [ ] 3.4 Hook into \Trigger.draw\ to call the rendering logic.
- [ ] 3.5 Apply frame budgeting and frustum culling to optimization path rendering.
- [ ] 3.6 Ensure paths are not drawn for the player's own active unit, but drawn for allies and enemies.
