## MODIFIED Requirements

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
