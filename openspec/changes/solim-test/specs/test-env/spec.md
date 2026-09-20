# test-env — Delta Spec

## ADDED Requirements

### Requirement: Shared test environment module
The build SHALL provide a `solim-test` Java library module containing the shared test environment, and every testable module SHALL consume it via `testImplementation`.

#### Scenario: Module depends on test-support
- **WHEN** a module's test source set compiles test code using `ArcTestEnv` or `SolimEnv`
- **THEN** compilation succeeds via the `testImplementation(project("solim-test"))` dependency without any module depending on `solim-test` in production configurations

#### Scenario: test-support is excluded from shipped artifacts
- **WHEN** the mod jar is built
- **THEN** no `solim-test` classes are included in the shipped jar

### Requirement: Idempotent Arc environment initialization
`ArcTestEnv` SHALL initialize Arc statics (`Core.app`, `Core.graphics`, `Core.gl`, `Core.gl20`) exactly once per JVM, only when they are not already initialized.

#### Scenario: Initialization runs before any test
- **WHEN** a test class using the env begins and `Core.app` is null
- **THEN** `Core.app`, `Core.graphics`, `Core.gl`, and `Core.gl20` are set to mock implementations

#### Scenario: Repeated initialization does not overwrite
- **WHEN** a second test class using the env begins and `Core.app` is already set
- **THEN** the existing `Core.*` instances are left untouched

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

### Requirement: Restoring teardown with leak asserts
The env SHALL restore all environment-owned globals (`Core.*` fields, `Core.scene`, `Core.settings`, and Mindustry-side stubbed globals such as the `Icon` registry where the mod extension applies) to their pre-test values in teardown, and SHALL fail the test if solim ambient state is non-empty at teardown.

#### Scenario: Globals restored after test
- **WHEN** a test finishes after the env (or test code) mutated `Core.scene`, `Core.settings`, or a stubbed `Icon` field
- **THEN** teardown restores each field to its pre-test value

#### Scenario: Leaked ambient state fails the test
- **WHEN** teardown runs while `ParentStack`, `ComponentContext`, `ReactiveContext`, or `SignalDispatcher` hold entries
- **THEN** the test fails with an assertion identifying which ambient state leaked

#### Scenario: Cleanup happens before asserts evaluate
- **WHEN** teardown runs on a failing test
- **THEN** ambient state is still cleaned up so subsequent tests are not contaminated

### Requirement: Layered environment classes
The env SHALL be layered: `SolimEnv` extends `ArcTestEnv`; pure-logic tests SHALL use `ArcTestEnv` and UI/reactive tests SHALL use `SolimEnv`.

#### Scenario: Pure-logic test without solim runtime
- **WHEN** a test only exercises code needing Arc statics and extends `ArcTestEnv`
- **THEN** it runs without any solim ambient state being initialized

### Requirement: Mindustry test extension in mod
The `mod` module SHALL provide a Mindustry-layer test extension (extending the shared env) that stubs Mindustry globals (`Icon` fields via save/restore, `FeatureManager` cleared) and restores them in teardown.

#### Scenario: Icon mutations restored
- **WHEN** a test stubs an `Icon` field and the test finishes
- **THEN** the original `Icon` field value is restored

#### Scenario: FeatureManager cleared between tests
- **WHEN** a test begins and a previous test registered features
- **THEN** `FeatureManager` is empty at the start of the test

### Requirement: forkEvery removed
The root build SHALL NOT configure `forkEvery = 1`; test classes within and across modules SHALL execute in reused worker JVMs.

#### Scenario: Reused JVM across test classes
- **WHEN** multiple test classes in the same or different modules run in one Gradle worker
- **THEN** no test fails due to another class's leaked static state

### Requirement: SolimTestHarness replaced
`SolimTestHarness` SHALL be removed and its tests re-parented onto `SolimEnv` with equivalent behavior.

#### Scenario: Old harness no longer exists
- **WHEN** the migration is complete
- **THEN** `solim-core/src/test/java/solim/test/SolimTestHarness.java` does not exist and all its former subclasses extend `SolimEnv`

### Requirement: All tests migrated
All existing test classes SHALL use the shared env instead of hand-rolled `MockApplication`/`MockGraphics` setup; test bodies SHALL NOT be modified during migration.

#### Scenario: No hand-rolled env setup remains
- **WHEN** the migration is complete
- **THEN** no test file outside `solim-test` and the mod Mindustry extension instantiates `MockApplication`, `MockGraphics`, or `MockGL20` directly
