## Context

Players in Mindustry frequently rotate directional blocks (unit factories, conveyors, sorters, reconstructors, etc.). Currently, when a unit factory is blocked because its output faces an obstacle, the player must switch to build mode, re-select the building, align the rotation, and place a replacement plan or deconstruct. This change introduces a quick-rotate in-world overlay that enables players to rotate targeted blocks directly with two buttons (clockwise and counter-clockwise).

The mod uses the Solim UI framework (`:solim`, `:solim-core`) with reactive signals, and adheres to Java 8 runtime compatibility and strict i18n rules (`assets/bundles/bundle.properties`).

## Goals / Non-Goals

**Goals:**
- Provide a clean in-world floating overlay adjacent to the selected/clicked rotatable building.
- Offer two step-rotate actions: Rotate CCW (`↺`) and Rotate CW (`↻`).
- Use `Call.rotateBlock(player, building, direction)` so rotation works seamlessly in multiplayer and respects permissions.
- Support all player-team rotatable blocks (`block.rotate == true`).
- Provide an automatic overlay lifecycle: shows on click/select, updates position relative to camera coordinates, and hides when deselected or clicked away.
- Provide setting toggles and translatable bundle keys.

**Non-Goals:**
- Free-form angle rotation (Mindustry blocks only support 4 orthogonal directions: 0, 1, 2, 3).
- Modifying enemy or non-player team blocks.
- Bypassing game build/interaction permissions.

## Decisions

### 1. Overlay Implementation via Solim
- **Decision**: Render the overlay using a Solim component attached to the HUD or scene, updating its screen position from the target building's world coordinates (`Core.camera.project(...)`).
- **Rationale**: Keeps UI declarative and consistent with the project's Solim architecture guidelines. Avoids raw imperative scene hacks while utilizing Solim's reactive binding capabilities.
- **Alternatives considered**:
  - *Drawing directly via Events.run(Trigger.draw)*: Only good for rendering textures, difficult to handle interactive button clicks, hover states, and tooltips.
  - *Injecting into vanilla block config menus*: Many blocks (like plain conveyors) do not have full config menus or require extra steps to open.

### 2. Networking and Mutation via `Call.rotateBlock`
- **Decision**: Invoke `Call.rotateBlock(Vars.player, building, direction)` where `direction` is boolean (`false` for CCW / -1, `true` for CW / +1).
- **Rationale**: `Call.rotateBlock` is the canonical Mindustry client-to-server RPC for block rotation. In singleplayer, it executes locally; in multiplayer, it validates permissions and broadcasts to all clients.
- **Alternatives considered**:
  - Direct assignment `building.rotation = ...`: Causes multiplayer desynchronization.
  - Queueing build plans: Slower and requires builder unit interaction.

### 3. Selection Lifecycle and Target Tracking
- **Decision**: Maintain a `Signal<Building>` in `QuickRotateFeature` that holds the currently focused building.
- **Trigger**: Listen for tap/click events or hook into player tap detection (`EventType.TapEvent` or input listener). Verify that:
  1. `building != null`
  2. `building.team == Vars.player.team()`
  3. `building.block.rotate == true`
- **Dismissal**: Reset the signal when clicking on empty space, clicking a non-rotatable block, or when the player deselects.

### 4. Visual Layout and Styling
- **Decision**: A compact Solim `row()` container using `WebStyles.ghost()` or `WebStyles.secondary()` styled buttons with icons (`Icon.rotate` / `Icon.redo` / `Icon.undo`), sized with standard `unit(11)` or a compact `unit(8)` floating profile.

## Risks / Trade-offs

- **[Risk] Screen projection jitter during camera movement** → **Mitigation**: Update screen coordinates during each frame update or hook onto camera updates.
- **[Risk] Accidental clicks when shooting or commanding units** → **Mitigation**: Only display when building is explicitly selected/tapped; buttons consume touch events so underlying world clicks are not triggered when pressing the rotate buttons.
- **[Risk] Blocks that disallow player rotation on certain servers** → **Mitigation**: Check `Vars.net.client()` permissions / `building.interactable(player.team())` before showing or firing rotations.
