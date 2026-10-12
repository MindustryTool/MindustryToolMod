## ADDED Requirements

### Requirement: Factory and collection cell bindings are fully owned

Collections and dynamic factories SHALL own any reactive cell constraints and temporary empty state components they construct, preventing orphaned bindings upon item removal or data changes.

#### Scenario: ForEach cell constraint effects are disposed with the item
- **WHEN** a `ForEach` item with reactive cell constraints is reconciled and subsequently removed
- **THEN** the effects created by `applyToCell` are disposed alongside the item component

#### Scenario: ReactiveGrid empty state temporary bindings are disposed
- **WHEN** a `ReactiveGrid` transitions from empty to populated
- **THEN** any bindings and components created by `empty(Runnable)` or `empty(Prov)` are disposed
