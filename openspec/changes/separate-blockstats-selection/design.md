## Context

Block Stats Display renders countdown timers and efficiency stats in world-space over active buildings. Currently, both progress and efficiency share a single block toggle and only cover `GenericCrafter` and unit factories (`UnitFactory`, `Reconstructor`, `UnitAssembler`).
The user requested:
1. Adding more block types for stats display.
2. Two separate sections for block selection: one for selecting which blocks display progress, and one for selecting which blocks display efficiency.
3. Supporting discrete production cycle blocks for Progress countdown, and production, drills, pumps, and power generators for Efficiency.

## Goals / Non-Goals

**Goals:**
- Provide two independent `BitSet` instances (`progressBlockEnabled`, `efficiencyBlockEnabled`) for instant $O(1)$ lookups in the draw loop.
- Support progress time remaining on discrete cycle blocks: `GenericCrafter`, `Separator`, `UnitFactory`, `Reconstructor`, `UnitAssembler`, `Constructor` / `BlockProducer`, and `PayloadDeconstructor`.
- Support efficiency percentage display on all the above plus `Drill`, `BurstDrill`, `BeamDrill`, `Pump`, `SolidPump`, and `PowerGenerator` (and subclasses).
- Adaptively render single-line or stacked dual-line text depending on which stats are active for the building.
- Build two distinct vertical sections in `BlockStatsDisplaySettingsView` with category chips and block grids.
- Zero-allocation rendering loop with cached percentage strings.

**Non-Goals:**
- Adding countdown timers to continuous production buildings (pumps, power generators) where no discrete end-of-cycle exists.
- Adding complex multi-item production breakdown charts.

## Decisions

### 1. Two Independent BitSets and Config Keys
- **Decision**: Store progress block settings as `mindustrytool.features.blockstats-display.progress.block.<blockName>` and efficiency block settings as `mindustrytool.features.blockstats-display.efficiency.block.<blockName>`.
- **Rationale**: Clean namespace separation prevents collision. Maintains $O(1)$ BitSet lookups without GC allocations in `draw()`.

### 2. Building Evaluation Logic
- In `drawStats(Building b)`:
  - Check `progressBlockEnabled.get(b.block.id)` and `efficiencyBlockEnabled.get(b.block.id)`. If neither is true, return early.
  - Compute `remainingSec` if progress is enabled and building is an eligible discrete block.
  - Compute `effPercent` if efficiency is enabled and building has non-zero efficiency.
  - Layout:
    - If both `remainingSec >= 0` and `effPercent >= 0`: render countdown at `(b.x, b.y + 3.5f * scale)` and efficiency at `(b.x, b.y - 3.5f * scale)`.
    - If only `remainingSec >= 0`: render countdown at `(b.x, b.y)`.
    - If only `effPercent >= 0`: render efficiency at `(b.x, b.y)`.

### 3. UI Structure in Settings View
- Two consecutive cards / sections:
  1. `Progress Display Blocks`: Category chips (All, Crafters, Units, Payload), "Enable All", "Disable All", reactive block grid.
  2. `Efficiency Display Blocks`: Category chips (All, Crafters, Units, Drills, Pumps, Power), "Enable All", "Disable All", reactive block grid.

## Risks / Trade-offs

- **[Risk]** Large number of blocks in settings view could cause UI sluggishness if rendered naively.
  - **Mitigation**: Use category filter chips and keyed `reactiveGrid` with Solim's declarative components.
- **[Risk]** Missing or unexpected null recipes in `Constructor` or `PayloadDeconstructor`.
  - **Mitigation**: Perform strict null and state validation (`recipe() != null`, `deconstructing != null`) before computing countdown seconds.
