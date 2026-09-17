## ADDED Requirements

### Requirement: Base AI Null Safety and Boundary Protection
The `BaseAutoplayAI` controller SHALL strictly guard against unassigned units (`unit == null`) and null target positions (`target == null` or `pos == null`) in all movement and steering delegations.

#### Scenario: AI movement method called when unit is null
- **WHEN** an AI movement method (`moveTo`, `circle`) is invoked while `unit` is null
- **THEN** the method SHALL safely return immediately without invoking Mindustry's `AIController` or throwing `NullPointerException`.

#### Scenario: AI movement method called with null target position
- **WHEN** `moveTo` is invoked with a null `Position`
- **THEN** the method SHALL safely return immediately without attempting coordinate reads or throwing `NullPointerException`.

### Requirement: Task Transition and State Cleanup
The Autoplay system SHALL manage transient unit action states during task switching and feature state transitions.

#### Scenario: Switching away from an offensive task
- **WHEN** Autoplay transitions the active task from `AttackTask` to another task (or to idle/disabled)
- **THEN** it SHALL reset shooting state via `unit.isShooting(false)`.

#### Scenario: Switching away from a mining task
- **WHEN** Autoplay transitions away from `MiningTask`
- **THEN** it SHALL clear the active mining tile target `unit.mineTile = null`.

#### Scenario: Player manual override or feature disable
- **WHEN** user input overrides autoplay or the feature is disabled
- **THEN** Autoplay SHALL yield control to `Vars.player`, reset transient states, and clear `currentTask`.

### Requirement: Safe Evaluation and Coordinate Resolution
All autoplay tasks SHALL operate without side effects during priority arbitration and MUST use null-safe coordinate lookups.

#### Scenario: Self-Heal task arbitration
- **WHEN** `SelfHealTask.update(unit)` is evaluated during task selection
- **THEN** it SHALL evaluate eligibility and status without calling AI movement methods.

#### Scenario: Self-Build and Rebuild on off-map or missing tiles
- **WHEN** build plans reference missing or boundary tiles (`tile() == null`)
- **THEN** distance calculation and movement targets SHALL compute from plan tile coordinates (`x * Vars.tilesize, y * Vars.tilesize`) rather than throwing `NullPointerException`.

#### Scenario: Mining boosting on map boundaries
- **WHEN** a ground unit updates boosting while near or outside map boundaries where `unit.floorOn()` may be null
- **THEN** it SHALL null-check the floor before inspecting floor properties (`isDuct`, `damageTaken`, `isDeep`).
