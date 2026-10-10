## Why

Currently, features with scalable UI elements (such as Autoplay, God Mode, Health Bar, Progress Display, Quick Access, Team Resource, Time Control, and Wave Preview) manage their scale independently. On mobile devices with higher pixel density or constrained touch screens, UI elements default to the same 1.0 scale as desktop, often leading to disproportionately large HUD widgets or awkward layout spacing. Users must manually tune the scale slider inside each feature's settings dialog.

Introducing a universal scale multiplier provides a global setting in `GeneralSettingsDialog` that defaults sensibly across platforms (1.0 on desktop, 0.8 on mobile) and cleanly multiplies each feature's individual scale.

## What Changes

- Add `ModSettings.universalScale` configuration value with platform-aware defaults (`1.0f` on desktop, `0.8f` on mobile).
- Add a slider in `GeneralSettingsView` (`GeneralSettingsDialog`) allowing adjustment of `universalScale` from 0.5x to 1.5x (step 0.05).
- Expose a reactive helper `ModSettings.effectiveScale(Readable<Float> featureScale)` returning `Computed<Float>` (and a direct float overload for non-reactive/world rendering contexts).
- Update HUD and overlay feature views to multiply feature-specific scale by `universalScale` via `ModSettings.effectiveScale(...)`:
  - `AutoplayHudView`
  - `GodModeHudView`
  - `QuickAccessHudView`
  - `TeamResourceHudView`
  - `TimeControlHudView`
  - `WavePreviewPanelView`
  - `HealthBarFeature` (in-world bar scale)
  - `ProgressDisplayFeature` (in-world bar scale)
- Add necessary i18n bundle keys and translator comments for universal scale in `bundle.properties`.

## Capabilities

### Modified Capabilities
- `app-settings`: Extend `GeneralSettingsDialog` / `GeneralSettingsView` to include the Universal Scale slider and persist it in `ModSettings`.

## Impact

- Mod-wide settings (`ModSettings.java`)
- Settings UI (`GeneralSettingsView.java`, `bundle.properties`)
- HUD views and world-renderers consuming scale configs (`AutoplayHudView`, `GodModeHudView`, `QuickAccessHudView`, `TeamResourceHudView`, `TimeControlHudView`, `WavePreviewPanelView`, `HealthBarFeature`, `ProgressDisplayFeature`)
- Existing feature scale configs and individual settings sliders remain backward-compatible and continue to operate as relative multipliers.
