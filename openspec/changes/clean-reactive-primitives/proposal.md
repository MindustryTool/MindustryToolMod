## Why

`Signal` and `Computed` carry two parallel notification paths — a reactive graph (`ReactiveObserver` set) and a raw callback list (`List<Cons<T>> listeners`) exposed via `.subscribe()`. The raw path bypasses the scheduler, runs inline during `set()`, and returns a `Subscription` the caller must manually dispose. This creates confusion about which path to use, introduces leak risk, and gives `Computed` surprising eager-push behavior when subscribers are present. Removing `.subscribe()` and the listener lists collapses the system to one clean reactive graph, eliminates the hybrid push/pull mode in `Computed`, and makes `Effect` the single answer for reacting to signal changes.

## What Changes

- **BREAKING** Remove `Signal.subscribe()` and `Signal.listeners` — all reactive reactions must go through `Effect`
- **BREAKING** Remove `Computed.subscribe()` and `Computed.listeners` — `Computed` becomes purely lazy pull
- **BREAKING** Remove eager recompute in `Computed.invalidate()` (the `!listeners.isEmpty()` branch) — `Computed` marks dirty and waits for next `get()`
- **BREAKING** Delete `Binding` class — its purpose is fully covered by `Effect.of(Readable, Cons)` which already auto-owns and does not double-register
- **BREAKING** Remove `Subscription` interface — no longer returned by any public API
- Add `Effect.of(Readable<T>, Cons<T>)` static overload as ergonomic replacement for `Binding.bind()`
- `Signal` and `Computed` retain `get()`, `peek()`, `map()`, `set()` / `update()` unchanged
- All other primitives (`MapSignal`, `Mutation`, `Query`, `TwoWayBinding`, `When`, `Dynamic`, `ForEach`, `QueryView`, `Signals`) are unaffected

## Capabilities

### New Capabilities

- `effect-binding-overload`: `Effect.of(Readable<T>, Cons<T>)` — a concise factory that creates an `Effect` tracking a single `Readable` and applying its value to a consumer, replacing all `Binding.*` call sites

### Modified Capabilities

- `solim-reactivity`: Remove the subscriber/listener mechanism from `Signal` and `Computed`; remove `Subscription` type; `Computed` is now always lazy; the reactive graph is the only notification path
- `typed-reactive-graph`: `Signal` and `Computed` no longer implement a dual notification model; the reactive-source contract (`ReactiveSource` / `ReactiveObserver`) remains the single wiring point

## Impact

- `solim-core`: `Signal.java`, `Computed.java`, `Binding.java`, `Subscription.java` changed or deleted; `Effect.java` gains one overload
- `solim-api`: `Subscription` interface deleted (was in `solim.reactive` but depended on `Disposable`)
- `:mod` module: any call sites using `.subscribe()` or `Binding.*` must migrate to `Effect.of()`; `TwoWayBinding` is unaffected (uses `Effect` internally already)
- No changes to `solim-api` contracts, `solim-runtime`, or any layout/overlay/style modules
