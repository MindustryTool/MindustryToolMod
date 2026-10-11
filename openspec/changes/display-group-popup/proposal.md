## Why

Currently, all display and rendering features (`HealthBarFeature`, `RangeDisplayFeature`, `PathfindingFeature`, `ProgressDisplayFeature`, `TeamResourceFeature`, `VisualizerFeature`, `WavePreviewFeature`, and `ToggleRenderingFeature`) have their own individual buttons pinned to the QuickAccess HUD bar by default. With default 4 columns, these 8+ display features alone consume 2–3 rows of screen space during active gameplay, cluttering the HUD. Merging these features into a dedicated anchored popup (following the `AutoplayFeature` pattern) dramatically reduces QuickAccess HUD footprint to a single icon while keeping all display overlays instantly accessible in 2 taps.

## What Changes

- **New DisplayGroupFeature**: Introduces a dedicated `DisplayGroupFeature` (`display-group`, icon `Icon.eye`) enabled by default and pinned to QuickAccess HUD.
- **QuickAccess Anchored Popup (`DisplayGroupPopup`)**: Tapping the `DisplayGroupFeature` button on QuickAccess opens an anchored Solim `Popup` containing a configurable reactive grid (default 4 columns) of individual display overlay buttons.
- **Independent Feature Toggles**: Each button inside the popup independently toggles that specific display feature (`f.setEnabled(!f.isEnabled())`) and reflects its active state reactively; long-pressing any button opens that feature's setting dialog.
- **Customizable Order and Visibility**: `DisplayGroupFeature` provides a settings dialog (`DisplayGroupSettingsDialog`) allowing players to configure column count (default 4), reorder display items, and hide/show individual items inside the popup.
- **QuickAccess HUD De-cluttering**: Sets `quickAccess(false)` on the 8 individual display features (`health-bar`, `range-display`, `pathfinding`, `progress-display`, `team-resource`, `visualizer`, `wave-preview`, `toggle-rendering`), shrinking default QuickAccess HUD down to essential tools.

## Capabilities

### New Capabilities
- `display-group`: Introduces `DisplayGroupFeature`, the anchored `DisplayGroupPopup`, configurable grid layout, reordering/visibility management, and settings dialog for aggregated display controls.

### Modified Capabilities
- `quickaccess-visibility`: Updates default QuickAccess feature inclusion so individual display overlay features are disabled by default in QuickAccess, delegating their in-game quick toggle access to `display-group`.

## Impact

- **New classes**: `DisplayGroupFeature`, `DisplayGroupPopup`, `DisplayGroupPopupView`, `DisplayGroupSettingsDialog`, `DisplayGroupSettingsView`.
- **Modified classes**: `Main.java` (registers `DisplayGroupFeature`), plus constructor metadata updates (`.quickAccess(false)`) in `HealthBarFeature`, `RangeDisplayFeature`, `PathfindingFeature`, `ProgressDisplayFeature`, `TeamResourceFeature`, `VisualizerFeature`, `WavePreviewFeature`, and `ToggleRenderingFeature`.
- **Localization**: Adds bundle keys for `display-group` name, description, settings, and tooltips in `assets/bundles/bundle.properties`.
- **No breaking changes**: Individual features remain fully functional independently; users who still wish to pin specific display features directly to the QuickAccess bar may still do so via QuickAccess settings.
