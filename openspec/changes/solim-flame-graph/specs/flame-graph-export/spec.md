## ADDED Requirements

### Requirement: Self-contained HTML flame export
The system SHALL provide a headless exporter that writes a single self-contained HTML file rendering the flame graph from a trace snapshot, with no game runtime or network dependency at view time.

#### Scenario: HTML opens without backend
- **WHEN** the exporter writes a file from a captured trace and the file is opened in a browser
- **THEN** the flame SHALL render from embedded data without fetching external resources required for correctness.

#### Scenario: Export works headlessly
- **WHEN** the exporter is invoked in a headless test with a synthetic span list
- **THEN** it SHALL produce a non-empty HTML file containing the embedded payloads.

### Requirement: Dual-format embedded payloads
The exported HTML SHALL embed both a Chrome Trace Events payload (`traceEvents` with complete `X` phases, timestamps, and durations) and a Speedscope-native profile derived from the same span list, so the trace opens in Perfetto, `chrome://tracing`, and speedscope.app.

#### Scenario: Chrome payload present
- **WHEN** the HTML is generated from a trace with nested spans
- **THEN** the embedded Chrome payload SHALL contain one `X` event per span with parent-implied nesting via matching timestamps and thread ids per lane.

#### Scenario: Speedscope payload present
- **WHEN** the HTML is generated from the same trace
- **THEN** the embedded Speedscope payload SHALL contain the same spans with names, start values, and durations, openable in speedscope.app.

#### Scenario: Payloads agree
- **WHEN** both payloads are decoded from one export
- **THEN** they SHALL describe the same span count, names, and total duration within rounding tolerance.

### Requirement: Export-time filtering and self-time
The exporter SHALL apply `minMs` duration pruning and compute self-time (`total - sum(children)`) at export time, leaving recorded spans complete and unmodified.

#### Scenario: Small spans pruned at export
- **WHEN** export is requested with `minMs` greater than zero and the trace contains sub-threshold spans
- **THEN** those spans SHALL be hidden or aggregated in the rendered flame while the stored trace SHALL remain unfiltered.

#### Scenario: Self-time displayed
- **WHEN** a parent span encloses child spans
- **THEN** the flame SHALL distinguish total time from self-time so container overhead is separable from children cost.

### Requirement: Span labeling
The exporter SHALL label each frame preferring an explicit component `name("...")` when present, falling back to `ClassName + element` pair, and SHALL never emit blank labels.

#### Scenario: Explicit name preferred
- **WHEN** a component set an explicit name
- **THEN** the flame frame SHALL show that name.

#### Scenario: Fallback label present
- **WHEN** a component has no explicit name
- **THEN** the flame frame SHALL show a `ComponentClass-elementClass` fallback rather than a blank or `unknown` label.
