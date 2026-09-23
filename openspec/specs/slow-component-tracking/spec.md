# slow-component-tracking Specification

## Purpose
TBD - created by archiving change schematic-browser-perf-tracker. Update Purpose after archive.
## Requirements
### Requirement: Bounded span buffer and thresholds

The system SHALL retain recent structural slow spans in a bounded in-process ring buffer owned by the debug stack variant, with a single configurable slow threshold applying to all structural spans.

#### Scenario: Recent spans are queryable

- **WHEN** profiling is enabled and slow structural spans have been recorded
- **THEN** callers SHALL be able to snapshot recent spans in recency order up to the buffer bound.

#### Scenario: Disabling clears state

- **WHEN** profiling is disabled or reset
- **THEN** the buffer SHALL be cleared and no further spans SHALL be recorded.

#### Scenario: Single threshold suppresses fast spans

- **WHEN** a structural span completes faster than the configured threshold
- **THEN** no span SHALL be recorded for it.

### Requirement: Browser repro coverage

The system SHALL provide headless coverage that reproduces a schematic browser page change with real cards and asserts structural subtree spans for the card containers built during the page change.

#### Scenario: Page change reports structural spans

- **WHEN** a headless test builds a `reactiveGrid` of real `SchematicCard` items with a fake image loader and swaps to a fully disjoint page-2 key set
- **THEN** the test SHALL assert structural subtree spans named after the card containers, including duration and depth detail.

### Requirement: AttachmentStack-owned structural profiling

The Solim runtime SHALL own all slow-span profiling within `AttachmentStack`, recording structural spans for container subtree completion, pending-component attach batches, and the maximum observed stack depth, with no profiling state or branches in the always-on base class.

#### Scenario: Subtree span on slow pop

- **WHEN** profiling is enabled and a container subtree exceeds the slow threshold when popped
- **THEN** the system SHALL record a structural span carrying container name, depth, and child count.

#### Scenario: Attach batch span

- **WHEN** profiling is enabled and a pending-component attach batch exceeds the slow threshold
- **THEN** the system SHALL record a structural span carrying parent name and attached child count.

#### Scenario: Maximum depth tracked

- **WHEN** profiling is enabled and the stack reaches a new deepest level
- **THEN** `maxDepthObserved` SHALL report the deepest level observed since the last reset.

#### Scenario: Disabled base records nothing

- **WHEN** profiling is disabled
- **THEN** no spans SHALL be allocated and no logging SHALL occur.

### Requirement: Debug stack variant isolation

The runtime SHALL provide a base `AttachmentStack` containing zero profiling logic and a `DebugAttachmentStack` subclass owning all profiling state; static methods SHALL delegate to a singleton instance that is a base instance by default and a debug instance only while profiling is enabled.

#### Scenario: Default instance is the base variant

- **WHEN** profiling has never been enabled
- **THEN** the active singleton SHALL be the base `AttachmentStack` and the debug variant SHALL not be instantiated.

#### Scenario: Enabling installs the debug variant

- **WHEN** profiling is enabled
- **THEN** a fresh `DebugAttachmentStack` SHALL become the active singleton.

#### Scenario: Disabling reverts and clears

- **WHEN** profiling is disabled
- **THEN** the active singleton SHALL revert to a base `AttachmentStack` and all recorded spans SHALL be cleared.

#### Scenario: Timing state stays consistent across isolation

- **WHEN** `isolate(...)` or `clear()` mutates the stack directly while the debug variant is active
- **THEN** the debug variant's parallel push-time state SHALL be saved and restored in lockstep with the stack so that no timing desynchronization occurs.

### Requirement: Public profiling facade

The system SHALL expose profiling enablement and span queries through a public facade in `solim-core` that delegates to `AttachmentStack` without exposing `solim-runtime` on consumer classpaths.

#### Scenario: Mod code can enable and query

- **WHEN** mod code calls the public facade to enable profiling, build components, and snapshot spans
- **THEN** it SHALL receive the recorded structural spans without referencing any `solim-runtime` type.

#### Scenario: Facade surface

- **WHEN** the public facade is inspected
- **THEN** it SHALL provide `setEnabled(boolean)`, `isEnabled()`, `snapshot(int)`, `totalRecorded()`, `reset()`, `setThreshold(float)`, and `maxDepthObserved()`.

### Requirement: Removal of the sink indirection and tracking singleton

The system SHALL NOT expose a `PerfSink` injection seam, a `SlowTracker` singleton, or a `SlowSpan` type; span collection SHALL be internal to the debug stack variant and exposed only as `PerfSpan` values through the public facade.

#### Scenario: No legacy tracking symbols remain

- **WHEN** the codebase is searched for `SlowTracker`, `SlowSpan`, or `PerfSink`
- **THEN** zero references SHALL remain in production and test sources.
