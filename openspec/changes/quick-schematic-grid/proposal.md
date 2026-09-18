## Why

In Mindustry gameplay, accessing frequently used schematics (e.g. power distribution, drill clusters, defense walls, logic blocks) requires opening the full schematics browser, searching or scrolling through hundreds of entries, and selecting one. This interrupts active gameplay and combat. A dedicated Quick Schematic Grid feature provides instant, 1-click access to player-configured schematics directly on screen via an in-game HUD or QuickAccess popup.

## What Changes

- Introduce `QuickSchematicGridFeature` with two display variants:
  - **HUD**: Floating, draggable on-screen grid with clamped positioning, saved orientation coordinates, and optional hide-drag-handle setting.
  - **Popup**: Anchored to the QuickAccess bar via `QuickAccessPopupHelper`, automatically closing upon selecting a schematic for immediate building visibility.
- Support an ordered list of configured schematic entries.
- Configurable grid layout:
  - Configurable column count (`cols`: 1 to 10), with rows flowing naturally based on item count.
  - Configurable button size (scale/pixels).
  - Configurable gap spacing between buttons (`gap`).
- Schematic entry management in the Settings dialog:
  - Reorder entries (`move left` / `move right`).
  - Remove entries.
  - Custom Solim Schematic Picker Dialog allowing players to search, filter by tag, and select from local schematics (`Vars.schematics.all()`).
  - Automatic preview thumbnails via `Vars.schematics.getPreview(schematic)` with optional custom icon/block overrides.
- 1-click in-game schematic selection via `Vars.control.input.useSchematic(schematic)`.

## Capabilities

### New Capabilities
- `quick-schematic-grid`: Quick access grid of buttons linked to local schematics, supporting HUD and Popup display variants, configurable geometry (columns, button size, gap), reordering and removal, and an integrated Solim schematic picker.

### Modified Capabilities
<!-- None -->

## Impact

- **Code additions**: New package `mindustrytool.features.schematicgrid` containing feature lifecycle, HUD view, popup view, settings view, schematic picker dialog, and data models.
- **Registration**: Register `QuickSchematicGridFeature` in `Main.java` features list.
- **Internationalization**: New translation keys in `assets/bundles/bundle.properties`.
- **Dependencies**: Uses existing Solim UI components, Mindustry's `Vars.schematics`, and `QuickAccessPopupHelper`.
