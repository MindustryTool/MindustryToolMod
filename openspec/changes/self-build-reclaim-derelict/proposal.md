## Why

Currently, the Autoplay `SelfBuildTask` settings panel only provides a **Deconstruct All Derelicts** action button to tear down abandoned `Team.derelict` structures. Players frequently want to reclaim intact derelict buildings on a map for their own team without clicking or dragging over every single structure manually, and without `SelfBuildTask` falsely stalling on core material checks when reclaiming derelict blocks (which costs zero resources in Mindustry).

## What Changes

- Add a **Reclaim All Derelicts** one-shot action button to `SelfBuildTask.buildSettings` stacked vertically in the settings column alongside **Deconstruct All Derelicts**.
- Implement `reclaimDerelicts()` in `SelfBuildTask` to iterate over `Team.derelict` buildings and queue non-breaking `BuildPlan(b.tileX(), b.tileY(), b.rotation, b.block, b.config())` entries for eligible derelict structures (`Vars.state.rules.derelictRepair`, `b.block.allowDerelictRepair`, and `Build.validPlaceIgnoreUnits`).
- Deduplicate and replace any existing `BuildPlan` at `(b.tileX(), b.tileY())` when queuing either reclaim or deconstruct derelict plans so repeated clicks or switching between Reclaim and Deconstruct updates the plans cleanly without duplicates.
- Update `SelfBuildTask.canAffordPlan` to treat derelict repair plans (`plan.isDerelictRepair()`) and rotation plans (`plan.isRotation(unit.team)`) as free (`true`), matching Mindustry's `BuilderComp.shouldSkip` behavior so `SelfBuildTask` executes derelict reclaim plans even when the core lacks materials to construct the block from scratch.
- Add localization keys (`feature.autoplay.settings.self-build.reclaim-derelict` and `feature.autoplay.settings.self-build.reclaim-derelict.tooltip`) with descriptive translator comments in `assets/bundles/bundle.properties`.

## Capabilities

### New Capabilities

### Modified Capabilities
- `autonomous-gameplay-ai`: Extend Task 6 (`SelfBuildTask`) requirements with one-shot derelict reclaiming, plan deduplication at identical tile coordinates, and zero-cost affordability evaluation for derelict repair plans.

## Impact

- `mod/src/mindustrytool/features/autoplay/tasks/SelfBuildTask.java`: Add the Reclaim All Derelicts button, `reclaimDerelicts()` handler, coordinate-based plan replacement helper, and `plan.isDerelictRepair()` check in `canAffordPlan`.
- `assets/bundles/bundle.properties`: Add translation keys and comments for the new button label and tooltip.
