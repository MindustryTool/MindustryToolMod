## Context

The Autoplay Mining Task allows users to configure a filter of target ores (`selectedItems` in `MiningTask`). In the current implementation (`MiningTask.buildSettings`), every unlocked item in `Vars.content.items()` is rendered as a toggle chip without inspecting whether that item is present on the map or whether the player's unit is capable of mining it.

## Goals / Non-Goals

**Goals:**
- Present only items that exist on the current map (`Vars.indexer.hasOre(item) || Vars.indexer.hasWallOre(item)`).
- Disable chips for items that the current player unit (`Vars.player.unit()`) cannot mine (insufficient mining tier or missing floor/wall mining capability).
- Display descriptive tooltips on disabled chips explaining the exact reason.
- Fall back gracefully to displaying all unlocked items when outside a match (`!Vars.state.isGame()`).
- Preserve the persisted selection state regardless of current unit capabilities.

**Non-Goals:**
- Automatically clearing or altering configured target items when a player switches units.
- Restricting unit mining logic during gameplay beyond what the AI already checks.
- Adding complex pathfinding or reachability checks; relying on standard Mindustry map indexing (`Vars.indexer`).

## Decisions

### 1. Determining Ore Availability on Map
- In an active match (`Vars.state.isGame() && Vars.indexer != null`), check:
  `boolean hasFloor = Vars.indexer.hasOre(item);`
  `boolean hasWall = Vars.indexer.hasWallOre(item);`
  `boolean onMap = hasFloor || hasWall;`
- If not `onMap`, the item is omitted from the settings chips.
- If outside an active match, all items where `item.unlockedNow()` are shown.

### 2. Determining Unit Mining Capability & Reason
- If outside match or player unit is null: item is enabled.
- Inside match with `Unit unit = Vars.player.unit()`:
  - If `!unit.canMine()`: unit cannot mine at all (disabled: `feature.autoplay.settings.mining.unit-cannot-mine`).
  - If `unit.type.mineTier < item.hardness`: unit tier too low (disabled: `feature.autoplay.settings.mining.tier-too-low`, formatted with required hardness and unit tier).
  - If `hasWall && !hasFloor && !unit.type.mineWalls`: item only on walls, but unit cannot mine walls (disabled: `feature.autoplay.settings.mining.no-wall-mining`).
  - If `hasFloor && !hasWall && !unit.type.mineFloor`: item only on floor, but unit cannot mine floors (disabled: `feature.autoplay.settings.mining.no-floor-mining`).
  - Otherwise, enabled.

### 3. Chip Styling & Behavior
- Disabled chips use `.enabled(false)`.
- Label color reflects both checked state and enabled state (`c -> Boolean.TRUE.equals(c) ? (canMine ? Color.white : Color.lightGray) : Color.gray`).
- When disabled, attach a tooltip with the translated reason.

## Risks / Trade-offs

- **[Risk] Unit switches mid-settings view** → Player respawns or changes units while the settings dialog is open.
  - *Mitigation*: The settings dialog is typically opened while paused or for quick changes, but we can compute unit capabilities reactively or poll/refresh on rebuild.
- **[Risk] Custom modded blocks or non-standard indexer state** → `Vars.indexer` throwing if not initialized.
  - *Mitigation*: Guard with `Vars.state.isGame() && Vars.indexer != null` check.
