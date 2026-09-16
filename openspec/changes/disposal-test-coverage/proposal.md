## Why

Disposal leaks are suspected across Solim components, signals, effects, and helpers, but there is no systematic verification that every `Disposable` in `solim` and `solim-*` packages actually releases everything it owns.

## What Changes

- Add systematic disposal verification covering every `Disposable` in `solim` and `solim-*` packages: `BaseComponent` family, leaf `Component` family, overlays (`Hud`, `SolimDialog`, `Popup`), reactive primitives (`Effect`, `Subscription`, `Computed`, `Signal` graph), structural helpers (`StructuralReconciler`, `TwoWayBinding`, `Binding`), config (`ConfigValue`, `ContextualConfigValue`), graphics (`RoundedDrawable`), and feedback non-components (`feedback/Badge`, `feedback/ProgressBar`).
- **BREAKING**: Promote `Computed` to implement `Disposable` so it can be passed to `own()` / `registerDisposable()` like every other disposable reactive primitive.
- **BREAKING**: Require `isDisposed()` to return `true` after `dispose()` for all `Disposable` types, including leaf components that currently inherit the default `false`.
- Verify each disposable in three mounting modes: standalone (direct instantiation), nested inside a parent `BaseComponent.build()` context, and nested inside structural containers (`ForEach`, `ReactiveGrid`, and equivalent keyed/structural parents).
- Use a hybrid strategy: a uniform contract sweep over all types plus focused deep tests for risky types (`ReactiveGrid`, `VirtualList`, `Tabs`, `NetworkImage`, `Hud`, `SolimDialog`, config values).
- Explicitly exclude global singletons from disposal assertions (`Signals` static events/`Timer`, `NetworkImage` static cache, `RoundedCache`, `StyleCache`); tests assert only instance detachment, never static clearing.

## Capabilities

### New Capabilities

- `disposal-verification`: systematic disposal test contracts for every `Disposable` in `solim` and `solim-*` packages across standalone, nested, and structural-container modes, with per-family predicates (effects dead, subscriptions removed, children disposed, events unregistered, idempotent).

### Modified Capabilities

- `solim-lifecycle`: `isDisposed()` contract changes — all components/disposables must report `true` after `dispose()` instead of only `Hud`/`SolimDialog`/`Effect`/`Subscription` tracking it.
- `solim-reactivity`: `Computed` contract changes — `Computed` becomes a `Disposable` (`implements Disposable`) with ownership/disposal semantics consistent with `Effect` and `Subscription`.

## Impact

- Affected code: every `Disposable` in `solim`, `solim-api`, `solim-core`, `solim-runtime`, and related `solim-*` modules; `Computed`, leaf-component `isDisposed()` implementations.
- APIs: `Computed implements Disposable` and uniform `isDisposed() == true` after dispose are breaking contract changes for callers implementing or checking these types.
- Systems: test-only addition plus two contract changes; no runtime feature change beyond making disposal observable and uniform. Global/static caches and signals explicitly untouched.
