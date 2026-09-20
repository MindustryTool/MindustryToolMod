# slow-component-tracking Specification

## Purpose
TBD - created by archiving change schematic-browser-perf-tracker. Update Purpose after archive.
## Requirements
### Requirement: Opt-in per-component build tracking

The system SHALL provide opt-in timing of Solim component builds with zero measurable overhead when disabled.

#### Scenario: Disabled tracker is a no-op

- **WHEN** slow-component tracking is disabled (the default)
- **THEN** component builds SHALL NOT allocate spans, write logs, or measurably change build latency.

#### Scenario: Slow build emits a span and warning

- **WHEN** tracking is enabled and a component build exceeds its slow threshold
- **THEN** the system SHALL record a span with component name, phase, and duration and emit an internal diagnostic warning.

### Requirement: Grid rebuild phase attribution

The system SHALL attribute schematic browser grid rebuilds to card builds versus reconcile versus reflow versus image decode.

#### Scenario: Page swap attributes reconcile and reflow

- **WHEN** tracking is enabled and a `reactiveGrid` reconciles a fully new page of items
- **THEN** the system SHALL record separate spans for reconcile (new-key builds) and reflow (table re-add plus spacing plus hierarchy invalidation) with item and new-key counts.

#### Scenario: QueryView DATA update is timed

- **WHEN** tracking is enabled and a `QueryView` mounts a DATA component for a new page
- **THEN** the system SHALL record a span covering the DATA factory application with fetching state.

#### Scenario: Image decode cost is separated

- **WHEN** tracking is enabled and a `NetworkImage` decodes bytes to a texture
- **THEN** the system SHALL record a decode span distinct from component build spans.

#### Scenario: Container subtree cost is attributed

- **WHEN** tracking is enabled and a layout container subtree exceeds the slow threshold
- **THEN** the system SHALL record a span with container name, depth, and child count, and SHALL track the maximum observed stack depth.

### Requirement: Bounded span buffer and thresholds

The system SHALL retain recent slow spans in a bounded in-process ring buffer with configurable slow thresholds.

#### Scenario: Recent spans are queryable

- **WHEN** tracking is enabled and slow spans have been recorded
- **THEN** callers SHALL be able to snapshot recent spans in recency order up to the buffer bound.

#### Scenario: Disabling clears state

- **WHEN** tracking is disabled or reset
- **THEN** the buffer SHALL be cleared and no further spans SHALL be recorded.

### Requirement: Browser repro coverage

The system SHALL provide headless coverage that reproduces a schematic browser page change with real cards and separates structure cost from image decode cost.

#### Scenario: Page change with fake image loading

- **WHEN** a headless test builds a `reactiveGrid` of real `SchematicCard` items with a fake image loader and swaps to a fully disjoint page-2 key set
- **THEN** the test SHALL report reconcile, reflow, and per-card build timings with image decode excluded.

#### Scenario: Decode variant quantifies image cost

- **WHEN** the same page-change reproduction runs with real image decode enabled
- **THEN** the test SHALL report the additional decode contribution separately from structure cost.

