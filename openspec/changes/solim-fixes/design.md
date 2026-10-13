## Context

Solim is a retained-mode, single-threaded declarative UI framework over Arc Scene2D. Three ambient static contexts carry all of its non-local behaviour:

```
OwnershipContext   Deque<DisposableOwner>   who owns what
AttachmentStack    Deque<Entry>             which Table a new child attaches to
ReactiveContext    Deque<ReactiveObserver>  who is collecting .get() dependencies
```

This design is sound and is not what this change revisits. What this change addresses is the code that accumulated around those three stacks over roughly three weeks of ownership unification (2026-09-27 through 2026-10-13), which left four distinct categories of debt. The audit behind this proposal was performed by reading `solim-api`, `solim-runtime`, and `solim-core/reactive` end to end and cross-referencing the archived changes `solim-reactivity-hardening`, `solim-ownership-unification`, and `cleanup-solim-ownership`.

---

## Decision 1 — Push and pull telemetry stay separate mechanisms

**The problem.** Debug reach into normal classes is currently: 16 `instanceof Debug*` casts in production code, 43 no-op `do*()`/`on*()` hooks existing only so a subclass can override them, 3 `volatile INSTANCE` service locators, and ~10 hand-written metric accessors.

**The diagnosis.** Two incompatible shapes of telemetry are being forced through one mechanism:

```
PUSH-shaped  reconciler new-vs-reuse counts, computed run duration,
             attachment subtree depth, effect run counts, disposal sizes
             → happen AT A MOMENT; cannot be queried afterwards
             → getLastNewCount() is the symptom of asking a push question
               with a pull-shaped getter

PULL-shaped  dependency graph, ownership tree, component tree, live values
             → queryable properties of a live object
             → DebugReactiveNode is already the right shape for this
```

Unifying them is what produced the no-op hooks. Making a class "expose every metric" by writing accessor methods cannot work for push-shaped data, because the datum no longer exists by the time you would read it.

**The decision.** Two mechanisms, each doing what it is good at:

- Push → `InstrumentationSink`. Producers `emit` one guarded line per moment; the sink owns every counter, histogram and flame span.
- Pull → `DebugReactiveNode`, retained unchanged.

**Consequence.** `Computed`/`Effect` no longer reference `DebugReactiveContext`. `AttachmentStack`/`ReactiveContext`/`SignalDispatcher` no longer have subclasses or service locators. `solim-core/performance` becomes a subscriber rather than a substituter.

**Rejected.** A single unified mechanism (either push-only or pull-only). Push-only cannot serve live graph walks, which `solim-mcp` and `ProfileReportExporter` both need. Pull-only is the status quo and is what generated the defect.

---

## Decision 2 — Zero-cost-disabled emission via a constant-folded boolean

```java
private static volatile boolean enabled = false;

public static boolean enabled() { return enabled; }

// at the call site
if (Instrumentation.enabled()) Instrumentation.emit(new Computed.Run(this, ns, changed));
```

**Rationale.** When no sink is installed `enabled` is provably constant `false`, so the JIT eliminates the branch *and* the allocation. This is why instrumentation may be sprinkled directly on hot paths (`Signal.set`, `Computed.recompute`, `Effect.runEffect`) without any per-call-site gating logic, which is precisely what the `Perf.isEnabled()` style checks scattered today cannot do.

**Tradeoff.** Event objects allocate per emission when profiling *is* active. Accepted: profiling is already orders of magnitude slower than the code it measures, and correctness of the recorded value matters more than the allocation.

**Alternative rejected.** Version-stamped mutable event objects to avoid allocation. Saves little, complicates every emit site, and risks cross-thread reuse bugs.

---

## Decision 3 — Profiler stays in `solim-core/performance` (package separation, no new module)

**Rationale.** `solim-mcp` is declared `implementation(project(":solim-core"))`, so a new `:solim-profiler` module that `solim-core` does not depend on cannot be reached from `solim-core/performance` — the profiler must live somewhere `solim-core` can see. The SPI itself lives in `solim-api` and declares no dependency on any profiler, which is what actually enforces the separation that matters: normal logic classes cannot reference a profiler type.

