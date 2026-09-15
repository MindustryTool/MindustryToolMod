## Why

The original god mode implementation was disabled and moved to legacy code due to security, architecture, and reliability issues identified in the audit (`docs/godmode.md`). The modern mod needs a secure, declarative Solim-based rewrite that restores all original features (player/team change, core items, unit spawning/killing, status effects, core placement) while introducing map fog toggles, interactive map position selection, and dual execution providers (Internal API and Remote Multiplayer Admin `/js`).

## What Changes

- Implement full `GodModeFeature` with floating, draggable Solim HUD bar with position persistence (portrait and landscape).
- Restore all 5 legacy capabilities with Solim dialogs, search bars, and amount presets (`+10`, `+100`, `+1000`, `Max`):
  - **Change Team**: Select player and team.
  - **Core Items**: Select item, target team, and add items with amount presets.
  - **Unit Spawner & Killer**: Select unit, target team, count presets, pick coordinate on map, spawn units or kill all alive units of that type.
  - **Status Effects**: Select status effect with search, configure duration, apply to/clear from player's unit.
  - **Place Core**: Select core block, team, pick coordinate on map, validate tile suitability, and construct core.
- Add new capability: **Reveal Map Fog** toggle switch (ON/OFF) to clear or restore fog of war.
- Implement interactive map position selection: hides active dialog, captures world tile click, and re-opens dialog with selected coordinates.
- Dual execution architecture:
  - `InternalGodModeProvider`: Direct Java API manipulation when in singleplayer or hosting.
  - `JSGodModeProvider`: Rhino Java-interop JavaScript execution via server chat command pipeline (`/js ...`) when connected as a remote server admin, with bug-free syntax and player lookup.
- Full internationalization: all user-facing strings translated in `bundle.properties` with comments.

## Capabilities

### New Capabilities
- `god-mode`: Comprehensive debug, testing, and sandbox control suite providing team modification, resource injection, unit spawning/mass-clearing, status effect control, core placement, and map fog toggling across local and remote admin environments.

### Modified Capabilities
<!-- None -->

## Impact

- `mod/src/mindustrytool/features/godmode/*`: Replaces the placeholder `GodModeFeature` with full Solim feature, HUD, dialogs, and providers.
- `assets/bundles/bundle.properties`: Adds localization keys for all god mode UI elements.
- Game HUD: Adds a draggable floating HUD element when god mode is enabled.
