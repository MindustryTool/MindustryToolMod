## Why

In Mindustry, game rules heavily determine gameplay dynamics (e.g., build speeds, damage multipliers, unit caps, banned blocks/units, wave timings, fog of war, and reactor explosion mechanics). However, vanilla Mindustry hides the game rules customization interface (`CustomRulesDialog`) unless `rules.allowEditRules` is enabled and the player is hosting or in singleplayer—which is false for virtually all campaign sectors, survival maps, and multiplayer games. Furthermore, clients connecting to multiplayer servers have no native way to inspect the server's active rules or discover restrictions without trial and error.

A dedicated Map Rules feature in MindustryTool provides full visibility into active game rules across all game modes, highlights customizations from default rules, and enables safe editing when permissions allow (direct mutation in singleplayer, network broadcast when hosting, and admin-assisted or visual-only edits on multiplayer servers).

## What Changes

- **New Map Rules Feature (`MapRulesFeature`)**: Registers a new feature accessible from the QuickAccess HUD bar.
- **Pure Solim Rules Dialog (`MapRulesDialog`)**: A declarative Solim interface to view and modify game rules, organized by category:
  - Combat & Multipliers (build speed, unit damage, block health, solar multiplier, etc.)
  - Waves & Spawns (wave spacing, win wave, drop zones, spawn mechanics)
  - Banned Content (visual grid badges of banned blocks and unit types)
  - Environment & Visuals (fog of war, lighting/darkness, map borders, weather)
  - Game Mechanics (reactor explosions, fire spread, core protection, possession)
- **Role-Based Permission Handling**:
  - **Single Player**: Full direct editing of active rules.
  - **Server Host**: Full direct editing with instant network synchronization via `Call.setRules(Rules)`.
  - **Multiplayer Client (Non-Admin)**: Informative read-only view of server rules to avoid desync, with local client-side overrides for visual rendering rules (fog, lighting).
  - **Multiplayer Client (Admin)**: Remote editing capability via `/js` console chat execution.
- **Rule Diff Engine**: Real-time comparison against default rules (`new Rules()`) to provide a `[★ Modified Only]` filter chip, allowing players to instantly pinpoint unique server or map customizations.
- **Search & Filtering**: Search bar to query rules by name, description, or banned item identifiers.
- **Internationalization**: Complete bundle coverage in `assets/bundles/bundle.properties` with translator comments.

## Capabilities

### New Capabilities
- `map-rules`: In-game inspection and contextual editing of active map rules, with role-based permission tiers, rule diffing against defaults, and QuickAccess HUD integration.

### Modified Capabilities
<!-- None -->

## Impact

- **New Classes**:
  - `mindustrytool.features.rules.MapRulesFeature`
  - `mindustrytool.features.rules.MapRulesDialog`
  - `mindustrytool.features.rules.MapRulesView`
  - `mindustrytool.features.rules.MapRulesSettingsDialog`
  - `mindustrytool.features.rules.MapRulesSettingsView`
- **Main Registration**: Registered into `FeatureManager.register(...)` in `mindustrytool.Main`.
- **Assets & Bundles**: New bundle keys in `assets/bundles/bundle.properties` and new feature icon in `assets/icons/`.
