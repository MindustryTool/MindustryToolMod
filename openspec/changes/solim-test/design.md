# Solim Test Environment Layer — Design

## Context

Root `build.gradle` sets `forkEvery = 1` for all modules: every test class gets a fresh JVM, making the suite extremely slow. The comment at build.gradle:100-102 records why — tests share global statics (Arc `Core.*`, solim ambient state, font caches) and Gradle reuses worker JVMs across modules, so static state leaks poison tests.

Current setup patterns are inconsistent:

- `solim-core/src/test/java/solim/test/SolimTestHarness.java` — proper harness: `@BeforeAll` idempotent Arc init, `@BeforeEach` ambient reset, `@AfterEach` strict teardown asserts. Only used by part of solim-core.
- ~75 test files hand-roll `MockApplication`/`MockGraphics` in three divergent ways:
  1. Null-check-if — never cleans up, leaks everything.
  2. Save/restore `prev*` fields — careful but copy-pasted per file; easy to miss a field.
  3. Harness subclass — proper, but not shared beyond solim-core.
- Mindustry-side globals are mutated and never restored (e.g. `FeatureKeybindTest` sets `Icon.book = new TextureRegionDrawable()` in `@BeforeAll`) — this is the cross-module poisoning `forkEvery` was compensating for.

Constraints: `solim-test` must be Java 8-compatible (`options.release = 8` like all subprojects), must not depend on Mindustry (only Arc, which is already on test classpaths via `compileOnly` — note: `solim-test` needs its own `compileOnly` Arc dependency), and must never be part of shipped jars.

## Goals / Non-Goals

**Goals:**

- Single shared, layered test environment used by all modules.
- Strict, restoring teardown with leak asserts so a reused JVM cannot be poisoned.
- Remove `forkEvery = 1` and keep suite correctness.
- Migrate all existing tests to the env.

**Non-Goals:**

- No production code changes.
- No new test framework (stay on JUnit 5).
- No refactoring of test *logic* — only environment setup/teardown is touched.
- No UI runtime testing changes for the mod module (headless only).

## Decisions

### D1: Dedicated `solim-test` module over `java-test-fixtures`

Plain Java module; each module adds `testImplementation(project("solim-test"))`. Zero Gradle plugin plumbing, trivially consumable cross-module, excluded from shipped jars. Alternatives considered: `java-test-fixtures` plugin (more idiomatic but adds config to every module and cross-module fixture consumption complexity), per-module duplication (recreates today's drift problem). Decided with user.

### D2: Layered env classes

```
ArcTestEnv   — Core.app/graphics/gl/gl20, per-test Scene + minimal styles,
               in-memory Settings; save/restore all touched Core fields
SolimEnv     — extends ArcTestEnv; + ambient reset (ParentStack,
               ComponentContext, ReactiveContext, SignalDispatcher,
               PendingCellConfig) + teardown leak asserts
```

Modules take only what they need: pure-logic tests use `ArcTestEnv`; UI/reactive tests use `SolimEnv`. `SolimTestHarness` evolves into `SolimEnv` (decided with user); old class deleted, solim-core tests re-parented.

### D3: Setup is idempotent per JVM; teardown is strict and restoring

`@BeforeAll`: init statics once if null. `@BeforeEach`: fresh Scene (with minimal label/button styles so scene-dependent tests don't each hand-roll styles), fresh in-memory `Settings`, ambient reset. `@AfterEach`: capture-and-restore semantics — every global the env owns is restored to its pre-test value; leak asserts fail the test if ambient state (stacks, dispatcher) is non-empty at teardown, matching existing `SolimTestHarness` behavior.

### D4: Scene semantics — headless null by default, `newScene()` opt-in

`SolimTestHarness` nulls `Core.scene` per test, and solim components (e.g. `Scroll`) have load-bearing headless fallbacks keyed on `Core.scene == null` (Scroll.java:66). The env preserves this: setup resets `Core.scene` to `null`; tests that need a live scene call the env's `newScene()` helper (fresh `Scene` + minimal styles) in their own setup, and teardown restores the previous value. This keeps migration mechanical and avoids silently changing component behavior under test.

### D5: Mindustry globals live in `mod/src/test`, not `solim-test`

`solim-test` must not depend on Mindustry. The mod module gets a `MindustryTestEnv` (extends `SolimEnv`) that stubs `Icon.*` fields with plain `TextureRegionDrawable`s and *restores them via reflection* in teardown (snapshot field values before, restore after), clears `FeatureManager`, resets `Vars` state where touched. Reflection-based registry save/restore is the pattern that kills the `Icon.book` class of poisoning.

### D6: forkEvery removal is immediate and unconditional

Decided with user. Safe only because teardown asserts catch leaks. Alternatives rejected: staged per-module removal (slower, no added safety since asserts are equivalent); keep until migration done (defeats the purpose — suite stays slow during migration).

### D7: Migration is mechanical per module, logic untouched

Convert each test file's `@BeforeAll`/`@BeforeEach`/`@AfterEach` env boilerplate to the env base class. Hand-rolled `prev*` save/restore blocks are deleted (the env owns save/restore). Test bodies are not modified.

## Risks / Trade-offs

- [First full run with reused JVMs surfaces latent leaks] → Strict teardown asserts fail fast with a named assert; fix each during migration. Expected worst offenders: `Icon.*` mutations, `Core.settings` residue, un-cleared `FeatureManager`.
- [Fresh JVM per class currently masks ordering-dependent tests] → Any test that silently depended on another test's leftovers will now break. Mitigation: treat each such failure as a real bug; the test was relying on accidental state.
- [`solim-test` adds a compileOnly Arc dependency and build wiring] → Small, one-time cost; mirrors existing subproject wiring.
- [Reflection-based Icon save/restore is fragile if Icon fields change] → Acceptable for tests; failure mode is a clear test-env error, not silent poisoning.
- [StyleCache/font caches are static and sticky] → Env restores references where replaceable; caches that cannot be unloaded are left as-is (benign, deterministic warm state, same as production).

## Migration Plan

1. Add `solim-test` module with `ArcTestEnv` + `SolimEnv`; wire `settings.gradle` and root dependencies.
2. Re-parent solim-core tests from `SolimTestHarness` to `SolimEnv`; delete `SolimTestHarness`.
3. Remove `forkEvery = 1`.
4. Migrate modules one at a time (solim-core → solim-runtime → solim-mcp → solim → mod), converting each test file; add `MindustryTestEnv` in mod.
5. Full clean test run across all modules as final validation; fix any surfaced leaks.

Rollback: revert is trivial — test-only change; `forkEvery = 1` can be re-added if an unfixable leak appears mid-migration.

## Open Questions

- None blocking. If teardown asserts fire repeatedly on the same un-restorable static (e.g. some Mindustry cache), decide at that point whether to add an env reset hook or document the static as warm-state.
