## Context

Solim's runtime has three ambient stacks: `ParentStack` (visual attachment: which `Table` a child `Element` goes to), `ComponentContext` (lifecycle ownership: which component disposes a `Disposable`), and `ReactiveContext` (dependency tracking). The first two are constantly used together — `BaseComponent` and `LeafComponent` constructors dual-register with both, `ElementResolver` splits them (own but isolate), and `StructuralReconciler` isolates both independently — yet `Parent` vs `Component` plus `Stack` vs `Context` hides that split. Readers guess wrong about which stack to push/isolate. Exploratory consensus: rename prefixes to the verbs already in the code (`attach*` → Attachment, `own*/register*` → Ownership), keep suffixes to minimize diff, include the `DebugParentStack` variant.

Constraints: `solim.runtime` is physically excluded from `:mod` (verified zero `mod/` references); Java 8 runtime APIs only; `arc.func.*` instead of `java.util.function`; single main-thread assumption guarded by `SolimAssert.checkMainThread()` stays unchanged.

## Goals / Non-Goals

**Goals:**
- Make call sites self-explanatory: `AttachmentStack.attachToParent()` vs `OwnershipContext.registerChild()`.
- Mechanical, reviewable rename: prefixes only, no signature or semantics change.
- Move nested types with their owner (`Entry`, `Attacher`, `CellConfigurator` → `AttachmentStack`; `CaptureRegistrar` stays inside `OwnershipContext`).
- Update every textual reference in the same change (code, tests, `PerfSpan` label, `SolimEnv` messages, docs, specs) so nothing lies.

**Non-Goals:**
- No behavior, lifecycle, threading, or reconciliation-algorithm changes.
- No `solim.UI` facade or `:mod` API changes.
- No `ReactiveContext` rename (stays as the third peer).
- No method renames (`withoutAutoOwnership`, `isolate`, `capture`, `registerPendingComponent` keep their names).

## Decisions

### 1. Prefix-only rename, suffixes kept

`ParentStack` → `AttachmentStack`, `ComponentContext` → `OwnershipContext`, `DebugParentStack` → `DebugAttachmentStack`.

- Rationale: matches ubiquitous language (`attachToParent`, `Attacher`, `own()`, `withoutAutoOwnership`) with the smallest possible diff. Reviewers can verify each hunk is import/class-name only.
- Alternative considered: unify suffixes (`AttachmentStack` + `OwnershipStack`, or `*Scope`). Rejected: doubles the conceptual change, touches spec prose about "context balance", and `ReactiveContext` would become the odd one out anyway.

### 2. No deprecation shims or type aliases

- Rationale: `solim.runtime` is an internal `implementation` dependency, never on `:mod`'s classpath (guarded by `SolimEncapsulationGuardTest` on the package, not class names). Hard rename is safe; shims would leave two names for one concept — the exact confusion being removed.
- Alternative considered: keep `@Deprecated` subclasses extending the new types. Rejected: prolongs migration, pollutes the internal API.

### 3. Nested types move with their owner, methods unchanged

`Entry`, `Attacher`, `CellConfigurator` move to `AttachmentStack` (e.g. `Column.ATTACHER` becomes `AttachmentStack.Attacher`). All static method names and signatures stay identical so the diff is `s/ParentStack/AttachmentStack/`, `s/ComponentContext/OwnershipContext/`.

- Alternative considered: also shorten `OwnershipContext.withoutAutoOwnership` → `withoutOwnership`. Rejected: expands blast radius into specs quoting the method verbatim for zero clarity gain.

### 4. String literals and messages move with the code

`DebugAttachmentStack` records `PerfSpan("AttachmentStack", …)`; `SolimEnv` teardown asserts/messages say `AttachmentStack` / `OwnershipContext`; `SolimEnvLeakTest` and `ParentStackPerfTest` expectations update. Tests that assert on the literal (not just the type) must change in the same commit or they fail.

### 5. Docs and specs updated as text, not requirements

`docs/solim_internals.md` (§3.1/§3.2, ambient-stacks table, `PendingCellConfig` hook) and spec prose naming the old types are updated mechanically. No requirement semantics change, so existing specs get text updates at implementation time; the change's own spec pins the new naming contract (see `specs/ambient-naming/spec.md`).

## Risks / Trade-offs

- [Risk] Missed textual reference (log label, doc, spec) leaves stale name → Mitigation: `tasks.md` includes a final `grep` sweep for `ParentStack|ComponentContext|DebugParentStack` excluding the archived `solim-flame-graph` history and `old/` legacy folder; `SolimEnv` + full test suite gate the change.
- [Risk] Large but shallow diff (~80+ files) obscures a real change → Mitigation: pure-rename discipline — any non-rename edit is out of scope and fails review; verify with `git diff --stat` + `git diff -w` showing only identifier lines.
- [Risk] Spec-text drift (specs still say `ParentStack`) → Mitigation: implementation updates spec prose in place; change spec asserts the new canonical names so future regressions fail fast.
- [Risk] MCP/snapshot tooling reflects on class names → Mitigation: verified — `solim-mcp` introspection reflects on instances/fields, not on the two class-name strings; only test-method display names mention them.
