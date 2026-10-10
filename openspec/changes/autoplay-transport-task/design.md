## Context

`AutoplayFeature` orchestrates priority-ordered autonomous tasks (`AutoplayTask`) for the player's unit (`Vars.player.unit()`). Existing tasks cover survival (`SelfHealTask`, `FleeTask`), combat (`AttackTask`), support (`RepairTask`, `HealUnitsTask`, `FollowAssistTask`), construction (`SelfBuildTask`, `RebuildTask`), and resource extraction (`MiningTask`).

Players often need to ferry items between storage buildings (Cores, Vaults, Containers, Drills, Crafters) and consumer buildings (Turrets, Unit Factories, Reconstructors, Crafters, Remote Vaults) without building long conveyor lines. This requires:
1. A new `TransportTask` integrated into `AutoplayFeature`.
2. In-game controls to select **Target** (source) and **Destination** (sink) buildings.
3. World-anchored floating Solim UI over selected buildings to configure which items to **transfer** (from targets) and **receive** (at destinations), plus a **remove** button, while hiding when zoomed out (`Vars.renderer.getScale() < 0.5f`).
4. Single-slot map-keyed persistence so reloading a match restores the setup without accumulating unbounded per-map entries in `Core.settings`.

## Goals / Non-Goals

**Goals:**
- Provide `TransportTask` (`id = "transport"`) placed above `MiningTask` in `AutoplayFeature.DEFAULT_ORDER`.
- Expose **Select Targets**, **Select Destinations**, and **Clear All** buttons in both the Autoplay HUD/Popup (when `TransportTask` is enabled) and `TransportTask.buildSettings(...)`.
- Support continuous building selection via `TapEvent` until toggled off or cancelled via `Escape` / `Back`.
- Render declarative Solim floating badges projected to screen coordinates (`Core.camera.project(build.x, build.y)`) over selected buildings, hidden when `Vars.renderer.getScale() < 0.5f`.
- Filter item options per building based on valid output/input items and apply smart defaults (auto-select valid items for specialized blocks like turrets/crafters/drills/generators; start empty with picker expanded for general storage like Cores/Vaults/Containers/Unloaders).
- Coordinate rate-limited `Call.requestItem` and `Call.transferInventory` inside `TransportAI`, prioritizing destinations with the lowest stock of the needed item (ties broken by closest distance).
- Persist only a single map's transport configuration at a time in `Core.settings` keyed by a composite `mapKey`.

**Non-Goals:**
- Controlling non-player AI units or logic processors (`ucontrol`).
- Multi-map historical storage of transport configurations across different maps.
- Liquid or power transport (Mindustry units only carry `ItemStack`).

## Decisions

### 1. Data Model & Single-Slot Map-Keyed Persistence
- **Representation**: Each selected building coordinate (`int tileX, int tileY`, packed via `Point2.pack(x, y)`) maps to a reactive `TransportNode` holding:
  - `int pos` (`Point2.pack(tileX, tileY)`)
  - `Signal<Boolean> isTarget`
  - `Signal<Boolean> isDestination`
  - `Signal<Seq<String>> transferItems` (selected item names to take when acting as a target)
  - `Signal<Seq<String>> receiveItems` (selected item names to deposit when acting as a destination)
  - `Signal<Boolean> expanded` (whether the floating item picker is expanded)
- **Map Identification Key (`mapKey`)**:
  - Computed from the active world state: `mapName + ":" + Vars.world.width() + "x" + Vars.world.height() + ":" + sectorId` (where `sectorId` is `Vars.state.rules.sector != null ? Vars.state.rules.sector.id : -1`).
- **Single-Slot Storage**:
  - Stored in a single `ConfigValue<String>` (`transport.saved-state`) under `AutoplayFeature`'s `ConfigGroup`.
  - Format: `mapKey` on the first line (or header segment) followed by semicolon-delimited node records `x,y,targetFlag,destFlag,transferItem1|transferItem2,receiveItem1|receiveItem2`.
  - On `WorldLoadEvent`: if the active `mapKey` matches the stored `mapKey`, deserialize the nodes into memory. If it does not match, clear the in-memory nodes (and only overwrite the persisted `ConfigValue` when the user adds or modifies nodes on the new map, or clears them). This guarantees at most one map's data is ever stored on disk.

### 2. Smart Item Filtering & Default Selection per Block
- **Valid Receive Items (Destination)**:
  - Inspect `block.consumes` (`ConsumeItems`, `ConsumeItemFilter`, `ConsumeItemDynamic`, turret `ItemTurret.ammoTypes`, `UnitFactory.plans`, `Reconstructor.upgrades`).
  - Alternatively or additionally, probe `build.acceptStack(item, 1, unit)` or check `block.itemCapacity > 0` for storage blocks (`CoreBlock`, `StorageBlock`, `Unloader`).
  - If a block is general storage (`CoreBlock`, `StorageBlock`, `Unloader`), all unlocked items (`item.unlockedNow()`) are valid choices, and default selection is **empty** (with `expanded = true` on creation so the player immediately picks items).
  - For specialized consumer blocks (Turrets, Crafters, Unit Factories, Reconstructors, Generators), only items accepted/consumed by that block are shown, and all valid items are **auto-selected** by default (`expanded = false`).
