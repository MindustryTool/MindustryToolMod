## Context

The current pathfinding visualization in MindustryTool uses outdated direct Arc UI components and fails to trace paths properly when playing on a server as a client. The game computes paths on the server using \Vars.pathfinder\ (flow fields for wave units) and \Vars.controlPath\ (HPA* for commanded units), but enemies don't compute these on the client.

## Goals / Non-Goals

**Goals:**
- Rewrite the UI to be 100% declarative using the \Solim\ UI framework.
- Accurately draw paths for wave units (enemy going to core) and commanded units.
- Translate all user-facing strings.
- **Two-System Architecture**: 
  - **Host mode**: Use the native \Vars.pathfinder\ and \Vars.controlPath\.
  - **Client mode**: Provide a custom \ClientPathfinder\ that replicates the server's pathfinding logic (BFS for flowfields, A* for commanded) locally to avoid interfering with game state while ensuring 100% accuracy.

**Non-Goals:**
- Do not draw paths for the player's own currently controlled unit.

## Decisions

1. **Two-System Tracing:**
   - **Host Mode (\!net.client()\)**: 
     - Wave units: Use \pathfinder.getField(team, costType, fieldCore)\ and trace via \pathfinder.getTargetTile()\.
     - Commanded units: Use \controlPath.getPathPosition(unit, targetPos)\ to trace trajectory.
   - **Client Mode (\
et.client()\)**:
     - We will copy and adapt the server's flowfield logic into a new \ClientPathfinder\ class.
     - Wave units: It will run an async BFS from the enemy core outward, using the exact \costTypes\ arrays from the server to guarantee 100% path accuracy.
     - Commanded units: It will run a local A* pathfind from the unit to its \	argetPos\.
     - This completely isolates our mod's pathfinding from the game's internal client state, preventing desyncs or side effects.

2. **Solim UI & i18n:**
   - Configuration is defined as \ConfigValue<T>\ fields (like \HealthBarFeature\).
   - The settings dialog uses \SolimDialog\ and a pure declarative \BaseComponent\ view.
   - All text uses \Core.bundle.get()\ with keys defined in \ssets/bundles/bundle.properties\.

3. **Caching & Deduplication:**
   - Keep the \PathfindingCacheManager\ logic for grouping units by \	ileX, tileY, pathType, team\.
   - Culling off-screen units and applying a frame budget (\MAX_UPDATES_PER_FRAME\) limits how many paths we compute per frame.

## Risks / Trade-offs

- **Risk**: Maintaining the copied pathfinding code (ClientPathfinder). If the game updates its pathfinding logic, this mod's client pathfinding might diverge.
  - **Mitigation**: We copy the logic exactly as it is in the current version. The core pathfinding rules (cost logic) haven't fundamentally changed in a long time.
