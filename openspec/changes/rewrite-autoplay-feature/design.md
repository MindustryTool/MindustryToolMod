## Context

The legacy Autoplay feature implementation had several critical gameplay deadlocks:
1. `SelfHealTask` got stuck indefinitely when damaged if no repair point existed on the map.
2. `FleeTask` triggered even at 100% HP, fled randomly, and ignored enemy turrets.
3. `AttackTask` sent unarmed units (like Mono) to their deaths and ignored enemy buildings.
4. `RepairTask` only checked weapons, ignoring repair beam units like Poly and Mega.
5. `SelfBuildTask` and `RebuildTask` stalled when core lacked resources, blocking `MiningTask` from ever gathering the required items.
6. `MiningTask` continued mining full resources and used simulated touch drops.
7. Player input override resumed instantaneously upon finger release with zero cooldown.

This design overhauls the AI architecture, resolves every deadlock, and migrates the UI to Solim.

## Goals / Non-Goals

**Goals:**
- Provide a robust priority-based autonomous AI system with all 8 specialized tasks.
- Implement intelligent deadlock prevention: verify healing destinations exist, verify core materials for building, and respect core item capacities for mining.
- Implement player manual override with a configurable cooldown timer (default 2s, slider 0.5s - 5s).
- Implement cross-platform "Follow Unit" camera setting (default: off).
- Build a declarative Solim settings dialog with task reordering, toggles, live status, and per-task configuration sub-panels.
- Render overhead task icons and target dashed lines above the player unit.

**Non-Goals:**
- We do not replace Mindustry's core pathfinding engine; tasks navigate using Mindustry's `moveTo` / flowfields.

## Decisions

1. **Task Execution & Lifecycle:**
   - Tasks are evaluated in order of priority. The first task whose `update(unit)` returns `true` becomes `currentTask`.
   - If player manual input (key press or screen touch) is detected:
     - Autoplay yields immediately.
     - A cooldown timer is set to `overrideCooldown` (configurable 0.5s - 5.0s, default 2.0s).
     - Autoplay does not run while the cooldown timer is active.
   - When disabled, `unit.controller(Vars.player)` is restored immediately.

2. **Deadlock & Resource Safeguards:**
   - **SelfHeal**: Must verify `indexer.getFlagged(unit.team, BlockFlag.repair).size > 0`; otherwise yields to allow other actions. Trigger threshold is a configurable slider (default 60%).
   - **Flee**: Only flees when HP < threshold (slider, default 40%) OR when unarmed. Evaluates threat radius against both enemy units and enemy turrets, retreating toward the nearest core/base structures.
   - **Attack**: Skips immediately if unit has no attack weapons. Targets units and structures within dynamic range `max(range * 1.2f, 400f)`, maintaining max weapon range (kiting).
   - **Repair**: Checks healing weapons, repair beams, and build beams. Repairs damaged structures and allied units.
   - **FollowAssist**: Only active in multiplayer. Assists target with building, attacking, and mining.
   - **SelfBuild & Rebuild**: Sorts plans by distance to player unit. Verifies the team core holds required materials before dedicating the unit, preventing resource deadlocks. Includes "Deconstruct All Derelicts" button.
   - **Mining**: Only mines items that have remaining capacity in the core. Transfers items via `Call.transferInventory(player, core)`. Exposes an item filter grid in settings.

3. **Camera & Visuals:**
   - Follow unit is cross-platform, defaulting to false. When true, `Core.camera.position.lerp(unit, 0.1f)` follows the unit.
   - Overhead icon (task icon) and target dash line rendered during `Trigger.draw` on `Layer.overlayUI`.

4. **Solim Settings UI:**
   - `AutoplaySettingsDialog` (SolimDialog) wrapping `AutoplaySettingsView`.
   - Task list with Reorder Up / Down buttons, enable/disable checkbox, task name, and live status (Active / Blocked / Disabled / context status).
   - Global controls: Follow Unit checkbox, Override Cooldown slider.

## Risks / Trade-offs

- **Risk**: Frequent scanning for ores or damaged blocks impacting frame rates.
  - **Mitigation**: Use intervals (`timer.get(...)`) and Mindustry's pre-computed spatial indexers (`indexer.findClosestOre`, `indexer.getFlagged`, `indexer.getDamaged`).
