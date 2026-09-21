## MODIFIED Requirements

### Requirement: Task 5 - Follow & Assist
The Follow & Assist task MUST follow designated teammates in multiplayer and mirror their actions without polluting the unit build queue.
- In singleplayer, the task MUST yield cleanly.
- In multiplayer Any mode (no designated player), the task MUST only track active builders, where active means the teammate unit is alive and either `activelyBuilding()` or has queued build plans, and MUST yield when no such builder exists.
- In multiplayer named mode, the task MUST follow the designated alive teammate even when idle; when the designated player is missing, it MUST fall back to active-builder search then yield.
- WHEN assisting an ally's construction THEN the task MUST verify that the build plan is not already present in the unit's build queue before appending.
- WHEN yielding for no active builder THEN the task MUST clear the follow target and report a distinct `No active builders` status.

#### Scenario: Assisting an active builder
- **WHEN** the followed teammate is actively placing or constructing a structure
- **THEN** FollowAssistTask navigates to assist without adding duplicate build plans to `unit.plans` every frame

#### Scenario: Any mode yields with no active builders
- **WHEN** no same-team alive teammate is actively building or has queued build plans
- **THEN** FollowAssistTask yields so lower-priority tasks can run and reports `No active builders`

#### Scenario: Queued plans count as active
- **WHEN** a teammate is not actively building this tick but still has queued build plans
- **THEN** FollowAssistTask treats that teammate as an active builder and follows

#### Scenario: Named player followed even when idle
- **WHEN** a designated player is configured and alive on the same team
- **THEN** FollowAssistTask follows that player even when the player is not building
