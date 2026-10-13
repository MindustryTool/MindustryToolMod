## MODIFIED Requirements

### Requirement: Performance capture covers reactive, layout, and reconciliation work
The performance harness SHALL be able to capture and report on reactive primitive evaluation, declarative attachment subtrees, and keyed-collection reconciliation. Capture SHALL be driven by instrumentation events, SHALL require no producer-side changes to enable, and SHALL report zero overhead when no sink is installed.

#### Scenario: Capture covers each instrumented moment
- **WHEN** a capture is run against a workload that performs signal writes, computed evaluations, effect runs, subtree attachments, and a reconciliation pass
- **THEN** a span or metric is produced for each of those moments

#### Scenario: No sink means no recorded output
- **WHEN** a capture is requested with no instrumentation sink installed
- **THEN** no spans are produced, no depth is recorded, and no totals are accumulated

#### Scenario: Capture state is resettable
- **WHEN** capture state is reset
- **THEN** all recorded spans, totals, and observed depths are discarded

#### Scenario: Threshold-gated capture elides fast work
- **WHEN** a capture threshold is configured above the elapsed time of a subtree
- **THEN** that subtree produces no span

#### Scenario: Instrumentation does not perturb the workload
- **WHEN** the same workload is executed with and without a sink installed
- **THEN** the resulting component tree and rendered output are identical, and the difference is limited to recording cost