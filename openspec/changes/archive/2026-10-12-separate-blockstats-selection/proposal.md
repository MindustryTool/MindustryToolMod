## Why

Currently, Block Stats Display configures a single per-block toggle that gates both progress countdown and efficiency rating together. Furthermore, only basic crafters and unit factories are supported, leaving out other discrete production blocks (such as separators and payload constructors/deconstructors) as well as continuous production, extraction, and power-generating blocks (drills, pumps, power generators). Players often want to monitor efficiency on drills, pumps, and reactors without countdowns, or want progress countdowns on factories without efficiency clutter, or both.

Decoupling progress display selection from efficiency display selection and expanding supported block types provides complete control over in-game information density.

## What Changes

- Decouple block toggling into two independent sets:
  - **Progress Display Blocks**: Discrete production and crafting blocks (crafters, separators, unit factories, reconstructors, unit assemblers, constructors, payload deconstructors).
  - **Efficiency Display Blocks**: Discrete crafters + resource extraction (drills, pumps) + power generators (solar, thermal, nuclear, impact, generator).
- Introduce separate BitSets (`progressBlockEnabled` and `efficiencyBlockEnabled`) with distinct configuration keys:
  - `mindustrytool.features.blockstats-display.progress.block.<blockName>`
  - `mindustrytool.features.blockstats-display.efficiency.block.<blockName>`
- Expand building evaluation logic:
  - Support discrete progress calculation for `Separator`, `BlockProducer` / `Constructor`, and `PayloadDeconstructor` alongside existing factories and crafters.
  - Support efficiency evaluation for `Drill`, `BurstDrill`, `BeamDrill`, `Pump`, `SolidPump`, and all `PowerGenerator` subclasses (using building efficiency or generator production efficiency).
- Adapt in-game world rendering dynamically per block:
  - Both enabled: Stacked lines (progress countdown above, efficiency % below).
  - Only progress enabled: Single line at building center.
  - Only efficiency enabled: Single line at building center.
  - Neither enabled or idle: Nothing drawn.
- Update Settings Dialog UI:
  - Render two distinct vertical sections: "Progress Display Blocks" and "Efficiency Display Blocks", each with category filters/chips and block toggle grids.

## Capabilities

### Modified Capabilities
- `progress-display`: Separate progress block selection from efficiency block selection, add additional block types for progress (separators, constructors, payload deconstructors) and efficiency (drills, pumps, power generators), and render adaptive 1-line or 2-line layouts.

## Impact

- Mod source files:
  - `BlockStatsDisplayFeature.java`: New BitSets, updated building evaluation logic, dynamic stacked/single-line drawing logic, separate config keys.
  - `BlockStatsDisplaySettingsView.java`: Two consecutive vertical sections for Progress and Efficiency selection, separate category groups.
  - `bundle.properties`: New translation keys for Progress section, Efficiency section, and new block category headers.
