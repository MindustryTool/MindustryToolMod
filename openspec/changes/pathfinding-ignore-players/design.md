## Context

`PathfindingFeature` filters units identically in its update pass (`updateProcessUnitPath`) and draw pass (`drawProcessUnitPath`): null/dead, local-player unit (`unit == Vars.player.unit()`), non-pathfind LogicAI units, ally-toggle gate, cost-type gate. The local-player check misses every remote human's driven unit in multiplayer, which then renders a meaningless AI flowfield path. `Unit.getPlayer()` (already referenced by the in-progress reactor-alert design) reports the possessing human player, if any.

Constraints: Java 8 runtime APIs only; no new config/UI/bundle surface per explicit decision; keep host/client path sources untouched.

## Goals / Non-Goals

**Goals:**
- Hide paths for any human-possessed unit (local + remote, any team) in both passes.
- Preserve RTS-commanded (`CommandAI`) paths including player-issued orders.

**Non-Goals:**
- No new setting, toggle, or user-visible strings.
- No changes to spawn-point paths, ally/cost-type toggles, or host/client path computation.
- No broadening to AI-driven ally units (the ally toggle already governs those).

## Decisions

### Possession check via `getPlayer()` (over scanning `Groups.player`)
A single `unit.getPlayer() != null` predicate covers local and remote possessors in O(1) and drops the `Vars.player` null-dependency. Alternative — matching each unit against `Groups.player` units — is O(players) per unit with identical semantics. `getPlayer()` is preferred unless the semantics spike (open question) shows it only covers the local player, in which case fall back to the scan.

### Replace, don't augment, the local-player check
When the local player drives a unit, that unit's controller *is* the `Player` object, so `getPlayer()` subsumes `unit == Vars.player.unit()`. Keeping both would be dead logic; the old condition is removed from both passes.

### Unconditional filter, no toggle (over a new "hide players" setting)
Mirrors the LogicAI filter precedent: a driven unit never has a meaningful AI path, so there is no legitimate "show" case to configure. Avoids settings UI, bundle keys, and config plumbing.

## Risks / Trade-offs

- [Risk] `getPlayer()` may only reflect the local player in our Mindustry version → Mitigation: verify semantics first (spike task); fall back to `Groups.player` scan if needed.
- [Risk] No meaningful headless unit test (real `Unit` entities aren't mockable; existing tests cover cache/config only) → Mitigation: state explicitly; verify via in-game multiplayer observation, same standing as the LogicAI filter.
- [Risk] Stale cache entry lingers after a human takes control → Mitigation: skip lives in both passes, so the draw pass hides immediately even if an entry was cached, mirroring the `ubind` handling.
