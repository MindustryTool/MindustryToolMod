## Context

The v1 grid (`mindustrytool.features.schematicgrid`) renders schematic previews via vanilla `SchematicsDialog.SchematicImage` attached with Solim `arc()` inside fixed-size buttons. The raw Arc `Image` keeps its native preferred size anchored top-left (Solim `Button.children` uses `Row.ATTACHER` with top-left defaults; only expanding children grow), so previews sit in the corner with dead space right/bottom instead of filling the button. The entry model carries `customIconType`/`customIconName` with no UI, no edit dialog exists, delete is instant, and the grid has no empty state. A pre-implementation spike verified all drag-reorder mechanisms against the codebase and Arc API.

## Goals / Non-Goals

**Goals:**
- Centered, aspect-correct (letterboxed) thumbnails in HUD, popup, and picker cards while keeping the vanilla tiled background, border, and hover accent.
- Full slot customization: label override, schematic replacement, custom icon (replaces preview).
- Safe destructive action (delete confirm) and discoverable ordering (drag preferred, menu fallback).
- Guidance text when the grid is empty.

**Non-Goals:**
- Online schematic browsing (owned by `SchematicBrowserFeature`).
- Multi-page/tab sets, schematic creation/editing (vanilla tools).
- New Solim framework primitives — reorder is built from existing signals plus raw Arc listeners via `arc()`.

## Decisions

### 1. Rendering fix: keep `SchematicImage`, fix bounds
- **Decision**: Set `setFillParent(true)` on each `SchematicImage` so it tracks its fixed-size parent bounds (button/cell set by Solim `.size()`), with centered alignment; keep `Scaling.fit` (letterbox). FillParent reacts to size changes for free, so no sync effect is needed.
- **Rationale**: Preserves vanilla look (tiled background, border, hover accent, disposed-texture recovery). Rejected Solim-native `image(getPreview)` (loses background/border) and bare `stack().size()` (Solim stack layers wrap content in a `Row`, so the image still keeps preferred size — verified against `SolimStack`/`Row.ATTACHER`).
- **Aspect**: Letterbox, matching vanilla `SchematicsDialog` exactly; cropping would hide schematic content.

### 2. Drag-to-reorder: long-press-to-grab with Arc-arbitrated gestures
- **Decision**: Long-press (300ms, existing `Button.onLongClick` precedent in `QuickAccessHudView`) on the row background/grip grabs the row; on fire, call `Scene.cancelTouchFocusExcept` plus `ScrollPane.setScrollingDisabled(true)` (reachable via Solim `Scroll.pane()`); drag maps finger Y to target index; drop persists via existing `moveById`/persist and unlocks scroll.
- **Rationale**: Spike confirmed every piece — `Hud.makeDraggable` precedent (stage-coordinate drag deltas, `ClickListener.cancel()` on drag threshold, `isInteractiveDescendant` guard for the 4 per-row buttons), `JoystickWidget` pointer hygiene (`activePointer`), and `StructuralReconciler` instance preservation on reorder (no rebuilds; touch focus survives re-parenting since focus is listener-bound).
- **Gesture arbitration**: If the finger moves before 300ms, the `ScrollPane` wins and cancels our press via its own `cancelTouchFocus` — standard platform behavior, to be noted in QA expectations, not treated as a bug.
- **Fallback (plan B)**: If implementation contradicts spike findings, ship a long-press action menu (move to top/up/down/bottom, edit, delete) instead. The fallback absorbs the edit and delete workstreams, so scope stays shippable either way.

### 3. Slot edit dialog with dedicated pencil button
- **Decision**: Per-row pencil button first after the text (`[text] ✎ ◄ ► ✕`); dialog contains live thumbnail (tap re-picks via existing `SchematicPickerDialog`), label text field (`customLabel`), and icon picker entry with clear-icon action.
- **Rationale**: Explicit over tap-thumbnail (discoverability, per user decision); keeps ◄/► until drag lands.

### 4. Icon picker: two-section vanilla-style, single-string storage
- **Decision**: Sections mirroring vanilla `showNewIconTag` — font glyphs grid plus unlocked content emojis; store the single resolved character (vanilla tags precedent). Collapse `customIconType`/`customIconName` to one field.
- **Display rule**: Custom icon replaces the preview on grid buttons (Solim `icon()` + `.size()` is already bounds-correct, sidestepping the rendering bug).

### 5. Delete: vanilla `showConfirm`
- **Decision**: `Vars.ui.showConfirm` with two new bundle keys (precedent: `AuthOverlay` logout confirm). No custom Solim dialog for a yes/no question.

### 6. Empty states
- **Decision**: `.empty()` branches on HUD and popup grids (API already used by the picker); HUD gets its own guiding key pointing at settings rather than reusing the settings-list string.

## Risks / Trade-offs

- **[Drag mid-gesture reconcile]** → Re-adding the dragged actor during live reorder is safe (focus is listener-bound; rows are stateless), but if visual glitches appear, fall back to commit-on-drop only (no live preview).
- **[Pressed-state cosmetics]** → Dragged row may keep pressed visuals; cosmetic only, acceptable for v1 of the gesture.
- **[Row density]** → Five elements per settings row is dense on narrow screens; accepted for explicitness, revisit if cramped.
- **[Slow-start drag scrolls]** → Moving before 300ms scrolls by design (Arc arbitration); document for QA.
- **[Preview texture lifecycle]** → Unchanged: `SchematicImage` keeps its disposed-texture recovery.

## Migration Plan

No migration step. Entry JSON reader is lenient (unknown properties ignored), so old entries with `customIconType`/`customIconName` parse fine and the new single icon field simply appears on next edit. Rollback is a clean revert with no data loss (new field ignored by old code).
