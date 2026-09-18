## Why

The pathfinding visualizer hides the local player's driven unit but still draws AI flowfield paths for every other human's driven unit in multiplayer. A human-steered unit follows no AI path, so those rendered paths are misleading — the same class of bug the LogicAI filter already fixed.

## What Changes

- Skip path rendering for any unit possessed by a human player (local or remote), on any team, with no setting or toggle.
- Apply the skip in both the update pass (no cache fill) and the draw pass (no stale render), mirroring the LogicAI filter.
- Keep showing RTS-commanded (`CommandAI`) paths, including player-issued orders, since those are genuine AI routes.
- Leave spawn-point paths, the ally toggle, and cost-type toggles unchanged.

## Capabilities

### New Capabilities

- None — this widens existing filtering.

### Modified Capabilities

- `pathfinding-visualization`: the unit path filtering requirement widens from "the player's actively controlled unit" to "any player-controlled unit".

## Impact

- Affected code: `PathfindingFeature` unit update/draw filtering only; no browser, config, UI, bundle, or API changes.
- No new settings, toggles, or user-visible strings.
- Negligible performance impact (one null-check per unit per frame).
