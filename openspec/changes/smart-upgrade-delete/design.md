## Context

`SmartUpgradeFeature` provides a single-tap (when armed) or `U`-keybind floating menu over any supported building (`CONVEYOR`, `CONDUIT`, `ITEM_BRIDGE`, `LIQUID_BRIDGE`, `WALL`, `DRILL`). Today, `showMenu(Tile tile)` queries `getUpgradeCandidates(tile.block())`, aborts with a toast if no upgrade candidates are available, and otherwise renders a 5-column grid of target block buttons that invoke `upgradeChain(tile, targetBlock)`.

Players want an option in this workflow to **delete (deconstruct)** the connected chain instead of upgrading it.

## Goals / Non-Goals

**Goals:**
- Provide a 1-click **Delete** button (`Icon.trash`) inside the floating Smart Upgrade popup menu.
- Allow the popup menu to open on any valid supported block even when `getUpgradeCandidates` returns an empty list, so the Delete action is always accessible.
- Implement `deleteChain(Tile startTile)` reusing the BFS traversal (`expandNeighbors`) and respecting `maxUpdatesConfig`, `onlySameTypeConfig`, and `traverseBridgesConfig`.
- Delete traversed connectors (bridges, junctions, routers, sorters, overflow gates/ducts) along `CONVEYOR` and `CONDUIT` chains when `traverseBridges` is enabled and `onlySameType` is disabled.
- Support deleting connected blocks of any size within the same `BlockGroup` (such as mixed 1×1 and 2×2 walls or drills) when `onlySameType` is disabled.

**Non-Goals:**
- Adding a separate "Delete Mode" toggle in `SmartUpgradeSettingsDialog` that bypasses the popup menu.
- Adding a separate setting checkbox for connector deletion (it is governed naturally by `traverseBridgesConfig` and `!onlySameTypeConfig`).
- Expanding `BlockGroup` to unrelated building categories (turrets, factories, power nodes).

## Decisions

### Decision 1: Inline Delete Action in Floating Menu
- **Choice**: Append a Delete button (`Icon.trash` tinted with `Pal.remove` / scarlet, 44×44 size matching candidate buttons, localized tooltip `feature.smart-upgrade.delete`) to `currentMenu` in `showMenu(Tile tile)`.
- **Rationale**: Keeps the interaction model unified—triggering Smart Upgrade on a tile always shows the popup menu where the player chooses either a target upgrade block or the Delete button. Removing the early return on `candidates.isEmpty()` ensures players can also delete chains when only a single tier of a block is unlocked.
- **Alternatives Considered**:
  - *Settings mode toggle*: Requires opening settings to switch between upgrade and delete modes, adding unnecessary friction.

### Decision 2: Connector & Multi-Size Deletion Rules in `deleteChain`
- **Choice**: Determine eligibility of each visited building in `deleteChain(Tile startTile)` via a helper `shouldDeleteBlock(Block startBlock, BlockGroup group, Block currentBlock)`:
  - If `cachedOnlySameType` is `true`: `currentBlock == startBlock`.
  - If `cachedOnlySameType` is `false`:
    - `getGroup(currentBlock) == group`, OR
    - `cachedTraverseBridges && ((group == BlockGroup.CONVEYOR && isConveyorTraversable(currentBlock)) || (group == BlockGroup.CONDUIT && isConduitTraversable(currentBlock)))`.
  - Do not restrict by `currentBlock.size == startBlock.size` when `cachedOnlySameType` is `false`.
- **Rationale**:
  - When tearing down a conveyor or conduit line with bridge/junction traversal enabled, leaving orphaned junctions, routers, sorters, and bridges scattered across the map requires tedious manual cleanup.
  - When `onlySameType` is enabled, the player explicitly asked to affect only the exact block type tapped, so connectors and different-sized blocks are preserved.
  - Unlike in-place upgrades (which require an identical footprint size), deconstruction (`new BuildPlan(bx, by)`) works on any building footprint size.

### Decision 3: Root-Tile Normalization and Plan Deduplication
- **Choice**: For each building to delete, resolve its root tile coordinates (`int bx = current.build.tileX(); int by = current.build.tileY();`), track queued buildings or root tiles so multi-tile buildings are counted once, remove any existing `BuildPlan` at `(bx, by)` in `Vars.player.unit().plans`, and add `new BuildPlan(bx, by)` via `Vars.player.unit().addBuild(plan)`.
- **Rationale**: `BuildPlan(int x, int y)` sets `breaking = true` in Mindustry. Normalizing to `build.tileX(), build.tileY()` guarantees that tapping a non-center tile of a 2×2+ wall or drill still creates a valid break plan at the building's origin and avoids duplicate plans.

## Risks / Trade-offs

- **[Risk] Junction shared by two crossing conveyor belts gets deleted when `traverseBridges` is ON** → Mitigation: `expandNeighbors` already traverses all 4 neighbors of a `Junction` when `traverseBridges` is ON; players who only want to delete a single belt segment without crossing junctions/bridges can disable `Traverse Bridges & Junctions` or enable `Only Replace Exact Block Type`.
- **[Risk] Headless unit testing of `deleteChain`** → Mitigation: Extract `shouldDeleteBlock(Block startBlock, BlockGroup group, Block currentBlock)` as a package-private or public method (and test `deleteChain` with a mock/initialized unit and world tiles) so deletion eligibility rules for same-type, multi-size, and traversed connectors are directly verifiable in `SmartUpgradeFeatureTest`.
