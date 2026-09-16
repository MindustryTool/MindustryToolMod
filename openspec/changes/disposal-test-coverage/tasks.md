## 1. Contract changes

- [x] 1.1 Promote `Computed` to `implements Disposable` with public `isDisposed()` returning true after dispose
- [x] 1.2 Add guarded idempotent `isDisposed` flags to leaf components missing them so all report true after dispose
- [x] 1.3 Verify existing lifecycle suites (`LifecycleCorrectnessTest`, `ComponentContractTest`) still pass after contract changes

## 2. Sweep harness

- [x] 2.1 Add uniform sweep covering every Disposable in `solim`/`solim-*`: owned resources released, double dispose no-op, `isDisposed()` true
- [x] 2.2 Cover three mounting modes in the sweep: standalone, nested in parent `BaseComponent.build()`, inside structural containers (`ForEach`, `ReactiveGrid`, `VirtualList`, `Dynamic`, `Popup`)
- [x] 2.3 Assert global exclusions hold: instance detachment verified without clearing `Signals` statics, `NetworkImage` cache, `RoundedCache`, or `StyleCache`

## 3. Reactive primitives

- [x] 3.1 Verify disposed `Effect` never re-runs on `Signal.set` plus flush and reports zero observer/dependency registrations
- [x] 3.2 Verify disposed `Subscription` (`Signal` and `Computed`) never fires again and is idempotent
- [x] 3.3 Verify `Signal` graph drains to zero listeners/observers after disposing all downstream subscriptions, effects, and computeds
- [x] 3.4 Verify disposed `Computed` unsubscribes, clears listeners, stops recomputing, and is ownable via `own()`/`registerDisposable()`

## 4. Structural helpers

- [x] 4.1 Verify `StructuralReconciler` disposes removed keys on reconcile and all active components on dispose
- [x] 4.2 Verify `TwoWayBinding` severs both directions (signal-to-widget and widget-to-signal) on dispose
- [x] 4.3 Verify `Dynamic`, `ForEach`, `ReactiveGrid`, `VirtualList`, and `Popup` dispose reconciler, current components, and item bindings on removal and on owner dispose

## 5. Risky components deep tests

- [x] 5.1 Deep test `Tabs`: tab button effects, header bar, and content effects disposed
- [x] 5.2 Deep test `NetworkImage`: url-binding effect disposed on re-url and on dispose (async late-callback guard excluded per design)
- [x] 5.3 Deep test `Hud`: resize listener unregistered (fired-event plus registry proof), unfocus called, bindings cleared, flag set
- [x] 5.4 Deep test `SolimDialog`: hide called once, event listeners and registered disposables cleared with error isolation, flag set

## 6. Config, graphics, and feedback

- [x] 6.1 Verify `ConfigValue` dispose stops persistence on later `set()`; `ContextualConfigValue` additionally stops discriminant reloads
- [x] 6.2 Verify `RoundedDrawable` disposes radius/fill/border/stroke effects so signal changes cause no mutation
- [x] 6.3 Verify `feedback/Badge` and `feedback/ProgressBar` dispose inner effects including never-bound and double-dispose cases

## 7. Final verification

- [x] 7.1 Run full `solim-core` and `solim-runtime` suites with no regressions and confirm repeated mount/unmount leaves zero accumulated observers/listeners
