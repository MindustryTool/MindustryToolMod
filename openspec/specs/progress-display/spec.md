# blockstats-display Specification

## Purpose

World-space text overlay showing remaining production countdown time and average efficiency percentage above active unit-production and crafter buildings, with per-block opt-out toggles and persisted global appearance settings.

## Requirements

### Requirement: Feature activates and registers draw hook
The feature SHALL identify itself as `blockstats-display` and set `enabledByDefault(true)`. It SHALL register a `Trigger.draw` listener in its constructor that runs `draw()` every frame.

#### Scenario: Feature is enabled during gameplay
- **WHEN** the feature is enabled and `Vars.state.isGame()` is true and the HUD is shown
- **THEN** `draw()` executes each frame and may render countdown and efficiency stats text above qualifying buildings

#### Scenario: Feature is disabled
- **WHEN** `isEnabled()` returns false
- **THEN** `draw()` returns immediately without rendering anything

---

### Requirement: Zoom threshold hides overlay when zoomed out
The feature SHALL expose a `zoomThresholdConfig` (`ConfigValue<Float>`, default `0.5`, range `0..2`). When the renderer scale is below this threshold and the threshold is greater than `0.01f`, `draw()` SHALL skip all rendering.

#### Scenario: Zoomed out past threshold
- **WHEN** `Vars.renderer.getScale() < zoomThresholdConfig` and threshold > 0.01f
- **THEN** no countdown text is drawn that frame

#### Scenario: Threshold set to 0 (off)
- **WHEN** `zoomThresholdConfig <= 0.01f`
- **THEN** countdown text is drawn regardless of zoom level

---

### Requirement: Per-block BitSet tracks enabled state
The feature SHALL maintain two independent `BitSet` instances indexed by `block.id`:
- `progressBlockEnabled`: Tracks whether progress countdown is displayed for each block.
- `efficiencyBlockEnabled`: Tracks whether efficiency percentage is displayed for each block.

Each BitSet SHALL be built (or rebuilt) from `Core.settings` whenever a corresponding block toggle changes. Default for any block not explicitly stored in either setting is `true`.

#### Scenario: BitSet lookup in draw loop
- **WHEN** `draw()` processes a building
- **THEN** `progressBlockEnabled.get(build.block.id)` and `efficiencyBlockEnabled.get(build.block.id)` are used to determine which elements to draw — no settings read, no map lookup

#### Scenario: Block toggle persisted and BitSet rebuilt
- **WHEN** a block checkbox is toggled for progress in the settings view
- **THEN** `Core.settings.put(progressBlockSettingKey(block), value)` is called and `rebuildProgressBitSet()` is called immediately

#### Scenario: Efficiency block toggle persisted and BitSet rebuilt
- **WHEN** a block checkbox is toggled for efficiency in the settings view
- **THEN** `Core.settings.put(efficiencyBlockSettingKey(block), value)` is called and `rebuildEfficiencyBitSet()` is called immediately

---

### Requirement: Countdown text drawn at building center
The feature SHALL draw remaining production time as a formatted countdown string (e.g., `"12.4s"`) and/or block efficiency percentage (e.g., `"100%"`) using `Fonts.outline` centered at `(build.x, build.y)`. It SHALL apply `opacityConfig` and `scaleConfig`.
- When both progress and efficiency are active for a building, countdown SHALL be drawn on the top line (`y + 3.5f * scale`) and efficiency on the bottom line (`y - 3.5f * scale`).
- When only progress is active, countdown SHALL be drawn as a single line centered at `y`.
- When only efficiency is active, efficiency SHALL be drawn as a single line centered at `y`.

#### Scenario: Active building with both progress and efficiency enabled
- **WHEN** a supported building has valid progress, its block has both progress and efficiency enabled, and efficiency is non-zero
- **THEN** countdown is drawn on the top line and efficiency percentage on the bottom line at the building's world position

#### Scenario: Active building with only progress enabled
- **WHEN** a supported building has valid progress, progress is enabled for the block, and efficiency is disabled for the block
- **THEN** countdown is drawn vertically centered as a single line at `y`

#### Scenario: Active building with only efficiency enabled
- **WHEN** a supported building is active, efficiency is enabled for the block, and progress is disabled or not applicable for the block
- **THEN** efficiency percentage is drawn vertically centered as a single line at `y`

#### Scenario: Idle or completed building
- **WHEN** progress fraction is less than `0.01f` (or total production time is zero) and building efficiency is zero
- **THEN** no text is drawn for that building

---

### Requirement: Zero-allocation efficiency percentage display
The feature SHALL pre-cache percentage strings from `0%` to `300%` in an array and draw the building's efficiency percentage (`Math.round(build.efficiency * 100f)` clamped to `0..300`) below the countdown number without allocating String objects in the draw loop.

