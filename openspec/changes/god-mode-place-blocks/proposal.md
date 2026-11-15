## Why

The current God Mode placement tool is strictly limited to core blocks (`CoreBlock`), always places blocks at fixed rotation 0, and aborts placement on any solid tile. Expanding this capability to all synthetic and sandbox blocks organized by Mindustry categories gives players and map/mod creators a comprehensive in-game sandbox placement utility without relying on manual console commands.

## What Changes

- **Universal Block Selection**: Replace `GodModeCoreDialog` with `GodModeBlockDialog`, exposing all synthetic and sandbox blocks (`block.synthetic() && !block.isAir()`), including debug/hidden blocks (e.g. Item Source, Liquid Source, World Processor).
- **Category Filtering**: Add category filter chips (All, Turret, Production, Distribution, Liquid, Power, Defense, Crafting, Units, Effect, Logic) with localized names and real-time query search.
- **Orientation & Rotation**: Add 4-direction orientation controls (0°, 90°, 180°, 270°) visible dynamically when the selected block is rotatable (`block.rotate`).
- **Force Overwrite Placement**: Update providers (`InternalGodModeProvider` and `JSGodModeProvider`) to place blocks with rotation and overwrite existing tiles directly rather than failing on solid tiles.
- **Provider API Generalization**: Update `GodModeProvider` to support `placeBlock(Block block, Team team, float x, float y, int rotation)`, while retaining `placeCore` as a default bridge method for compatibility.
- **HUD & Localization**: Update HUD button tooltip and bundle properties from core-specific keys to general block placement keys.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `god-mode`: Expand core placement requirement into universal block placement with category filtering, rotation support, and force overwrite placement.

## Impact

- `mindustrytool.features.godmode.GodModeProvider`: Add `placeBlock(Block, Team, float, float, int)` and default bridge for `placeCore`.
- `mindustrytool.features.godmode.InternalGodModeProvider`: Implement `placeBlock` with rotation and force tile replacement.
- `mindustrytool.features.godmode.JSGodModeProvider`: Implement `placeBlock` via `/js` with rotation parameter.
- `mindustrytool.features.godmode.GodModeCoreDialog`: Replaced by `GodModeBlockDialog` with search, category filtering, and rotation controls.
- `mindustrytool.features.godmode.GodModeHudView`: Point the hammer button to `GodModeBlockDialog` with updated tooltip.
- `assets/bundles/bundle.properties`: Add block placement and category strings.
