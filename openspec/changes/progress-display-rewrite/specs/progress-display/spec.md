## ADDED Requirements

### Requirement: Feature activates and registers draw hook
The feature SHALL remove the `development` guard and set `enabledByDefault(true)`. It SHALL register a `Trigger.draw` listener in its constructor that runs `draw()` every frame.

#### Scenario: Feature is enabled during gameplay
- **WHEN** the feature is enabled and `Vars.state.isGame()` is true and the HUD is shown
- **THEN** `draw()` executes each frame and may render countdown text above qualifying buildings

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
The feature SHALL maintain a `BitSet blockEnabled` indexed by `block.id`. It SHALL be built (or rebuilt) from `Core.settings` whenever a block toggle changes. Default for any block not explicitly stored is `true`.

#### Scenario: BitSet lookup in draw loop
- **WHEN** `draw()` processes a building
- **THEN** `blockEnabled.get(build.block.id)` is used to determine whether to draw — no settings read, no map lookup

#### Scenario: Block toggle persisted and BitSet rebuilt
- **WHEN** a block checkbox is toggled in the settings view
- **THEN** `Core.settings.put(blockSettingKey(block), value)` is called and `rebuildBitSet()` is called immediately

---

### Requirement: Countdown text drawn at building center
The feature SHALL draw remaining production time as a formatted string (e.g., `"12.4s"`) using `Fonts.outline` at `(build.x, build.y)` center-aligned. It SHALL apply `opacityConfig` and `scaleConfig`. No fill bar SHALL be drawn.

#### Scenario: Active building with progress
- **WHEN** a supported building has `fraction >= 0.01f` and its block is enabled in the BitSet
- **THEN** a countdown string is drawn at the building's world position

#### Scenario: Idle or completed building
- **WHEN** `fraction < 0.01f` or total production time is zero
- **THEN** no text is drawn for that building

---

### Requirement: Supported building categories
The draw predicate SHALL match buildings of four types:
- `UnitFactory.UnitFactoryBuild` (only when `currentPlan != -1` and plan is valid)
- `Reconstructor.ReconstructorBuild` (only when `constructTime > 0`)
- `UnitAssembler.UnitAssemblerBuild` (only when `plan() != null`)
- `GenericCrafter.GenericCrafterBuild` (only when `craftTime > 0`)

Buildings outside these types SHALL be ignored.

#### Scenario: Unit factory is producing
- **WHEN** a `UnitFactoryBuild` has a valid `currentPlan` and `progress > 0`
- **THEN** remaining seconds are computed as `(plan.time - b.progress) / 60f / b.timeScale()` and displayed

#### Scenario: Crafter is crafting
- **WHEN** a `GenericCrafterBuild` has `craftTime > 0` and `progress > 0`
- **THEN** remaining seconds are computed as `(block.craftTime - b.progress) / 60f / b.timeScale()` and displayed

---

### Requirement: Declarative Solim settings view with per-block toggles
The settings dialog SHALL use `SolimDialog` + `ProgressDisplaySettingsView extends BaseComponent`. The view SHALL group blocks by category (Unit Factories, Reconstructors, Unit Assemblers, Crafters) with section headers. Crafters SHALL use `wrap()` layout. Each block checkbox SHALL be labeled with the block's localized name.

#### Scenario: Settings view opened during gameplay
- **WHEN** the settings dialog is shown for the first time
- **THEN** the view renders all matching blocks grouped by category with a checkbox per block

#### Scenario: Crafter section renders without overflow
- **WHEN** 30+ crafter blocks are present
- **THEN** checkboxes wrap into multiple rows within the available width, not a single unbounded column

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
Every user-visible string in the settings view and dialog SHALL use a `bundle.properties` key under `feature.progress-display.settings.*`. No hardcoded display strings are permitted.

#### Scenario: Settings dialog rendered
- **WHEN** the settings dialog is shown
- **THEN** all labels, section headers, and button text resolve from `Core.bundle`
