## Context

Solim splits disposal across two component families (`BaseComponent` with LIFO `own()` list, `isDisposed` flag, and `element()`-throws semantics vs. leaf `Component`s with manual `bindings` lists and no flag), plus non-component `Disposable`s (`Effect`, `Subscription`, `StructuralReconciler`, `TwoWayBinding`, `ConfigValue`/`ContextualConfigValue`, `RoundedDrawable`, `feedback/Badge`, `feedback/ProgressBar`, `Hud`, `SolimDialog`). Two contract anomalies were confirmed during exploration: `Computed` exposes `dispose()` without implementing `Disposable` (so it cannot be owned via `own()`/`registerDisposable()`), and most leaf components inherit `isDisposed() == false` even after `dispose()`. Static singletons (`Signals` event registrations/`Timer`, `NetworkImage` cache, `RoundedCache`, `StyleCache`) are never disposed by design. Current tests (`LifecycleCorrectnessTest`, `ComponentContractTest`, `StructuralReactivityTest`) cover the framework core but not the per-type sweep.

Stakeholders: Solim framework maintainers; mod UI authors relying on navigation without leaks.

## Goals / Non-Goals

**Goals:**

- Verify every `Disposable` in `solim` and `solim-*` packages releases what it owns.
- Promote `Computed` to `Disposable` and require `isDisposed() == true` after `dispose()` on all types.
- Cover three mounting modes for components: standalone, nested in a parent `BaseComponent.build()`, and inside structural containers (`ForEach`, `ReactiveGrid`, `VirtualList`, `Dynamic`, `Popup`).
- Prove `Effect` death both behaviorally (no re-run on `Signal.set` + flush) and structurally (observer/dependency counts reach zero).
- Prove event unregistration both behaviorally (fire event post-dispose, assert no reaction) and by inspection where the registry allows.
- Prove `Signal` graphs drain: after disposing everything downstream, `listenerCount`/`observerCount` return to zero.
- Keep global/static state explicitly excluded: assert instance detachment only, never static clearing.
- Colocate tests by module (`solim-core` for components/config/graphics/feedback, `solim-runtime` for reconciler).

**Non-Goals:**

- No late-callback async guard for `NetworkImage` (fake-`ImageLoader` late-`onSuccess` assertion is out of scope; only binding disposal is verified).
- No disposal of global singletons or static caches.
- No production UI behavior changes beyond the two contract changes (`Computed implements Disposable`, uniform `isDisposed()`).
- No virtual-DOM, global diffing, or full-render reconciliation changes.

## Decisions

### Decision: Hybrid sweep plus deep dives for risky types only

Uniform contract sweep over all types for the shared predicates, plus bespoke deep tests only for the risky subset (`ReactiveGrid`, `VirtualList`, `Tabs`, `NetworkImage`, `Hud`, `SolimDialog`, `ConfigValue`/`ContextualConfigValue`, with `Dynamic`/`ForEach` as containers).

- Alternatives considered: sweep only (rejected: misses bespoke resources like dialog `hide()`, resize listener, discriminant subscription); deep test for every type (rejected: maintenance weight without proportional value).
- Rationale: this is the confirmed strategy from proposal review.

### Decision: Promote `Computed` to `implements Disposable`

Add `implements Disposable` to `Computed` (its `dispose()` body already exists) and override `isDisposed()` publicly so it reports `true` after dispose.

- Alternatives considered: keep as-is with a separate test contract (rejected); leave undecided for implementation (rejected).
- Rationale: confirmed contract change; makes `Computed` ownable via `own()`/`registerDisposable()` like `Effect` and `Subscription`.

### Decision: Uniform `isDisposed() == true` after dispose

Every `Disposable` type, including leaf components, must track and report disposal. Leaf types add a guarded flag (idempotent `dispose()`, matching `Hud`/`SolimDialog` precedent); `Computed.isDisposed()` becomes public.

- Alternatives considered: document current behavior with no new requirement (rejected); leave open (rejected).
- Rationale: confirmed requirement; makes the sweep predicate uniform.

### Decision: Three mounting modes including all structural containers

Standalone, nested-in-`BaseComponent`, and inside each structural container type (`ForEach`, `ReactiveGrid`, `VirtualList`, `Dynamic`, `Popup`). Plain Java loops mounting children are covered implicitly wherever a container test builds items in a loop; no separate loop harness.

- Alternatives considered: only `ForEach`/`ReactiveGrid` (rejected); nested-only or standalone-only (rejected).
- Rationale: confirmed mode matrix; catches ambient-ownership gaps (e.g. effects registered with parent but missing from local `dispose()`).

### Decision: Dual proof for effects and events; drain proof for signals

- `Effect`: assert no re-run on `Signal.set` + dispatcher flush AND observer/dependency counts at zero.
- Events (`Hud` resize, `BaseComponent.listen`, `SolimDialog.listen`): fire the event post-dispose and assert no reaction AND inspect the registry where accessible.
- `Signal`: no `dispose()` on `Signal` itself; assert downstream drain (`listenerCount`/`observerCount` zero) after disposing subscriptions, effects, and computeds.
- Alternatives considered: behavioral-only or counts-only / fire-only or inspect-only / skip-`Signal` (all rejected during review).
- Rationale: confirmed proof standards; behavioral proof guards user-visible leaks, counts guard silent observer accumulation.

### Decision: Exclude globals, colocate by module

`Signals` static registrations/`Timer`, `NetworkImage` static cache, `RoundedCache`, `StyleCache` are documented exclusions. Tests live next to their modules (`solim-core` vs `solim-runtime`).

- Alternatives considered: include globals (rejected); single disposal package (rejected).
- Rationale: confirmed scope and location answers.

## Risks / Trade-offs

- [Risk] Adding `isDisposed` flags to ~20 leaf types touches many files → Mitigation: mechanical guarded-flag pattern copied from `Hud`/`SolimDialog`; sweep test pins it once.
- [Risk] Promoting `Computed` to `Disposable` may break callers that define conflicting overloads or assume non-disposability → Mitigation: breaking change is declared in proposal; method body already exists so behavioral delta is interface-only plus public `isDisposed()`.
- [Risk] Skipping the `NetworkImage` late-callback guard leaves a known async hole → Mitigation: explicitly recorded as non-goal; binding-disposal test still covers the `Effect` slot; a follow-up change can add the fake-loader test.
- [Risk] `Signal` drain assertions depend on package-visible count helpers (`listenerCount()`/`observerCount()` are package-private today) → Mitigation: tests for counts live in matching packages (`solim.signal`, `solim.runtime`) where helpers are visible; no production API widening required unless implementation prefers it.
- [Risk] Structural-container matrix multiplies test count (types × 3 modes) → Mitigation: sweep harness parameterizes modes; deep tests only for the risky subset.
