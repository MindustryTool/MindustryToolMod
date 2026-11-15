## Context

The mod targets Android under Mindustry's software GL backend, where per-frame dispatch and per-frame allocation dominate cost. The existing `agent-perf-discipline` spec and AGENTS.md "Known Debt" table (D7-D15) already constrain new code; this change scopes the accepted fixes from a memory-growth hotspot scan. Decisions were made per hotspot (see proposal). Only fixes that were accepted are in scope; deferred items are recorded but not implemented.

## Goals / Non-Goals

**Goals:**
- Release world-scoped references on world load / reset (TeamResourceState histories, Visualizer state pool).
- Remove per-frame boxing, lambda capture, reflection, and string concat from the accepted hot features.
- Gate hot (draw/update) listeners on `isEnabled()`.
- Bound the pathfinding cache with a hard cap and more frequent cleanup.

**Non-Goals:**
- ChatMessages copy-on-write refactor (decided leave as-is).
- Trimming chat messages/users (D15) (decided leave as-is).
- TeamResourceState.usedItems shrinking (decided leave as-is).
- Music trackStates pruning (decided leave as-is).
- Disposing evicted ImageCache textures (decided keep no-dispose).
- Solim-reactive defensive copies (decided leave as-is).
- UI/event listener gating for the ~68 constructor-registered listeners (deferred).
- Any new user-visible text or feature.

## Decisions

1. **TeamResourceState histories**: clear the maps entirely (not just buffers) on reset/world-load/team-change. Rationale: buffers are fixed-size and cheap; the maps hold object references whose lifetime is the world. Clearing buffers alone keeps stale refs. Alternative (bounded LRU) rejected as more complex for no benefit.

2. **ToggleRenderingFeature**: replace `Groups.draw.each(lambda)` with a manual indexed loop over `Groups.draw` into a reused `Seq<Unit> toHide`; replace `tileview.removeAll(lambda)` with a manual loop removing by index; read `unitDrawIndexField` once into a local. Rationale: removes per-frame allocation and reflection; keeps behavior.

3. **BlockStatsDisplayFeature**: mirror HealthBar/Visualizer: add `cachedZoomThreshold`/`cachedOpacity`/`cachedScale` primitive fields updated by `Effect`, and read them in `draw()`. Remove `signal().peek()` calls. Rationale: consistency with existing pattern; eliminates Float boxing.

4. **AutoplayFeature**: add `cachedCooldownLabel` String field; recompute only when the rounded tenths value changes. Rationale: avoids per-frame concat; minimal state.

5. **MusicFeature**: add `if (!isEnabled()) return;` as first statement in the 0.1s timer callback. Rationale: matches perf rule; cheapest correct fix. Alternative (cancel/reschedule timer) considered but adds lifecycle complexity for a default-disabled feature.

6. **LogicVisualizerFeature**: replace `Groups.unit.each(unitDrawer)` with `Groups.unit.intersect(viewBounds, unitDrawer)` or manual bounds check using `Core.camera.bounds`. Rationale: large-map win; consistent with HealthBar.

7. **Pathfinding cache**: hard cap 4096 entries (evict oldest lastUsedTime); cleanup every 30 frames (was 60). Rationale: bounds memory on large maps while preserving hot entries.

8. **Visualizer state pool**: call `statePool.clear()` inside `clearFlowStates()` (invoked on world load/reset). Rationale: releases evicted states across worlds.

## Risks / Trade-offs

- [ToggleRendering refactor] Changing `tileview.removeAll` to manual removal must preserve index validity; mitigation: iterate from the end and use `Groups.draw` semantics carefully; add a smoke test.
- [TeamResource clear maps] Brief empty sparkline until next sample; acceptable, matches existing reset behavior.
- [Pathfinding cap] Evicting oldest may drop a still-useful entry under churn; mitigation: 4096 is large relative to map tiles × cost types × teams.
- [Hot listener gating] Only draw/update listeners gated; UI/event listeners remain ungated and recorded as deferred debt.

## Migration Plan

No migration needed; behavior-preserving except memory characteristics. Rollback: revert the feature commits; no data format changes.

## Open Questions

- None outstanding; all 15 hotspot decisions recorded in the proposal.