## ADDED Requirements

### Requirement: Single-source rule definitions
The system SHALL define each map rule exactly once in a descriptor table, with all diff, dispatch, reset, and rendering behavior derived from it.

#### Scenario: Adding a rule touches one site
- **WHEN** a developer adds a new `Rules` field to the dialog
- **THEN** only one descriptor entry is required for diff, mutation, reset, search, and rendering to cover it

### Requirement: Preserved user-visible semantics
The system SHALL keep all rule labels, ordering, categories, filter behavior, editing modes, save flow, and reset outcomes identical to the pre-rework implementation.

#### Scenario: Dialog parity spot check
- **WHEN** the player opens Map Rules, searches, filters by Modified Only, edits sliders/toggles/bans, saves, and resets
- **THEN** every outcome matches the pre-rework behavior, including `*` modified markers and ban restore on reset

### Requirement: Unchanged localization surface
The system SHALL NOT rename, add, or remove any bundle key.

#### Scenario: Bundle verification passes untouched
- **WHEN** `./gradlew checkBundles` runs after the rework
- **THEN** all 12 bundles pass with 1102 keys each and no missing, obsolete, or untranslated keys

### Requirement: Clean verification gates
The system SHALL compile with no new warnings and pass all static checks after the rework.

#### Scenario: Verification passes
- **WHEN** `./gradlew :mod:compileJava :mod:checkstyleMain checkNoFullyQualifiedNames` runs
- **THEN** the build succeeds with no errors
