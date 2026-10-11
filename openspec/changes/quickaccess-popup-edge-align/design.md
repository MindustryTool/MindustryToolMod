## Context

`QuickAccessPopupHelper.show(menu, data, bar, gap)` currently centers every popup: `desiredX = barCenterX - menuW/2`, with vertical placement above the bar when the bar is in the upper half and below otherwise. `Popup.show()` + `Popup.place()` then clamps inside the stage. Near screen edges the centered anchor overflows, so the clamp detaches the popup from its HUD bar. All five popups (`GodMode`, `TimeControl`, `Autoplay`, `DisplayGroup`, `QuickSchematicGrid`) share this helper and read live bar bounds (`x`, `y`, `getWidth()`, `getHeight()`).

## Goals / Non-Goals

**Goals:**
- Align popup horizontal edge to HUD edge based on HUD half: left-half HUD left-aligns, right-half HUD right-aligns.
- Preserve existing vertical rule and stage clamping.
- Single uniform behavior for all helper callers, no config or bundle changes.

**Non-Goals:**
- Side flyouts (popup to the left/right of HUD); vertical placement unchanged.
- 3-zone center-preserving behavior; per-popup overrides; user-facing alignment setting.
- Changes to generic `Popup.place()` semantics.

## Decisions

- **Binary split on `barCenterX >= stageW/2` over 3-zone center:** binary is predictable, trivially testable, and fixes the reported edge case. 3-zone preserves centered aesthetics mid-screen but adds thresholds and drag-across snap; rejected per user decision.
- **Compute in helper, not `Popup`:** helper already resolves bar geometry and does the post-`show()` reposition via `menu.table().setPosition()`. Keep `Popup.place()` generic (anchor + clamp + vertical flip) untouched; pass `anchorX = leftHalf ? barX : barX + barW - menuW` semantics through `desiredX`. Alternatives (adding alignment param to `Popup`) would leak QuickAccess concerns into Solim core.
- **Uniform application:** all callers get the behavior automatically since they delegate to the helper. Per-popup param rejected — inconsistent UX and larger API surface.
- **Clamp stays last:** `clampedX = clamp(desiredX, 0, stageW - menuW)`, `0` when `menuW >= stageW`; same for Y. Guarantees on-screen even for oversized popups.

## Risks / Trade-offs

- [Snap when HUD crosses midpoint with popup open] → Accept; popups close on resize/outside-tap and HUD drag while open is rare.
- [Popup measured before layout (`getWidth()==0` on first show)] → Mitigate by keeping existing two-phase flow: `show()` packs/measures, then reposition; fall back to current centered `anchorX` when `menuW<=0`.
- [`quickAccessBar == null` (headless/tests)] → Keep existing stage-center fallback unchanged.