**Consequence.** Debug code is still shipped inside the `solim-core` artifact, in a separate package. `Perf.init()` stays where its single call site (`Main.java:74`) already reaches it. If stronger physical isolation is later wanted, it is a build-file change only — the SPI boundary is already correct.

---

## Decision 4 — `Component.adopt` gets identity-based idempotence before `ModifierOwnership` is deleted

**The dependency.** `ModifierOwnership.own` exists solely to avoid double-adoption:

```java
if (resource != null && OwnershipContext.current() == null) ((DisposableOwner) component).adopt(resource);
```

The guard is necessary because `Component.adopt` does `if (!disposables.contains(resource))` — an `O(n)` `equals` scan that makes double-adoption harmless but expensive. Removing `ModifierOwnership` first would require making double-adoption cheap *first*.

**The sequence.**

1. **C3** — add an `IdentityHashMap`-backed index alongside the `disposables` list. Disposal order is preserved (still a list); dedupe becomes `O(1)` identity.
2. **D1** — replace `ModifierOwnership.own(this, e)` with unconditional `((Component) this).own(e)` at all 33 sites, then delete the class.

**Behaviour change, stated plainly.** An effect created by `.width(Readable)` inside a *parent's* `build()` moves from parent-owned to self-owned. This is more correct — the effect configures *this* component's width, so its lifetime should track *this* component — but it does alter teardown order. `DisposalSweepTest`, `LifecycleCorrectnessTest`, and `ComponentOwnershipContractTest` are the three tests that must be reviewed, and they are reviewed as part of tasks 3.3–3.5 rather than after the fact.

**Rejected.** Keeping `ModifierOwnership` and only documenting the non-determinism. The root cause is that ownership is decided by invisible ambient state, which is the exact failure mode the 2026-10-12 ownership unification set out to eliminate.

---

## Decision 5 — One `OwnershipScope.build(Prov)` helper replaces six hand-rolled copies

**Evidence.** The protocol `open → untracked → isolate → build → adoptInto → close` is reimplemented at `StructuralReconciler.create:102`, `Effect.runEffect:100`, `Fragment.capture:17`, `QueryView.updateView:314`, `Popup.render:236`, and `SolimStack.layer:67` — each with a different subset of protections:

| Site | untracked | isolate | forces `element()` | rollback |
|---|---|---|---|---|
| `StructuralReconciler` | yes | yes | yes | disposes `newlyCreated` |
| `Effect.runEffect` | no (pushes self) | no | no | `scope.close()` |
| `Fragment.capture` | no | yes | caller | caller |
| `QueryView` | yes | yes | no | `scope.close()` |
| `Popup.render` | yes | yes | yes | — |
| `SolimStack.layer` | yes | yes | yes | try/catch re-dispose |

**Why this matters.** `Tabs.tab(...)` (`Tabs.java:122-134`) and `ReactiveGrid.empty()` (`ReactiveGrid.java:274-281`) push `AttachmentStack` but **not** `OwnershipContext`. Their resources land in whatever scope happens to be open — which today is the one `Effect.runEffect` opened. They work by accident. This is not a hypothetical bug: it is the structural reason the archived audit listed ten "scope gaps", and it will recur for every new factory added without a shared helper to call.

**The decision.** Add `OwnershipScope.build(Prov<Component>)` to `solim-runtime`, which always untracks, always isolates, always forces `element()`, and always rolls back via `scope.close()`. `Effect.runEffect` keeps its own shape (it pushes itself rather than forcing `element()`) and is documented as the deliberate exception.

---

## Decision 6 — `AttachmentStack.isolate` truncates and discards residue

**Decision.** Replace save/restore with depth truncation:

```java
int depth = stack.size();
try { return Prov.get(); }
finally { while (stack.size() > depth) stack.pop(); }
```

