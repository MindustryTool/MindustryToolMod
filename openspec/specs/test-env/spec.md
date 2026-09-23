# test-env Specification

## Purpose

Shared, idempotent, layered test environment for Arc/solim/Mindustry tests, providing per-test fresh state, restoring teardown with leak asserts, and module-level wiring so tests run in reused worker JVMs without leaked static state. Created by archiving change solim-test.
## Requirements
### Requirement: Shared test environment module
The build SHALL provide a `solim-test` Java library module containing the shared, layered test environment (`ArcTestEnv` plus the runtime-level `SolimEnv`), and modules that can consume it without introducing a build-path cycle SHALL consume it via `testImplementation`. A module SHALL NOT declare a `solim-test` dependency when `solim-test` (directly or transitively) depends on that module.

#### Scenario: Module depends on test-support
- **WHEN** a module's test source set compiles test code using `ArcTestEnv` or `SolimEnv`, and the module is not a dependency of `solim-test`
- **THEN** compilation succeeds via the `testImplementation(project("solim-test"))` dependency without any module depending on `solim-test` in production configurations

#### Scenario: test-support is excluded from shipped artifacts
- **WHEN** the mod jar is built
- **THEN** no `solim-test` classes are included in the shipped jar

#### Scenario: No cyclic consumer
- **WHEN** a module that `solim-test` depends on (for example `solim-runtime`) needs shared test setup
- **THEN** it does not declare `testImplementation(project("solim-test"))` and instead uses a local environment defined in its own test sources

---

### Requirement: Idempotent Arc environment initialization
`ArcTestEnv` SHALL initialize Arc statics (`Core.app`, `Core.graphics`, `Core.gl`, `Core.gl20`) exactly once per JVM, only when they are not already initialized.

#### Scenario: Initialization runs before any test
- **WHEN** a test class using the env begins and `Core.app` is null
- **THEN** `Core.app`, `Core.graphics`, `Core.gl`, and `Core.gl20` are set to mock implementations

#### Scenario: Repeated initialization does not overwrite
- **WHEN** a second test class using the env begins and `Core.app` is already set
- **THEN** the existing `Core.*` instances are left untouched

---

### Requirement: Per-test fresh state
`ArcTestEnv` SHALL reset solim-relevant per-test state before each test: `Core.scene` SHALL be set to `null` (enabling headless component fallbacks, matching legacy `SolimTestHarness` behavior) and `Core.settings` SHALL be a fresh in-memory `Settings` instance. The env SHALL provide a `newScene()` helper that creates a fresh `Scene` with a minimal style set for tests needing a live scene.

#### Scenario: Fresh settings per test
- **WHEN** a test begins
- **THEN** `Core.settings` is a new empty `Settings` instance and values set in a previous test are absent

#### Scenario: Scene is headless-null by default
- **WHEN** a test begins
- **THEN** `Core.scene` is `null` and components with headless fallbacks work unchanged

#### Scenario: Explicit styled scene
- **WHEN** a test needs a live scene and calls the env's `newScene()` in setup
- **THEN** `Core.scene` is a fresh `Scene` with minimal styles registered and teardown restores the previous value

---

### Requirement: Restoring teardown with leak asserts
The env SHALL restore all environment-owned globals (`Core.*` fields, `Core.scene`, `Core.settings`, module-layer globals such as the default cell configurator where the relevant layer owns them, and Mindustry-side stubbed globals such as the `Icon` registry where the mod extension applies) to their pre-test values in teardown, and SHALL fail the test if solim ambient state is non-empty at teardown.

#### Scenario: Globals restored after test
- **WHEN** a test finishes after the env (or test code) mutated `Core.scene`, `Core.settings`, or a stubbed `Icon` field
- **THEN** teardown restores each field to its pre-test value

#### Scenario: Leaked ambient state fails the test
- **WHEN** teardown runs while `AttachmentStack`, `OwnershipContext`, `ReactiveContext`, or `SignalDispatcher` hold entries
- **THEN** the test fails with an assertion identifying which ambient state leaked

#### Scenario: Cleanup happens before asserts evaluate
- **WHEN** teardown runs on a failing test
- **THEN** ambient state is still cleaned up so subsequent tests are not contaminated

#### Scenario: Core configurator restored
- **WHEN** a test in the solim-core layer replaces the default cell configurator and finishes
- **THEN** the default configurator is reinstalled before the next test runs

---

