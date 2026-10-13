# Solim Profile Report

## Purpose

Provides a Ctrl+D-toggled profiling session and a self-contained, dependency-free interactive HTML profile report presenting flame graph, component tree, and reactive hotspot views.

## Requirements

### Requirement: Ctrl+D profiling session toggle when Perf is enabled
When `Perf.isEnabled()` is `true`, pressing `Ctrl + D` SHALL toggle an active profiling session: the first press SHALL start profiling and show a start notification via `Vars.ui.showInfoFade(...)`, and the second press SHALL stop profiling, show an end notification via `Vars.ui.showInfoFade(...)`, write the HTML report to `Core.settings.getDataDirectory().child("solim-profiles/profile-<timestamp>.html")`, and open the report via `Core.app.openURI(...)`.

#### Scenario: Ctrl+D ignored when Perf is disabled
- **WHEN** `Perf.isEnabled()` is `false` and the user presses `Ctrl + D`
- **THEN** no profiling session SHALL start and no notification or report SHALL be generated.

#### Scenario: First Ctrl+D starts profiling session
- **WHEN** `Perf.isEnabled()` is `true`, `Perf.isProfiling()` is `false`, and `Ctrl + D` is pressed
- **THEN** `Perf.isProfiling()` SHALL become `true`, unified flame tracing and session counters SHALL be reset and enabled, and `Vars.ui.showInfoFade(...)` SHALL be invoked with the start message.

#### Scenario: Second Ctrl+D stops profiling and exports report
- **WHEN** `Perf.isEnabled()` is `true`, `Perf.isProfiling()` is `true`, and `Ctrl + D` is pressed
- **THEN** `Perf.isProfiling()` SHALL become `false`, a self-contained HTML profile report SHALL be written under `solim-profiles/`, `Vars.ui.showInfoFade(...)` SHALL be invoked with the completion message, and `Core.app.openURI(...)` SHALL be called with the report URI.

### Requirement: Self-contained 3-tab interactive HTML/CSS/JS profile report
The system SHALL generate a single self-contained HTML/CSS/JS profile report inside `solim.performance` with no external network dependencies, providing three interactive tabs: (1) Unified Flame Graph, (2) Component Tree & Reactive Side Inspector, and (3) Reactive Hotspots.

#### Scenario: Flame Graph tab renders unified timeline and details
- **WHEN** the exported HTML report is opened
- **THEN** the Flame Graph tab SHALL render spans across phases (`build`, `signal`, `computed`, `effect`, `flush`) with interactive zoom/pan, search filtering, and a detail inspector showing total time, self time, phase, and parent/child counts.

#### Scenario: Component Tree tab renders hierarchy and reactive state
- **WHEN** a profiling session finishes and the HTML report is generated
- **THEN** the Component Tree tab SHALL include all live components rooted at `SessionRoot.get()` with their component name, class name, element type, layout bounds, child components, and associated `Signal`s, `Computed`s, `Effect`s, and other `Disposable`s.

#### Scenario: Reactive Hotspots tab ranks reactive nodes
- **WHEN** the HTML report is generated after signal writes, computed evaluations, and effect executions
- **THEN** the Reactive Hotspots tab SHALL provide sortable data for Signals (write count, observer count, latest value), Computeds (recompute count, total/last duration, dirty state, value), and Effects (run count, total/last duration, dependency count).