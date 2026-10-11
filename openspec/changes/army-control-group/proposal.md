## Why

Controlling large unit armies in Mindustry is tedious during fast-paced RTS gameplay, especially on mobile or touchscreen devices where precision box-selecting is cumbersome. Players lack a fast way to select all units across the map or on-screen, save unit squads into control groups (e.g. Zeniths in Group 1, Fortresses in Group 2), and quickly deselect unwanted unit types (such as builder/miner drones like Mono) from their active selection.

## What Changes

- Add an **Army Control** feature accessible via the QuickAccess bar (popup dialog by default, with option for persistent HUD docking).
- Add one-click scope selection buttons:
  - **Select All (Map)**: Selects all player team units across the entire map.
  - **Select Screen**: Selects all player team units currently visible within the camera view.
  - **Deselect All**: Clears active unit selection.
- Add **Control Groups**:
  - Create and save unit groups from Map, Screen, or Current Selection.
  - One-tap recall of saved groups to immediately select their units and switch into RTS Command Mode.
  - Prune dead units automatically and manage/delete groups.
- Add an **Active Selection Bar & Unit Deselection**:
  - Displays unit type chips with counts for currently selected units.
  - Single-tapping a unit type chip immediately deselects/excludes that unit type from the active selection.
- Integrate seamlessly with Mindustry's vanilla RTS Command Mode (`input.commandMode = true`) without moving or centering the camera.

## Capabilities

### New Capabilities
- `army-control-group`: Army selection buttons (map/screen), RTS control group creation and recall, and quick unit type deselection via interactive chips.

### Modified Capabilities
<!-- None -->

## Impact

- **UI / Features**: New `ArmyControlFeature`, settings view, popup, and HUD components under `mindustrytool.features.armycontrol`.
- **QuickAccess**: Registers Army Control icon into the QuickAccess toolbar.
- **Input System**: Interacts with `Vars.control.input.selectedUnits` and `Vars.control.input.commandMode`.
- **Localization**: New bundle strings in `assets/bundles/bundle.properties`.
