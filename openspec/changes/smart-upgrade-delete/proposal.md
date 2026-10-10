## Why

Players frequently need to tear down obsolete or misplaced conveyor lines, conduit pipelines, bridge chains, drill arrays, or wall perimeters. Currently, `SmartUpgradeFeature` only supports replacing connected chains with another unlocked block of the same group and size—meaning players must manually deconstruct chains tile by tile (or risk deleting adjacent unrelated structures with area selection), and cannot use the smart chain menu at all when no alternative upgrade candidates are unlocked.

Adding a **Delete** option directly inside the Smart Upgrade popup menu lets players bulk-deconstruct an entire connected chain in a single click using the same familiar traversal rules (`maxUpdates`, `onlySameType`, `traverseBridges`).

## What Changes

- **Delete Button in Smart Upgrade Popup Menu (`SmartUpgradeFeature.showMenu`)**:
  - Adds a Delete action button (`Icon.trash` tinted scarlet with localized tooltip) alongside the candidate upgrade blocks in the floating menu.
  - Opens the floating menu even when `getUpgradeCandidates(tile.block())` is empty, so players can still bulk-delete a chain of supported blocks when no alternative upgrade tier is unlocked.
- **Chain Deconstruction (`SmartUpgradeFeature.deleteChain(Tile startTile)`)**:
  - Traverses the connected chain from `startTile` using the existing `expandNeighbors` BFS logic and `cachedMaxUpdates` cap.
  - When **Only Replace Exact Block Type** (`onlySameTypeConfig`) is **enabled**: deletes only buildings whose block matches `startBlock` exactly.
  - When **Only Replace Exact Block Type** is **disabled**:
    - Deletes any building in the same `BlockGroup` regardless of block size (e.g., mixed 1×1 and 2×2 walls or drills).
    - When **Traverse Bridges & Junctions** (`traverseBridgesConfig`) is also **enabled** on a `CONVEYOR` or `CONDUIT` chain, also deletes traversed connector blocks (`isConveyorTraversable` / `isConduitTraversable`: bridges, junctions, routers, sorters, overflow gates/ducts).
  - Normalizes deconstruction coordinates to the building's root tile (`build.tileX()`, `build.tileY()`), deduplicates existing unit build plans at that coordinate, and queues a breaking `BuildPlan(bx, by)` on `Vars.player.unit()`.
  - Displays a HUD toast notification with `Icon.trash` showing the number of blocks queued for deletion.
- **Internationalization**:
  - Adds translation keys (`feature.smart-upgrade.delete` and `feature.smart-upgrade.deleted`) with descriptive translator comments in `assets/bundles/bundle.properties` and `assets/bundles/bundle_vi.properties`, and updates feature description/help text if appropriate.

## Capabilities

### New Capabilities
<!-- None -->

### Modified Capabilities
- `smart-upgrade-trigger`: Expands the Smart Upgrade popup menu and chain execution to support bulk chain deletion alongside block upgrades, including menu opening when zero upgrade candidates exist and connector/multi-size deletion rules.

## Impact

- **Modified Classes**:
  - `mindustrytool.features.smartupgrade.SmartUpgradeFeature`
  - `mindustrytool.features.smartupgrade.SmartUpgradeFeatureTest`
- **Localization Bundles**:
  - `assets/bundles/bundle.properties`
  - `assets/bundles/bundle_vi.properties`
