## Why

Players with the QuickAccess HUD enabled juggle multiple persistent overlays, each taking permanent screen space. TimeControl and GodMode need a lighter control surface: an on-demand popup instead of always-visible HUD furniture.

## What Changes

- Each of TimeControl and GodMode gains a per-feature display-mode option (`ConfigValue<String>`, values HUD/popup, default HUD) so the status quo is unchanged unless opted in.
- Both features join QuickAccess (`quickAccess(true)`); GodMode is de-devved into a placeholder shell with no cheat logic (cheat controls arrive in a later rewrite).
- In popup mode with QuickAccess on: tapping the QuickAccess button opens a popup reusing the shared HUD row layout (with popup-only title) instead of toggling; the standalone HUD is fully suppressed; long-press still opens settings; enable/disable lives only in the settings dialog and FeatureCard.
- The shared control surface is extracted to a static function in each feature's `HudView`, mirroring the HUD horizontal row, icon-only everywhere (tooltips only), excluding the draggable handle, and accepting a `canEdit` parameter for disabling controls in the popup when disabled or in net client mode.
- The popup anchors above or below the whole QuickAccess bar depending on the bar's screen position and follows the feature's UI scale setting.
- In popup mode with QuickAccess off: silent fallback to the standalone HUD with no message; the setting stays at popup.
- Net clients opening the TimeControl popup see the controls visibly disabled via the shared layout's `canEdit` parameter.
- All new labels/tooltips resolve from `Core.bundle` with translator comments.

## Capabilities

### New Capabilities

- `god-mode`: de-devved GodMode placeholder shell with QuickAccess membership, display-mode setting, and popup shell; explicitly no cheat logic.

### Modified Capabilities

- `time-control`: QuickAccess membership, display-mode setting, popup control surface with placement/suppression/fallback rules, disabled-controls client state.
- `app-settings`: QuickAccess tap behavior for popup-mode features (tap opens popup instead of toggling), GodMode leaving the development-stub registry (stub count and size expectations).

## Impact

- `TimeControlFeature`/`GodModeFeature` metadata, settings views, and HUD lifecycle; `QuickAccessHudView` item-tap handling gains a popup branch for popup-mode features.
- New Solim `Popup`-based views reusing feature signals; standalone HUD code paths unchanged for default mode.
- `time-control` spec gains popup requirements; `app-settings` QuickAccess-interaction, stub-registry, and count requirements change.