**Rationale.** `ReactiveContext.doUntracked` — solving the identical problem in the adjacent class — already does this, and `solim-reactivity`'s "Untracked Execution in ReactiveContext" requirement already mandates it: *"Restoration SHALL occur even when the block pushes an observer without popping it."* Aligning `AttachmentStack` to that contract makes the two isolation primitives identical and removes an existing inconsistency where `isolate` reinstates pollution and `untracked` discards it.

**Cost.** `O(pushed)` instead of `O(depth)`, and no per-call `ArrayDeque` allocation, on the hottest path in the framework (`ElementResolver.resolve`, `Dynamic.mount`, `QueryView.updateView`, `Popup.render`, `SolimStack.layer`, `StructuralReconciler.create`).

**Behaviour change, stated plainly.** A block that pushes without popping no longer reinstates that residue. If any caller depends on the leak-out it surfaces as "component attached to the wrong parent". `AttachmentStackTest` and `AttachmentStackCaptureTest` gate this.

**Note.** This modifies the `solim-reactivity` requirement *"Unified AttachmentStack entry encapsulation"*, which currently says `isolate()` "SHALL save and restore the complete stack context". That requirement is deltaed, not left contradicting the code.

---

## Decision 7 — `Effect.pending` becomes a state machine

**The bug.**

```java
private void runEffect() {
    if (disposed || running) { if (running) pending = true; return; }   // latched, never enqueued
    …
}
public void invalidate() {
    if (disposed || pending) return;                                    // now a permanent no-op
    pending = true; SignalDispatcher.enqueue(this);
}
```

A re-entrant `runEffect` latches `pending` without queueing. The effect is dead but still subscribed to every dependency.

**The decision.** Model the state explicitly so the broken configuration is unrepresentable:

```java
enum State { idle, queued, running }
```

`runPending()` called while `running` returns to `queued` **and re-enqueues**, so liveness is restored by the same mechanism that established it. This also removes the reason `QueryView` could call `runPending()` directly at all.

**Trigger today.** `QueryView.data()/loading()/error()` call `effect.runPending()` directly, bypassing the dispatcher's `flushing` re-entrancy guard. Decision 8 removes that call site, which removes the only known trigger; the state machine makes the class correct regardless.

---

## Decision 8 — `QueryView` reconfiguration goes through the dispatcher

`QueryView.java:121,132,143,154` call `effect.runPending()` directly, running `updateView()` synchronously in whatever frame the caller happens to be in — mid-layout, mid-dispatch, or inside another effect. Every other reconfiguration path in the framework defers. Replace all four with `effect.invalidate()`, which enqueues like everything else.

**Tradeoff.** One frame of latency when swapping `.data(...)` after mount. Imperceptible for view-state changes, and it removes an entire class of re-entrancy.

---

## Decision 9 — Main thread is bound explicitly at `UI.init()`

**The bug.** `SolimAssert.checkMainThread()` is a no-op until `mainThread` is set, and it is set lazily by `SignalDispatcher.register()` — which is first called from `Effect.invalidate → enqueue → ensureRegistered`. If the first `Effect` in the process is created on a worker thread, the *worker* is recorded as main and every subsequent legitimate main-thread UI operation throws.

**The decision.** `UI.init()` calls `SolimAssert.setMainThread(Thread.currentThread())` before `SignalDispatcher.register()`. The lazy inference in `register()` is removed.

**Secondary.** `checkMainThread()` calls `Thread.dumpStack()` on violation and then throws with a freshly captured stack. The dump is pure waste and is removed.

**Risk acknowledged.** `UI.init()` becomes load-bearing for thread safety. It already exists, is already called by `mod`, and is already the documented bootstrap point.

---

## Decision 10 — `reparentTo` detaches then adopts, fail-fast on Arc conflict

`ElementResolver.resolve()` currently calls `OwnershipContext.registerChild(c)` unconditionally. `Component.adopt` moves the component between `children` lists, but the Arc `Element` stays attached to the old `Table` — so the previous owner can no longer dispose it (it is no longer in `children`), and the new owner will dispose it while its element is still live.

