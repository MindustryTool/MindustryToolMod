## Why

A source-level audit of `solim-api`, `solim-runtime`, and `solim-core/reactive` surfaced 29 defects and structural problems, grouped into six independent themes:

1. **Correctness** — `CacheEntry.hasData()` cannot distinguish a cached `null` from "never fetched", so legitimately-empty `Query` results become permanently sticky. `ForEach.children(...)` after `build()` is a silent no-op. `ElementResolver.resolve()` unconditionally re-parents a component, orphaning its Arc `Element` in its old `Table`. `applyDefaultName` lowercases without `Locale.ROOT`, breaking every `Element.name` under a Turkish locale and silently disabling the whole MCP debug surface. `Component.children()`/`disposables()` return live views while `SessionRoot` returns copies, giving the same accessor two different contracts.

2. **Liveness** — `Effect.pending` latches to `true` inside a re-entrant `runEffect()` without ever enqueueing, after which `invalidate()` is a permanent no-op: the effect is dead but still subscribed to every dependency, holding hard references. `Computed.peek()` has no disposed guard, so reading a disposed `Computed` re-executes its supplier against a torn-down dependency set. `OwnershipContext.pop()` and `ReactiveContext.pop()` do not verify LIFO identity, so one unbalanced ambient frame silently corrupts ownership for every subsequent registration.

3. **Ownership non-determinism** — `ModifierOwnership` decides ownership by inspecting the *ambient* stack (`if (OwnershipContext.current() == null)`). The lifetime of a `.width(Readable)` effect therefore depends on where the call site happens to be — parent-scoped, self-scoped, or effect-scoped. It exists only because `Component.adopt` has no cheap identity-based idempotence check, and its 33 call sites are why the `OwnershipScope` protocol has been hand-rolled six times with six different subsets of protections.

4. **Instrumentation coupled to logic** — debug reach into normal classes is measured, not theoretical: **16** `instanceof Debug*` casts in production code (`Computed`, `Effect`, `Perf`, `ComponentSnapshotBuilder`), **43** no-op `do*()`/`on*()` hooks existing purely so a subclass can override them (`AttachmentStack` 31, `ReactiveContext` 12), **3** `volatile INSTANCE` service locators, and ~10 hand-written metric accessors (`getLastNewCount`, `getMountedCount`, `getLastHideTime`, `maxDepthObserved`, …). Every new metric costs an edit to a normal logic class. The root cause is that push-shaped telemetry (timings, counts, subtree depths) is being answered with pull-shaped getters, which cannot work.

5. **Performance** — `AttachmentStack.doIsolate` allocates and copies the entire stack per call on the hottest factory path, and reinstates stack residue on exit; `ReactiveContext.doUntracked` — solving the identical problem in the adjacent class — correctly truncates in O(pushed) and discards residue. `Signal.set` copies its observer set on every write.

6. **Drift** — `docs/solim_internals.md` (690 lines) documents `BaseComponent`, `OwnershipContext.withoutAutoOwnership()`, `OwnershipContext.paused`, `addDependency(Object)`, and `Ui.isExpanding(...)`: none of which exist. `AGENTS.md` asserts `BaseComponent.own()` is package-private; it is `public final` and used by 27 framework files. `net-signals` requires a "poll backstop" that the code is being asked to remove.

## What Changes

### NEW — Pluggable instrumentation SPI (`solim-instrumentation`)

- **BREAKING** Add `solim-api` SPI: `Instrumentation`, `InstrumentationSink`, and a semantic event hierarchy. Producers `emit` one guarded line per *moment*; sinks own all logic.
- **BREAKING** Delete `DebugAttachmentStack`, `DebugReactiveContext`, `DebugSignalDispatcher` and the 43 no-op hooks they override.
- **BREAKING** Delete the `AttachmentStack`/`ReactiveContext`/`SignalDispatcher` `volatile INSTANCE` + `install(...)` service locators. `solim-core/performance` subscribes to events instead of replacing runtime instances.
- **BREAKING** Delete hand-written metric accessors (`getLastNew/Reuse/TotalCount`, `getMountedCount`, `getLastHideTime`, `maxDepthObserved`, `totalRecorded`, `isTracing`); sinks hold the counters.
- Retain `DebugReactiveNode` as the **pull** path for live object graphs (dependency graph, ownership tree) — pull and push are different shapes and stay different mechanisms.
- Profiler implementation stays in `solim-core/performance` under package separation; no new Gradle module.
- Disabled cost is one `static volatile boolean` read, constant-folded by the JIT.

