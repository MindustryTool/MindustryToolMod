## Why

Players cannot customize how far the camera zooms in or out while playing. A standalone camera-zoom feature with persisted min/max limits gives them control, including going beyond vanilla zoom in both directions.

## What Changes

- Add standalone Camera Zoom feature (`features/camera-zoom/`) with feature card and QuickAccess HUD entry.
- Persist `min-zoom` and `max-zoom` limits; defaults equal vanilla limits so a fresh install is a no-op until the user moves a slider.
- Clamp the live renderer scale into `[min, max]` every frame while in-game, covering desktop wheel and mobile pinch uniformly.
- Enforce the `min <= max` invariant by pushing the other value when sliders cross.
- Add Solim settings dialog with min/max slider rows, reactive value labels, and reset-to-defaults.
- Add `feature.camera-zoom.*` bundle keys with translator comments and a Lucide zoom/camera feature icon.

## Capabilities

### New Capabilities
- `camera-zoom-limits`: adjustable camera min/max zoom enforcement with persisted settings and settings UI.

### Modified Capabilities
- None.

## Impact

- New code under `mod/src/mindustrytool/features/camera-zoom/` (feature, settings dialog/view); no changes to existing features.
- Per-frame `Trigger.update` hook gated on enabled + in-game + HUD shown; client-side zoom only, no protocol changes.
- New bundle keys in `assets/bundles/bundle.properties`; feature icon via existing `assets/icons/` conventions.
