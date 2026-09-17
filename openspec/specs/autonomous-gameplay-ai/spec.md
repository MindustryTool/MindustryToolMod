# Autonomous Gameplay AI

## Requirements

1. **Task Hierarchy & Evaluation:**
   - The AI MUST evaluate tasks in descending order of user-configured priority.
   - The first enabled task whose trigger condition evaluates to true MUST be executed as the active task for the player unit.
   - When no tasks trigger or the feature is disabled, control MUST return to the player.

2. **Player Manual Override:**
   - When player manual input (touch or key press) is detected, Autoplay MUST immediately yield control to the player.
   - Autoplay MUST remain paused for a configurable cooldown duration (slider 0.5s - 5.0s, default 2.0s) after player input ceases.

3. **Task 1 - Self-Heal:**
   - MUST trigger when unit health falls below a configurable threshold (slider 20% - 80%, default 60%), healing until 100%.
   - MUST navigate to the nearest Repair Point (`BlockFlag.repair`).
   - If no repair points exist on the map, it MUST yield rather than blocking other tasks.

4. **Task 2 - Flee:**
   - MUST trigger when unit health is below a configurable threshold (slider, default 40%) OR when the unit has no offensive weapons.
   - MUST evaluate threats from both enemy units and enemy turrets.
   - MUST retreat toward the nearest friendly core or base structures away from the threat.

5. **Task 3 - Attack:**
   - MUST automatically skip/yield if the player unit has no attack weapons.
   - MUST target both enemy units and enemy buildings within `max(unit.range * 1.2f, 400f)`.
   - MUST kite or maintain maximum weapon range while engaging targets.

6. **Task 4 - Repair:**
    - MUST detect healing capability across healing weapons (`w.bullet.heals()` or `RepairBeamWeapon`) and repair field abilities (`RepairFieldAbility`).
    - Units lacking both healing weapons and repair field abilities MUST be deemed incapable of healing and yield immediately, regardless of building capabilities (`canBuild()`).
    - When a unit only possesses `RepairFieldAbility` without healing weapons, it MUST navigate within aura distance to heal targets passively and suppress non-healing weapon firing against allied blocks or units.
    - MUST repair damaged friendly buildings and heal damaged allied units across the map.

7. **Task 5 - Follow & Assist:**
   - MUST allow following another player in multiplayer (either a selected player or any building player).
   - MUST assist with building, attacking the leader's target, and mining if the leader is mining.
   - In singleplayer, it MUST yield cleanly.

8. **Task 6 - Self-Build & Task 7 - Rebuild:**
   - MUST prioritize the closest build/rebuild plans to minimize travel time.
   - MUST verify that the team core contains required resources; if resources are lacking, it MUST skip the plan to prevent deadlock.
   - Self-Build MUST include an action button in settings to queue deconstruction of all derelict structures.

9. **Task 8 - Mining:**
   - MUST mine the lowest-supply item in core from user-selected items.
   - MUST verify the core has capacity remaining for that item; if all selected items are full, it MUST yield.
   - MUST deposit mined resources directly via `Call.transferInventory(player, core)`.
   - MUST provide item selection checkboxes in settings.

10. **Camera & Visuals:**
    - MUST provide a cross-platform "Follow Unit" toggle, defaulting to `false`.
    - MUST render the active task icon above the player unit and a dashed line pointing to the current objective.

11. **Settings & Internationalization:**
     - MUST provide task reordering (up/down), per-task enable toggles, and live task statuses.
     - Each task row MUST be enclosed in a rounded Card with a background and border.
     - The task status / reason to skip MUST be positioned on a dedicated row beneath the task control buttons to prevent overlapping the toggle button.
     - Expandable per-task settings MUST be wrapped in a SolimCollapser to prevent blank space when collapsed.
     - All user-visible strings MUST be translatable via `bundle.properties`.

12. **Base AI Null Safety and Boundary Protection:**
    - The `BaseAutoplayAI` controller SHALL strictly guard against unassigned units (`unit == null`) and null target positions (`target == null` or `pos == null`) in all movement and steering delegations.
    - WHEN an AI movement method (`moveTo`, `circle`) is invoked while `unit` is null THEN the method SHALL safely return immediately without invoking Mindustry's `AIController` or throwing `NullPointerException`.
    - WHEN `moveTo` is invoked with a null `Position` THEN the method SHALL safely return immediately without attempting coordinate reads or throwing `NullPointerException`.

13. **Task Transition and State Cleanup:**
    - The Autoplay system SHALL manage transient unit action states during task switching and feature state transitions.
    - WHEN Autoplay transitions the active task from `AttackTask` to another task (or to idle/disabled) THEN it SHALL reset shooting state via `unit.isShooting(false)`.
    - WHEN Autoplay transitions away from `MiningTask` THEN it SHALL clear the active mining tile target `unit.mineTile = null`.
    - WHEN user input overrides autoplay or the feature is disabled THEN Autoplay SHALL yield control to `Vars.player`, reset transient states, and clear `currentTask`.

14. **Safe Evaluation and Coordinate Resolution:**
    - All autoplay tasks SHALL operate without side effects during priority arbitration and MUST use null-safe coordinate lookups.
    - WHEN `SelfHealTask.update(unit)` is evaluated during task selection THEN it SHALL evaluate eligibility and status without calling AI movement methods.
    - WHEN build plans reference missing or boundary tiles (`tile() == null`) THEN distance calculation and movement targets SHALL compute from plan tile coordinates (`x * Vars.tilesize, y * Vars.tilesize`) rather than throwing `NullPointerException`.
    - WHEN a ground unit updates boosting while near or outside map boundaries where `unit.floorOn()` may be null THEN it SHALL null-check the floor before inspecting floor properties (`isDuct`, `damageTaken`, `isDeep`).
