## Context

The god mode feature in `old/` was decommissioned due to architectural coupling, raw Arc widget hierarchies, and security flaws identified in `docs/godmode.md`. The mod architecture requires pure Solim UI, strict Java 8 runtime compatibility, automatic lifecycle ownership, and complete internationalization via `assets/bundles/bundle.properties`.

## Goals / Non-Goals

**Goals:**
- Implement `GodModeFeature` as a clean, feature-oriented Solim module.
- Provide a draggable, floating Solim HUD (`GodModeHudView`) with persisted screen coordinates across portrait and landscape orientations.
- Restore all 5 legacy capabilities (Team Switcher, Item Adder, Unit Spawner/Killer, Status Effects, Core Placer) and add a Map Fog toggle.
- Support dual execution:
  - `InternalGodModeProvider`: Direct Mindustry API calls when in singleplayer or hosting.
  - `JSGodModeProvider`: Standard `/js` chat command dispatching for remote multiplayer when the player is an admin, using valid Rhino-compatible Java-in-JS syntax.
- Provide interactive coordinate selection: dialog temporarily closes, world tap captures coordinates, dialog restores with values filled in.
- Provide search bars and quantity presets (`+10`, `+100`, `+1000`, `Max`) across item, unit, and status effect selection dialogs.
- Area collision and bounds check before placing cores.

**Non-Goals:**
- Bypassing vanilla server permissions (clients without admin rights cannot and should not bypass server authority).
- Arbitrary code execution consoles outside structured god-mode operations.

## Decisions

### 1. Floating Solim HUD (`GodModeHudView`)
- **Rationale**: Following `TimeControlHudView` and `QuickAccessHudView`, a floating draggable HUD element mounted on `Vars.ui.hudGroup` provides quick access to God Mode actions during gameplay.
- **Orientation**: Stores `portraitX`, `portraitY`, `landscapeX`, `landscapeY` via `ConfigGroup`.

### 2. Dual Provider Abstraction (`GodModeProvider`)
- **Interface**:
  - `changeTeam(Player player, Team team)`
  - `addItems(Item item, int amount, Team team)`
  - `spawnUnits(UnitType unit, int amount, Team team, float x, float y)`
  - `killUnits(UnitType unit, Team team)`
  - `applyEffect(StatusEffect effect, float duration)`
  - `clearEffect(StatusEffect effect)`
  - `placeCore(Block coreBlock, Team team, float x, float y)`
  - `setFog(boolean enabled)`
- **Implementation**:
  - `InternalGodModeProvider`: Direct manipulation (`player.team(team)`, `team.core().items.add(...)`, `unit.spawn(...)`, `tile.setNet(...)`, `Vars.state.rules.fog`).
  - `JSGodModeProvider`: Dispatches `/js ...` via `Call.sendChatMessage()`. Fixed player lookup using `Groups.player.getByID(@)` or `Groups.player.find(p => p.name.equals("@"))`.

### 3. Interactive Position Picker (`PositionPicker`)
- Temporarily hides the calling dialog.
- Hooks a one-shot tap handler (or input processor) on the world scene.
- On tap: converts screen tap to world coordinates, unregisters listener, re-shows dialog, and updates coordinate signals.

### 4. Searchable Solim Dialogs
- Reusable UI patterns for selecting Content (`Item`, `UnitType`, `StatusEffect`, `Block`, `Team`, `Player`).
- Reactive search query filter bound to Solim list/grid components.

## Risks / Trade-offs

- **[Multiplayer /js Chat Visibility]** → Mindustry chat commands starting with `/` are intercepted by the server command handler, but errors or feedback may print in chat. Handled by validating input parameters client-side before sending.
- **[Core Placement Structure Overwrite]** → Checking tile emptiness / buildability prevents accidental destruction of invalid terrain.
