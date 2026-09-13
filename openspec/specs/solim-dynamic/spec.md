## ADDED Requirements

### Requirement: Dynamic value equality guard
The system SHALL skip rebuilding its child component when the source signal emits a value that is equal to the previous value, and SHALL cleanly manage parent cell sizing constraints across collapse and expand transitions.

#### Scenario: Same value does not trigger rebuild
- **WHEN** the source signal emits a value that is `Objects.equals()` to the current value
- **THEN** the existing child component is preserved without disposal or recreation

#### Scenario: Different value triggers rebuild
- **WHEN** the source signal emits a value that is not equal to the current value
- **THEN** the existing child component is disposed and a new one is created from the factory

#### Scenario: First emission triggers build
- **WHEN** the source signal emits for the first time (no previous value)
- **THEN** the child component is created from the factory

#### Scenario: Null value collapses parent cell
- **WHEN** the source signal emits a null value or factory returns null
- **THEN** the container is hidden (`visible = false`), the parent cell size is collapsed to `0f`, and padding is set to `0f`

#### Scenario: Non-null value expands parent cell with unconstrained bounds
- **WHEN** the source signal transitions from null to a non-null value
- **THEN** the container becomes visible (`visible = true`), parent cell min and max size constraints are restored to unconstrained sentinel (`Float.NEGATIVE_INFINITY`), size constraints configured on the Dynamic component are re-applied, and the layout hierarchy is invalidated
