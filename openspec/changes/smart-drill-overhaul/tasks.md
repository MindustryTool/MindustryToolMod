## 1. Spikes and verification

- [ ] 1.1 Read `PowerNode.createLinks` and confirm whether a power node links past an intervening duct; record the answer in `design.md` and adjust the beam-node offset if it does not
- [x] 1.2 Build a scratch schematic containing an item bridge and confirm which config classes must be rotated (`Point2`, `Point2[]`, block-level `pointConfig`)
- [ ] 1.3 Measure the cost of drawing full ghost sprites for 400 plans on the target device; if it exceeds budget, fall back to the rectangle renderer beyond a sprite cap
- [ ] 1.4 Confirm the v146 drill roster and each drill's `size`, `tier`, and `range` from the running game, and record it for the strategy table
- [x] 1.5 Confirm the 2x2 lattice geometry against `Tile.getLinkedTilesAs` and `Block.sizeOffset`, and record that a 2x2 footprint at `(x, y)` covers `(x..x+1, y..y+1)`

## 2. Planning core scaffolding

- [x] 2.1 Create `features/smartdrill/plan/` with `DrillPlanRequest`, `DrillPlanResult`, and `PlannedBuild` as plain data with no Arc or `Vars` reference
- [x] 2.2 Define `DrillWorldView` exposing tile lookup, solidity, ore kind at a tile, and a buildability check
- [x] 2.3 Implement `MindustryDrillWorldView` delegating buildability to the engine placement check
- [x] 2.4 Implement `TestDrillWorld` with a builder for base floor, rectangles, scatter, and wall bands
- [x] 2.5 Define `DrillPlanner` and a registry keyed by drill kind and size, with a greedy fallback for unregistered sizes
- [x] 2.6 Add a test asserting the planner produces identical output when run twice against the same in-memory world

## 3. Ore targeting and floods

- [x] 3.1 Resolve the target ore kind (floor vs wall) from the tapped tile and reject tiles with neither
- [x] 3.2 Implement the 4-connected floor-ore flood with a 100-tile cap and no traversal through non-target tiles
- [x] 3.3 Implement the 8-connected wall-ore flood with the same 100-tile cap
- [x] 3.4 Clamp both bounding boxes to 10x10 tiles
- [x] 3.5 Test that two patches separated by rock produce one region, not two merged
- [x] 3.6 Test that a diagonal vein clamps to 10x10 rather than its diagonal extent

## 4. Coverage rectangle model

- [x] 4.1 Add the rectangle to `DrillPlanRequest` as integer world bounds
- [x] 4.2 Enforce "floor footprints must lie inside the rectangle" in the floor strategies
- [x] 4.3 Enforce "beam rectangle bounds the wall band" and place bodies outside it
- [x] 4.4 Enforce "schematic drills inside, rig free" in the schematic candidate check
- [x] 4.5 Test that a schematic whose drills fit but whose conveyors overflow is accepted

## 5. Floor drill strategies

- [x] 5.1 Implement the strict ore dominance check as a shared predicate used by every strategy
- [x] 5.2 Port the 2x2 lattice to `Math.floorMod` and verify the pattern is identical across all four quadrants
- [x] 5.3 Implement the 2x2 strategy with no greedy fill
- [x] 5.4 Implement densest-first greedy for 1x1 and 3x3-or-larger
- [x] 5.5 Test that a mixed-ore footprint is rejected and a pure footprint is accepted
- [x] 5.6 Test that densest-first places the higher-coverage position first

## 6. Beam drill strategy

- [x] 6.1 Implement the wall-band scan with per-lane resolution against the target wall ore
- [x] 6.2 Reject positions where any lane resolves to a different ore or where no lane resolves
- [x] 6.3 Place the unit as body, duct directly behind, node two tiles further back, using the verified offset from 1.1
- [x] 6.4 Test that the duct is orthogonally adjacent to the body for both 1x1 and 2x2 beam drills
- [x] 6.5 Test that a lane hitting a different ore rejects the position

## 7. Bridge chains

- [x] 7.1 Compute required lattice nodes as those orthogonally adjacent to any planned drill
- [x] 7.2 Choose the root node furthest along the chosen facing direction
- [x] 7.3 Build the shortest-path tree and prune non-required leaves to a minimal subtree
- [x] 7.4 Configure each kept bridge with the delta to its parent
- [x] 7.5 Place the single duct stub at the root, facing away from the field, and skip it when blocked
- [x] 7.6 Test that a lattice node touching no drill gets no bridge
- [x] 7.7 Test that every kept bridge has a path to the root

