## Why

The "Free camera enabled" pill renders visibly right of screen center: `Hud.position()` anchors the actor's bottom-left corner, so the pill's left edge sits on the centerline instead of the pill being centered on it. The Solim HUD approach needs fragile post-pack width compensation; drawing the label directly each frame centers it exactly with no layout involved.

## What Changes

- Replace the `FreeCameraHudView` Solim overlay with a `Trigger.draw`-based world-space label (following the `AutoplayFeature.draw()` precedent): `Pal.accent` text, horizontally centered on the camera, near the top edge, drawn only while the feature is enabled during gameplay.
- Remove `FreeCameraHudView` and its mount/unmount wiring in `FreeCameraFeature` (including the `ResizeEvent`/`keepInScreen` handling, which becomes unnecessary).
- `JoinApprovalHudView` keeps its current anchor (same latent pattern, different owner — noted as a follow-up, not touched here).

## Capabilities

### New Capabilities

- None — no standalone capability is introduced.

### Modified Capabilities

- `free-camera`: the enabled-state indicator requirement changes render mechanism (frame-drawn centered label instead of HUD pill) while keeping position, color, visibility, and i18n behavior.

## Impact

- Affects `FreeCameraFeature` (draw hook registration) and deletes `FreeCameraHudView`; no changes to input handlers, camera logic, bundle keys (`status.free-camera.enabled` reused as-is), or any other feature.
- Drawn labels never participate in the scene graph, so input pass-through is structural rather than configured.
