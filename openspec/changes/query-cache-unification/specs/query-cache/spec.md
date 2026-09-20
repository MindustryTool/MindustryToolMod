## ADDED Requirements

### Requirement: Unified Fetch Policy
`QueryCache` SHALL expose a single fetch policy: serve cached data when present, deduplicate concurrent inflight requests per key, write cache on success, and preserve last good data on failure without caching errors.

#### Scenario: Cached data served without network
- **WHEN** `fetch` is called with a key that has a cache entry with data
- **THEN** the cached data SHALL be returned without creating a new fetcher future, and staleness decisions SHALL be made by the `Query` layer via `staleTime`

#### Scenario: Concurrent callers share one request
- **WHEN** two callers fetch the same key simultaneously with no inflight request
- **THEN** only one fetcher future SHALL be created and both callers SHALL observe its result

#### Scenario: Failure preserves last good data
- **WHEN** a fetch fails and a cache entry already holds data
- **THEN** the cached data SHALL be retained unchanged and no error SHALL be written to the cache entry

#### Scenario: Failure with no cached data leaves cache empty
- **WHEN** a fetch fails and no cache entry data exists
- **THEN** the cache SHALL remain without data and a subsequent fetch SHALL retry with a new future
