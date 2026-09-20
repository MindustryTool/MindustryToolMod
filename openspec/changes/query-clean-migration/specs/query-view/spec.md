## ADDED Requirements

### Requirement: Mount-Time Freshness Trigger
`QueryView<T>` SHALL invoke `query.ensureFresh()` inside its `build()` method when mounting into the component hierarchy.

#### Scenario: QueryView mounts stale query
- **WHEN** `QueryView` mounts while bound to a query whose data is stale
- **THEN** it SHALL display the cached data immediately and trigger a background refetch without flickering

#### Scenario: QueryView mounts errored query
- **WHEN** `QueryView` mounts while bound to a query whose previous fetch failed
- **THEN** it SHALL automatically trigger `refetch()` to retry loading

#### Scenario: QueryView mounts fresh query
- **WHEN** `QueryView` mounts while bound to a query whose data was fetched recently within `staleTime`
- **THEN** it SHALL display the cached data without triggering a network fetch
