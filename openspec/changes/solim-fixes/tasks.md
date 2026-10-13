## 1. Pluggable instrumentation SPI (new capability `solim-instrumentation`)

- [ ] 1.1 Add `Instrumentation`, `InstrumentationSink`, and the semantic event hierarchy to `solim-api` (`solim.instrumentation`): `SignalWrite`, `ComputedRun`, `EffectRun`, `ReconcilePass`, `AttachSubtree`, `Dispose`, with `enabled()` reading a `static volatile boolean` that is `false` whenever no sink is installed
- [ ] 1.2 Instrument `Signal.set`, `Computed.recompute`, `Effect.runEffect`, `StructuralReconciler.reconcile`, `AttachmentStack.doPop` (subtree span), and `Component.dispose` with single guarded `if (Instrumentation.enabled()) Instrumentation.emit(...)` calls
- [ ] 1.3 Delete `DebugAttachmentStack`, `DebugReactiveContext`, `DebugSignalDispatcher`, the 31 no-op `do*()` hooks in `AttachmentStack`, and the 12 no-op `on*()` hooks in `ReactiveContext`
- [ ] 1.4 Delete the `volatile INSTANCE` + `install(...)` service locators from `AttachmentStack`, `ReactiveContext`, and `SignalDispatcher`; reduce the three classes to final classes with private stacks and public static delegates
- [ ] 1.5 Rewrite `solim-core/performance/Perf` as an `InstrumentationSink` subscriber preserving `init()`, `setEnabled()`, `snapshot()`, `reset()`, `setThreshold()` and the `solim.debug.enabled` property contract; keep `Perf.init()` as the single entry point called from `Main.java:74`
- [ ] 1.6 Rewrite `ComponentSnapshotBuilder`, `FlameExporter`, `ProfileReportExporter`, and `UiProfiler` against the sink; delete every `instanceof DebugReactiveContext` / `DebugAttachmentStack` / `DebugSignalDispatcher` cast (16 sites across `Computed`, `Effect`, `Perf`, `ComponentSnapshotBuilder`)
- [ ] 1.7 Verify `solim-mcp` (`ObjectGraphScanner`, `SignalIntrospector`) compiles and passes without modification; it reflects over public reactive types and references no `Debug*` type
- [ ] 1.8 Delete the hand-written metric accessors whose data now lives in sinks: `StructuralReconciler.getLastNew/Reuse/TotalCount`, `VirtualList.getMountedCount`, `Popup.getLastHideTime`, `AttachmentStack.maxDepthObserved/totalRecorded/isTracing`, and their now-dead counters
- [ ] 1.9 Retain `DebugReactiveNode` unchanged as the pull path for live dependency and ownership graph walks; add a scenario pinning that push metrics and pull graph traversal are separate mechanisms
- [ ] 1.10 Add `solim-instrumentation` tests: zero-allocation when disabled (verified via a disabled-sink benchmark), event ordering through a flush, sink swap at runtime, and a test proving no profiler type is referenced from `solim-runtime`

## 2. Correctness fixes

- [ ] 2.1 Add a `fetched` flag to `CacheEntry` distinct from `hasData()`; set it in `setData`, clear it in `clear`; update `Query` so a cached `null` suppresses neither the initial fetch nor `ensureFresh`
- [ ] 2.2 Make `ForEach.children(...)` call `reconcile()` immediately when `isBuilt()`, matching `ReactiveGrid.children(...)`
- [ ] 2.3 Hoist the double `tip.get()` in `ElementConfig.tooltip(Readable)` to a single read per run
- [ ] 2.4 Add `Component.reparentTo(@Nullable DisposableOwner)` that detaches a live parent `Element` before moving ownership, and route `ElementResolver.resolve()` through it
- [ ] 2.5 Change `Component.applyDefaultName` to use `toLowerCase(Locale.ROOT)`; add a test asserting element names under a Turkish default locale
- [ ] 2.6 Make `Component.children()` and `Component.disposables()` return snapshots, matching `SessionRoot`
- [ ] 2.7 Dedupe `QueryCache.attachObserver` so a repeated attach does not inflate the refcount or duplicate invalidation callbacks

## 3. Liveness and robustness

- [ ] 3.1 Replace `Effect.pending` with an explicit `State` machine so a re-entrant `runEffect()` returns to `queued` and re-enqueues instead of latching to death; add a test reproducing the re-entrant `QueryView`-style call
- [ ] 3.2 Add the disposed guard to `Computed.peek()`; add a test asserting a disposed `Computed` returns its cached value from both `get()` and `peek()` without invoking the supplier
- [ ] 3.3 Replace the four direct `effect.runPending()` calls in `QueryView` with `effect.invalidate()` so the dispatcher owns all `updateView` execution
- [ ] 3.4 Add debug-mode LIFO identity verification to `OwnershipContext.pop()` and `ReactiveContext.pop()`; add an unbalanced-push test
- [ ] 3.5 Bind the main thread explicitly in `UI.init()` via `SolimAssert.setMainThread(Thread.currentThread())` before `SignalDispatcher.register()`; remove the lazy inference from `register()`; remove the redundant `Thread.dumpStack()` from `checkMainThread()`
- [ ] 3.6 Make `Query` re-attach its `QueryCache` observer when a `key(Prov)` supplier resolves to a different key; add a test that invalidating the second key triggers a refetch

