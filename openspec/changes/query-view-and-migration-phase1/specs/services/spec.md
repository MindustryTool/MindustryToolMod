## ADDED Requirements

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
