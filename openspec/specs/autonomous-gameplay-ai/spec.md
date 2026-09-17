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
   - MUST detect healing capability across healing weapons, repair beam abilities, and build beams.
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
    - All user-visible strings MUST be translatable via `bundle.properties`.
