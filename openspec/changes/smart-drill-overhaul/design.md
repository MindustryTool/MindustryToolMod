## Context

Smart Drill currently fuses four concerns into one 585-line class:

| Concern | Where it lives today |
| --- | --- |
| Which ore a drill may target | `isValidDrill` / `isValidBeamDrill` (`tier >= hardness` only) |
| Which tiles form the patch | `findAllConnectedOreTiles` (flood) + `expandTiles` (3x dilation) |
| Where drills and bridges go | `isDrillTile` / `isBridgeTile` lattice, `place2x2Drill`, `placeBeamDrill` |
| How the player drives it | `showDirectionMenu` / `showDrillMenu`, a raw `arc.scene.ui.layout.Table` |

That fusion is why none of it is testable: `findAllConnectedOreTiles` needs `Vars.world`, `place2x2Drill` needs `Vars.player.unit()`, and the UI is a `Table` built imperatively in a callback. The only feedback loop is "run the game and look at the map".

Three concrete defects motivate the rewrite and are fixed as part of it:

1. **Signed modulo.** `isDrillTile` uses `tile.x % 6`. Java `%` preserves the sign, so `-1 % 6 == -1`, and columns `x ≡ 5 (mod 6)` for negative `x` fall through to `default: return false`. The lattice is only correct in the first quadrant.
2. **Flood through walls.** `findAllConnectedOreTiles` continues on `neighbor.drop() != drop && neighbor.wallDrop() != drop`, so a wall with wall-ore connects two floor-ore patches. `tiles.retainAll(t -> t.drop() == drop)` then deletes the wall tiles, leaving one region that was only connected through solid rock.
3. **Diagonal duct for 1x1 beam drills.** `placeBeamDrill` computes `reach = half + 1` where `half = (size - 1) / 2`. For `size == 1` the duct lands at `(+1, +1)` relative to the body — diagonal. `BeamDrill` outputs non-directionally into *adjacent* accepting blocks, so the plasma bore has no adjacent output, fills to `itemCapacity`, and stalls.

Constraints that shape the design:

- Mindustry `v146`, Java 17 syntax on a Java 8 runtime (no `java.util.function`, no `List.of`, no `String.isBlank`).
- The mod UI must be declarative Solim; raw `arc.scene.*` tables are not allowed for new UI.
- Per-frame handlers must be allocation-free and gate on `isEnabled()` as their first statement.
- All user-visible text goes through `Core.bundle` with a translator comment above every new key.

## Goals / Non-Goals

**Goals:**

- A placement core that can be exercised without a running game: given an ore target, a rectangle, a drill and a world description, return a plan.
- The same plan object drives the in-game ghost preview, the real build queue, and the PNG used for review.
- A single compact panel that replaces the two-step popup, with the drill/bridge pattern visibly preserved for 2x2 drills.
- Ore targeting that matches what `Drill` actually produces, so a drill never silently mines a different item than the one clicked.
- A review loop where placement quality is judged from images, not from playing the game.

**Non-Goals:**

- Changing how Mindustry itself drills, transports, or builds. The planner only decides *where*; the engine keeps doing the rest.
- A general-purpose schematic auto-placer. Bound schematics are placed once at their best position, not tiled or optimised across the rectangle.
- Reconciling the legacy stub registry in `app-settings`, which already lists smart-drill as a metadata-only stub despite the feature having logic today.
- Rewriting `SmartUpgradeFeature`, which shares `BlockGroup.DRILL` classification but is otherwise independent.

## Decisions

### D1 — A pure planner behind a world-view interface

Placement decides *where* things go. That decision needs a map, not Mindustry.

```
DrillPlanRequest ──▶ DrillPlanner ──▶ DrillWorldView ──▶ List<PlannedBuild>
   ore                 (pure)          (interface)          (plain data)
                                          │
                        ┌─────────────────┴──────────────────┐
                        ▼                                    ▼
              MindustryDrillWorldView                TestDrillWorld
              (Vars.world + Build.validPlace)        (in-memory grid)
```

`DrillWorldView` exposes only what the algorithms need: tile lookup, solidity, ore kind at a tile, and a buildability check. The Mindustry implementation delegates buildability to `Build.validPlace(block, team, x, y, rotation)`, which is exactly what `BuildPlan.placeable` calls, so game fidelity is preserved. The test implementation uses a plain occupancy-and-terrain model.

*Alternative considered:* keep using `Vars.world` directly and extract only the lattice predicates. Rejected because the lattice predicates are the cheap part; the flood, the greedy scan and the bridge tree are what need testing, and all three need a world.

*Alternative considered:* reimplement `validPlace` inside the planner. Rejected because it would drift from the engine and silently diverge on env, fog, banned blocks and core range.

### D2 — Ore targeting is "every ore tile in the footprint is the target"

`Drill.countOre` counts every mineable tile under the block, and `DrillBuild.updateTile` produces only `dominantItem`. A drill on three lead tiles and one copper tile produces lead, even if the player clicked copper. The rule that matches engine behaviour is therefore the strict one: a footprint is valid only when at least one tile is the target and no tile is any other ore.

