## MODIFIED Requirements

### Requirement: Feature activates and registers draw hook
The feature SHALL identify itself as `blockstats-display` and set `enabledByDefault(true)`. It SHALL register a `Trigger.draw` listener in its constructor that runs `draw()` every frame.

#### Scenario: Feature is enabled during gameplay
- **WHEN** the feature is enabled and `Vars.state.isGame()` is true and the HUD is shown
- **THEN** `draw()` executes each frame and may render countdown and efficiency stats text above qualifying buildings

#### Scenario: Feature is disabled
- **WHEN** `isEnabled()` returns false
- **THEN** `draw()` returns immediately without rendering anything

---

### Requirement: Countdown text drawn at building center
The feature SHALL draw remaining production time as a formatted countdown string (e.g., `"12.4s"`) and block efficiency percentage (e.g., `"100%"`) using `Fonts.outline` vertically centered at `(build.x, build.y)` with center alignment. It SHALL apply `opacityConfig` and `scaleConfig`.

#### Scenario: Active building with progress
- **WHEN** a supported building has `fraction >= 0.01f` and its block is enabled in the BitSet
- **THEN** the countdown string is drawn on the top line and efficiency percentage on the bottom line at the building's world position

#### Scenario: Idle or completed building
- **WHEN** `fraction < 0.01f` or total production time is zero
- **THEN** no text is drawn for that building

---

### Requirement: Declarative Solim settings view with per-block toggles
The settings dialog SHALL use `SolimDialog` + `BlockStatsDisplaySettingsView extends BaseComponent`. The view SHALL group blocks by category (Unit Factories, Reconstructors, Unit Assemblers, Crafters) with section headers. Crafters SHALL use `wrap()` layout. Each block checkbox SHALL be labeled with the block's localized name.

#### Scenario: Settings view opened during gameplay
- **WHEN** the settings dialog is shown for the first time
- **THEN** the view renders all matching blocks grouped by category with a checkbox per block

#### Scenario: Crafter section renders without overflow
- **WHEN** 30+ crafter blocks are present
- **THEN** checkboxes wrap into multiple rows within the available width, not a single unbounded column

## ADDED Requirements

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
- **THEN** `"30%"` is retrieved from the pre-cached array and drawn with `Pal.scarlet` color

### Requirement: Efficiency threshold color coding
The efficiency text SHALL be tinted according to efficiency tiers:
- `Pal.accent` / `Pal.power` when efficiency is $\ge 100\%$
- `Color.orange` when efficiency is $> 50\%$ and $< 100\%$
- `Pal.scarlet` when efficiency is $\le 50\%$

#### Scenario: Tint selection
- **WHEN** efficiency text is drawn for a building
- **THEN** the appropriate tier color is applied with alpha multiplied by `frameOpacity`
