# solim-ui-perf-testing Specification

## Purpose
TBD - created by archiving change solim-ui-perf-testing. Update Purpose after archive.
## Requirements
### Requirement: Headless Layout and Hierarchy Benchmarks
The system SHALL provide automated headless benchmarks in `solim-core` that measure component instantiation time, element tree size, and `validate()` layout traversal duration for large component collections (100–1,000 items).

#### Scenario: Large list layout validation
- **WHEN** a list of 100 items containing rows, cards, and labels is constructed and validated
- **THEN** total layout traversal time remains under 5 milliseconds in headless execution.

#### Scenario: Element count and hierarchy depth verification
- **WHEN** components are built declaratively
- **THEN** the benchmark measures total Scene2D Element count, Table cell count, and maximum nesting depth.

### Requirement: Reactive Signal Dispatch Benchmarks
The system SHALL provide automated benchmarks measuring Signal update propagation, Computed memoization, and Effect execution latency under rapid state mutations.

#### Scenario: Rapid signal value updates
- **WHEN** a bound Signal receives 10,000 sequential value changes
- **THEN** dispatch completes without memory churn and updates only affected downstream observers.

### Requirement: Runtime Solim UI Profiler and Diagnostic Metrics
The system SHALL provide a runtime performance profiler capable of capturing active Scene2D Element counts, active Solim bindings, per-frame layout invalidation counts, and frame execution duration.

#### Scenario: Profiler metrics capture
- **WHEN** the Solim UI profiler is enabled
- **THEN** it records total registered components, active bindings, layout invalidation count per frame, and average frame time.

#### Scenario: Layout thrashing detection
- **WHEN** an element or binding repeatedly calls `invalidateHierarchy()` or mutates size within a single frame
- **THEN** the profiler flags excessive layout recalculations and logs a diagnostic warning.

### Requirement: MCP Performance Inspection Tool
The system SHALL provide an MCP tool (`get_ui_perf_metrics`) allowing automated AI agents and developers to inspect live Solim UI performance metrics from a running Mindustry client.

#### Scenario: Querying UI performance metrics via MCP
- **WHEN** the `get_ui_perf_metrics` tool is called
- **THEN** it returns a structured JSON report containing FPS, active element counts, visible element counts, layout pass counts, and active reactive bindings.

