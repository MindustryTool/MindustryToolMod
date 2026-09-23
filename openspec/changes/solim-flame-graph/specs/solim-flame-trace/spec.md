## ADDED Requirements

### Requirement: Hierarchical TraceSpan model
The system SHALL provide an immutable `TraceSpan` value in `solim-api` carrying `traceId`, `spanId`, `parentId` (-1 for roots), `name`, `phase`, `startNs`, `endNs`, `depth`, and `childCount`, with duration derived as `endNs - startNs` in nanoseconds.

#### Scenario: Span carries parent linkage and timing
- **WHEN** a container subtree and a leaf build complete inside one trace
- **THEN** each span SHALL expose a unique `spanId`, a `parentId` referencing its enclosing span (or -1), and `endNs` greater than or equal to `startNs`.

#### Scenario: PerfSpan unchanged
- **WHEN** the codebase is searched for the `PerfSpan` type after this change
- **THEN** its fields and slow-span behavior SHALL remain unchanged and flame data SHALL use `TraceSpan`.

### Requirement: Trace stack variant isolation
The runtime SHALL provide a profiling-enabled trace stack variant owning all flame-capture state (open-span stack, ring buffer, trace generation), with the base `ParentStack` containing zero tracing branches. Static entry points SHALL delegate to the singleton, which is a base instance by default and a trace instance only while tracing is enabled.

#### Scenario: Default instance carries no trace state
- **WHEN** tracing has never been enabled
- **THEN** the active singleton SHALL be the base `ParentStack` and no trace buffer SHALL be allocated.

#### Scenario: Enabling installs the trace variant
- **WHEN** tracing is enabled via the public facade
- **THEN** a fresh trace stack variant SHALL become the active singleton with an empty buffer and a new `traceId` generation.

#### Scenario: Disabling reverts and clears
- **WHEN** tracing is disabled
- **THEN** the active singleton SHALL revert to a base `ParentStack` and all recorded trace spans SHALL be discarded.

#### Scenario: Isolation keeps parent chains consistent
- **WHEN** `isolate(...)`, `capture(...)`, or `clear()` mutates the stack while the trace variant is active
- **THEN** the variant's open-span state SHALL be saved and restored in lockstep so no span is attributed to the wrong parent.

### Requirement: Full container and leaf capture
The system SHALL record a span for every container subtree bracketed by stack push/pop and for every leaf `BaseComponent.element()` build while tracing is enabled, with no duration threshold applied at record time.

#### Scenario: Container subtree recorded
- **WHEN** tracing is enabled and a container is popped
- **THEN** a span SHALL be recorded carrying container name, depth, and child count regardless of duration.

#### Scenario: Leaf build recorded
- **WHEN** tracing is enabled and a leaf component builds its element
- **THEN** a span SHALL be recorded carrying the component name and nested under the enclosing container span.

#### Scenario: Disabled tracing records nothing
- **WHEN** tracing is disabled
- **THEN** no spans SHALL be allocated and push/pop/build SHALL incur no tracing overhead.

### Requirement: Bounded rolling buffer and snapshots
The system SHALL retain trace spans in a bounded ring buffer with capacity 4096, evicting oldest spans on overflow, and SHALL expose document-order snapshots plus overflow accounting.

#### Scenario: Snapshots are document-ordered
- **WHEN** tracing captured a build and a snapshot is requested
- **THEN** spans SHALL be returned in creation (document) order suitable for flame reconstruction, not recency order.

#### Scenario: Overflow is reported
- **WHEN** more spans are recorded than the buffer capacity
- **THEN** the snapshot metadata SHALL report the number of dropped oldest spans.

#### Scenario: Reset clears generation state
- **WHEN** tracing is reset
- **THEN** the buffer SHALL be cleared and depth/overflow counters SHALL return to zero.

### Requirement: Public trace facade
The system SHALL expose trace control through the public `Perf` facade in `solim-core` without exposing `solim-runtime` types, providing enablement, snapshot, reset, capacity query, and depth observation.

#### Scenario: Mod code can trace without runtime types
- **WHEN** mod code enables tracing, builds components, and snapshots via the facade
- **THEN** it SHALL receive `TraceSpan` values without referencing any `solim-runtime` type.

#### Scenario: Facade surface
- **WHEN** the public facade is inspected
- **THEN** it SHALL provide trace enablement, `isTracing()`, `traceSnapshot(int)`, `traceReset()`, capacity reporting, and `maxDepthObserved()` continuity.
