## 1. Data Model & Feature Foundation

- [ ] 1.1 Create `QuickSchematicEntry` model class for serialized entries with identifier, schematic reference, custom icon/label overrides, and serialization helpers
- [ ] 1.2 Implement `QuickSchematicGridFeature` subclassing `Feature` with configs: `displayMode` (HUD/Popup), `cols` (1-10), `buttonSize`, `buttonGap`, `hideDragHandle`, orientation-keyed positions, and `entries`
- [ ] 1.3 Implement list management methods in `QuickSchematicGridFeature`: add, remove, moveEarlier, moveLater, and schematic resolution from `Vars.schematics.all()`
- [ ] 1.4 Register `QuickSchematicGridFeature` in `Main.java`

## 2. Localization & Bundle Strings

- [ ] 2.1 Add bundle keys and translator comments in `assets/bundles/bundle.properties` for settings titles, display mode options, geometry sliders (cols, size, gap), reorder/remove buttons, tooltips, and warning messages

## 3. Solim Schematic Picker Dialog

- [ ] 3.1 Create `SchematicPickerDialog` using Solim with search input, tag filtering, and responsive list/grid
- [ ] 3.2 Implement schematic card components showing preview thumbnail via `Vars.schematics.getPreview`, title, dimensions, and selection callback

## 4. Settings Dialog & List Management UI

- [ ] 4.1 Create `QuickSchematicGridSettingsDialog` and `QuickSchematicGridSettingsView` with display mode toggle (HUD/Popup), columns slider (1-10), button size slider, and button gap slider
- [ ] 4.2 Build interactive entry list in `QuickSchematicGridSettingsView` showing configured items with inline reorder (`◄` / `►`) and remove (`✕`) controls
- [ ] 4.3 Add `+ Add Schematic` button in settings view that opens `SchematicPickerDialog` and appends selected schematics to the list
- [ ] 4.4 Add reset position action button

## 5. In-Game Popup View

- [ ] 5.1 Implement `QuickSchematicGridPopup` anchored to QuickAccess bar using `QuickAccessPopupHelper`
- [ ] 5.2 Build reactive grid layout in popup respecting `colsSignal`, `sizeSignal`, and `gapSignal`
- [ ] 5.3 Implement 1-click schematic activation that auto-dismisses the popup before calling `Vars.control.input.useSchematic(schematic)`

## 6. In-Game HUD View

- [ ] 6.1 Implement `QuickSchematicGridHudView` as a draggable, floating Solim element with screen clamping and orientation-aware position persistence
- [ ] 6.2 Build reactive grid layout in HUD respecting `colsSignal`, `sizeSignal`, `gapSignal`, and `hideDragHandleConfig`
- [ ] 6.3 Implement button rendering with schematic preview thumbnails, tooltips, and click-to-place integration

## 7. Verification & Integration

- [ ] 7.1 Write unit tests verifying `QuickSchematicEntry` serialization, JSON persistence, and reordering logic
- [ ] 7.2 Run project build and tests to verify Java 8 compatibility and clean compilation
