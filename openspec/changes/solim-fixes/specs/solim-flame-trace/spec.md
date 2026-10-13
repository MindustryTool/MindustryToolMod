## MODIFIED Requirements

### Requirement: Flame spans are derived from instrumentation events
Flame trace spans SHALL be produced from instrumentation events delivered by the active sink, rather than from timing hooks overridden inside the runtime classes. The flame exporter SHALL NOT require the attachment stack to expose an overridable span-emission surface, and the trace shall be produced from the same event stream that feeds the rest of the profiler.

#### Scenario: Subtree spans come from attachment events
- **WHEN** a declarative container is popped after attaching children, and tracing is active
- **THEN** a subtree span is emitted as an event carrying the owner, the observed depth, the child count, and the elapsed duration

#### Scenario: Span threshold suppresses fast subtrees
- **WHEN** a span threshold is configured above a given subtree's elapsed time
- **THEN** no span is emitted for that subtree

#### Scenario: No overridable span hooks in the runtime
- **WHEN** the attachment stack class is inspected
- **THEN** it declares no overridable methods whose sole purpose is to emit tracing spans

#### Scenario: Tracing toggling does not substitute runtime services
- **WHEN** tracing is enabled or disabled
- **THEN** the runtime services are unchanged and only the sink's recording behaviour changes