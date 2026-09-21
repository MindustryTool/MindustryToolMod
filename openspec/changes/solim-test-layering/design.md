## Context

The `solim-test` change created a single shared test environment module consumed by every module's test source set. That produced this real edge set:

```
main:  solim-test ──implementation──▶ solim-core, solim-runtime
test:  solim-core, solim-runtime, solim, solim-mcp, mod ──testImplementation──▶ solim-test
```

Gradle is acyclic across configurations — `testImplementation` is not exported, so there is no compile or runtime cycle. Eclipse/JDT (redhat.java), however, models each Gradle project as one Eclipse project whose build path contains **all** project dependencies, main and test alike. It therefore sees `solim-test → solim-core` and `solim-core → solim-test` as a build-path cycle, likewise for `solim-runtime`, and reports a transitive 3-cycle. `mod`, `solim`, and `solim-mcp` are reported only as *paths into* those cycles, because they depend on a cycle member.

Investigation findings that bound the fix:

- `solim-runtime` and `solim-mcp` tests contain **zero** references to `solim.test`; their `testImplementation project(':solim-test')` lines are dead and are the entire source of the `solim-test ⇄ solim-runtime` cycle.
- `SolimEnv` (in `solim-test`) touches `solim-runtime` types pervasively (`ParentStack`, `ComponentContext`, `ReactiveContext`, `SignalDispatcher`) but touches `solim-core` in exactly one place: `PendingCellConfig.install()` in `resetAmbientState()`, which restores the default cell configurator after a test replaces it.
- Only solim-core tests replace the cell configurator (`ModifiersAndGapRegressionTest`, `SchedulerAndContextsRegressionTest`). `ParentStackTest` (solim-runtime) sets it to `null` and restores nothing, but does not use an env today.
- The governing invariant this change adopts: **a test-support module must never depend on a module that depends back on it.** Since a test-support class that references module X's types can only be consumed by modules with X on their classpath, the env layer that references X must be owned at or below X.

## Goals / Non-Goals

**Goals:**

- Eliminate the Eclipse build-path cycles without changing Gradle semantics.
- Keep a single, layered, shared test environment; no per-file boilerplate returns.
- Encode the acyclic-layering invariant in the `test-env` spec so this cannot regress.

**Non-Goals:**

- No production code changes.
- No `java-test-fixtures` (see D1).
- No behavioral change to any existing test.
- No attempt to migrate `solim-runtime`/`solim-mcp` tests onto a shared env in this change (see D4).

## Decisions

### D1: Layer the envs by module level; skip `java-test-fixtures`

Env layers mirror the module towers:

```
mod/src/test     MindustryTestEnv extends SolimEnv        (Mindustry globals)
solim-core/test  SolimCoreEnv     extends SolimEnv        (PendingCellConfig reset)   ← new
solim-test       SolimEnv         extends ArcTestEnv      (solim ambient state)  dep: solim-runtime only
solim-test       ArcTestEnv                               (Arc statics)          dep: none internal
```

`SolimEnv` stays in `solim-test` with only `solim-runtime` as a project dependency. A solim-core-owned `SolimCoreEnv` adds the core-layer reset where `PendingCellConfig` lives. Because a test-support class referencing X is consumed only by X's dependents, this is the minimal layering that is acyclic: nothing `solim-test` depends on depends back on `solim-test`.

Alternatives considered:

- **`java-test-fixtures` on `solim-core`, with `SolimEnv` moved into its fixtures and `ArcTestEnv` staying in `solim-test`.** Idiomatic Gradle, but it reopens the explicit D1 decision from the `solim-test` change and adds fixture-variant plumbing to `solim`/`mod` consumers. Rejected in favor of the smaller, plugin-free split.
- **Keep one env and drop `solim-core` tests' env usage.** Rejected: reintroduces per-test configurator boilerplate and loses the shared teardown guarantees.
- **Reflection from `SolimEnv` to force `PendingCellConfig.install()`.** Rejected: fragile, hides a real dependency, and conflicts with the project's "no abstraction without value" and nullability/import conventions.
- **Move `PendingCellConfig` down into `solim-runtime`.** Rejected: it references `solim.core.Disposable`, `SolimToken`, `Effect`, `Readable`, and `GapContainer`; it cannot live below `solim-core`.

### D2: `solim-test` depends only on `solim-runtime`

Drop `implementation project(':solim-core')` from `solim-test`. `ArcTestEnv` has no internal dependencies; `SolimEnv` needs only `solim-runtime`. `solim-test`'s own `SolimEnvLeakTest` continues to work unchanged because it never exercises the core configurator.

### D3: Core-layer reset via a solim-core-owned subclass and a protected hook