### Batch 1 — Correctness (A1, A2, A3, A4, A6, A7, B6)

- `CacheEntry` gains a distinct `fetched` flag so a cached `null` is not read as "never fetched".
- **BREAKING** `ForEach.children(...)` reflows immediately when already built, matching `ReactiveGrid`.
- `ElementConfig.tooltip(Readable)` evaluates its source once per run.
- **BREAKING** Add `Component.reparentTo(@Nullable DisposableOwner)`: detach-then-adopt, removing the `Element` from its live parent `Table` before moving ownership, fail-fast on Arc conflict. `ElementResolver.resolve()` routes through it.
- `applyDefaultName` uses `Locale.ROOT`.
- `Component.children()`/`disposables()` return snapshots, matching `SessionRoot`.
- `QueryCache.attachObserver` dedupes, so a repeated attach cannot leak the refcount and block GC.

### Batch 2 — Liveness (B1, B2, B3, B4, B5, B7)

- `Effect.pending` becomes a state machine so a re-entrant `runEffect` re-enqueues instead of latching to death.
- `Computed.peek()` gains a disposed guard; lazy recompute on a live `Computed` is preserved.
- **BREAKING** `QueryView` reconfiguration routes through `Effect.invalidate()` instead of calling `runPending()` directly, so the dispatcher owns all `updateView` execution.
- `OwnershipContext.pop()` / `ReactiveContext.pop()` verify LIFO identity in debug builds.
- **BREAKING** `UI.init()` sets the main thread explicitly instead of `SignalDispatcher.register()` inferring it from whichever thread first creates an `Effect`. Drops the redundant `Thread.dumpStack()`.
- **BREAKING** `Query` with a dynamic `key(Prov)` re-attaches its `QueryCache` observer when the resolved key changes.

### Batch 3 — Ownership consolidation (C3 → D1 → D3, delivers A5)

- **BREAKING** `Component.adopt` gains an identity-based index, making double-adoption harmless.
- **BREAKING** Delete `ModifierOwnership` and all 33 call sites; modifier effects are owned by the component they configure. Completes Decision 2 of `2026-10-13-cleanup-solim-ownership`, which was never executed.
- Add a shared `OwnershipScope.build(Prov<Component>)` helper in `solim-runtime`; collapse the six hand-rolled copies of the protocol (`StructuralReconciler`, `Effect`, `Fragment.capture`, `QueryView`, `Popup.render`, `SolimStack.layer`).
- `Tabs` tab-content mounting and `ReactiveGrid.empty()` gain an explicit ownership scope instead of depending on `Effect.runEffect` happening to have one open.

### Batch 4 — Performance (C1, C2, C4, C5)

- **BREAKING** `AttachmentStack.doIsolate` truncates to the entry depth in O(pushed) and **discards** residue, matching `ReactiveContext.doUntracked` and the `net-signals`-adjacent `untracked` contract already in `solim-reactivity`. Adds the missing `SolimAssert.checkMainThread()`.
- `Signal.set` iterates an immutable observer snapshot maintained copy-on-write, eliminating the per-write `LinkedHashSet` allocation.
- `PendingCellConfig.applyToCell` reads `minWidth` untracked, removing a spurious dependency on the mounting `Effect`.
- **BREAKING** `ReactiveGrid.columns(...)` drops its eager reflow; the `Computed` drives a single tracked reflow.

### Batch 5 — Module & docs hygiene (D5, D6, D7, D8, E1, E2, E3)

