# Pathfinding Visualization

## Requirements

1. **Host & Client Accuracy:** The path renderer MUST accurately display unit paths regardless of whether the user is hosting or playing as a client.
2. **Path Sources (Host Mode):**
   - Wave units MUST use \Vars.pathfinder.getField(team, costType, fieldCore)\.
   - Commanded units MUST use \Vars.controlPath.getPathPosition(unit, targetPos)\.
3. **Path Sources (Client Mode):**
   - MUST copy the server's pathfinding logic into a separate \ClientPathfinder\ utility.
   - Wave units MUST trace a locally-computed BFS flowfield from the enemy core.
   - Commanded units MUST trace a locally-computed A* path from the unit to its \	argetPos\.
   - The local paths MUST be 100% accurate, reproducing the game's cost/passability logic.
4. **Filtering:**
   - Display paths for enemy units.
   - Display paths for ally units (if toggled on).
   - DO NOT display paths for the player's actively controlled unit.
5. **Visuals:** Paths MUST be drawn in solid team colors (no fade effect).
6. **UI & Configuration:**
   - MUST use the Solim UI framework.
   - MUST provide toggles for unit paths, spawn point paths, and specific cost types.
   - \enabledByDefault\ MUST be false.
7. **I18n:** All UI text MUST be translatable via \undle.properties\.
