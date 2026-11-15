## 1. Spikes and verification

- [ ] 1.1 Read `PowerNode.createLinks` and confirm whether a power node links past an intervening duct; record the answer in `design.md` and adjust the beam-node offset if it does not
- [ ] 1.2 Build a scratch schematic containing an item bridge and confirm which config classes must be rotated (`Point2`, `Point2[]`, block-level `pointConfig`)
- [ ] 1.3 Measure the cost of drawing full ghost sprites for 400 plans on the target device; if it exceeds budget, fall back to the rectangle renderer beyond a sprite cap
- [ ] 1.4 Confirm the v146 drill roster and each drill's `size`, `tier`, and `range` from the running game, and record it for the strategy table

## 2. Planning core scaffolding

- [ ] 2.1 Create `features/smartdrill/plan/` with `DrillPlanRequest`, `DrillPlanResult`, and `PlannedBuild` as plain data with no Arc or `Vars` reference
- [ ] 2.2 Define `DrillWorldView` exposing tile lookup, solidity, ore kind at a tile, and a buildability check
- [ ] 2.3 Implement `MindustryDrillWorldView` delegating buildability to the engine placement check
- [ ] 2.4 Implement `TestDrillWorld` with a builder for base floor, rectangles, scatter, and wall bands
- [ ] 2.5 Define `DrillPlanner` and a registry keyed by drill kind and size, with a greedy fallback for unregistered sizes
- [ ] 2.6 Add a test asserting the planner produces identical output when run twice against the same in-memory world

## 3. Ore targeting and floods

- [ ] 3.1 Resolve the target ore kind (floor vs wall) from the tapped tile and reject tiles with neither
- [ ] 3.2 Implement the 4-connected floor-ore flood with a 100-tile cap and no traversal through non-target tiles
- [ ] 3.3 Implement the 8-connected wall-ore flood with the same 100-tile cap
- [ ] 3.4 Clamp both bounding boxes to 10x10 tiles
- [ ] 3.5 Test that two patches separated by rock produce one region, not two merged
- [ ] 3.6 Test that a diagonal vein clamps to 10x10 rather than its diagonal extent

## 4. Coverage rectangle model

- [ ] 4.1 Add the rectangle to `DrillPlanRequest` as integer world bounds
- [ ] 4.2 Enforce "floor footprints must lie inside the rectangle" in the floor strategies
- [ ] 4.3 Enforce "beam rectangle bounds the wall band" and place bodies outside it
- [ ] 4.4 Enforce "schematic drills inside, rig free" in the schematic candidate check
- [ ] 4.5 Test that a schematic whose drills fit but whose conveyors overflow is accepted

## 5. Floor drill strategies

- [ ] 5.1 Implement the strict ore dominance check as a shared predicate used by every strategy
- [ ] 5.2 Port the 2x2 lattice to `Math.floorMod` and verify the pattern is identical across all four quadrants
- [ ] 5.3 Implement the 2x2 strategy with no greedy fill
- [ ] 5.4 Implement densest-first greedy for 1x1 and 3x3-or-larger
- [ ] 5.5 Test that a mixed-ore footprint is rejected and a pure footprint is accepted
- [ ] 5.6 Test that densest-first places the higher-coverage position first

## 6. Beam drill strategy

- [ ] 6.1 Implement the wall-band scan with per-lane resolution against the target wall ore
- [ ] 6.2 Reject positions where any lane resolves to a different ore or where no lane resolves
- [ ] 6.3 Place the unit as body, duct directly behind, node two tiles further back, using the verified offset from 1.1
- [ ] 6.4 Test that the duct is orthogonally adjacent to the body for both 1x1 and 2x2 beam drills
- [ ] 6.5 Test that a lane hitting a different ore rejects the position

## 7. Bridge chains

- [ ] 7.1 Compute required lattice nodes as those orthogonally adjacent to any planned drill
- [ ] 7.2 Choose the root node furthest along the chosen facing direction
- [ ] 7.3 Build the shortest-path tree and prune non-required leaves to a minimal subtree
- [ ] 7.4 Configure each kept bridge with the delta to its parent
- [ ] 7.5 Place the single duct stub at the root, facing away from the field, and skip it when blocked
- [ ] 7.6 Test that a lattice node touching no drill gets no bridge
- [ ] 7.7 Test that every kept bridge has a path to the root