Consequences accepted deliberately:

- On a copper/lead boundary, lattice anchors whose 2x2 straddles the seam are rejected, so coverage on mixed patches is lower than the theoretical maximum.
- Because no other ore is present, `dominantItems` equals the number of target ore tiles in the footprint. That makes the efficiency metric exact rather than approximate.

*Alternative considered:* "at least one tile" (matches `Drill.canPlaceOn`). Rejected because it produces drills that mine a different item than requested — the exact bug the rule exists to prevent.

*Alternative considered:* "allow mixed with a warning". Rejected because a warning about a build that has already been queued is not actionable.

### D3 — The rectangle bounds ore coverage, and floor drills make that the same as placement

The rectangle is the region of ore the player wants drained. For a floor drill the ore it covers *is* the ground it stands on, so for that path the rectangle is also the placement region and the two readings coincide. For beam drills the rectangle is the wall band. For bound schematics only the drills must land inside; conveyors, ducts and nodes may hang outside, because requiring a 7x7 rig to fit inside a 5x5 patch would make rigs unusable exactly where they are most valuable.

*Alternative considered:* rectangle as pure placement region (R1). Rejected because a schematic's non-drill blocks then have no defined relationship to the ore they serve.

*Alternative considered:* rectangle as a hard boundary for every block in a schematic. Rejected as above.

### D4 — Two independent ore floods, each capped

Floor ore and wall ore are different resources reached by different drills, so they get separate floods:

```
click floor ore ──▶ flood floor ore (4-connected, same drop) ──▶ bbox, clamp 10x10
click wall ore  ──▶ flood wall ore  (8-connected, same drop) ──▶ bbox, clamp 10x10
```

Both floods stop at 100 ore tiles and both bounding boxes are clamped to 10x10 tiles. The two caps are both necessary: the tile cap bounds the flood work, the box clamp bounds the rectangle. On a diagonal vein the tile cap alone would allow a 100x100 box — precisely the large build plan the cap exists to prevent.

*Alternative considered:* cap the flood but not the box. Rejected for the reason above.

*Alternative considered:* one unified flood that walks both kinds. Rejected; it is the current behaviour and it merges patches through rock.

### D5 — World-anchored lattice with `floorMod`

The 2x2 lattice stays anchored to the world origin so that drill positions do not move as the rectangle is dragged. The predicate switches to `Math.floorMod(x, 6)`, making the pattern identical in all four quadrants.

*Alternative considered:* anchor to the rectangle. Rejected because every resize would reshuffle every drill, which is visually noisy and makes the preview jump.

Accepted trade-off: a small rectangle at an unlucky offset may contain zero lattice anchors, and the panel reports a drill count of zero rather than silently shifting the pattern.

### D6 — One strategy per drill kind and size

| Kind | Strategy |
| --- | --- |
| 1x1 floor | greedy, densest-first |
| 2x2 floor | world-anchored lattice, no greedy fill |
| 3x3 and larger floor | greedy, densest-first |
| Beam drill | wall-band scan, one unit per position |

Greedy scores each candidate by the number of currently-uncovered target ore tiles in its footprint, places the best, removes those tiles, and repeats. Densest-first was chosen over a top-left scan because large drills can strand usable ore near an already-placed neighbour, and over nearest-first because nearest-first produces a visibly lopsided result on a large rectangle.

*Alternative considered:* greedy fill on top of the 2x2 lattice. Rejected because the requirement is to preserve the visible drill/bridge pattern, and the fill would break it wherever it ran.

### D7 — Bridges are a pruned shortest-path tree, not a filled lattice

The bridge lattice is unchanged: nodes at `x % 3 == 0 && y % 3 == 0`, links between nodes four tiles apart, which form a 4-connected grid graph.

1. `required` = nodes orthogonally adjacent to at least one planned drill.
2. `root` = the required-or-boundary node furthest along the chosen facing direction.
3. Build a shortest-path tree from `root` over the reachable graph.
4. Keep the union of paths from `root` to each required node.
5. Prune non-required leaves until none remain.
6. Each kept bridge's config is the delta to its parent; the root's config points at the stub.

The result is the minimal subtree spanning the drills and the output. Node *positions* are identical to today; only the unused bridges disappear.

*Alternative considered:* keep every bridge in the region. Rejected; that is the defect being fixed.

### D8 — The beam-drill unit

```
facing LEFT, wall on the left

  wall │ W W W W
       │
       │        N      beam node
       │        D      duct
       │        B      body (rotation = toward wall)
```

The duct is directly behind the body so that the body has an adjacent accepting block. The node sits two tiles further back so power chains down the wall line. Every lane must resolve to target wall-ore and at least one lane must resolve, which is the beam-drill analogue of D2.

*Alternative considered:* node adjacent to the body, duct behind the node. Rejected because the duct would then sit between the body and its only output.

### D9 — Schematic placement is a lazy, single-shot search with an explicit metric

Bound schematics are stored as `block name -> schematic file name`, persisted as JSON in the feature config, the same way `QuickSchematicGridFeature` persists its entries. The player taps a drill to reveal its bound rigs, taps one, and the search runs for that schematic only and caches per rectangle. Other bound schematics cost nothing until selected.