### Requirement: Layered environment classes
The env SHALL be layered by module dependency level: `ArcTestEnv` (Arc statics only) is extended by `SolimEnv` (solim ambient state from `solim-runtime`), which is extended by a solim-core-owned environment (core-specific reset such as `PendingCellConfig`), which is extended by the mod's Mindustry extension. Tests SHALL use the lowest layer that meets their needs: pure-logic tests use `ArcTestEnv`, runtime/solim tests use `SolimEnv`, and solim-core tests that replace module-level globals use the solim-core layer.

#### Scenario: Pure-logic test without solim runtime
- **WHEN** a test only exercises code needing Arc statics and extends `ArcTestEnv`
- **THEN** it runs without any solim ambient state being initialized

#### Scenario: Runtime-level env does not reference solim-core
- **WHEN** `SolimEnv` (in `solim-test`) is compiled
- **THEN** it references no `solim-core` type and `solim-test` declares no `solim-core` dependency

#### Scenario: Core-level env extends the runtime env
- **WHEN** a test module above the solim-core layer needs the default cell configurator restored
- **THEN** the module-level environment extends `SolimEnv` and reinstalls `PendingCellConfig`'s default in its setup and teardown (solim-core tests via a solim-core-owned env, mod tests via `MindustryTestEnv`)

---

### Requirement: Mindustry test extension in mod
The `mod` module SHALL provide a Mindustry-layer test extension (extending the shared env) that stubs Mindustry globals (`Icon` fields via save/restore, `FeatureManager` cleared) and restores them in teardown.

#### Scenario: Icon mutations restored
- **WHEN** a test stubs an `Icon` field and the test finishes
- **THEN** the original `Icon` field value is restored

#### Scenario: FeatureManager cleared between tests
- **WHEN** a test begins and a previous test registered features
- **THEN** `FeatureManager` is empty at the start of the test

---

### Requirement: forkEvery removed
The root build SHALL NOT configure `forkEvery = 1`; test classes within and across modules SHALL execute in reused worker JVMs.

#### Scenario: Reused JVM across test classes
- **WHEN** multiple test classes in the same or different modules run in one Gradle worker
- **THEN** no test fails due to another class's leaked static state

---

### Requirement: SolimTestHarness replaced
`SolimTestHarness` SHALL be removed and its tests re-parented onto `SolimEnv` with equivalent behavior.

#### Scenario: Old harness no longer exists
- **WHEN** the migration is complete
- **THEN** `solim-core/src/test/java/solim/test/SolimTestHarness.java` does not exist and all its former subclasses extend `SolimEnv`

---

### Requirement: All tests migrated
Test classes that consume a shared env SHALL use the appropriate layer instead of hand-rolled `MockApplication`/`MockGraphics` setup; test bodies SHALL NOT be modified during migration. Modules whose tests do not consume a shared env SHALL manage any ambient state they touch locally and SHALL NOT declare an unused `solim-test` dependency.

#### Scenario: No hand-rolled env setup remains
- **WHEN** the migration is complete
- **THEN** no test file outside `solim-test` and the mod Mindustry extension instantiates `MockApplication`, `MockGraphics`, or `MockGL20` directly

#### Scenario: No unused test-support dependency
- **WHEN** a module's test sources contain no reference to `solim.test`
- **THEN** the module declares no `testImplementation(project("solim-test"))`

#### Scenario: Mod tests do not reference engine internals
- **WHEN** a mod test must pump pending reactive effects without a frame loop
- **THEN** it calls the `solim-test` facade (`SolimEnv.flushEffects()`) and `mod` declares no direct `solim-runtime` dependency; the runtime jar reaches mod's test runtime classpath only transitively

### Requirement: Acyclic test-support layering
A test-support type that references module X's types SHALL be owned by module X or by a module that X does not depend on, so that no test-support module is depended upon by a module it depends on. The project build-path graph (main and test dependencies combined) SHALL contain no cycles.

#### Scenario: solim-test depends only downward
- **WHEN** `solim-test`'s project dependencies are resolved across all configurations
- **THEN** it depends only on `solim-runtime`/`solim-api` and on no module that declares a test dependency on `solim-test`

#### Scenario: Core-specific reset is owned by solim-core
- **WHEN** the default cell configurator must be restored after a solim-core test replaces it
- **THEN** the restore is performed by a solim-core-owned test environment extending `SolimEnv`, not by `solim-test`

#### Scenario: No build-path cycle
- **WHEN** the workspace is imported into Eclipse/JDT
- **THEN** no project reports a build-path cycle warning

---

