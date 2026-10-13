# Solim Instrumentation

## Purpose

A push/pull-separated observability SPI that lets Solim's profiling, flame-trace, and debug-introspection tooling be plugged in and out without any normal logic class knowing a profiler exists. Producers emit one guarded event per moment; all counters, histograms, flame spans, and tree assembly live in swappable sinks.

## ADDED Requirements

### Requirement: Instrumentation SPI is declared in solim-api with no profiler dependency

The system SHALL declare `Instrumentation`, `InstrumentationSink`, and a semantic event hierarchy in `solim-api`. The SPI SHALL NOT reference, import, or depend on any profiler, flame-trace, debug, or reporting implementation. `Instrumentation` SHALL expose `enabled()` and `install(@Nullable InstrumentationSink)`. Installing a sink SHALL set the enabled flag; installing `null` SHALL clear it. A sink MAY be replaced at runtime.

#### Scenario: SPI has no profiler dependency
- **WHEN** the `:solim-api` module is inspected
- **THEN** it contains only the sink interface, the static facade, and event types, and no reference to any profiler implementation

#### Scenario: Sink swap at runtime
- **WHEN** `Instrumentation.install(sinkA)` is called and later `Instrumentation.install(sinkB)`
- **THEN** events emitted after the second call are delivered to `sinkB` and not to `sinkA`

#### Scenario: Uninstall clears the enabled flag
- **WHEN** `Instrumentation.install(sink)` is called and later `Instrumentation.install(null)`
- **THEN** `Instrumentation.enabled()` returns `false`

### Requirement: Disabled emission is allocation-free on hot paths

`Instrumentation.enabled()` SHALL read a `static volatile boolean` that is provably constant when no sink is installed, so that the JVM constant-folds `if (Instrumentation.enabled()) Instrumentation.emit(...)` to a no-op at the emitting call site. Normal logic classes SHALL be permitted to instrument hot paths directly without per-call-site enablement logic.

#### Scenario: No allocation when disabled
- **WHEN** no sink is installed and a `Signal.set`, `Computed.recompute`, or `Effect.runEffect` executes repeatedly
- **THEN** no event object is allocated and the emit branch is eliminated

#### Scenario: Profiling overhead is accepted when enabled
- **WHEN** a sink is installed and a hot-path event is emitted
- **THEN** an event object is allocated and delivered to the sink

### Requirement: Producers emit semantic events and reference no debug type

`Signal`, `Computed`, `Effect`, `StructuralReconciler`, `AttachmentStack`, and `Component` SHALL each emit events describing a *moment* (a write occurred, a computation ran, a reconciliation pass completed, a subtree was attached, a component was disposed) and SHALL NOT declare `instanceof` checks against any debug implementation, SHALL NOT override no-op hooks for instrumentation purposes, and SHALL NOT expose accessor methods whose only purpose is to publish a recorded metric to a profiler.

#### Scenario: No debug coupling in reactive primitives
- **WHEN** `Computed.java` and `Effect.java` are inspected
- **THEN** neither contains an `instanceof DebugReactiveContext` check or a cast to a debug implementation

#### Scenario: No no-op override surface in the runtime
- **WHEN** `AttachmentStack` and `ReactiveContext` are inspected
- **THEN** neither declares overridable `do*()` or `on*()` methods that exist solely so a debug subclass can override them

#### Scenario: Metrics are not published by accessors
- **WHEN** a metric such as reconciliation created/reused/total counts or attachment subtree depth is produced
- **THEN** it is delivered to a sink via an emitted event and is not read back through a getter on the producing class

### Requirement: Push-shaped metrics and pull-shaped graph traversal use separate mechanisms

Telemetry SHALL be split by shape. Push-shaped data (timings, counts, subtree depths, disposal sizes) SHALL be delivered by event emission at the moment it occurs, because it is not queryable afterwards. Pull-shaped data (live dependency graphs, ownership trees, component hierarchies, current values) SHALL remain queryable from live objects through the existing debug-node contract. The two SHALL NOT be unified into a single mechanism.

#### Scenario: Push metrics arrive as events
- **WHEN** a `StructuralReconciler` completes a pass
- **THEN** the created, reused, and total counts are delivered to the installed sink as part of a reconcile event

#### Scenario: Pull graph traversal remains queryable
- **WHEN** a sink walks the dependency graph of a live `Computed`
- **THEN** it queries the computed's debug-node contract directly, with no event replay or recorded snapshot required

#### Scenario: Both shapes coexist
- **WHEN** profiling is enabled
- **THEN** recorded run durations are available from the sink's event history while the current dependency graph is available by querying the live objects

### Requirement: Debug runtime services are no longer substitutable

`AttachmentStack`, `ReactiveContext`, and `SignalDispatcher` SHALL NOT expose `instance()`, `install(...)`, or swappable singleton state for debug purposes. `solim-core/performance` SHALL consume instrumentation events as a subscriber and SHALL NOT replace runtime service instances.

#### Scenario: Perf consumes events rather than replacing services
- **WHEN** `Perf.setEnabled(true)` is called
- **THEN** `Perf` installs an instrumentation sink and begins receiving events, and no runtime service instance is substituted

#### Scenario: Disabling Perf releases recorded state
- **WHEN** `Perf.setEnabled(false)` is called after profiling was active
- **THEN** the sink is uninstalled and all recorded metrics and registries are released

#### Scenario: Property-driven enablement is preserved
- **WHEN** `Perf.init()` is invoked and the `solim.debug.enabled` system property parses to `true`
- **THEN** profiling is active, one informational message naming the property is logged, and enablement depends on the explicit `init()` invocation rather than on `Perf` merely being loaded

#### Scenario: Mod bootstrap is unchanged
- **WHEN** `mindustrytool.Main` completes initialization
- **THEN** it has invoked `Perf.init()` before registering any feature, and does not read the `solim.debug.enabled` property or call `Perf.setEnabled(boolean)` itself

### Requirement: Instrumentation does not alter functional reactive behaviour

Installing or uninstalling a sink SHALL NOT change signal notification, computed evaluation, effect scheduling, reconciliation results, or ownership. Enabling instrumentation SHALL be observability-only.

#### Scenario: Behaviour identical with and without a sink
- **WHEN** the same UI workload is executed twice, once with a sink installed and once without
- **THEN** the resulting component tree, ownership assignment, and rendered output are identical

#### Scenario: Event delivery does not alter notification order
- **WHEN** a `Signal` with multiple observers is set while a sink is installed
- **THEN** observers are notified in the same order as when no sink is installed