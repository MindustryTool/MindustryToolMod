## Why

Smart Drill is the mod's fastest way to turn a clicked ore patch into a working drill field, but it is a two-step popup that only understands one drill (2x2 floor drills), caps the whole patch with a `maxTiles` quota the player cannot see, and places bridges that are never used. Its ore detection walks through solid walls and then deletes them, its 2x2 lattice is built with `%` on signed coordinates so it breaks in three quadrants of the map, and its beam-drill chain places a duct diagonally for 1x1 drills, so the plasma bore fills up and stalls. None of the placement rules can be tested: they are fused to `Vars.world`, `unit.addBuild`, and Arc tables, so the only feedback is "look at the map and see".

Rebuilding the feature around a pure planner — one that takes an ore target, a rectangle and a drill, and returns a plan — makes the algorithm testable, makes the panel declarative, and lets the same plan drive both the in-game ghost preview and a generated PNG a reviewer can look at.

## What Changes

- **BREAKING** — Removes the `maxTiles` setting. The visible, draggable rectangle replaces the quota entirely.
- **BREAKING** — Removes the two-step popup (direction pad, then drill grid) and the floating `arc.scene.ui.layout.Table` it was built from. Replaced by a docked Solim panel.
- **Target ore rule** — A drill is only valid when *every* ore tile in its footprint is the ore the player clicked. Mixed-ore footprints are rejected, because `Drill` produces only its dominant item and would silently mine something else.
- **Two separate ore floods** — Clicking floor ore floods floor ore only; clicking wall ore floods wall ore only. The old flood merged patches that were only connected through solid rock. Both floods are capped at 100 ore tiles and the bounding box is clamped to 10x10.
- **Visible coverage rectangle** — Replaces the invisible quota with a rectangle the player drags. Edge handles only, so camera panning is unaffected. Recompute happens on drag release. No hard size ceiling.
- **Per-type placement strategies** — 1x1 and 3x3+ floor drills use a densest-first greedy scan; 2x2 floor drills keep the existing world-anchored lattice, now with `Math.floorMod` so the pattern is identical in every quadrant; beam drills use a rewritten wall-band scan.
- **Minimal bridge chains** — The 3-periodic bridge lattice is unchanged, but only nodes required to connect the planned drills to the output are built. A shortest-path tree is pruned to a minimal subtree, and a single duct stub is placed at the root.
- **Beam-drill chain fixed** — One unit is body facing the wall, duct directly behind it, beam node two tiles further back. The current code places the duct diagonally for 1x1 drills, which leaves the drill with no adjacent output.
- **Schematic binding** — Players can bind many schematics to a drill block. Selecting one places it once at the highest-scoring position where every drill's footprint is pure target ore and every other tile is placeable. Drills must land inside the rectangle; the rest of the rig may extend outside it.
- **Efficiency metric** — Best position is the one that maximises total mineable target-ore tiles under the drills, then minimises blocks placed, then minimises distance to the rectangle centre.
- **Ghost preview** — The plan is drawn as full block sprites (culled to the camera view) without ever entering `unit.plans`, so it cannot be built accidentally. `Place` converts the ghosts into real build plans; `Cancel` discards them. `autoDisable` fires after `Place`.
- **Testable planning core** — Placement rules move behind a `DrillPlanner` interface over a `DrillWorldView`, with a Mindustry-backed implementation for the game and an in-memory implementation for tests.
- **Plan preview renderer** — A scenario-driven harness renders a floor image and a placed-plan image per scenario to `mod/build/smartdrill-preview/`, so placement quality can be reviewed visually instead of inferred from the map.

## Capabilities

### New Capabilities

- `smart-drill-planning`: The pure planning core — target ore resolution, floor and wall ore floods, the coverage rectangle, per-size and per-type placement strategies, minimal bridge chains, beam-drill units, schematic placement, and the efficiency metric.
- `smart-drill-ui`: The compact docked panel, the world-space area overlay with edge drag handles, the facing row, drill and rig rows, the ghost preview, and the Place/Cancel lifecycle including `autoDisable`.
- `smart-drill-plan-preview`: The deterministic scenario harness that renders a floor image and a placed-plan image per scenario, plus the invariants the scenarios assert.

### Modified Capabilities

<!-- None. The legacy stub registry in `app-settings` still lists smart-drill as a metadata-only stub, which already does not match the current implementation; this change neither relies on nor alters that registry, and reconciling it is out of scope. -->

## Impact

- `mod/src/mindustrytool/features/smartdrill/` — `SmartDrillFeature` is reduced to tap handling, panel state and overlay wiring. `place2x2Drill`, `placeBeamDrill`, `findAllConnectedOreTiles`, `findAllConnectedWallOreTiles`, `expandTiles`, `isDrillTile` and `isBridgeTile` move behind the planner. New `plan/` and `ui/` packages.
- `mod/src/test/java/mindustrytool/features/smartdrill/` — `SmartDrillFeatureTest` is rewritten. Its `Direction`, `isDrillTile`, `isBridgeTile` and `maxTilesConfig` assertions move to the planner or disappear with the setting.
- `assets/bundles/bundle.properties` — New keys for the panel rows, the rectangle size readout, the facing row, the drill count / efficiency readout, and the large-plan warning. The `feature.smart-drill.max-tiles` key is removed.
- `SmartDrillSettingsView` / `SmartDrillSettingsDialog` — Lose the `maxTiles` slider; keep `autoDisable`.
- No new dependencies. The planner is pure Java 8; the preview renderer uses `arc.graphics.Pixmap` and `PixmapIO` only.