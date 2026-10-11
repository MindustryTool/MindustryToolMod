## Why

QuickAccess popups (GodMode, TimeControl, Autoplay, DisplayGroup, SchematicGrid) are always horizontally centered on the HUD bar. When the HUD is dragged near the left or right screen edge, the centered popup overflows and gets screen-clamped, visually detaching from the HUD that spawned it.

## What Changes

- Anchor popups below/above the HUD as today (upper-half rule unchanged), but choose horizontal edge by HUD position.
- When HUD center is on the left half of the stage, align popup left edge to HUD left edge; when on the right half, align popup right edge to HUD right edge.
- Clamp the result inside the stage as a safety net (including popup-wider-than-stage fallback).
- Apply uniformly to all popups routed through `QuickAccessPopupHelper.show()` with no per-popup option and no new setting.

## Capabilities

### New Capabilities

- `quickaccess-popup-align`: horizontal edge alignment of QuickAccess popups relative to HUD position.

### Modified Capabilities

- None — existing visibility/order specs describe which buttons render, not where popups anchor.

## Impact

- Affected code: `mod/src/mindustrytool/features/quickaccess/QuickAccessPopupHelper.java`, callers (`GodModePopup`, `TimeControlPopup`, `AutoplayPopup`, `DisplayGroupPopup`, `QuickSchematicGridPopup`) unchanged.
- No API/bundle/config changes; automatic behavior only.
- Risk: HUD dragged across screen midpoint causes a one-frame horizontal snap of an open popup; acceptable since popups close on resize and HUD drag is infrequent while open.
