## Why

With the mod installed, the stock desktop detach-camera behaves differently than vanilla even when the mod's own Free Camera is off: the camera detaches but the unit no longer chases it. Players using the original game feature get subtly wrong behavior, and a stale vanilla `spectating` target can additionally pull the camera away right after a snap-to-player.

## What Changes

- When `FreeCameraFeature` is OFF on desktop and vanilla `detach-camera` is ON, `ModDesktopInput` reproduces stock behavior: the unit moves toward the camera position (vanilla chase logic) instead of staying keyboard-driven.
- `FreeCameraFeature.snapToPlayer()` additionally clears the vanilla `spectating` target, mirroring what vanilla's own detach-toggle-off does.
- Explicitly unchanged: vanilla `detach_camera` key being a silent no-op while the mod's free camera is ON, and the per-frame force/restore of the `detach-camera` setting.

## Capabilities

### New Capabilities

- None — no standalone capability is introduced.

### Modified Capabilities

- `unified-input-handling`: desktop input SHALL reproduce stock detach-camera unit behavior when the mod's free camera is inactive.
- `free-camera`: `snapToPlayer()` SHALL clear the vanilla `spectating` target in addition to centering the camera and resetting panning.

## Impact

- Affects `ModDesktopInput.updateMovement()` (desktop-only branch) and `FreeCameraFeature.snapToPlayer()`; no changes to `ModMobileInput` (vanilla mobile has no detach concept), HUD, settings, keybinds, or camera panning paths.
- Players who never touch vanilla detach-camera see zero behavior change.
