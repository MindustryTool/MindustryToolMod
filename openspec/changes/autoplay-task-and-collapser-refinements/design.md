## Context

Autoplay recently introduced 8 autonomous gameplay tasks and a Solim settings dialog. In playtesting, three issues were identified:
1. Builder units without repair capabilities (such as Alpha, Beta, Gamma, Mono) get stuck attempting to heal damaged blocks because `unit.canBuild()` was counted as a repair capability, while in Mindustry build beams only build `BuildPlan`s and cannot repair block health.
2. The settings dialog layout crowded the task enable toggle, reorder buttons, and status/reason strings into a single horizontal row, causing overlapping text when reason strings were long.
3. Collapsible task settings toggled via `.visible(expanded)` left large empty spaces in Arc table cells even when hidden.

## Goals / Non-Goals

**Goals:**
- Fix `RepairTask` so units without heal weapons (`w.bullet.heals()`, `RepairBeamWeapon`) or repair field abilities (`RepairFieldAbility`) yield immediately.
- Prevent non-healing units possessing only `RepairFieldAbility` from shooting hostile/regular bullets at allied buildings while healing passively.
- Create `SolimCollapser` in `solim-core` (wrapping Arc `Collapser`) with reactive expansion/collapse and 0-height collapse.
- Expose `collapser(...)` in `solim.UI`.
- Redesign `TaskRow` in `AutoplaySettingsView` as a styled `Card` with a 2-row layout (controls on top, reason text below) and `collapser()` for settings.

**Non-Goals:**
- Alter other autonomous tasks (`SelfHealTask`, `FleeTask`, `MiningTask`, etc.) whose trigger logic is functioning correctly.
- Implement complex custom animation interpolation beyond Arc `Collapser`'s built-in transition system.

## Decisions

### 1. Separation of Build Capabilities from Repair Capabilities
- **Decision**: Remove `canBuild()` checks from `RepairTask`. Only treat `hasHealWeapon` and `hasRepairField` as repair capabilities.
- **Rationale**: Mindustry's build beam cannot repair damage to existing tiles; it only places or reconstructs blocks. Units that cannot heal must yield so lower-priority tasks (e.g. mining or building) can execute.
- **Alternative considered**: Forcing player to build a Mend Projector instead. Rejected because that belongs to construction AI, not unit repair behavior.

### 2. Aura-Only Healing Handling in `RepairAI`
- **Decision**: If a unit only has `RepairFieldAbility` and no heal weapons, navigate within aura range (~35-40f) and suppress weapon controls (`unit.controlWeapons(false, false)`).
- **Rationale**: Prevents units from shooting regular bullets into friendly blocks or wasting energy when they only possess a passive healing pulse.

### 3. SolimCollapser Container Wrapping Arc `Collapser`
- **Decision**: Implement `SolimCollapser` in `solim.layout` wrapping `arc.scene.ui.layout.Collapser(Table content, boolean collapsed)`.
- **Rationale**: Arc `Collapser` dynamically animates `currentHeight`, clips rendering during transition, sets touchable to disabled when collapsed, and returns `prefHeight = 0` when collapsed. This completely solves empty cell whitespace without manual table clearing.
- **API**:
  - `collapser().expanded(Readable<Boolean>).duration(0.2f).children(Runnable)`
  - `solim.UI.collapser(Readable<Boolean> expanded, Runnable r)`

### 4. TaskRow Multi-Row Card Layout
- **Decision**: Wrap each `TaskRow` in a `card()` with `WebStyles.Colors.SECTION_BORDER` (1px) and `WebStyles.Colors.SECTION_BG`. Arrange contents into a header row (reorder up/down, toggle chip, spacer, settings expander) and a second row for the status text, followed by `collapser` for settings.
- **Rationale**: Guarantees the status/skip reason has full card width to display text without colliding with the toggle button.

## Risks / Trade-offs

- **[Risk]** `Collapser` action execution requires `act(delta)` on the scene graph.
  - *Mitigation*: Arc scene loop automatically invokes `act` on all UI elements in normal runtime. For headless unit tests, `Collapser.setCollapsed(val, false)` or manually calling `act(delta)` ensures instantaneous layout updates.
