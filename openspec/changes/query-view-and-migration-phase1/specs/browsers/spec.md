## ADDED Requirements

### Requirement: Cached Browser Filter Metadata
The browser filter dialog SHALL use cached `Query` instances for retrieving tag categories and planets, retaining data with a 10-minute stale time to avoid redundant network requests when reopening the filter dialog.

#### Scenario: Cached tags reuse
- **WHEN** user opens the browser filter dialog multiple times within 10 minutes
- **THEN** the tag list SHALL be served from the query cache without making new HTTP requests

#### Scenario: Cached planets reuse
- **WHEN** user switches filter tabs to planets multiple times within 10 minutes
- **THEN** the planet list SHALL be served from the query cache without making new HTTP requests

### Requirement: Cached Detail Author Resolution
Item detail dialogs (`MapDetailDialog` and `SchematicDetailDialog`) SHALL resolve item author profiles using a keyed `Query` (`QueryKey.of("user", authorId)`) rendered via `QueryView`.

#### Scenario: Author profile resolution
- **WHEN** a user opens a detail dialog for an item created by an author
- **THEN** the author profile SHALL be fetched and rendered via `QueryView`

#### Scenario: Deduplicated author lookups across dialogs
- **WHEN** user inspects multiple items created by the same author
- **THEN** only one network request SHALL be executed for that author, and subsequent detail dialogs SHALL render the cached author profile immediately