- Move `solim.performance` DTOs out of `solim-api` into the new SPI package / `solim-runtime`.
- **BREAKING** Delete the `Signals` 1 Hz `Timer.schedule` poll; net state becomes event-driven with an explicit `Signals.requestRefresh()` escape hatch. Delta the `net-signals` "Event-plus-poll update driver" requirement, which currently mandates the poll.
- Add `loading`, `button.retry`, and `query.error.default` bundle keys; remove the hardcoded `"An error occurred"` from `QueryView`.
- Rename the duplicate `solim/feedback/Badge` to disambiguate from `solim/display/Badge`.
- Regenerate `docs/solim_internals.md` from HEAD; correct `AGENTS.md`'s `BaseComponent` and `own()` claims; delete the empty `QueryViewTest.java`.

### Batch 6 — Audit hardening appendix (decided 2026-10-13)

All items below were open audit gaps against `solim-fixes` HEAD and were decided one-by-one via `opsx-question`:

- `DisposableAction.EMPTY` becomes stateless (`isDisposed()` constant, `dispose()` noop without flag).
- `Component.dispose()` wraps `onDispose()` as `firstFailure` and always continues through detach + children + disposables; later failures use `addSuppressed`.
- `Component.element()` applies `SpacingAware` on fresh build and cached access; `applySpacing()` is required idempotent.
- Thread model is main-confined: `checkMainThread` on all `Component`/`OwnershipContext`/`OwnershipScope` mutators and readers, `disposed` flags `volatile`, `SessionRoot` de-synced to match; `SolimAssert` null-opt-out documented as test-only.
- `AttachmentBridge` hardens now as a composite hook list with exception aggregation (latent defect, single installer today).
- `SignalDispatcher` dedups the queue (`LinkedHashSet` or pending-flag, FIFO preserved).
- `OwnershipScope` pool is guarded by main-thread asserts plus pop-identity verification; oversize `ArrayList` dropped instead of retained.
- `Effect` disposes/transfers prior-run `owned` at each rerun start (per-run scope).
- `Query` retry backoff saturates the shift (~10) and caps the delay.
- `QueryCache` uses compare-and-remove (`remove(key,entry)`) and creates the fetch future outside `synchronized(entry)` with double-checked inflight.
- `Query` lifetime: clear/set `loading` on disable, `mutate` uses `keyForFetch()`, `fetchedKeys` bounded/evicted, supplier validated before attach.
- `CacheEntry` gains a generation token ignoring post-`clear` completions; staleness uses a monotonic clock.
- Scheduler is shared/injectable across `QueryCache`/`Query` with documented TTL/cap.
- Collapse behavior is centralized (save/restore parent cell, gate `respace` to gap parents, `peek()` in refresh).
- `Mutation` documents last-registration-wins for typed context, cancels prior future when cancellable, clears terminal signals on dispose.
- Snapshot DTOs defensively copy arrays/lists and wrap unmodifiable.
- Nullability annotation pass over `solim-api` interfaces, `register` returns, and snapshot fields.
- DTO unknown-string constants and clamp policy unified.
- `ElementResolver` error reports safe class-name plus truncated preview with a single null policy.
- `TwoWayBinding` guards both directions, null-checks ctor args, requires immutable values or custom equality.
- Error fidelity: preserve wrapper as suppressed, skip retry on cancellation, route headless completions via scheduler.

## Capabilities

### New Capabilities

- `solim-instrumentation`: a push/pull-separated observability SPI in `solim-api`. Normal logic classes emit one guarded event per moment and reference no debug type; all counters, histograms, flame spans, and tree assembly live in pluggable sinks.

### Modified Capabilities

- `solim-reactivity`: `Effect` pending-state machine; `Computed.peek()` disposed guard; `Signal.set` copy-on-write observer snapshot; removal of stale `Subscription`/`Binding` scenario text left by the spec-domain merge.
- `solim-debug-runtime`: debug services become `InstrumentationSink` subscribers; `Debug*` subclasses and `install(...)` service locators deleted.
- `solim-flame-trace`, `solim-profile-report`, `solim-ui-perf-testing`: consume sink events instead of casting runtime instances.
- `net-signals`: event-driven refresh with an explicit trigger replaces the poll backstop.
- `solim-lifecycle`, `disposable-contract`, `disposal-verification`: snapshot accessor semantics; LIFO-verified `pop()`.
- `thread-safety-assertions`: explicit main-thread binding at `UI.init()`; `Thread.dumpStack()` removed.
- `solim-ownership-tree`, `solim-component-ownership`: identity-based adoption, deterministic modifier ownership, shared `OwnershipScope.build`, `Component.reparentTo`.
- `solim-layout`: `ForEach` post-build reflow; `AttachmentStack.isolate` truncation; untracked `PendingCellConfig` read; single-reflow `ReactiveGrid.columns`.
- `query-cache`: `fetched` flag; observer attach dedupe.
- `query-primitive`: dynamic-key cache-observer rebinding.
- `query-view`: dispatcher-owned reconfiguration; translated default strings.
- `solim-widgets`: single-read tooltip binding; `Badge` rename.
- `ambient-naming`: locale-stable default element names.