#### Scenario: Normal operating building efficiency
- **WHEN** a building is operating at 100% efficiency
- **THEN** `"100%"` is retrieved from the pre-cached array and drawn below the remaining time

#### Scenario: Overdriven building efficiency
- **WHEN** a building is overdriven to 150% efficiency
- **THEN** `"150%"` is retrieved from the pre-cached array and drawn below the remaining time

#### Scenario: Low or throttled efficiency
- **WHEN** a building is lacking power/resources and running at 30% efficiency
- **THEN** `"30%"` is retrieved from the pre-cached array and drawn with `Color.scarlet` color

---

### Requirement: Efficiency threshold color coding
The efficiency text SHALL be tinted according to efficiency tiers:
- `Color.green` when efficiency is $\ge 95\%$
- `Color.orange` when efficiency is $> 50\%$ and $< 95\%$
- `Color.scarlet` when efficiency is $\le 50\%$

#### Scenario: Tint selection
- **WHEN** efficiency text is drawn for a building
- **THEN** the appropriate tier color is applied with alpha multiplied by `frameOpacity`

---

### Requirement: Supported building categories
The draw predicate SHALL support progress countdown for discrete cycle production buildings:
- `UnitFactory.UnitFactoryBuild` (only when `currentPlan != -1` and plan is valid)
- `Reconstructor.ReconstructorBuild` (only when `constructTime > 0`)
- `UnitAssembler.UnitAssemblerBuild` (only when `plan() != null`)
- `GenericCrafter.GenericCrafterBuild` (only when `craftTime > 0`)
- `Separator.SeparatorBuild` (only when `craftTime > 0`)
- `Constructor.ConstructorBuild` / `BlockProducer.BlockProducerBuild` (only when `recipe() != null && recipe().buildTime > 0`)
- `PayloadDeconstructor.PayloadDeconstructorBuild` (only when `deconstructing != null && deconstructing.buildTime() > 0`)

The draw predicate SHALL support efficiency evaluation for all progress-supported buildings, PLUS:
- `Drill.DrillBuild`
- `BurstDrill.BurstDrillBuild`
- `BeamDrill.BeamDrillBuild`
- `Pump.PumpBuild`
- `SolidPump.SolidPumpBuild`
- `PowerGenerator.GeneratorBuild` and subclasses (using `productionEfficiency` when greater than 0, otherwise `efficiency`)

Buildings outside these types SHALL be ignored.

#### Scenario: Separator is running
- **WHEN** a `SeparatorBuild` has `craftTime > 0` and `progress > 0` and progress is enabled
- **THEN** remaining seconds are calculated as `(1f - b.progress) * block.craftTime / 60f / b.timeScale()` and displayed

#### Scenario: Power generator is operating
- **WHEN** a power generator is operating and efficiency is enabled for its block
- **THEN** its operational efficiency is displayed as a single centered line without countdown

#### Scenario: Drill is operating
- **WHEN** a drill is mining and efficiency is enabled for its block
- **THEN** its mining efficiency percentage is displayed

---

### Requirement: Declarative Solim settings view with per-block toggles
The settings dialog SHALL use `SolimDialog` + `BlockStatsDisplaySettingsView extends BaseComponent`. The view SHALL present two consecutive vertical sections:
1. **Progress Display Blocks**: Filter chips and block grid containing discrete cycle production blocks (Unit Factories, Reconstructors, Unit Assemblers, Crafters, Separators, Constructors/Payload Deconstructors).
2. **Efficiency Display Blocks**: Filter chips and block grid containing production blocks, resource extraction (Drills, Pumps), and power generation blocks.

Each section SHALL include filter chips (e.g. All, Crafters, Units, Power, Drills) and search or category groups using `reactiveGrid` and `wrap()` layout.

#### Scenario: Settings view opened during gameplay
- **WHEN** the settings dialog is shown
- **THEN** the view renders the Progress Display Blocks section followed by the Efficiency Display Blocks section with independent toggles

---

### Requirement: Global config sliders in settings
The settings view SHALL include sliders for:
- Zoom threshold (`0..2`, step `0.1`, displays `"Off"` when ≤ 0.01)
- Opacity (`0..1`, step `0.05`, displays `"XX%"`)
- Scale (`0.5..1.5`, step `0.1`, displays `"XX%"`)

All sliders SHALL bind bidirectionally to their `ConfigValue<Float>` signal.

#### Scenario: Opacity slider moved
- **WHEN** the user drags the opacity slider to 50%
- **THEN** `opacityConfig` signal updates to `0.5f` and the displayed label shows `"50%"`

---

### Requirement: All settings text is translatable
Every user-visible string in the settings view and dialog SHALL use a `bundle.properties` key under `feature.blockstats-display.settings.*`. No hardcoded display strings are permitted.

#### Scenario: Settings dialog rendered
- **WHEN** the settings dialog is shown
- **THEN** all labels, section headers, and button text resolve from `Core.bundle`