- **Valid Transfer Items (Target)**:
  - For `Drill` / `BeamDrill`: `drill.dominantItem` or mined ore drops under the drill.
  - For `GenericCrafter` / `Separator`: `crafter.outputItem` / `outputItems` / `results`.
  - For general storage (`CoreBlock`, `StorageBlock`, `Unloader`) or other inventory blocks: all unlocked items (`item.unlockedNow()`), starting empty with `expanded = true`.

### 3. World-Anchored Floating UI & Zoom Threshold
- **Component Structure**:
  - Managed by a Solim overlay container mounted to `Vars.ui.hudGroup` (with `Touchable.childrenOnly`) that reconciles floating node views via reactive bindings.
  - Each `TransportNodeView` (`BaseComponent`) binds its screen position on `Trigger.update` using `Core.camera.project(worldX, worldY)` centered above the building (`worldY + block.size * Vars.tilesize / 2f`).
- **Visibility Rules**:
  - Hidden when `!feature.isEnabled()`, `!feature.isTaskEnabled(TransportTask.ID)`, `!Vars.state.isGame()`, `!Vars.ui.hudfrag.shown`, or when the camera is zoomed out past `0.5` (`Vars.renderer.getScale() < 0.5f`).
  - Also hidden if no valid allied building with `hasItems` currently exists at `(tileX, tileY)`, or if its projected position is outside the screen viewport bounds.
- **Badge Contents**:
  - Compact row per active role (`Transfer` / `Receive`): role icon, selected item icons (or a prompt label if empty), `[Edit]` toggle button (`Icon.pencil` / `Icon.add`) to expand/collapse the item picker, and `[Remove]` button (`Icon.cancel` / `Icon.trash`) to remove that role (or the entire node).
  - Expanded section: `wrap()` of item filter chips (`WebStyles.filterChip()`) for all valid items of that role.

### 4. Transport AI & Priority Matching
- **Demand-Driven Evaluation (`TransportTask.update(Unit)`)**:
  - Requires `unit.type.itemCapacity > 0`.
  - **Case 1: Unit is carrying an item (`unit.stack.amount > 0`)**:
    - Let `held = unit.stack.item`.
    - Search all valid Destination buildings configured to receive `held` where `dest.acceptStack(held, 1, unit) > 0`.
    - Pick the destination with the **lowest `dest.items.get(held)`** (ties broken by `unit.dst2(dest)`).
    - If found: set AI state to deliver to `dest`.
    - If no destination needs `held`: fall back to `unit.closestCore()` if `core != null && core.acceptStack(held, 1, unit) > 0` to deposit the leftover item, or `Call.clearItems(unit)` / `unit.clearItem()` if the core is also full.
  - **Case 2: Unit inventory is empty (`unit.stack.amount == 0`)**:
    - Evaluate all valid `(Target T, Destination D, Item I)` triplets where:
      - `T` is a valid allied building with `I` enabled in `transferItems` and `T.items != null && T.items.get(I) > 0` and `T != D`.
      - `D` is a valid allied building with `I` enabled in `receiveItems` and `D.acceptStack(I, 1, unit) > 0`.
    - Select the best triplet by **lowest `D.items.get(I)`** (ties broken by `unit.dst2(D) + unit.dst2(T)`).
    - Set AI target to move to `T` and request `Math.min(T.items.get(I), Math.min(unit.type.itemCapacity, D.acceptStack(I, unit.type.itemCapacity, unit)))` via rate-limited `Call.requestItem(Vars.player, T, I, amount)`.
- **Packet Rate Limiting**:
  - Mirrors `MiningTask.MinerAI` with `TRANSFER_COOLDOWN = 25f` ticks between `Call.requestItem` / `Call.transferInventory` calls to prevent network packet spam on multiplayer servers.

## Risks / Trade-offs

- **[Risk] Screen clutter with many selected buildings** → Mitigated by compact badge mode (collapsing the item picker unless expanded), hiding badges when `Vars.renderer.getScale() < 0.5f`, and culling off-screen badges.
- **[Risk] Multiplayer packet spam during item pickup/drop** → Mitigated by enforcing a 25-tick cooldown timer inside `TransportAI` for both `Call.requestItem` and `Call.transferInventory`.
- **[Risk] Unit inventory deadlock when destination fills up mid-flight** → Mitigated by falling back to depositing leftover carried items into the closest Core (or clearing inventory if Core cannot accept them).
