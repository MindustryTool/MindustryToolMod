## Context

`RangeDisplayFeature` renders circles and squares indicating ranges for player units, ally/enemy turrets, support blocks, and enemy drop spawners. Currently, support blocks and turrets are rendered with the team color of their building (`build.team.color`), except that inactive turrets without ammo are rendered in `Color.gray`.
In intense gameplay, distinguishing overdrive coverage, repair field coverage, and point defense interception zones is difficult because all friendly structures display identical colors.

## Goals / Non-Goals

**Goals:**
- Provide distinct semantic colors for key tactical blocks:
  - Overdrives (`OverdriveProjector`) -> Orange (`Pal.accent`).
  - Menders and healing structures (`MendProjector`, `RegenProjector`, `RepairTower`, `RepairTurret`) -> Green (`Pal.heal`).
  - Point-defense turrets (`PointDefenseTurret`, e.g. Segment) and `ShockwaveTower` -> White (`Color.white`).
- Allow toggling display of Overdrives, Menders, and Point Defense separately, enabled by default.
- Provide a setting (`use-team-color-for-special`, default: `false`) to preserve original team coloring if preferred.
- Preserve out-of-ammo / inoperable dimming (`Color.gray`) when turrets cannot fire.
- Maintain zero GC overhead in the 60 FPS draw loop by caching configuration flags per-frame.

**Non-Goals:**
- Custom hex color pickers for every individual block (preset semantic colors are used for clarity and simplicity).
- Altering the drawing geometry, dashed styling, or layer ordering of ranges.

## Decisions

### Decision 1: Pre-computed Category Checks vs Reflection
- **Approach**: Categorize blocks using fast `instanceof` checks (`OverdriveProjector`, `MendProjector`, `RegenProjector`, `RepairTower`, `RepairTurret`, `PointDefenseTurret`, `ShockwaveTower`).
- **Rationale**: In Mindustry, all vanilla and standard modded variations extend these base classes. `instanceof` checks are compiled to efficient bytecode and run without heap allocations during iteration.

### Decision 2: Cached Primitives for Draw Loop
- **Approach**: Store `cachedDrawOverdriveRange`, `cachedDrawMenderRange`, `cachedDrawPointDefenseRange`, and `cachedUseTeamColorForSpecial` as primitive booleans in `RangeDisplayFeature`, updated via signal subscriptions and captured into frame-local fields before iterating over buildings.
- **Rationale**: Aligns with existing `RangeDisplayFeature` performance guidelines (zero GC and no signal resolution inside the draw loop).

### Decision 3: Color Determination Hierarchy
- When evaluating building color in `drawBuildingRange`:
  1. If `isTurret` and `!canShoot`, use `Color.gray`.
  2. If `frameUseTeamColorForSpecial` is true, use `build.team.color`.
  3. Otherwise, check specialized block categories:
     - Overdrives -> `Pal.accent`
     - Menders/healers -> `Pal.heal`
     - Point defense / Shockwave -> `Color.white`
  4. Fallback -> `build.team.color`.

## Risks / Trade-offs

- **[Risk]** Modded point defense or shockwave towers might extend custom classes.
  - *Mitigation*: Checking `PointDefenseTurret` and `ShockwaveTower` covers almost all content; modded turrets extending `PointDefenseTurret` inherit the white range indicator seamlessly.
- **[Risk]** Confusion between hostile and friendly overdrives/menders when custom colors are active.
  - *Mitigation*: The `use-team-color-for-special` toggle lets players immediately revert to team colors if desired.
