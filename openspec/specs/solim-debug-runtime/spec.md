# Solim Debug Runtime

## Purpose

Provides swappable debug runtime services and reactive primitive instrumentation that record performance and structural metadata while leaving functional reactive behavior unchanged, with zero debug state allocated when Perf is disabled.

## Requirements

### Requirement: Swappable runtime services coordinated by Perf
The system SHALL abstract `ReactiveContext` and `SignalDispatcher` in `solim-runtime` into singleton services supporting `instance()` and `install(...)` delegation, and `Perf.setEnabled(boolean)` SHALL atomically swap `AttachmentStack`, `ReactiveContext`, and `SignalDispatcher` between their normal base implementations and their debug subclasses (`DebugAttachmentStack`, `DebugReactiveContext`, `DebugSignalDispatcher`). Debug mode SHALL additionally be enterable without a programmatic call, by `Perf.init()` reading a `solim.debug.enabled` JVM system property that defaults to `"false"`; the property is the only supported external trigger and its absence SHALL leave the runtime in its normal state. Enablement SHALL be triggered solely by an explicit `Perf.init()` invocation and SHALL NOT depend on the `Perf` class being loaded, so that a build in which nothing else references `Perf` still honours the property.

#### Scenario: Default runtime uses normal service instances
- **WHEN** the `solim.debug.enabled` system property is absent or does not parse to `true`, and `Perf.setEnabled(true)` has not been called
- **THEN** `AttachmentStack.instance()`, `ReactiveContext.instance()`, and `SignalDispatcher.instance()` SHALL be base instances with zero debug tracking state allocated, and the `Ctrl + D` profiling keybind SHALL perform no input polling

#### Scenario: Init honours the debug property
- **WHEN** `Perf.init()` is invoked and the `solim.debug.enabled` system property parses to `true`
- **THEN** `Perf.isEnabled()` SHALL be `true`, `AttachmentStack.instance()` SHALL be a `DebugAttachmentStack`, `ReactiveContext.instance()` SHALL be a `DebugReactiveContext`, and `SignalDispatcher.instance()` SHALL be a `DebugSignalDispatcher`

#### Scenario: Property-driven enablement is logged at startup
- **WHEN** `Perf.init()` is invoked and the `solim.debug.enabled` system property parses to `true`
- **THEN** `arc.util.Log` SHALL receive one informational message naming the property and its effective value, so the developer's log records that debug mode is active

#### Scenario: Init is inert when the property is absent or malformed
- **WHEN** `Perf.init()` is invoked and the `solim.debug.enabled` system property is absent or does not parse to `true`
- **THEN** all three runtime services SHALL remain base instances and no informational message SHALL be logged

#### Scenario: Enablement does not depend on class loading
- **WHEN** `Perf` has been loaded but its initializer has not yet been triggered, and the property parses to `true`
- **THEN** the system SHALL NOT enter debug mode until `Perf.init()` is explicitly invoked

#### Scenario: Enabling Perf installs all debug services
- **WHEN** `Perf.setEnabled(true)` is called
- **THEN** `AttachmentStack.instance()` SHALL be a `DebugAttachmentStack`, `ReactiveContext.instance()` SHALL be a `DebugReactiveContext`, and `SignalDispatcher.instance()` SHALL be a `DebugSignalDispatcher`.

#### Scenario: Disabling Perf restores normal services and discards debug state
- **WHEN** `Perf.setEnabled(false)` is called after debug mode was active
- **THEN** all three runtime services SHALL revert to base instances and all recorded debug metrics and registries SHALL be released.

#### Scenario: Explicit setEnabled calls override property-derived state
- **WHEN** `Perf.init()` caused debug mode to be active and `Perf.setEnabled(false)` is subsequently called
- **THEN** debug mode SHALL be fully disabled, and a later `Perf.init()` SHALL remain able to re-enable it

#### Scenario: The mod bootstrap triggers Perf without knowing the property
- **WHEN** `mindustrytool.Main`'s initialization completes
- **THEN** it SHALL have invoked `Perf.init()` before registering any feature, and it SHALL NOT read or reference the `solim.debug.enabled` property and SHALL NOT call `Perf.setEnabled(boolean)`

### Requirement: Reactive primitive instrumentation in debug mode
When `DebugReactiveContext` and `DebugSignalDispatcher` are installed, the runtime SHALL track `Signal` creations and writes, `Computed` evaluations and dirty states, `Effect` executions, and reactive dependency edges without altering functional reactive behavior.

#### Scenario: Signal write metrics recorded
- **WHEN** `Perf.setEnabled(true)` is active and a `Signal` value is updated via `set(...)`
- **THEN** `DebugReactiveContext` SHALL increment the signal's write count, record its latest value summary, and track its active observers.

#### Scenario: Computed evaluation metrics recorded
- **WHEN** `Perf.setEnabled(true)` is active and a `Computed` recomputes its value
- **THEN** `DebugReactiveContext` SHALL record the recomputation count, evaluation duration in nanoseconds, cached value summary, and current `ReactiveSource` dependencies.

#### Scenario: Effect execution metrics recorded
- **WHEN** `Perf.setEnabled(true)` is active and an `Effect` runs initially or during a `SignalDispatcher` flush
- **THEN** `DebugReactiveContext` SHALL record the effect's run count, total and last execution duration in nanoseconds, and current `ReactiveSource` dependencies.

### Requirement: Component-to-reactive association tracking
In debug mode, the system SHALL associate each live `Component` with both the `Signal`s created while that component is the active owner in `OwnershipContext` and the `Signal`s and `Computed`s reachable through the dependency graphs of the component's owned `Effect`s and `Computed`s.

#### Scenario: Signal created during component build is linked to component
- **WHEN** `Perf.setEnabled(true)` is active and `Signal.of(...)` is invoked while a component is building
- **THEN** the component tree snapshot SHALL include that signal under the building component's associated signals.

#### Scenario: External signal read by component effect or computed is linked to component
- **WHEN** `Perf.setEnabled(true)` is active and a component owns an `Effect` or `Computed` that reads an externally created `Signal`
- **THEN** the component tree snapshot SHALL include that external signal and its metrics under the component's associated reactive sources.