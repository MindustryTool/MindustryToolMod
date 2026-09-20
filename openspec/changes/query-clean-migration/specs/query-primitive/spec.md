## ADDED Requirements

### Requirement: Mount-Aware Freshness Check
`Query<T>` SHALL expose `isStale()`, `isError()`, and `ensureFresh()` methods to support mount-time refetching. `ensureFresh()` SHALL trigger `refetch()` if the query is not disposed, not currently fetching, and satisfies any of:
1. `!hasData()` (no data fetched yet)
2. `isError()` (previous fetch resulted in an error)
3. `isStale()` (cached data has exceeded `staleTime`)

If the query has cached data that is still fresh (within `staleTime`) and has not errored, `ensureFresh()` SHALL be a no-op.

#### Scenario: Stale query refetches on ensureFresh
- **WHEN** `query.ensureFresh()` is called on a query whose data has exceeded `staleTime`
- **THEN** a background `refetch()` SHALL be triggered

#### Scenario: Fresh query does not refetch on ensureFresh
- **WHEN** `query.ensureFresh()` is called on a query whose data was fetched within `staleTime`
- **THEN** no fetch SHALL be triggered

#### Scenario: Errored query refetches on ensureFresh
- **WHEN** `query.ensureFresh()` is called on a query whose last fetch failed with an error
- **THEN** a `refetch()` SHALL be triggered to retry the request
