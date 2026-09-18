## MODIFIED Requirements

### Requirement: Camera & Visuals
The Autoplay feature MUST render visual status indicators overhead and connect to valid target coordinates, and MUST respect `FreeCameraFeature` when updating camera position.
- MUST provide a cross-platform "Follow Unit" toggle, defaulting to `false`.
- MUST render the active task icon above the player unit.
- WHEN a task has an active, non-null destination target THEN it SHALL render a dashed line connecting the unit to the target.
- WHEN a task is idle or has no target destination THEN no dashed line SHALL be drawn.
- WHEN `FreeCameraFeature` is enabled THEN Autoplay SHALL NOT pull or lerp the camera position toward the unit.

#### Scenario: Active task with destination
- **WHEN** an active task is moving toward a target coordinate
- **THEN** a dashed line is rendered from the unit to the target position

#### Scenario: Task is idle or has no target
- **WHEN** autoplay has no active target or movement destination
- **THEN** no line is drawn to coordinate (0, 0)

#### Scenario: Free camera active suppresses Autoplay camera pull
- **WHEN** Autoplay is running and `FreeCameraFeature` is enabled
- **THEN** Autoplay does not lerp `Core.camera.position` toward the unit, allowing unconstrained map viewing