Search order: for each of four rotations, for each origin that puts every *drill* footprint inside the rectangle, reject early unless every drill's footprint is pure target ore, then verify every non-air stile is placeable at its transformed position. Config values that are `Point2` are rotated with the schematic.

Score: maximise total mineable target-ore tiles under the drills, then minimise blocks placed, then minimise distance from the rectangle centre.

*Alternative considered:* evaluate every bound schematic eagerly so the panel can rank them. Rejected because the cost multiplies by the number of bound rigs on every drag release.

*Alternative considered:* tile the schematic across the rectangle. Rejected; it is a different algorithm (choose a phase, then repeat) and was explicitly scoped out.

### D10 — The preview is drawn, never queued

Mindustry builds anything in `unit.plans` automatically, so "queue it as a preview" is not achievable safely. Instead the feature draws the plan itself with `block.drawPlan(plan, list, valid, alpha)` inside a `Trigger.draw` handler, culled to `Core.camera.bounds`. Nothing enters `unit.plans`; `Place` converts the ghosts into real `BuildPlan`s and hands them to `unit.addBuild`.

This also means rectangle changes cannot reorder or disturb the player's own build queue, which mutating `unit.plans` would have done.

*Alternative considered:* queue into `unit.plans` and suppress the mod's autoplay while the panel is open. Rejected because it mutates the player's real queue on every drag release, and because `SelfBuildTask` (`features/autoplay/tasks/SelfBuildTask.java:73`) already iterates `unit.plans` and would need a second suppression path.

### D11 — The overlay and ghost pass are allocation-free

Both run per frame. The draw handler caches the plan, the camera bounds and the style values in fields, and reads no bundle keys and no reactive signals. The guard on `isEnabled()` is the first statement. This mirrors `RangeDisplayFeature.draw()`.

### D12 — The panel is a Solim component

`SmartDrillPanel` is a `Component` built from `row()` / `column()` / `reactiveGrid(...)`, mounted into `Vars.ui.hudGroup` on open and disposed on close. It replaces the raw `Table` and the per-frame `currentMenu.update(...)` camera projection.

### D13 — The review harness renders images, and asserts invariants rather than counts

`PlanRenderer` draws one pixel per tile into a `Pixmap` — floor, ore, wall, wall-ore, then planned blocks in distinct colours — and writes a PNG via `PixmapIO`. No GL, no renderer, no content loading. Each scenario emits a floor image and a plan image into `mod/build/smartdrill-preview/`.

Scenarios assert invariants that survive refactors: every drill footprint contains target ore, no two footprints overlap, every position is inside the rectangle, every footprint is pure target ore, and the bridge set is connected. Exact drill counts are deliberately not asserted, because the PNG is the review artifact and a count is an implementation detail.

*Alternative considered:* assert exact counts per scenario. Rejected as brittle against any future strategy tuning.

## Risks / Trade-offs

- **Beam node may not link past the duct.** `PowerNode` builds links by scanning outward and may stop at the duct, leaving the body unpowered. → Verify against `PowerNode.createLinks` before committing. Fallback: place the node before the duct, or offset it sideways.
- **Ghost sprite cost.** Full sprites for several hundred plans every frame is the expensive option, and the mod targets a software GL backend on Android. → Cull to camera bounds, keep the pass allocation-free, and measure on device early. Fallback: drop to rectangles beyond a sprite cap.
- **No hard ceiling on the rectangle.** A large rectangle on a dense patch produces a large plan, and the greedy and schematic searches scale with area. → Report the drill count and a warning above a threshold before `Place`; never block.
- **`floorMod` moves existing drills on the negative side.** Bases built from the old pattern will not match the new one at `x < 0` or `y < 0`. → Accepted as a correctness fix; called out in the summary.
- **Strict ore rule lowers coverage on mixed patches.** Some ore that Mindustry would happily drill is left alone. → Accepted; it is the price of never producing the wrong item.
- **Schematic config rotation.** `Point2` configs on bridges, beam nodes and logic must be rotated with the schematic or the rig silently mislinks. → Treat as a spike item; verify with a schematic that contains an item bridge.
- **Planner fidelity.** The pure planner's model can drift from `Build.validPlace`. → The Mindustry view delegates to the engine for buildability, so only the terrain/ore model is approximated.

## Migration Plan

- No world or save migration. The change adds planner packages and removes one setting.
- `mindustrytool.smart-drill.max-tiles` becomes an orphaned settings key. It is harmless and is not deleted, because removing keys from a live settings store is riskier than leaving them unread.
- `autoDisable` is retained with unchanged semantics, now firing after `Place`.
- Rollback is reverting the change; the old popup and lattice are self-contained in `SmartDrillFeature`.

## Open Questions

- Does `PowerNode` link past a duct? Determines the final beam-node offset.
- How large may a bound schematic be before the search is unacceptably slow? `Vars.maxSchematicSize` is 4000; the search should probably consider only schematics whose drill set is non-trivial.
- Should the `app-settings` legacy stub registry be reconciled with the real feature in a separate change?