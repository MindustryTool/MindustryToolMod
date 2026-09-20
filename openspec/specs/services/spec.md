# services Specification

## Purpose
Specifies query-backed caching, interval synchronization, and prefetching behaviors for mod core services including `ServerService`, `Github`, and `UpdateService`.

## Requirements

### Requirement: Query-Backed Server Directory
`ServerService` SHALL manage server list synchronization using a `Query` configured with a 15-minute `refetchInterval`.

#### Scenario: Periodic server directory update
- **WHEN** 15 minutes elapse while the game is running
- **THEN** the server directory `Query` SHALL automatically refetch server listings in the background

#### Scenario: Server directory manual refresh
- **WHEN** the multiplayer join dialog is opened
- **THEN** the server directory `Query` SHALL be refreshed if stale

### Requirement: Query-Backed Update and Metadata Prefetching
`UpdateService` and `Github` SHALL use `QueryCache` prefetching at startup to warm mod version and release metadata.

#### Scenario: Startup prefetch of mod metadata
- **WHEN** `ClientLoadEvent` fires during game startup
- **THEN** mod version metadata and GitHub releases SHALL be prefetched into `QueryCache`

#### Scenario: Update check cache hit
- **WHEN** `UpdateService.checkUpdate()` runs
- **THEN** it SHALL use the prefetched cache entry without blocking the main thread

### Requirement: Github Uses Unified Cache Path
`Github` metadata getters SHALL use the unified `QueryCache` fetch policy so startup prefetch warming is effective and duplicate callers share one network request.

#### Scenario: Prefetched releases served without refetch
- **WHEN** `prefetchAll` has warmed `QueryKey.of("github", "releases")` and it is still fresh
- **THEN** `getReleases()` SHALL return the cached body without issuing a new HTTP request

### Requirement: Translation Direct Fetch
Translation calls SHALL NOT retain long-term `QueryCache` entries. `TranslationFeature.translate` and `MindustryTool.translate` SHALL call the provider directly on every invocation, guarding only against duplicate concurrent taps via UI disablement.

#### Scenario: Repeated phrase re-fetches
- **WHEN** the same phrase is translated twice in separate actions
- **THEN** two provider requests SHALL be issued and no `QueryKey.of("translation", ...)` entry SHALL be retained afterwards

#### Scenario: Empty text short-circuits
- **WHEN** `translate` is called with null or blank text
- **THEN** it SHALL return the input (or empty string) without a network request
