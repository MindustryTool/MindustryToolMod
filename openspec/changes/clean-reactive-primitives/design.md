## Context

`Signal` and `Computed` each carry a `List<Cons<T>> listeners` field alongside the `Set<ReactiveObserver> observers` field. The listener list is exposed as a `.subscribe(Cons<T>)` method returning a `Subscription` that callers must manually dispose. This creates two competing notification paths:

- **Listeners** — raw callbacks fired synchronously inside `Signal.set()`, before the reactive graph is notified. Callers hold a `Subscription` and must dispose it manually to prevent leaks.
- **Observers** — the reactive graph (Effects and Computeds) scheduled through `SignalDispatcher` and notified asynchronously on the next game frame.

The listener path also introduces hidden push behavior in `Computed`: when `listeners` is non-empty and `invalidate()` is called, `Computed` immediately calls `recompute()` inline, converting a lazy pull type into an eager push one with no API-visible signal.

`Binding` is a static utility that wraps `Effect.of()` but also calls `OwnershipContext.register()` a second time, causing the returned `Effect` to be adopted twice by the ambient owner.

## Goals / Non-Goals

**Goals:**
- One notification path: the reactive graph via `ReactiveObserver`
- `Computed` is always and only lazy pull — dirty-on-invalidate, recompute-on-get
- `Signal` and `Computed` have no mutable listener lists — no `subscribe()`, no `Subscription`
- `Effect` is the single API for reacting to signal changes, with full scheduler integration and auto-ownership
- `Binding` class is deleted; `Effect.of(Readable<T>, Cons<T>)` is the ergonomic replacement
- Zero change to `solim-api` contracts (`ReactiveSource`, `ReactiveObserver`, `SchedulableEffect`, `Component`, `OwnershipContext`)
- Zero change to `solim-runtime` internals
- Zero change to layout, overlay, style, input, or display modules

**Non-Goals:**
- Changing `When`, `Dynamic`, `ForEach`, `QueryView`, `MapSignal`, `Mutation`, `Query`, `TwoWayBinding`, `Signals`
- Changing the `StructuralReconciler` or `AttachmentStack`
- Adding synchronous reactive execution (all effects remain scheduled)
- Providing migration shims or deprecation period — callers migrate directly

## Decisions

### Decision: Delete `Subscription` and `.subscribe()` entirely (no deprecation)

**Chosen**: Hard removal.

**Rationale**: `Subscription` has no use once `listeners` is gone. A deprecation period would keep dead code alive, force `Subscription` to remain compilable, and invite new call sites. The migration is mechanical — every `.subscribe(λ)` becomes `Effect.of(() → λ.get(signal.get()))` — and the build fails loudly on the first attempt, guiding the developer to the fix immediately.

**Alternatives considered**:
- Deprecate and remove in a later change — rejected because there is no external API surface and the change is fully within the monorepo.

---

### Decision: `Effect.of(Readable<T>, Cons<T>)` replaces `Binding.bind()`

**Chosen**: New static overload on `Effect`.

**Rationale**: `Binding.bind(source, target)` was always `Effect.of(() → target.get(source.get()))` plus a redundant second `OwnershipContext.register()` call. The new overload lives where the behavior actually runs, removes the double-registration bug, and is naturally discovered by developers already familiar with `Effect.of(Runnable)`.

Arc-specific binding helpers (`bindText`, `bindColor`, `bindVisible`, `bindWidth`, `bindHeight`, `bindDisabled`) that lived on `Binding` are deleted. Their call sites in `:mod` are replaced with the component-level modifier equivalents (`text(signal)`, `color(signal)`, `visible(signal)`, etc.) which already wrap an Effect internally. Where no component modifier exists, developers use `Effect.of(source, target)` directly.

**Alternatives considered**:
- Keep `Binding` as a static alias — rejected because it keeps the double-registration bug and a redundant class alive.
- Move Arc helpers to `ElementConfig` — rejected because most call sites already use component modifiers; the few remaining migrate to `Effect.of(source, target)`.

---

### Decision: `Computed.invalidate()` never calls `recompute()` eagerly

**Chosen**: `invalidate()` only sets `dirty = true` and propagates to downstream observers.

**Rationale**: `Computed` is a pull type. The eager path existed solely to serve `listeners` subscribers. Without listeners, there is no reason to recompute eagerly — downstream `Effect`s will read the `Computed` during their scheduled run, triggering a lazy recompute at that point. This restores a single, predictable `Computed` lifecycle: dirty → queried → recomputed.

**Alternatives considered**:
- Keep eager path for performance — rejected because it was never faster; it caused wasted recomputes when the value was overwritten again before anyone read it, and it complicated the execution model.

---

### Decision: `Signal` listeners list removed entirely — not replaced

**Chosen**: Remove `List<Cons<T>> listeners`, `subscribe()`, `listenerCount()` from `Signal`.

**Rationale**: The listeners list fired synchronously mid-`set()`, before the reactive graph propagated. This was a footgun — side effects running while a signal was mid-mutation, with no scheduler protection, no deduplication, and no main-thread assertion beyond the one on `set()`. `Effect.of()` is strictly better: batched, deduplicated, scheduled, auto-owned.

## Risks / Trade-offs

- **[Risk] Synchronous reaction eliminated** → Some callers may have relied on the inline-during-set timing of `subscribe()`. After migration, reactions run on the next game frame via `SignalDispatcher`. In practice this is the correct behavior (no mid-mutation side effects), but any code that assumed inline execution must be reviewed. Mitigation: review all call sites in `:mod` before implementing.

- **[Risk] `TwoWayBinding` uses `Effect` internally** → `TwoWayBinding` is unaffected; it never used `subscribe()`. No risk here.

- **[Risk] `Computed` eager-push callers** → If any code subscribed to a `Computed` expecting synchronous propagation, the delay until the next flush will be observable. Mitigation: all such call sites in `:mod` should be using `Effect` already; verify during migration.

## Migration Plan

1. Audit `:mod` for all `.subscribe()` call sites on `Signal` and `Computed`.
2. Audit all `Binding.*` call sites.
3. For each `.subscribe(λ)` → replace with `Effect.of(() → λ.get(signal.get()))` inside the relevant component `build()` (auto-owned) or as an explicit `Effect` field (manual dispose).
4. For each `Binding.bind(source, target)` → replace with `Effect.of(source, target)`.
5. For each `Binding.bindText/bindColor/...` → replace with component modifier `text(signal)` / `color(signal)` / etc., or `Effect.of(source, element::setter)` where no modifier exists.
6. Delete `Binding.java`, `Subscription.java` after all call sites are migrated.
7. Remove `listeners`, `subscribe()`, `listenerCount()` from `Signal` and `Computed`.
8. Remove eager recompute branch from `Computed.invalidate()`.
9. Add `Effect.of(Readable<T>, Cons<T>)` overload.
10. Run full test suite.
