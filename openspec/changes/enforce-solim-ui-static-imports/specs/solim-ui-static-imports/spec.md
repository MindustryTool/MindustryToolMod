## ADDED Requirements

### Requirement: Solim UI re-exports modifier factories
The `solim.UI` class SHALL re-export all modifier factory methods provided by `solim.modifier.Values` so that callers can access all modifiers via `import static solim.UI.*`.

#### Scenario: Modifier accessibility through solim.UI
- **WHEN** a component imports `static solim.UI.*`
- **THEN** it can call modifier methods such as `wrap()`, `size()`, `prefWidth()`, `prefHeight()`, `border()`, `rounded()`, `margin()`, `tooltip()`, and `fontScale()` without referencing `solim.modifier.Values`.

### Requirement: Disallow Values references in mod sources
The build verification system SHALL enforce that no file in the `:mod` subproject references `solim.modifier.Values` directly in source code or import declarations.

#### Scenario: Failure on explicit Values qualification
- **WHEN** a Java file in `mod/src` or `mod/src/test/java` contains `Values.<method>(...)` or `solim.modifier.Values.<method>(...)`
- **THEN** running `./gradlew check` or `./gradlew checkSolimModifierImports` SHALL fail with a violation message pointing to the file and line number.

#### Scenario: Failure on Values import
- **WHEN** a Java file in `mod/src` or `mod/src/test/java` imports `solim.modifier.Values` or `static solim.modifier.Values.*`
- **THEN** running `./gradlew check` or `./gradlew checkSolimModifierImports` SHALL fail with a violation message instructing the caller to use `import static solim.UI.*`.

#### Scenario: Pass on compliant imports
- **WHEN** all Java files in `mod/src` and `mod/src/test/java` use `import static solim.UI.*` and contain no direct references to `Values`
- **THEN** running `./gradlew checkSolimModifierImports` SHALL succeed without errors.
