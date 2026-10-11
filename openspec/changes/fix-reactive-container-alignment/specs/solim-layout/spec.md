## ADDED Requirements

### Requirement: Reactive container alignment propagation
Reactive container components (`When`, `Dynamic`, `ForEach`, `QueryView`) SHALL support alignment modifiers (`align(int)`, `center()`, `top()`, `bottom()`, `left()`, `right()`) that propagate alignment to both the parent layout cell and the internal container children.

#### Scenario: Aligning When component
- **WHEN** `.center()` is called on a `When` component attached to a parent `Column`
- **THEN** the parent cell holding the `When` component is centered, the internal container defaults are centered, and mounted branch children are centered

#### Scenario: Aligning Dynamic component
- **WHEN** `.center()` is called on a `Dynamic` component attached to a parent layout
- **THEN** the parent cell holding the `Dynamic` component is centered and mounted children are centered

#### Scenario: Aligning ForEach component
- **WHEN** `.center()` is called on a `ForEach` component attached to a parent layout
- **THEN** the parent cell holding the `ForEach` component is centered and mounted children are centered

#### Scenario: Aligning QueryView component
- **WHEN** `.center()` is called on a `QueryView` component attached to a parent layout
- **THEN** the parent cell holding the `QueryView` component is centered and child views (loading, error, data) are centered

#### Scenario: Preserving alignment across dynamic updates
- **WHEN** the condition or data source of a centered reactive container triggers a rebuild
- **THEN** the newly mounted children and container retain the centered alignment without resetting to top-left
