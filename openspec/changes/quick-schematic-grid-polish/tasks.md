## 1. Localization

- [ ] 1.1 Add bundle keys with translator comments for empty-grid hint, delete confirm title/message, edit dialog labels, icon picker sections, and drag hint

## 2. Rendering Fix

- [ ] 2.1 Fix `SchematicImage` bounds in HUD/popup grid buttons so previews center letterboxed within `buttonSize`
- [ ] 2.2 Apply the same bounds fix to picker card previews
- [ ] 2.3 Add `.empty()` guidance branches to the HUD and popup grids

## 3. Model Simplification

- [ ] 3.1 Collapse `customIconType`/`customIconName` to a single icon field on `QuickSchematicEntry` with legacy-tolerant parsing
- [ ] 3.2 Render a saved custom icon in place of the schematic preview on grid buttons

## 4. Slot Edit & Icon Picker

- [ ] 4.1 Create slot edit dialog with live thumbnail (tap re-picks schematic), label field, and icon entry with clear action
- [ ] 4.2 Create vanilla-style icon picker with font-glyph and content-emoji sections
- [ ] 4.3 Add per-row pencil button in settings opening the edit dialog

## 5. Safe Delete & Reorder

- [ ] 5.1 Gate entry removal behind `Vars.ui.showConfirm`
- [ ] 5.2 Implement long-press-to-grab with scroll lock, live drag preview, and drop-to-persist reordering (fallback to long-press action menu if spike findings break)

## 6. Verification

- [ ] 6.1 Extend unit tests for icon field serialization, legacy entry parsing, and reorder helpers
- [ ] 6.2 Run project build and tests to verify Java 8 compatibility and clean compilation
