## MODIFIED Requirements

### Requirement: Computed with lazy recomputation and dynamic dependencies
`Computed<T>` SHALL be created via `Signal.computed(Supplier<T>)` or `signal.map(...)`, SHALL implement `solim.core.Disposable` so it can be passed to `own()` and `registerDisposable()` like `Effect` and `Subscription`, with automatic dependency tracking, lazy recomputation (recompute on `get()` after invalidation), dynamic dependency cleanup when source branch changes, cycle detection, public `isDisposed()` reporting `true` after disposal, and `Subscription subscribe(Consumer<T>)` + `void dispose()`.

#### Scenario: Lazy recomputation only on get
- **WHEN** `Computed<String> title = Signal.computed(() -> "Count: " + count.get())` and `count.set(10)` invalidates `title`
- **THEN** supplier does NOT re-execute until `title.get()` or a subscriber/effect reads it

#### Scenario: Dynamic dependencies cleanup
- **WHEN** `Computed<String> v = Signal.computed(() -> darkMode.get() ? username.get() : email.get())` and `darkMode` toggles from true to false
- **THEN** `v` unsubscribes from `username` and subscribes to `email`; subsequent `username.set(...)` does NOT invalidate `v` while `email.set(...)` does

#### Scenario: Cycle detection does not infinite loop
- **WHEN** a `Computed` directly or transitively reads itself during evaluation
- **THEN** implementation detects the cycle and throws or logs without stack overflow / infinite loop, and old dependencies are not leaked

#### Scenario: Invalidation propagates to dependent Computeds
- **WHEN** `Computed b = a.map(...)` and `Computed c = b.map(...)` chain and `a` changes
- **THEN** both `b` and `c` are marked invalid and recompute correctly on next `get()` in topological order

#### Scenario: Computed subscription and dispose
- **WHEN** `Subscription s = computed.subscribe(v -> render(v))` then `computed.dispose()` is called
- **THEN** `computed` unsubscribes from all dependencies, clears listeners, and no longer recomputes or notifies

#### Scenario: Computed is ownable as a Disposable
- **WHEN** a `Computed` is passed to `own()` or a dialog `registerDisposable()` inside a component build and the owner is disposed
- **THEN** the computed is disposed with the owner and `isDisposed()` returns `true`
