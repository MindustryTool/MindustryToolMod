## MODIFIED Requirements

### Requirement: Task 4 - Repair
The Repair task MUST detect healing capability across healing weapons (`w.bullet.heals()` or `RepairBeamWeapon`) and repair field abilities (`RepairFieldAbility`). Units lacking both healing weapons and repair field abilities MUST be deemed incapable of healing and yield immediately, regardless of building capabilities (`canBuild()`). When a unit only possesses `RepairFieldAbility` without healing weapons, it MUST navigate within aura distance to heal targets passively and suppress non-healing weapon firing against allied blocks or units.

#### Scenario: Unit lacks healing weapons and abilities
- **WHEN** a unit with building capability but no heal weapons or repair field abilities evaluates the Repair task
- **THEN** the task yields immediately and sets status to "Cannot heal" without targeting damaged buildings

#### Scenario: Unit with RepairFieldAbility heals passively
- **WHEN** a unit possessing RepairFieldAbility but no healing weapons approaches a damaged building or unit
- **THEN** the unit moves within repair aura distance and does not fire non-healing weapons

### Requirement: Settings & Internationalization
The settings view MUST provide task reordering (up/down), per-task enable toggles, and live task statuses. Each task row MUST be enclosed in a rounded Card with a background and border. The task status / reason to skip MUST be positioned on a dedicated row beneath the task control buttons to prevent overlapping the toggle button. Expandable per-task settings MUST be wrapped in a SolimCollapser to prevent blank space when collapsed. All user-visible strings MUST be translatable via `bundle.properties`.

#### Scenario: Task card presentation and status separation
- **WHEN** the autoplay settings dialog is displayed
- **THEN** each task is rendered inside a styled Card, and its status text is displayed on a line below the toggle and reorder buttons

#### Scenario: Expandable settings collapse cleanly
- **WHEN** task settings are collapsed
- **THEN** the collapsible container height is 0 and no blank layout space remains
