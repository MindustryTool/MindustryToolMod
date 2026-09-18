## Why

The Quick Schematic Grid v1 (`quick-schematic-grid` change) is functionally complete but rough in daily use: schematic thumbnails render misplaced inside their buttons, an empty grid shows a bare drag handle with no guidance, entries can be deleted with one accidental tap, and the slot-customization UI promised in the v1 design (edit entry, custom icon) was never built.

## What Changes

- Fix schematic thumbnail rendering so previews fill their buttons centered with correct aspect (letterboxed), in HUD, popup, and picker cards.
- Show a guidance hint when the HUD/popup grid has no configured entries.
- Require confirmation before deleting a configured entry.
- Add a slot edit dialog per entry: custom display label, replace linked schematic (via existing picker), and custom icon.
- Add a vanilla-style icon picker (font glyphs + unlocked content emojis) for slot icons; a custom icon replaces the schematic preview on grid buttons.
- Simplify `QuickSchematicEntry` icon storage to a single emoji string (old persisted fields are ignored on read, no migration needed).
- Add drag-to-reorder for settings entries via long-press-to-grab (plan B: long-press action menu if the spike findings don't hold during implementation).
- Add missing bundle keys with translator comments for all new user-visible text.

## Capabilities

### New Capabilities

- `quick-schematic-grid-polish`: Polish behaviors for the schematic grid — correct thumbnail rendering, empty-state guidance, delete confirmation, slot editing with custom label/schematic/icon, vanilla-style icon picking, and drag-to-reorder with defined gesture arbitration and fallback.

### Modified Capabilities

<!-- None: the v1 quick-schematic-grid spec was never archived to openspec/specs/, so all polish behavior is specified fresh under the new capability above. -->

## Impact

- Code: `mindustrytool.features.schematicgrid` package (HUD view, popup, picker dialog, settings view, entry model); no other features touched.
- Persistence: entry JSON gains a single icon field and drops the two unused icon fields; old stored entries keep parsing (lenient reader), so no migration step.
- Localization: new keys appended to `assets/bundles/bundle.properties`.
- Dependencies: none — uses existing Solim primitives, Arc Scene APIs, and vanilla `Vars.ui.showConfirm`.
