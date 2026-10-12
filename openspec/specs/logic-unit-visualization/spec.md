## ADDED Requirements

### Requirement: Standalone LogicVisualizerFeature Registration & DisplayGroup Integration
The system SHALL register `LogicVisualizerFeature` as a standalone feature with `id="logic-visualizer"`, icon `Icon.logic`, `displayGroup(true)`, `quickAccessByDefault(false)`, and `enabledByDefault(false)`.

#### Scenario: DisplayGroup discovery
- **WHEN** `DisplayGroupFeature` collects display candidates via `displayCandidates()`
- **THEN** `LogicVisualizerFeature` is included among the candidates because its metadata specifies `isDisplayGroup() == true`

#### Scenario: Toggle via DisplayGroup popup
- **WHEN** the player taps the `LogicVisualizerFeature` icon button in `DisplayGroupPopup`
- **THEN** `LogicVisualizerFeature` toggles between enabled and disabled

### Requirement: Logic Unit Line Rendering
The `LogicVisualizerFeature` SHALL draw a solid line connecting each logic-controlled unit to its controlling logic processor building when enabled.

#### Scenario: Unit controlled by logic processor
- **WHEN** `LogicVisualizerFeature` is enabled, a unit is controlled by a `LogicAI` instance referencing a valid `Building` controller, and either the unit or the building is within the camera viewport
- **THEN** a solid line of width 1.5f and opacity 0.7f is rendered connecting `(unit.x, unit.y)` to `(controller.x, controller.y)` using the controller's team color

#### Scenario: Unit not controlled by logic
- **WHEN** a unit is controlled by player input or default formation/command AI
- **THEN** no line is drawn for that unit

#### Scenario: Controller building destroyed or invalid
- **WHEN** a unit's `LogicAI` references an invalid or destroyed controller
- **THEN** no line is drawn for that unit

### Requirement: Team Filtering Configuration
`LogicVisualizerFeature` SHALL provide configuration options to filter logic unit lines by allied and enemy teams.

#### Scenario: Allied units filtering
- **WHEN** `LogicVisualizerFeature` is enabled and `logic-unit-ally` is true
- **THEN** lines are drawn for logic-controlled units on the local player's team

#### Scenario: Enemy units filtering disabled
- **WHEN** `LogicVisualizerFeature` is enabled and `logic-unit-enemy` is false
- **THEN** no lines are drawn for logic-controlled units on opposing teams

#### Scenario: Enemy units filtering enabled
- **WHEN** `LogicVisualizerFeature` is enabled and `logic-unit-enemy` is true
- **THEN** lines are drawn for logic-controlled units on opposing teams

### Requirement: Settings UI
`LogicVisualizerFeature` SHALL provide a settings dialog and view conforming to `SettingScaffold`, `SettingCard`, and WebStyles standards.

#### Scenario: Opening settings dialog
- **WHEN** the player long-presses the `LogicVisualizerFeature` button in `DisplayGroupPopup` or opens its settings dialog
- **THEN** `LogicVisualizerSettingsDialog` opens showing the header card, team filter toggles, and reset actions
