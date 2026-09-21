## ADDED Requirements

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

## MODIFIED Requirements

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

### Requirement: Restoring teardown with leak asserts
The env SHALL restore all environment-owned globals (`Core.*` fields, `Core.scene`, `Core.settings`, module-layer globals such as the default cell configurator where the relevant layer owns them, and Mindustry-side stubbed globals such as the `Icon` registry where the mod extension applies) to their pre-test values in teardown, and SHALL fail the test if solim ambient state is non-empty at teardown.

#### Scenario: Globals restored after test
- **WHEN** a test finishes after the env (or test code) mutated `Core.scene`, `Core.settings`, or a stubbed `Icon` field
- **THEN** teardown restores each field to its pre-test value

#### Scenario: Leaked ambient state fails the test
- **WHEN** teardown runs while `ParentStack`, `ComponentContext`, `ReactiveContext`, or `SignalDispatcher` hold entries
- **THEN** the test fails with an assertion identifying which ambient state leaked

#### Scenario: Cleanup happens before asserts evaluate
- **WHEN** teardown runs on a failing test
- **THEN** ambient state is still cleaned up so subsequent tests are not contaminated

#### Scenario: Core configurator restored
- **WHEN** a test in the solim-core layer replaces the default cell configurator and finishes
- **THEN** the default configurator is reinstalled before the next test runs

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