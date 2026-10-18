## ADDED Requirements

### Requirement: Identical ban search results
The system SHALL return the same banned-block and banned-unit matches for any search query after the cleanup as before it.

#### Scenario: Search query matches unchanged
- **WHEN** the player types a query in the Map Rules search field
- **THEN** the displayed rule rows and ban chips are identical to the pre-cleanup behavior for that query

### Requirement: No user-visible text changes
The system SHALL NOT add, remove, or alter any user-visible string as part of the cleanup.

#### Scenario: Bundle verification passes untouched
- **WHEN** `./gradlew checkBundles` runs after the cleanup
- **THEN** all 12 bundles pass with 1102 keys each and no missing, obsolete, or untranslated keys

### Requirement: Preserved editing and reset semantics
The system SHALL preserve rule mutation, debounce, optimistic `/js` sync, map saving, and reset-to-snapshot behavior exactly.

#### Scenario: Reset restores snapshot including bans
- **WHEN** the player edits rules (including bans) and clicks Reset
- **THEN** active rules revert to the dialog-open snapshot, including banned blocks and units

### Requirement: Clean verification gates
The system SHALL compile with no new warnings and pass all static checks after the cleanup.

#### Scenario: Verification passes
- **WHEN** `./gradlew :mod:compileJava :mod:checkstyleMain checkNoFullyQualifiedNames` runs
- **THEN** the build succeeds with no errors
