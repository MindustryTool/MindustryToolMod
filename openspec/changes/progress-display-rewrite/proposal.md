## Why

The `ProgressDisplayFeature` exists as a registered placeholder with an icon and i18n keys but has no gameplay logic — it cannot be enabled and does nothing. The legacy implementation lives in `old/` using imperatively constructed Arc dialogs, global mutable config statics, and raw `Core.settings` reads on every draw frame. This rewrite delivers a fully functional feature following current mod architecture.

## What Changes

- Remove `development(true)` guard from `ProgressDisplayFeature`; set `enabledByDefault(true)`
- Implement `draw()` loop via `Trigger.draw` that overlays countdown text above active unit-production and crafter buildings
- No visual bar — countdown text only (`"12.4s"` via `Fonts.outline` at building center)
- Per-block enabled toggles backed by a `BitSet` indexed by `block.id` for zero-overhead draw-loop lookup
- Per-block settings persisted to `Core.settings` (`mindustrytool.features.progress-display.block.<block.name>`)
- Configurable zoom threshold, opacity, and scale via `ConfigValue<Float>` (reactive, same pattern as `HealthBarFeature`)
- `ProgressDisplaySettingsDialog` + `ProgressDisplaySettingsView` in Solim declarative style
- Settings view groups blocks by category (Unit Factories / Reconstructors / Unit Assemblers / Crafters) using `wrap()` layout for the large crafter set
- All user-visible strings added to `bundle.properties`

## Capabilities

### New Capabilities

- `progress-display`: In-world countdown text overlay for active unit-production and crafter buildings, with per-block and global config controls

### Modified Capabilities

_(none — no existing spec requirements change)_

## Impact

- **Modified**: `mod/src/mindustrytool/features/progressdisplay/ProgressDisplayFeature.java`
- **New**: `mod/src/mindustrytool/features/progressdisplay/ProgressDisplaySettingsDialog.java`
- **New**: `mod/src/mindustrytool/features/progressdisplay/ProgressDisplaySettingsView.java`
- **Modified**: `assets/bundles/bundle.properties` (settings keys added)
- **No new dependencies**; follows the same `HealthBarFeature` / `BridgeVisualizerFeature` architectural pattern
