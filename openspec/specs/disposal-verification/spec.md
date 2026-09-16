# disposal-verification Specification

## Purpose

Systematic disposal verification for every `Disposable` in `solim` and `solim-*` packages across standalone, nested, and structural-container modes. Created by archiving change disposal-test-coverage.

## Requirements

### Requirement: Uniform sweep predicates for every Disposable

Every `Disposable` in `solim` and `solim-*` packages SHALL satisfy the uniform disposal contract verified by the sweep: `dispose()` releases all owned resources, a second `dispose()` is a no-op without throwing, and `isDisposed()` returns `true` after disposal. Global singletons (`Signals` static registrations/`Timer`, `NetworkImage` static cache, `RoundedCache`, `StyleCache`) are excluded; only instance detachment is asserted, never static clearing.

#### Scenario: Sweep disposes owned resources
- **WHEN** any covered `Disposable` is disposed after being fully initialized
- **THEN** all effects, subscriptions, bindings, child components, reconciler entries, and listener handles it owns are disposed or unregistered

#### Scenario: Sweep double disposal is safe
- **WHEN** `dispose()` is called a second time on any covered `Disposable`
- **THEN** no exception is thrown, no resource is disposed again, and `isDisposed()` still returns `true`

### Requirement: Effect disposal dual proof

A disposed `Effect` SHALL satisfy both proofs: behaviorally it never re-runs when a dependency changes (`Signal.set` followed by dispatcher flush causes zero additional runs), and structurally its observer and dependency registrations reach zero.

#### Scenario: Disposed effect does not re-run
- **WHEN** an `Effect` reading a `Signal` is disposed and the `Signal` is then set to a new value with the dispatcher flushed
- **THEN** the effect body executes zero additional times

#### Scenario: Disposed effect detaches from graph
- **WHEN** an `Effect` is disposed
- **THEN** its dependency and observer registrations are removed and counted as zero

### Requirement: Subscription disposal stops notifications

A disposed `Subscription` from `Signal.subscribe` or `Computed.subscribe` SHALL never invoke its callback again, and repeated disposal SHALL be idempotent.

#### Scenario: Disposed subscription is silent
- **WHEN** a subscription is disposed and the source is then set to a new value
- **THEN** the disposed callback is not invoked

### Requirement: Signal graph drain

`Signal` has no `dispose()`; disposal verification SHALL instead assert that after disposing everything downstream (all `Subscription`s, `Effect`s, and derived `Computed`s), the signal's `listenerCount` and `observerCount` return to zero.

#### Scenario: Mount and unmount drains the graph
- **WHEN** components and effects subscribing to a `Signal` are mounted and then all disposed
- **THEN** the signal reports zero listeners and zero observers

### Requirement: Event unregistration dual proof

Disposables registering Arc event listeners (`Hud` resize listener, `BaseComponent.listen`, `SolimDialog.listen`) SHALL unregister them on `dispose()`, proven both by firing the event post-dispose and asserting no reaction, and by registry inspection where accessible.

#### Scenario: Fired event causes no reaction after dispose
- **WHEN** a component with an event listener is disposed and the event is then fired
- **THEN** no listener body, signal update, or UI mutation occurs

### Requirement: Structural and config disposal chains

`StructuralReconciler` SHALL dispose removed-key components on reconcile and all active components on `dispose()`; `Dynamic`/`ForEach`/`ReactiveGrid`/`VirtualList`/`Popup` SHALL dispose their reconciler, current component(s), and item bindings via `onDispose()`; `TwoWayBinding` SHALL dispose both its `Effect` and its widget listener handle; `ConfigValue` SHALL dispose its signal subscription and `ContextualConfigValue` SHALL additionally dispose its discriminant subscription so post-dispose `set()` performs no persistence and discriminant changes cause no reload; `RoundedDrawable`, `feedback/Badge`, and `feedback/ProgressBar` SHALL dispose their inner effects so signal changes cause no mutation.

#### Scenario: Reconciler removes and clears
- **WHEN** keys are removed by a reconcile call and the reconciler is later disposed
- **THEN** removed components were disposed at removal time and all remaining components are disposed at reconciler disposal

#### Scenario: Two-way binding severs both directions
- **WHEN** a two-way-bound input component is disposed, the signal is then set, and the widget is then changed
- **THEN** the widget is not updated by the signal and the signal is not updated by the widget

#### Scenario: Config values go quiet after dispose
- **WHEN** a `ConfigValue` or `ContextualConfigValue` is disposed and its setter source or discriminant then changes
- **THEN** no persistence call occurs and no reload occurs

### Requirement: Three mounting modes

Component disposal SHALL be verified in three modes: standalone direct instantiation, nested inside a parent `BaseComponent.build()` context, and inside structural containers (`ForEach`, `ReactiveGrid`, `VirtualList`, `Dynamic`, `Popup`).

#### Scenario: Standalone disposal holds without a parent
- **WHEN** a component bound to a `Signal` is created directly, disposed, and the `Signal` changes
- **THEN** no UI mutation or effect run occurs

#### Scenario: Nested disposal holds exactly once
- **WHEN** a component is created inside a parent `BaseComponent.build()`, the parent is disposed, and the `Signal` changes
- **THEN** the child stops reacting and double disposal (child then parent, or parent twice) throws nothing

#### Scenario: Structural container disposes removed and cleared items
- **WHEN** items are removed from a structural container and the container is later disposed
- **THEN** removed item components were disposed at removal and remaining item components are disposed with the container