```java
public void reparentTo(@Nullable DisposableOwner newOwner)  // detach Element, then move ownership
```

`ElementResolver.resolve()` routes through it, preserving current call-site behaviour while making it safe. If the element is attached to a live `Table`, `Element.remove()` is invoked; any Arc conflict propagates as an exception rather than being swallowed, consistent with Decision 4 of `2026-10-13-cleanup-solim-ownership` (fail fast, fail loud).

**Rejected.** Orphan-only adoption (`if (c.owner() == null) adopt(c)`). Cheaper and strictly safe, but it silently changes behaviour for any caller relying on the steal. Explicit reparenting keeps the capability while making the hazard nameable.

---

## Decision 11 — Net signals become event-driven with an explicit trigger

The `net-signals` capability currently requires *"a slow poll backstop so no transition path leaves them stale"*, with a scenario *"Poll backstop corrects missed paths"*. Deleting the 1 Hz `Timer.schedule` directly contradicts a live requirement, so this is deltaed rather than silently broken.

`Signals` already subscribes to 11 events (Host, ClientServerConnect, Connection, PlayerConnect, PlayerJoin, PlayerLeave, StateChange, WorldLoad, Reset, GameOver, Dispose, Resize). The poll is removed and replaced by a public `Signals.requestRefresh()` that gameplay code can invoke when it detects a net transition that fired none of those events.

**Tradeoff.** The failure mode if the event list proves incomplete is a stale signal — a wrong UI state, not a crash, so it may go unnoticed for a long time. Accepted in exchange for eliminating a process-lifetime timer that runs regardless of whether any Solim UI is mounted. `requestRefresh()` is the documented escape hatch and is already the existing package-private method name.

---

## Decision 12 — `peek()` keeps lazy recompute; only the disposed guard is added

After `clean-reactive-primitives` removed `Computed`'s eager-recompute branch, `Computed` is pure pull: `invalidate()` marks dirty and returns. `get()` and `peek()` now differ only by their disposed handling — and `peek()` has none.

```java
public T peek() {
    if (disposed) return cachedValue;   // added
    if (dirty || !hasValue) recompute();
    return cachedValue;
}
```

**Rejected.** Making `peek()` a pure cache read (`return hasValue ? cachedValue : null`). It would match `Signal.peek()` exactly, but `ReactiveGrid.respace()` and `columns()` call `peek()` on `resolvedColumnCount` and would receive a stale `null` on first read. Keeping lazy recompute is correct for a memoized value; the disposed guard closes the actual bug.

---

## Risks

| Risk | Mitigation |
|---|---|
| Deleting the `Debug*` subclasses breaks `solim-core/performance`, which casts to them | Tasks 1.5–1.7 rewrite `Profiler`, `ComponentSnapshotBuilder`, `FlameExporter`, `ProfileReportExporter` against the sink in the same batch as the deletion. `solim-mcp` verified in task 1.6 to reference no `Debug*` type. |
| Modifier ownership relocation breaks disposal-order tests | C3 lands before D1 and makes double-adoption harmless. Tests 3.3–3.5 review and update `DisposalSweepTest`, `LifecycleCorrectnessTest`, `ComponentOwnershipContractTest` as part of the task, not after. |
| `isolate` residue discard orphans a pending attachment | `AttachmentStackTest` + `AttachmentStackCaptureTest` gate task 4.1. If either fails, the fix is at the call site that fails to balance, not a revert. |
| Removing the `Signals` poll leaves net signals stale | Delta'd against `net-signals`; `Signals.requestRefresh()` is public and documented as the escape hatch. |
| `UI.init()` becomes load-bearing for main-thread binding | `UI.init()` is already the documented bootstrap and is called by `mod` before feature registration. |
| `Signal.set` copy-on-write makes subscribe/unsubscribe `O(n)` | Both are construction/disposal-time, never hot. Guarded by `ReactivityBenchmarkTest` in task 4.2. |