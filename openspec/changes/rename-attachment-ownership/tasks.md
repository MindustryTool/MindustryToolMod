## 1. Runtime core rename

- [x] 1.1 Rename `solim.runtime.ParentStack` to `AttachmentStack` (file, class, `Entry`/`Attacher`/`CellConfigurator` references, singleton install sites)
- [x] 1.2 Rename `solim.runtime.ComponentContext` to `OwnershipContext` (file, class, `CaptureRegistrar` pool, `pushCapture` cross-call from capture path)
- [x] 1.3 Rename `solim.runtime.DebugParentStack` to `DebugAttachmentStack` and update `PerfSpan("AttachmentStack", …)` label
- [x] 1.4 Update intra-runtime references (`StructuralReconciler`, `ElementResolver`, `ReactiveContext.trackWithWarning`, capture/isolate lockstep overrides)

## 2. Framework and test call sites

- [x] 2.1 Update `solim-core` main sources (layouts, reactive primitives, `BaseComponent`, `LeafComponent`, modifiers, overlays, `Perf` facade)
- [x] 2.2 Update `solim` facade (`UI.java`) and `solim-test` harness (`SolimEnv` reset/teardown messages, leak test)
- [x] 2.3 Update all test sources and literal assertions (`ParentStackPerfTest` component label, `SolimEnvLeakTest` message, `FlameTraceTest` singleton class checks, ambient-balance tests)

## 3. Docs and spec prose

- [x] 3.1 Update `docs/solim_internals.md` (§3.1/§3.2, ambient-stacks table, `PendingCellConfig` hook, lifecycle rules)
- [x] 3.2 Update active spec prose naming old types (`solim-lifecycle`, `solim-widgets`, `solim-layout`, `solim-test-harness`, `thread-safety-assertions`, `slow-component-tracking`, `typed-reactive-graph`, `query-primitive`, `mutation-primitive`, `reactive-map-signal`, `solim-framework-regression-suite`, `stack-direct-layers`, `test-env`, `solim-styling`) without changing requirement semantics

## 4. Verification

- [x] 4.1 Sweep for stale names (`ParentStack|ComponentContext|DebugParentStack` excluding `openspec/changes/solim-flame-graph` history and `old/`) and confirm zero hits
- [x] 4.2 Run full build and test suite (`SolimEnv` balance, `SolimEncapsulationGuardTest`, reconciler/disposal/regression suites) green
- [x] 4.3 Review `git diff -w` to confirm pure-rename discipline (no behavior edits) per AGENTS.md checklist (Java 8 runtime, `arc.func.*`, `@Nullable`, no FQCNs, i18n untouched)