`SolimEnv.resetAmbientState()` becomes runtime-only (`ParentStack.clear()`, `ComponentContext.clear()`, `ReactiveContext.clear()`, `SignalDispatcher.resetForTests()`). To let the core layer extend reset deterministically, `SolimEnv` exposes a `protected` hook invoked by its setup/teardown after the static resets; `SolimCoreEnv` overrides it to call `PendingCellConfig.install()`. Solim-core tests that override the configurator are re-parented to `SolimCoreEnv` (or all solim-core tests are, for uniformity — an implementation choice, not a behavioral one).

This preserves today's exact behavior: after any solim-core test that replaces the configurator, the default is reinstalled before the next test, and any same-JVM subsequent module sees the default.

### D4: Delete dead test wiring; do not force env usage on runtime/mcp

Remove `testImplementation project(':solim-test')` from `solim-runtime` and `solim-mcp`. Their tests do not reference `solim.test` and self-manage ambient state (`ParentStackTest` clears the stack and nulls the configurator). The `test-env` spec's blanket "all tests migrated" / "every testable module consumes it" claims are corrected to describe actual behavior.

If runtime tests later want a shared env, it must be a `solim-runtime`-owned env (test sources), because a `solim-test`-hosted env that references `solim-runtime` can never be consumed by `solim-runtime` without recreating the cycle. Recorded as out of scope.

### D5: `solim` and `mod` keep consuming `SolimEnv` directly

Neither module's own tests override the cell configurator, so neither needs a full core env. `mod`'s `MindustryTestEnv` continues to extend `SolimEnv`; its existing `testImplementation project(':solim-runtime')` remains (test-only, unchanged by this change).

**Correction found during implementation:** because Gradle reuses worker JVMs *across* modules, a mod test can run after `ParentStackTest` (solim-runtime) nulled the configurator. The runtime-level `SolimEnv` no longer reinstalls it, so `MindustryTestEnv` overrides the reset hook to call `PendingCellConfig.install()`. The mod is above the solim-core layer and already uses `PendingCellConfig` in production code, so this is layer-consistent. First `:mod:test` run confirmed it: 19 chat-height failures (56 vs 54) without the override, green with it.

### D6: Facade flush for mod tests; drop mod's explicit runtime dependency

Mod tests call `SignalDispatcher.flush()` directly (4 files) because test environments have no frame loop to pump pending effects — production flushes on the frame lifecycle. That direct call is the only reason `mod` declares `testImplementation project(':solim-runtime')`, keeping runtime types visible to mod test compilation, which AGENTS.md forbids.

Fix: `SolimEnv` gains `public static void flushEffects()` wrapping `SignalDispatcher.flush()`. `solim-test` already legally depends on `solim-runtime`, so the facade introduces no new edges. Mod tests swap their calls; the explicit dependency is removed. The runtime jar still reaches mod's test *runtime* classpath transitively (project dependency variants propagate implementation deps), while mod test *compile* no longer sees runtime internals — matching the AGENTS.md module-separation intent.

Alternatives considered: a public flush API in solim-core (exposes engine scheduling to production code — misuse risk); keeping the dependency and documenting a test exception (leaves the AGENTS.md rule bent for no benefit).

## Risks / Trade-offs

- [Removing `PendingCellConfig.install()` from `SolimEnv` could leak a custom configurator from a solim-core test into a later test/module] → `SolimCoreEnv` reinstalls it in both setup and teardown hooks; only solim-core tests replace it.
- [The Eclipse warning may not fully clear if Buildship still maps test dependencies as project references for another reason] → Verify by reopening the workspace; if a residual warning remains, it is isolated to modules that no longer form a true 2-cycle and can be assessed separately.
- [A new env class adds a layer to understand] → It is three lines of override and documents exactly why (core-only reset), which is clearer than the current hidden `solim-test → solim-core` edge.
- [Spec correction could look like scope creep] → It only removes claims that were already untrue; no capability is dropped.

## Migration Plan

1. Remove the dead `testImplementation project(':solim-test')` from `solim-runtime` and `solim-mcp`.
2. Remove `solim-core` from `solim-test`'s dependencies; make `SolimEnv` runtime-only and add the protected reset hook.
3. Add `SolimCoreEnv` in `solim-core` test sources; re-parent the solim-core tests that override the configurator.
4. Run `gradlew test` for `solim-test`, `solim-core`, `solim`, `mod` to confirm behavior parity.
5. Reopen the IDE and confirm the `mod` build-path cycle warning is gone.

Rollback: revert the build files and re-add the two dependency directions; test-only change, no production impact.

## Open Questions

- None blocking. Whether to re-parent **all** solim-core tests to `SolimCoreEnv` or only the two configurator-overriding tests is a style call to settle during implementation.