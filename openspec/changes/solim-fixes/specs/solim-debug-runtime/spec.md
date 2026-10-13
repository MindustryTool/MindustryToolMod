## MODIFIED Requirements

### Requirement: Swappable runtime services coordinated by Perf
Debug-mode recording SHALL be provided by an instrumentation sink subscribed to semantic events, not by substituting debug subclasses for `AttachmentStack`, `ReactiveContext`, or `SignalDispatcher`. The three runtime services SHALL expose no `instance()`, `install(...)`, or swappable singleton state. `Perf.setEnabled(boolean)` SHALL install or uninstall a sink atomically and SHALL NOT swap runtime service implementations. Debug mode SHALL additionally be enterable without a programmatic call, by `Perf.init()` reading a `solim.debug.enabled` JVM system property that defaults to `"false"`; the property is the only supported external trigger. Enablement SHALL be triggered solely by an explicit `Perf.init()` invocation and SHALL NOT depend on the `Perf` class being loaded.

#### Scenario: Default runtime records nothing
- **WHEN** the `solim.debug.enabled` system property is absent or does not parse to `true`, and `Perf.setEnabled(true)` has not been called
- **THEN** `Instrumentation.enabled()` SHALL be `false`, no sink is installed, no metrics are recorded, and the `Ctrl + D` profiling keybind SHALL perform no input polling

#### Scenario: Init honours the debug property
- **WHEN** `Perf.init()` is invoked and the `solim.debug.enabled` system property parses to `true`
- **THEN** `Perf.isEnabled()` SHALL be `true` and an instrumentation sink SHALL be installed

#### Scenario: Property-driven enablement is logged at startup
- **WHEN** `Perf.init()` is invoked and the `solim.debug.enabled` system property parses to `true`
- **THEN** `arc.util.Log` SHALL receive one informational message naming the property and its effective value, so the developer's log records that debug mode is active

#### Scenario: Init is inert when the property is absent or malformed
- **WHEN** `Perf.init()` is invoked and the `solim.debug.enabled` system property is absent or does not parse to `true`
- **THEN** no sink SHALL be installed and no informational message SHALL be logged

#### Scenario: Enablement does not depend on class loading
- **WHEN** `Perf` has been loaded but its initializer has not yet been triggered, and the property parses to `true`
- **THEN** the system SHALL NOT enter debug mode until `Perf.init()` is explicitly invoked

#### Scenario: Enabling Perf installs a sink without substituting services
- **WHEN** `Perf.setEnabled(true)` is called
- **THEN** an instrumentation sink SHALL be installed, and `AttachmentStack`, `ReactiveContext`, and `SignalDispatcher` SHALL remain their base implementations

#### Scenario: Disabling Perf releases recorded state
- **WHEN** `Perf.setEnabled(false)` is called after debug mode was active
- **THEN** the sink SHALL be uninstalled and all recorded debug metrics and registries SHALL be released

#### Scenario: Explicit setEnabled calls override property-derived state
- **WHEN** `Perf.init()` caused debug mode to be active and `Perf.setEnabled(false)` is subsequently called
- **THEN** debug mode SHALL be fully disabled, and a later `Perf.init()` SHALL remain able to re-enable it

#### Scenario: The mod bootstrap triggers Perf without knowing the property
- **WHEN** `mindustrytool.Main`'s initialization completes
- **THEN** it SHALL have invoked `Perf.init()` before registering any feature, and it SHALL NOT read or reference the `solim.debug.enabled` property and SHALL NOT call `Perf.setEnabled(boolean)`

### Requirement: Reactive primitive instrumentation in debug mode
When a sink is installed, the runtime SHALL record `Signal` writes, `Computed` evaluations and dirty states, `Effect` executions, `StructuralReconciler` pass outcomes, `AttachmentStack` subtree attachments, and `Component` disposals without altering functional reactive behavior. These records SHALL be delivered as events at the moment they occur rather than read back through accessors on the producing classes.

#### Scenario: Signal write metrics recorded
- **WHEN** a sink is installed and a `Signal` value is updated via `set(...)`
- **THEN** the sink SHALL receive a signal-write event carrying the source, the old value, and the new value

#### Scenario: Computed evaluation metrics recorded
- **WHEN** a sink is installed and a `Computed` recomputes its value
- **THEN** the sink SHALL receive a computed-run event carrying the node, the evaluation duration in nanoseconds, and whether the value changed

#### Scenario: Effect execution metrics recorded
- **WHEN** a sink is installed and an `Effect` runs initially or during a `SignalDispatcher` flush
- **THEN** the sink SHALL receive an effect-run event carrying the effect, its execution duration in nanoseconds, and its current `ReactiveSource` dependencies

#### Scenario: Reconciliation metrics recorded
- **WHEN** a sink is installed and a `StructuralReconciler` completes a pass
- **THEN** the sink SHALL receive a reconcile event carrying the reconciler, the number of components created, the number reused, and the resulting total

#### Scenario: Component disposal metrics recorded
- **WHEN** a sink is installed and a `Component` is disposed
- **THEN** the sink SHALL receive a dispose event carrying the component, its child count, and its owned-resource count