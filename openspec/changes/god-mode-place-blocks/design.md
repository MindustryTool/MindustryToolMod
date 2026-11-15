## Context

God Mode currently features a "Place Core" dialog (`GodModeCoreDialog`) accessed via the hammer button on the God Mode HUD. This dialog only queries `Vars.content.blocks().select(b -> b instanceof CoreBlock)`, always places with fixed rotation 0, and validates that every tile in the block footprint is not solid. In sandbox play, players frequently need to place non-core structures (turrets, power generators, conveyor lines, logic processors, item sources, etc.) at arbitrary locations with specific rotations.

## Goals / Non-Goals

**Goals:**
- Replace `GodModeCoreDialog` with a full-featured `GodModeBlockDialog`.
- Support all synthetic buildable blocks (`block.synthetic() && !block.isAir()`), including sandbox/hidden blocks (e.g., `Item Source`, `Liquid Source`, `World Processor`).
- Organize blocks with responsive Category filter chips (`All`, `Turret`, `Production`, `Distribution`, `Liquid`, `Power`, `Defense`, `Crafting`, `Units`, `Effect`, `Logic`) and real-time search filtering.
- Provide 4-way orientation selection ($0^\circ, 90^\circ, 180^\circ, 270^\circ$) dynamically when `selectedBlock.rotate` is true.
- Generalize `GodModeProvider` with `placeBlock(Block, Team, float, float, int)` and implement force overwrite placement across `InternalGodModeProvider` and `JSGodModeProvider`.
- Update HUD icon tooltip and provide complete i18n bundle keys with descriptive comments.

**Non-Goals:**
- Natural environment editing (ores, floor tiles, deep water, boulders) - terrain placement belongs to map editors or dedicated terrain brushes.
- Continuous multi-block brush painting (the established modal position-picker workflow is preserved).
- Schematic-based block group placement.

## Decisions

### 1. Block Filter Predicate & Caching
- **Decision**: Filter available blocks on dialog creation using:
  ```java
  Vars.content.blocks().select(b -> b.synthetic() && !b.isAir());
  ```
- **Rationale**: Captures all real buildings and sandbox blocks while cleanly omitting terrain, floors, and air.
- **Alternatives Considered**: Checking `b.buildVisibility != BuildVisibility.hidden` would exclude valuable sandbox tools (`Item Source`, `Liquid Source`) that are critical in god mode.

### 2. Category System & Reactivity
- **Decision**: Use `mindustry.type.Category` enum values for filter chips.
  - Active category maintained via `Signal<Category>` (nullable, where `null` means "All").
  - Search query maintained via `Signal<String>`.
  - Filtered blocks computed via `Computed<Seq<Block>>` combining category matching (`cat == null || b.category == cat`) and localized name query matching.
- **Rationale**: Leverages Solim's fine-grained reactive updates so only the block icon grid updates when category or search query changes.

### 3. Dynamic Rotation Control
- **Decision**: Track rotation in `Signal<Integer> rotation` (values 0, 1, 2, 3). Show rotation chip row conditionally via `when(selectedBlock.map(b -> b != null && b.rotate))`.
- **Rationale**: Keeps the UI compact and clean for non-rotatable blocks (like cores and walls) while exposing clear direction controls for rotatable blocks.

### 4. Overwrite Tile Logic in Providers
- **Decision**:
  - `InternalGodModeProvider`:
    ```java
    Tile tile = Vars.world.tileWorld(x, y);
    if (tile == null) return false;
    tile.setNet(block, team, rotation);
    return true;
    ```
  - `JSGodModeProvider`:
    ```java
    js("var t = Vars.world.tileWorld(@, @); if(t != null) t.setNet(Vars.content.block(@), Team.get(@), @);",
       x, y, block.id, team.id, rotation);
    ```
- **Rationale**: Mindustry's `tile.setNet` natively handles clearing previous building structures and rebuilding with the new block footprint. Solid checks are eliminated to allow god-mode force placement.

### 5. Backward Compatibility & Clean Deprecation
- **Decision**: Keep `default boolean placeCore(Block coreBlock, Team team, float x, float y)` in `GodModeProvider` delegating to `placeBlock(coreBlock, team, x, y, 0)`.
- **Rationale**: Prevents breaking any external or test callers while standardizing on `placeBlock`.

## Risks / Trade-offs

- **[Large block count]** Rendering 150+ block buttons $\to$ Solim scroll container with fixed bounds and keyed filter chips keeps rendering fast; category tabs and search narrow down items immediately.
- **[Out of bounds tap]** User taps off-map or tile is null $\to$ `tileWorld` returns null and provider returns `false`, surfacing the existing toast notification to inform the user.
