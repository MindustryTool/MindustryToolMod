## Why

In Mindustry, rotatable blocks (such as unit factories, reconstructors, conveyors, sorters, mass drivers, and unloaders) frequently need their orientation adjusted during gameplay. Specifically, when a unit factory finishes producing a unit but its output direction is obstructed, the factory becomes clogged. In vanilla gameplay, reorienting an existing building requires selecting the block in the build menu, rotating, and rebuilding or deconstructing, which is cumbersome and wastes time. Providing an in-world clickable rotate overlay allows players to immediately rotate any owned rotatable block in either direction with a single click.

## What Changes

- Add a new feature `block-quick-rotate` under `features/quickrotate/`.
- Introduce an in-world floating overlay widget that renders adjacent to the selected or clicked rotatable block.
- Provide two directional controls:
  - `↺` Rotate Counter-Clockwise (left)
  - `↻` Rotate Clockwise (right)
- Synchronize rotations safely using Mindustry's networking protocol (`Call.rotateBlock(...)`).
- Implement lifecycle management: the overlay automatically appears when clicking/selecting an eligible rotatable block on the player's team and auto-hides when deselecting, clicking away, or moving the camera far away.
- Add configuration settings to enable/disable the feature or customize behavior.
- Add translatable localization bundle keys for all user-facing UI elements and tooltips.

## Capabilities

### New Capabilities
- `block-quick-rotate`: In-world floating rotation overlay for rotatable buildings with multiplayer-safe rotation controls and automatic selection lifecycle.

### Modified Capabilities
<!-- None -->

## Impact

- **UI / HUD**: Adds an in-world overlay element using Solim components projected onto screen coordinates.
- **Networking**: Dispatches `Call.rotateBlock(player, building, direction)` which interacts with Mindustry's standard server synchronization.
- **Localization**: Adds new translation keys to `assets/bundles/bundle.properties`.
- **Feature Management**: Registers `QuickRotateFeature` in `FeatureManager`.
