## MODIFIED Requirements

### Requirement: Profile reports are assembled from recorded metrics
Profile reports SHALL be assembled from the metrics recorded by the active instrumentation sink. Report generation SHALL NOT depend on casting runtime service instances to debug implementations, and SHALL NOT require the producer classes to expose accessor methods that exist only to publish a recorded counter.

#### Scenario: Reconciler metrics come from the event stream
- **WHEN** a profile report includes reconciliation statistics
- **THEN** the created, reused, and total counts are read from the recorded reconcile events rather than from getters on the reconciler

#### Scenario: Component-to-reactive association is preserved
- **WHEN** a profile report includes the component tree snapshot
- **THEN** signals created during a component's build and signals reachable through its owned effects and computeds are still associated with that component

#### Scenario: No debug cast is required to build a report
- **WHEN** the report builder is inspected
- **THEN** it contains no check for a debug implementation of a runtime service

#### Scenario: Reports reflect an equivalent workload after the refactor
- **WHEN** the same UI workload is profiled before and after instrumentation is moved behind the sink
- **THEN** the reported metrics for that workload match