## Impact

- **`solim-api`**: gains `Instrumentation`/`InstrumentationSink`/events; loses `solim.performance` DTOs; `Component` gains `reparentTo`; `pop()` gains an identity check; `SolimAssert` gains explicit thread binding.
- **`solim-runtime`**: `AttachmentStack` loses 31 hooks and the `install` service locator; `ReactiveContext` loses 12 hooks and its service locator; `SignalDispatcher` loses its service locator and keybind polling; `OwnershipScope` gains `build(...)`.
- **`solim-core`**: `Computed`/`Effect` lose 4 `instanceof DebugReactiveContext` casts; `solim-core/performance` rewritten as sinks; `ModifierOwnership` deleted (33 sites); `ForEach`/`ReactiveGrid`/`Tabs`/`Popup`/`SolimStack` adopt the shared helper; `Query`/`CacheEntry`/`QueryCache`/`QueryView`/`Mutation` fixes; `solim/feedback/Badge` renamed.
- **`:mod`**: no API migration required. `Perf.init()` (`Main.java:74`, the sole call site) is unchanged. `Signal.subscribe` was removed by `clean-reactive-primitives`; nothing left to migrate.
- **`:solim-mcp`**: unaffected — `ObjectGraphScanner`/`SignalIntrospector` reflect over public reactive types and reference no `Debug*` class. `solim-mcp` is declared as `implementation(project(":solim-runtime"))`, so the removed `install(...)` surface was never on its compile path in a meaningful way; this is verified in task 1.6.
- **Behaviour changes requiring test review**: `DisposalSweepTest`, `LifecycleCorrectnessTest`, `ComponentOwnershipContractTest` (modifier ownership relocation), `AttachmentStackTest`, `AttachmentStackCaptureTest` (isolate residue discard), `ReactivityFailFastTest` (B1/B3), `QueryViewTest`, `QueryCacheTest`.

## Non-Goals

- **`clean-reactive-primitives` overlap.** Archived to `2026-10-13-clean-reactive-primitives` and fully landed: `Signal.subscribe`, `Computed.subscribe`, `Subscription`, and `Binding` are deleted, `mod` has zero remaining call sites, and its deltas are applied to `solim-reactivity` / `typed-reactive-graph` / `effect-binding-overload`. Findings that that change resolved are **not** repeated here: the unowned-subscription leak (former D2) and the `Signal.set` listener-list copy. `Signal.set` still copies its observer set, so C2 survives in reduced form.
- **New Gradle module for the profiler.** Explicitly chosen against; package separation within `solim-core` is sufficient for the required separation.
- **Making `peek()` a pure cache read.** Rejected; `Computed` keeps lazy recompute on `peek()` with only a disposed guard added.
- **Orphan-only adoption in `ElementResolver`.** Rejected in favour of an explicit `reparentTo`.
- **Making `AttachmentStack` / `ReactiveContext` / `SignalDispatcher` classes `final`.** Out of scope; subclass removal is achieved by deleting the subclasses, and remaining `protected` surface is left for a separate change.
- **Rewriting `docs/solim_internals.md` to a different format.** Scope is limited to correcting it against HEAD.
- **`QueryView` defaults/dedup, `ForEach` reorder, `MapSignal` eviction, `Duration` overloads.** Explicitly deferred in the 2026-10-13 question pass (kept as-is): `QueryView` mutable defaults plus O(n) `equals` dedup, `ForEach` clear-rebuild versus reorder, `MapSignal` cache pin plus copy-per-`get`, and `long-ms` overloads alongside `Duration`.