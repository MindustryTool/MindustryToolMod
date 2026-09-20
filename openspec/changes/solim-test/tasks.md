## 1. test-support module

- [x] 1.1 Register `solim-test` in `settings.gradle` and create module directory with `build.gradle` (java plugin, `options.release = 8`, `compileOnly` Arc dependency matching subproject ivy repos, JUnit 5 `testImplementation` for its own tests)
- [x] 1.2 Create `ArcTestEnv` base class: idempotent `@BeforeAll` Arc init (app/graphics/gl/gl20), per-test fresh `Scene` with minimal styles + fresh in-memory `Settings`, save/restore of env-owned `Core.*` fields in `@AfterEach`
- [x] 1.3 Create `SolimEnv` extending `ArcTestEnv`: solim ambient reset (`ParentStack`, `ComponentContext`, `ReactiveContext`, `SignalDispatcher`, `PendingCellConfig`) in setup, cleanup-before-asserts strict teardown with leak asserts (port from `SolimTestHarness`)
- [x] 1.4 Add behavior tests for the env itself in `solim-test` (idempotency, fresh scene/settings, restore, leak-assert failure, cleanup-before-asserts)
- [x] 1.5 Add `testImplementation(project(":solim-test"))` wiring to `solim-core`, `solim-runtime`, `solim-mcp`, `solim`, `mod` build files
- [x] 1.6 Verify `solim-test` compiles and its own tests pass; verify shipped mod jar excludes test-support classes

## 2. SolimTestHarness replacement

- [x] 2.1 Re-parent all solim-core tests extending `SolimTestHarness` to `SolimEnv`; delete `SolimTestHarness`
- [x] 2.2 Run solim-core tests and fix any teardown-assert failures surfaced (each is a real leak)

## 3. forkEvery removal

- [x] 3.1 Remove `forkEvery = 1` and its comment block from root `build.gradle`
- [x] 3.2 Run full multi-module test suite with reused JVMs; record and fix any cross-test contamination failures

## 4. Migrate solim-core remaining tests

- [x] 4.1 Convert remaining solim-core tests with hand-rolled `MockApplication`/`MockGraphics`/Scene/prev*-restore setup to `SolimEnv` (or `ArcTestEnv` for pure-logic tests); delete hand-rolled boilerplate, keep any test-local extra style registration on top of env scene
- [x] 4.2 Run solim-core suite; fix surfaced leaks

## 5. Migrate solim-runtime tests

- [x] 5.1 Convert solim-runtime tests using hand-rolled setup to `SolimEnv`
- [x] 5.2 Run solim-runtime suite; fix surfaced leaks

## 6. Migrate solim-mcp tests

- [x] 6.1 Convert solim-mcp tests using hand-rolled setup to the env
- [x] 6.2 Run solim-mcp suite; fix surfaced leaks

## 7. Migrate solim facade module tests

- [x] 7.1 Convert `solim` module tests using hand-rolled setup to the env
- [x] 7.2 Run `solim` suite; fix surfaced leaks

## 8. Mindustry extension and mod migration

- [x] 8.1 Create `MindustryTestEnv` in `mod/src/test` extending `SolimEnv`: `Icon` registry snapshot/restore via reflection, `FeatureManager` clear in setup, `Settings` handling
- [x] 8.2 Convert mod tests using hand-rolled setup to `MindustryTestEnv` (delete `Icon.book`-style unguarded mutations and per-file `prev*` boilerplate)
- [x] 8.3 Run mod suite; fix surfaced leaks

## 9. Final validation

- [x] 9.1 Full clean `gradlew test` across all modules; confirm green with reused JVMs and compare runtime against forked baseline
- [x] 9.2 Grep verification: no hand-rolled `MockApplication`/`MockGraphics`/`MockGL20` instantiation remains outside `solim-test` and `MindustryTestEnv`
- [x] 9.3 Run `gradlew check` (checkstyle) on touched modules