## 8. Schematic placement

- [ ] 8.1 Persist bindings as `block name -> schematic file name` JSON in the feature config
- [ ] 8.2 Implement rotation and translation of schematics, including `Point2` configs
- [ ] 8.3 Implement the candidate check: every drill pure target ore, every non-air tile placeable
- [ ] 8.4 Implement the lazy per-schematic search, cached per rectangle
- [ ] 8.5 Implement the score: target ore covered, then fewer blocks, then distance to centre
- [ ] 8.6 Test that a blocked tile inside the rig rejects the candidate
- [ ] 8.7 Test that more covered ore wins and that equal coverage prefers fewer blocks

## 9. Preview and scenario harness

- [ ] 9.1 Implement `PlanRenderer` drawing one pixel per tile into a `Pixmap` with distinct colours per tile kind
- [ ] 9.2 Implement `PreviewWriter` emitting floor and plan PNGs to `mod/build/smartdrill-preview/`
- [ ] 9.3 Write scenarios: dense patch, thin vein, mixed ore, diagonal vein, wall seam, empty patch
- [ ] 9.4 Assert the invariants: pure footprints, no overlap, inside rectangle
- [ ] 9.5 Confirm the suite writes both images for every scenario on every run and that no count assertions exist

## 10. Panel and overlay

- [ ] 10.1 Create `SmartDrillPanel` as a Solim `Component` with target, facing, area, drill, rig and action rows
- [ ] 10.2 Show the facing row always, the drill row only with valid drills, the rig row only with bound schematics
- [ ] 10.3 Add the live drill count and efficiency readout plus the large-plan warning
- [ ] 10.4 Implement `DrillAreaOverlay` drawing the rectangle and edge handles in a `Trigger.draw` handler guarded on `isEnabled()`
- [ ] 10.5 Wire edge-handle drags to resize and recompute on release; leave other drags to camera panning
- [ ] 10.6 Ignore world taps while the panel is open, except drags beginning on a handle
- [ ] 10.7 Mount the panel into `Vars.ui.hudGroup` on open and dispose it on close

## 11. Ghost preview and commit

- [ ] 11.1 Draw the ghost plan with full block sprites, culled to the camera view, with zero per-frame allocation
- [ ] 11.2 Wire `Place` to convert the ghost plan into real build plans and add them to the player's build queue
- [ ] 11.3 Wire `Cancel` to discard the ghost plan
- [ ] 11.4 Fire `autoDisable` after a successful `Place` when the setting is enabled
- [ ] 11.5 Test that nothing enters the build queue before `Place`

## 12. Feature wiring and cleanup

- [ ] 12.1 Reduce `SmartDrillFeature` to tap handling, panel state and overlay wiring
- [ ] 12.2 Delete `place2x2Drill`, `placeBeamDrill`, `findAllConnectedOreTiles`, `findAllConnectedWallOreTiles`, `expandTiles`, `isDrillTile`, `isBridgeTile`, and the raw `Table` popup
- [ ] 12.3 Remove `maxTilesConfig` from `SmartDrillFeature` and the settings view, keeping `autoDisable`
- [ ] 12.4 Remove the now-unused `feature.smart-drill.max-tiles` bundle key and its comments
- [ ] 12.5 Confirm the draw handler returns on a disabled feature before reading any state

## 13. Localization

- [ ] 13.1 Add bundle keys for the target row, facing row, area readout, drill row, rig row and action row
- [ ] 13.2 Add bundle keys for the drill count, efficiency readout, no-drill message and large-plan warning
- [ ] 13.3 Add a descriptive comment above every new key, documenting placeholders
- [ ] 13.4 Confirm no new user-visible string is hardcoded in Java

## 14. Verification

- [ ] 14.1 Rewrite `SmartDrillFeatureTest` around the planner; drop `Direction`, `isDrillTile`, `isBridgeTile` and `maxTilesConfig` assertions
- [ ] 14.2 Run the full module test suite and confirm the preview images were regenerated
- [ ] 14.3 Run `:checkBundles` and the Solim modifier import check
- [ ] 14.4 Review the generated images for each scenario and record any placement that looks wrong
- [ ] 14.5 Confirm a disabled feature performs no frame-loop work and the ghost pass allocates nothing