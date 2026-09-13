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

### Requirement: Nested dynamic subtree lifecycle
The system SHALL support nesting `Dynamic` components within other `Dynamic` components, ensuring that inner subtrees update independently while the outer condition is stable, and cascade full disposal when the outer condition switches or collapses.

#### Scenario: Inner dynamic switches while outer condition remains stable
- **WHEN** an inner `Dynamic` component's source signal updates while the enclosing outer `Dynamic` condition has not changed
- **THEN** the inner dynamic disposes its previous child component and builds the new child component without re-evaluating or recreating the outer component

#### Scenario: Outer condition switch or collapse cascades disposal to nested dynamic subtree
- **WHEN** an outer `Dynamic` source signal changes to a different value or collapses to null
- **THEN** the active inner `Dynamic` component, its active child, and all registered effects in the inner subtree are disposed recursively

#### Scenario: Mutating inner signal after outer branch collapse does not trigger factory
- **WHEN** the inner source signal updates after the enclosing outer branch has been collapsed or switched away
- **THEN** the inner dynamic factory SHALL NOT be invoked and no new elements or listeners are created

#### Scenario: Re-expanding outer condition recreates active nested dynamic subtree
- **WHEN** the outer `Dynamic` source signal transitions from collapsed/null back to an active state
- **THEN** a fresh inner `Dynamic` component is instantiated, bound to the current inner source signal, and renders the current state

#### Scenario: Simultaneous update to outer and inner signals resolves deterministically
- **WHEN** both outer and inner signals are mutated prior to a signal dispatcher flush
- **THEN** if the outer condition evaluates to active, the newly mounted inner dynamic reflects the updated inner signal; if the outer condition evaluates to collapsed, the outer container collapses and the inner factory is not invoked for the collapsed state
