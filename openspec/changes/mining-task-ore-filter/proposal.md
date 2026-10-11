## Why

Currently, the Autoplay Mining Task settings display all unlocked items in the game without considering whether those items exist on the current map or whether the player's unit is actually capable of mining them. Players have to guess which resources are available and mineable by their unit, often selecting items that the unit cannot mine or that do not exist on the map.

## What Changes

- Filter the mining item selection chips in the Mining Task settings:
  - Only show items that are present on the current map (either as floor ore via `Vars.indexer.hasOre(item)` or wall ore via `Vars.indexer.hasWallOre(item)`).
  - Hide items that do not exist as ores on the active map.
  - Disable (gray out and prevent toggle on) items that the current player's unit cannot mine (e.g., unit mining tier is lower than item hardness, or unit lacks wall/floor mining capability for that ore deposit).
  - Add descriptive tooltips explaining why an item is disabled when the unit cannot mine it.
  - When outside an active match (e.g., accessed from the Main Menu), fall back to displaying all unlocked items with all chips enabled.
- Retain existing configured item selections even if a unit currently cannot mine them, so selections persist when swapping units.

## Capabilities

### New Capabilities
- `mining-task-ore-filter`: Dynamic filtering and capability-based disabling of mining target items in Autoplay Mining Task settings based on active map ores and player unit mining abilities.

### Modified Capabilities
<!-- None: No existing capability specifications in openspec/specs/ cover autoplay mining settings. -->

## Impact

- Mod UI: `MiningTask.java` (`buildSettings` method) in `mindustrytool.features.autoplay.tasks`.
- Localization: `assets/bundles/bundle.properties` with translated tooltips/reasons for disabled ore chips.
- Non-breaking UI enhancement; configuration persistence format remains backward compatible.