## 8. Schematic placement

- [x] 8.1 Persist bindings as `block name -> schematic file name` JSON in the feature config
- [x] 8.2 Implement rotation and translation of schematics, including `Point2` configs
- [x] 8.3 Implement the candidate check: every drill pure target ore, every non-air tile placeable
- [x] 8.4 Implement the lazy per-schematic search, cached per rectangle
- [x] 8.5 Implement the score: target ore covered, then fewer blocks, then distance to centre
- [x] 8.6 Test that a blocked tile inside the rig rejects the candidate
- [x] 8.7 Test that more covered ore wins and that equal coverage prefers fewer blocks

## 9. Preview and scenario harness

- [x] 9.1 Implement `PlanSchematics` emitting a floor and a plan `.msch` per scenario, with the floor image rendered locally from block `mapColor` and the plan image rendered by the schematic image API
- [x] 9.2 Write the scenario previews to `mod/build/smartdrill-preview/` and treat an API failure as a skipped image, not a test failure
- [x] 9.3 Write scenarios: dense patch, thin vein, mixed ore, diagonal vein, wall seam, empty patch
- [x] 9.4 Assert the invariants: pure footprints, no overlap, inside rectangle
- [x] 9.5 Confirm the suite writes both images for every scenario on every run and that no count assertions exist

## 10. Panel and overlay

- [x] 10.1 Create `SmartDrillPanel` as a Solim `Component` with target, facing, area, drill, rig and action rows
- [x] 10.2 Show the facing row always, the drill row only with valid drills, the rig row only with bound schematics
- [x] 10.3 Add the live drill count and efficiency readout plus the large-plan warning
- [x] 10.4 Implement `DrillAreaOverlay` drawing the rectangle and edge handles in a `Trigger.draw` handler guarded on `isEnabled()`
- [x] 10.5 Wire edge-handle drags to resize and recompute on release; leave other drags to camera panning
- [x] 10.6 Ignore world taps while the panel is open, except drags beginning on a handle
- [x] 10.7 Mount the panel into `Vars.ui.hudGroup` on open and dispose it on close

## 11. Ghost preview and commit

- [x] 11.1 Draw the ghost plan with full block sprites, culled to the camera view, with zero per-frame allocation
- [x] 11.2 Wire `Place` to convert the ghost plan into real build plans and add them to the player's build queue
- [x] 11.3 Wire `Cancel` to discard the ghost plan
- [x] 11.4 Fire `autoDisable` after a successful `Place` when the setting is enabled
- [ ] 11.5 Test that nothing enters the build queue before `Place`

## 12. Feature wiring and cleanup

- [x] 12.1 Reduce `SmartDrillFeature` to tap handling, panel state and overlay wiring
- [x] 12.2 Delete `place2x2Drill`, `placeBeamDrill`, `findAllConnectedOreTiles`, `findAllConnectedWallOreTiles`, `expandTiles`, `isDrillTile`, `isBridgeTile`, and the raw `Table` popup
- [x] 12.3 Remove `maxTilesConfig` from `SmartDrillFeature` and the settings view, keeping `autoDisable`
- [x] 12.4 Remove the now-unused `feature.smart-drill.max-tiles` bundle key and its comments
- [x] 12.5 Confirm the draw handler returns on a disabled feature before reading any state

## 13. Localization

- [x] 13.1 Add bundle keys for the target row, facing row, area readout, drill row, rig row and action row
- [x] 13.2 Add bundle keys for the drill count, efficiency readout, no-drill message and large-plan warning
- [x] 13.3 Add a descriptive comment above every new key, documenting placeholders
- [x] 13.4 Confirm no new user-visible string is hardcoded in Java

## 14. Verification

- [x] 14.1 Rewrite `SmartDrillFeatureTest` around the planner; drop `Direction`, `isDrillTile`, `isBridgeTile` and `maxTilesConfig` assertions
- [x] 14.2 Run the full module test suite and confirm the scenario previews were regenerated
- [x] 14.3 Run `:checkBundles` and the Solim modifier import check
- [ ] 14.4 Review the generated images for each scenario and record any placement that looks wrong
- [ ] 14.5 Confirm a disabled feature performs no frame-loop work and the ghost pass allocates nothing