# Solim Test Environment Layer

## Why

`forkEvery = 1` makes every test class spawn a fresh JVM, stretching the full test suite to take far longer than necessary. It was added because tests share global statics (Arc `Core.*`, solim ambient state, font/icon caches) and Gradle reuses worker JVMs across modules, letting one module's static state poison another's tests. The root cause is inconsistent test environment setup: `solim-core` has a proper `SolimTestHarness`, but ~75 test files across modules hand-roll `MockApplication`/`MockGraphics` setup with divergent save/restore patterns, several of which never clean up (e.g. `FeatureKeybindTest` mutates `Icon.book` and never restores it).

## What Changes

- Add a new `solim-test` module (plain Java library, Java 8-compatible) providing a shared, layered test environment:
  - `ArcTestEnv`: idempotent per-JVM Arc init (`Core.app/graphics/gl/gl20`), per-test fresh `Scene` with minimal styles, in-memory `Settings`, and strict save/restore of everything a test touched.
  - `SolimEnv` extends `ArcTestEnv`: adds solim ambient state reset (`ParentStack`, `ComponentContext`, `ReactiveContext`, `SignalDispatcher`, `PendingCellConfig`) and strict teardown leak asserts (evolved from `SolimTestHarness`).
- Migrate the existing `SolimTestHarness` (solim-core) into `SolimEnv` in `solim-test`; delete the old class and re-parent solim-core tests.
- Add a Mindustry-layer test extension in `mod/src/test` for stubbing/restoring Mindustry globals (`Icon.*` registry via reflection save/restore, `FeatureManager`, `Vars`).
- Convert all existing test files (~75 hand-rolled setups) to use the shared env, module by module.
- Remove `forkEvery = 1` from the root `build.gradle` immediately — safe because the env's strict teardown asserts detect leaks instead of a fresh JVM hiding them.
- Collapse per-file scene/style setup where the env provides a fresh styled `Scene` per test.

## Capabilities

### New Capabilities
- `test-env`: Shared layered test environment (`solim-test` module) providing idempotent Arc/solim test setup, strict restoring teardown with leak detection, consumed by all modules via `testImplementation`.

### Modified Capabilities
- (none — no existing spec capabilities change; this is test infrastructure only)

## Impact

- **Build**: root `build.gradle` (new `solim-test` module wiring, `forkEvery` removal); `settings.gradle` (module registration).
- **Modules affected**: `solim-core`, `solim-runtime`, `solim-mcp`, `solim`, `mod` (test source sets only; no production code changes).
- **Removed**: `SolimTestHarness` (replaced by `SolimEnv` in `solim-test`).
- **Test-only change**: no shipped jar contents, no runtime behavior, no i18n impact.
- **Risk**: first full run with reused worker JVMs is the real validation — leak asserts may surface latent cross-test contamination which must be fixed during migration.
