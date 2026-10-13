## MODIFIED Requirements

### Requirement: Disposal is idempotent, ordered, and fail-fast
A `Component`'s `dispose()` SHALL be safe to call more than once. On the first call it SHALL mark the component disposed, run the component's disposal hook, detach the component from its owner, and then dispose owned children and owned resources in reverse registration order. Failures raised while disposing children or resources SHALL NOT prevent the remaining items from being disposed; the first failure SHALL be captured and rethrown after every owned item has been given a chance to dispose. Failures raised by the component's disposal hook SHALL propagate immediately and SHALL NOT be aggregated, because the hook runs before the owned items are reachable through the component's own disposal path.

#### Scenario: Repeated disposal is a no-op
- **WHEN** `dispose()` is called twice on the same component
- **THEN** the second call performs no work and raises nothing

#### Scenario: Owned items are disposed in reverse registration order
- **WHEN** a component owns three resources registered in order A, B, C
- **THEN** they are disposed in the order C, B, A

#### Scenario: A failing child does not prevent siblings from disposing
- **WHEN** one owned child throws during disposal and two others do not
- **THEN** all three are given a disposal attempt and the first failure is rethrown afterwards

#### Scenario: A failing owned resource does not prevent siblings from disposing
- **WHEN** one owned resource throws during disposal and two others do not
- **THEN** all three are given a disposal attempt and the first failure is rethrown afterwards

#### Scenario: Disposal hook failure propagates immediately
- **WHEN** a component's disposal hook throws
- **THEN** the exception escapes `dispose()` and is not wrapped or suppressed

#### Scenario: Disposed components reject further use
- **WHEN** any operation requiring a live component is invoked after disposal
- **THEN** an `IllegalStateException` naming the component type is raised

### Requirement: Component child and resource accessors return snapshots
`Component`'s child-component and owned-resource accessors SHALL return immutable snapshots rather than live views of the underlying collections. Accessors with the same name across all ownership-owner implementations SHALL have the same contract, so that a caller cannot observe a different aliasing guarantee depending on which implementation it received.

#### Scenario: Snapshot is unaffected by later mutation
- **WHEN** `children()` is called and a child is subsequently added to the component
- **THEN** the previously returned list does not change and does not include the new child

#### Scenario: Returned list is immutable
- **WHEN** a caller attempts to modify the list returned by `children()`
- **THEN** an `UnsupportedOperationException` is raised

#### Scenario: Contracts match across implementations
- **WHEN** the same accessor is called on a component and on the session root
- **THEN** both return immutable snapshots with the same aliasing behaviour

#### Scenario: Iteration is safe during concurrent structural change
- **WHEN** a snapshot obtained from `children()` is iterated while the component's children are being disposed
- **THEN** iteration does not raise a concurrent modification failure