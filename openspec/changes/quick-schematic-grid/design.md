## Context

In Mindustry, placing schematics quickly during gameplay is crucial for efficiency. Opening the full schematics browser dialog disrupts real-time play. Players need an on-demand, customizable grid of their favorite schematics that allows 1-click selection and placement.

This feature follows the architectural patterns established by `TimeControlFeature`:
- Dual presentation mode: persistent floating `HUD` or on-demand `Popup` anchored to the `QuickAccess` bar.
- Solim declarative UI components and reactive signals.
- Clean separation between feature lifecycle, state/config, and view components.

## Goals / Non-Goals

**Goals:**
- Provide a `QuickSchematicGridFeature` allowing instant 1-click in-game placement of player-configured schematics via `Vars.control.input.useSchematic(schematic)`.
- Support two display variants:
  - `HUD`: Persistent, draggable floating window with orientation-aware position persistence and clamp to screen bounds.
  - `Popup`: Lightweight popup anchored to the QuickAccess bar via `QuickAccessPopupHelper` that automatically dismisses upon item selection.
- Represent configured schematics as an ordered list (`List<QuickSchematicEntry>`) rendered into a grid with configurable column count (`cols`: 1 to 10), button size, and button gap.
- Provide a rich settings UI allowing players to:
  - Add schematics using a dedicated Solim Schematic Picker dialog with search, tag filtering, and preview thumbnails.
  - Reorder items (`move left` / `move right`) and remove items.
  - Automatically display schematic thumbnails via `Vars.schematics.getPreview(schematic)` with optional icon/block override.
- Follow Solim and Mindustry mod rules (no `:solim-runtime` imports, Java 8 runtime compliance, non-nullable annotations, full i18n bundle keys).

**Non-Goals:**
- Creating, capturing, or modifying schematics in-game (vanilla Mindustry tools handle creation).
- Online schematic browsing/downloading within this grid (handled by `SchematicBrowserFeature`).
- Multi-page or multi-tab sets in v1 (kept to a single configurable list for simplicity and speed).

## Decisions

### 1. Data Model & Ordered List Architecture
- **Decision**: Represent configured schematics as an ordered list of `QuickSchematicEntry` objects rather than a sparse 2D grid matrix.
- **Rationale**: An ordered list allows dynamic reflowing when column count changes, makes adding/removing/reordering trivial, and avoids empty holes or corrupted coordinate maps.
- **Schema**:
  ```java
  public class QuickSchematicEntry {
      public String id; // unique entry id
      public String schematicName; // name of the schematic
      public @Nullable String schematicFile; // file name for exact match
      public @Nullable String customIconType; // "block", "item", "icon" or null for auto
      public @Nullable String customIconName; // block/item/icon identifier
      public @Nullable String customLabel; // optional label override
  }
  ```
- **Serialization**: Stored as a JSON string within `ConfigValue<String> entriesJson` or serialized via `mindustry.util.JsonIO` / `JsonUtils`.

### 2. Layout & Reactive Geometry
- **Decision**: Grid geometry is controlled by three reactive configs:
  - `colsConfig`: integer (1 to 10), default 5.
  - `sizeConfig`: float button size in units/pixels (default ~44f - 48f).
  - `gapConfig`: float spacing between buttons in units/pixels (0f to 16f, default 4f).
- **HUD/Popup Layout**:
  - The view renders items sequentially into a Solim `grid(colsSignal)` or responsive row wrapping with `.gap(gapSignal)`.
  - In HUD mode, the root container includes a draggable handle (unless `hideDragHandle` is enabled).
  - In Popup mode, `QuickAccessPopupHelper` positions the popup relative to the QuickAccess bar.

### 3. Selection & Placement Interaction
- **Decision**: Clicking a grid button in either HUD or Popup calls `Vars.control.input.useSchematic(schematic)`.
- **Popup Dismissal**: In Popup mode, `QuickAccessPopupHelper.hide()` / `menu.hide()` is called immediately before activating the schematic so the player's view and build cursor are unobstructed.
- **Rule Verification**: If `!Vars.state.rules.schematicsAllowed`, an informative warning message is displayed instead of placement.

### 4. Solim Schematic Picker Dialog
- **Decision**: Build a Solim dialog (`SchematicPickerDialog`) to select from local schematics (`Vars.schematics.all()`) instead of launching vanilla Mindustry's browser.
- **Features**:
  - Real-time search query filtering schematic names and descriptions.
  - Tag/label filtering chips.
  - Keyed `reactiveGrid` rendering schematic cards with thumbnail preview, title, and dimensions.
  - 1-click selection immediately adds the item to the quick grid list.

### 5. Settings Dialog UX
- **Decision**: Provide inline card actions in the settings dialog for each entry:
  - `◄` / `►`: Swap position with adjacent items in the list.
  - `✕`: Remove entry from the list.
  - Clicking entry thumbnail: open slot customization (override icon or choose different schematic).
  - Prominent `[ + Add Schematic ]` button at the end of the list.

## Risks / Trade-offs

- **[Missing or Deleted Schematic]** → The player may delete a schematic from Mindustry outside the mod.
  - *Mitigation*: Look up schematics first by `schematicFile`, then by `schematicName`. If not found, render the slot with a warning icon (`Icon.warning`), disabled state, and tooltip indicating the schematic was deleted or moved.
- **[HUD Dragging Off-screen]** → Dragging the HUD could place it out of bounds on screen resolution change.
  - *Mitigation*: Re-use orientation-aware `xConfig`/`yConfig` with clamping logic (`keepInScreen()`) as implemented in `TimeControlHudView`.
- **[Preview Generation Overhead]** → Loading previews for dozens of schematics simultaneously could spike frame times.
  - *Mitigation*: Mindustry caches schematic previews in `Vars.schematics.getPreview()`. Solim renders texture regions lazily.