## 4. Ownership consolidation (C3 → D1 → D3)

- [ ] 4.1 Add an identity-based index to `Component.adopt` (`Collections.newSetFromMap(new IdentityHashMap<>())`) so double-adoption is `O(1)` and harmless; preserve list order for disposal
- [ ] 4.2 Add `OwnershipScope.build(Prov<Component>)` to `solim-runtime`: always untracked, always isolated, always forces `element()`, rolls back via `scope.close()`
- [ ] 4.3 Migrate `StructuralReconciler.create`, `Fragment.capture`, `QueryView.updateView`, `Popup.render`, and `SolimStack.layer` to `OwnershipScope.build`
- [ ] 4.4 Give `Tabs.tab(...)` and `ReactiveGrid.empty(Runnable)` an explicit ownership scope instead of relying on the enclosing `Effect`'s
- [ ] 4.5 Replace all 33 `ModifierOwnership.own(this, e)` call sites (`CellConfig` 11, `TableConfig` 12, `ElementConfig` 7, `ModifierOwnership` 1, `Tabs` 1, plus `Button`/`Text`/`Grid`/`Card`/`Row` if any) with unconditional `((Component) this).own(e)`
- [ ] 4.6 Delete `solim/modifier/ModifierOwnership.java`
- [ ] 4.7 Review and update `DisposalSweepTest`, `LifecycleCorrectnessTest`, and `ComponentOwnershipContractTest` for the modifier-ownership relocation and the new snapshot accessor semantics
- [ ] 4.8 Add a test proving a modifier effect created inside a parent's `build()` is disposed with the component it configures, not the parent

## 5. Performance

- [ ] 5.1 Replace `AttachmentStack.doIsolate` (both overloads) with depth truncation, add the missing `SolimAssert.checkMainThread()`, and gate on `AttachmentStackTest` + `AttachmentStackCaptureTest`
- [ ] 5.2 Make `Signal.set` iterate an immutable copy-on-write observer snapshot, eliminating the per-write `LinkedHashSet` allocation; guard with `ReactivityBenchmarkTest`
- [ ] 5.3 Read `minWidth` untracked in `PendingCellConfig.applyToCell`'s `growX` branch; add a test proving the mounting `Effect` gains no spurious dependency
- [ ] 5.4 Remove `ReactiveGrid.columns(...)`'s eager reflow so the `Computed` drives a single tracked reflow; update `ReactiveGridTest`

## 6. Module and docs hygiene

- [ ] 6.1 Move `solim.performance` DTOs out of `solim-api` into `solim-runtime` / the new `solim.instrumentation` package; update `solim-runtime` imports
- [ ] 6.2 Delete the `Signals` 1 Hz `Timer.schedule` poll; make `requestRefresh()` public and document it as the explicit escape hatch; add a test that a `requestRefresh()` call after a simulated event-less transition updates the signals
- [ ] 6.3 Add `loading`, `button.retry`, and `query.error.default` keys to `assets/bundles/bundle.properties` with descriptive comments; remove the hardcoded `"An error occurred"` from `QueryView`'s default error factory
- [ ] 6.4 Rename `solim/feedback/Badge` to disambiguate from `solim/display/Badge`; update its tests
- [ ] 6.5 Regenerate `docs/solim_internals.md` against HEAD: `Component` (not `BaseComponent`), `OwnershipScope` (not `withoutAutoOwnership`), `ReactiveSource` (not `Object`), `SolimToken.isExpandingChild` (not `Ui.isExpanding`), per-`Entry` `pendingComponents`, and the 3-arg `CellConfigurator`
- [ ] 6.6 Correct `AGENTS.md`: the `:solim-core` export list, and the claim that `own()` is package-private (it is `public final` and used by 27 framework files)
- [ ] 6.7 Delete the empty `solim-core/src/test/java/solim/core/QueryViewTest.java`
- [ ] 6.8 Remove stale `Subscription`/`Binding` scenario text from the `solim-reactivity` capability (left by the spec-domain merge; references the deleted types)

## 7. Verification

- [ ] 7.1 Run `:solim-api`, `:solim-runtime`, `:solim-core` unit and regression suites
- [ ] 7.2 Run `:solim` and `:solim-mcp` suites; verify `solim-mcp` needed no changes (task 1.7)
- [ ] 7.3 Run `:mod` suite; confirm no API migration is required and `Perf.init()` is the only integration point
- [ ] 7.4 Run `gradlew checkNoFullyQualifiedNames` and `checkBundles`
- [ ] 7.5 Verify no dangling references to `ModifierOwnership`, `DebugReactiveContext`, `DebugAttachmentStack`, `DebugSignalDispatcher`, or any `install(...)` service locator remain
- [ ] 7.6 Run `openspec validate solim-fixes --strict`
- [ ] 7.7 Confirm the profiler produces a flame graph and a profile report after the SPI refactor, with metrics matching the pre-change output for an equivalent workload