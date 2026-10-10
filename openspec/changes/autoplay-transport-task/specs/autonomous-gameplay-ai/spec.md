## MODIFIED Requirements

### Requirement: Shared task strip quick surface
The system SHALL provide a shared horizontal task strip used by both the standalone HUD and the QuickAccess popup. The strip SHALL contain a master autoplay on/off cell at the far left followed by all registered task icon toggles (including `TransportTask`) in priority order laid out in a grid with 5 columns per row, SHALL highlight the currently active task via icon color, SHALL keep default touch targets, and SHALL remain editable even while autoplay is disabled. When `TransportTask` is enabled, the HUD and popup surface SHALL also render quick action buttons to toggle target selection, toggle destination selection, and clear all transport nodes. The strip SHALL be toggle-only for task ordering; reprioritizing tasks SHALL remain available only through the settings dialog reorder controls.

#### Scenario: Toggle task from strip
- **WHEN** the player taps a task icon in the HUD or popup strip
- **THEN** that task's enabled state flips via `disabledTasks` and the strip icon color reflects the new state without rebuilding

#### Scenario: Strip toggle binding stays reactive
- **WHEN** a task's enabled state changes from any surface
- **THEN** the strip icon reflects the change because the binding tracks the `disabledTasks` signal rather than a peeked snapshot

#### Scenario: Active task highlighted
- **WHEN** autoplay switches its current task
- **THEN** the corresponding strip icon shows the active highlight while others do not

#### Scenario: Master switch toggles autoplay
- **WHEN** the player taps the master cell in the HUD or popup strip
- **THEN** autoplay enables or disables without affecting per-task toggle states

#### Scenario: Transport selection buttons shown when Transport task is enabled
- **WHEN** `TransportTask` is enabled in `AutoplayFeature`
- **THEN** the Autoplay HUD and QuickAccess popup display the `Select Targets`, `Select Destinations`, and `Clear All` buttons alongside the task strip
