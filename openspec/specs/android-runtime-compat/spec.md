# android-runtime-compat Specification

## Purpose

Shipped code must load under Mindustry's Android `ModClassLoader` without desugaring-companion classes; specifically, no references to `java.util.function` types in shipped sources. Created by archiving change arc-func-migration.

## Requirements

### Requirement: No java.util.function in shipped sources
Shipped sources SHALL NOT reference `java.util.function` types. All callables in shipped code SHALL use `arc.func.*` equivalents or minimal plain SAM interfaces with no default or static methods.

#### Scenario: Shipped tree is clean
- **WHEN** scanning `src/main` of every shipped module (`solim-api`, `solim-runtime`, `solim-core`, `solim`, `mod`)
- **THEN** no file imports or names a `java.util.function` type

#### Scenario: ReactiveGrid initializes without companions
- **WHEN** `ReactiveGrid` is constructed on Android under Mindustry's `ModClassLoader`
- **THEN** construction succeeds without resolving `Function$-CC` or any `-CC` companion class

### Requirement: Identical runtime semantics
The migration SHALL preserve call-site behavior exactly. Every substituted callable SHALL accept the same inputs and produce the same outputs as before.

#### Scenario: Existing tests pass unchanged in behavior
- **WHEN** running the full unit test suite after migration
- **THEN** all previously passing tests still pass (updated only for type names where they referenced old types)

#### Scenario: QuickAccess HUD builds at startup
- **WHEN** `FeatureManager.init` enables `QuickAccessFeature` and builds `QuickAccessHudView`
- **THEN** the HUD builds without `NoClassDefFoundError` on Android

### Requirement: Permanent guardrail
The repository SHALL enforce the ban via automated checks and documented constraints so the class of bug cannot re-enter.

#### Scenario: Checkstyle fails shipped violations
- **WHEN** a `java.util.function` import is added to any shipped source
- **THEN** the check build fails with a clear rule message

#### Scenario: AGENTS.md documents the ban
- **WHEN** reading the Java Compatibility section of AGENTS.md
- **THEN** it lists `java.util.function.*` as forbidden with the `arc.func` replacements
