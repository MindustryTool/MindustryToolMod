## Context

Mindustry RTS army commanding operates via `Vars.control.input.selectedUnits` and `Vars.control.input.commandMode`. When units are selected and command mode is active, right-clicking on PC or tapping on mobile issues movement, attack, or hold commands to the selected group.

However, selecting units requires dragging selection rectangles manually or tapping units individually. During intense multiplayer or survival games, players need to:
1. Instantly grab all combat units across the map or on screen.
2. Form persistent army groups (Group 1: Zeniths, Group 2: Fortresses).
3. Exclude unwanted unit types (e.g. Mono, Poly) with a single tap.
4. Keep the control interface open while issuing move/attack orders without the camera abruptly moving.

## Goals / Non-Goals

**Goals:**
- Provide a responsive Solim UI (popup anchored to QuickAccess button with option for persistent HUD display mode).
- Provide one-click actions: "Select All (Map)", "Select Screen", "Deselect All".
- Support creation, storage, and recall of control groups (with configurable capacity, default 5-9 groups).
- Provide an Active Selection Bar showing unit type chips with counts.
- Allow excluding/deselecting an entire unit type with a single tap on its chip.
- Automatically activate vanilla RTS Command Mode upon unit selection/recall.
- Prune destroyed/despawned units automatically on group recall.

**Non-Goals:**
- Overhauling Mindustry's vanilla Command AI pathfinding or unit order netcode.
- Re-centering/panning the camera when selecting or recalling groups (decided against by user preference).
- Automated AI micro-management or combat tactics (this is pure player army control).

## Decisions

### 1. Feature Architecture
- Create `ArmyControlFeature` extending `BaseFeature`, managed by `FeatureManager`.
- Expose state via Solim signals:
  - `activeSelection`: List/map of `UnitType` to count for the current selection.
  - `controlGroups`: Observable list of `ControlGroup` objects containing group ID, name/icon, unit ID list (`IntSeq`), and summary counts.
  - `displayMode`: Configurable setting between `POPUP` (default) and `HUD`.

### 2. Selection Mechanics
- **Select All (Map)**: Iterates `Groups.unit` for all units matching `u.team == Vars.player.team() && u != Vars.player.unit()`. Copies them to `input.selectedUnits` and sets `input.commandMode = true`.
- **Select Screen**: Same as Map, but filtered with `Core.camera.bounds.contains(u.x, u.y)`.
- **Deselect Unit Type**: Filters `input.selectedUnits.removeAll(u -> u.type == targetType)`.
- **Recall Group**: Iterates saved unit IDs, queries `Groups.unit.getByID(id)`, prunes invalid/dead units, populates `input.selectedUnits`, and enables command mode.

### 3. Solim UI & WebStyles
- Use `SettingScaffold`, `SettingCard`, and `SettingSlider` for the Settings Dialog.
- Popup UI built using Solim's `Popup` / `QuickAccessPopupHelper.show()`.
- Interactive chips built using Solim's `wrap()` and `button().style(WebStyles.filterChip())`.
- Direct tap on chip deselects that unit type.

## Risks / Trade-offs

- **[Risk] High unit count performance**: If a map has thousands of units, scanning `Groups.unit` every frame could stutter.
  - *Mitigation*: Perform unit scans only on user action (button click / hotkey), not in the continuous update loop. Active selection summary is updated reactively on change or periodic lightweight poll (every 10-15 ticks).
- **[Risk] Multiplayer desync of unit selection**:
  - *Mitigation*: Unit commanding in Mindustry client-side delegates to vanilla `Call.commandUnits` when orders are issued. Modifying `input.selectedUnits` locally is the standard vanilla mechanism used by DesktopInput box select.
- **[Risk] Popup closing prematurely**:
  - *Mitigation*: Keep popup open on actions; only dismiss when user clicks outside or clicks the close button.
