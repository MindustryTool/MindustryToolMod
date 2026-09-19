## ADDED Requirements

### Requirement: Structured Query Key
`QueryKey` SHALL be an immutable value type that wraps an ordered sequence of objects and provides structural equality via `equals()` and `hashCode()`. QueryKey SHALL be created via a static factory method `QueryKey.of(Object... parts)`.

#### Scenario: Key equality
- **WHEN** two `QueryKey` instances are created with the same parts in the same order
- **THEN** they SHALL be equal via `equals()` and produce the same `hashCode()`

#### Scenario: Key inequality
- **WHEN** two `QueryKey` instances are created with different parts or different order
- **THEN** they SHALL NOT be equal via `equals()`

#### Scenario: Key with mixed types
- **WHEN** a `QueryKey` is created with `QueryKey.of("messages", "channel-123")`
- **THEN** it SHALL be a valid cache key distinguishable from `QueryKey.of("messages", "channel-456")`

#### Scenario: Key string representation
- **WHEN** `toString()` is called on a `QueryKey`
- **THEN** it SHALL return a human-readable representation of its parts for debugging

### Requirement: Global Cache Singleton
`QueryCache` SHALL be a global singleton accessed via `QueryCache.getInstance()`. It SHALL store cache entries keyed by `QueryKey`.

#### Scenario: Singleton access
- **WHEN** `QueryCache.getInstance()` is called from any location
- **THEN** it SHALL return the same instance

#### Scenario: Cache is initially empty
- **WHEN** the cache is first accessed
- **THEN** it SHALL contain no entries

### Requirement: Cache Entry Lifecycle
Each cache entry SHALL track: the cached data value, the last error (if any), the timestamp of the last successful fetch, the count of active Query observers, and any in-flight `CompletableFuture`.

#### Scenario: Entry creation on first query
- **WHEN** a `Query` with a new key is created and fetches data
- **THEN** a cache entry SHALL be created for that key

#### Scenario: Entry shared across queries
- **WHEN** two `Query` instances use the same `QueryKey`
- **THEN** they SHALL share the same cache entry and observe the same data/loading/error state

### Requirement: Stale Time
Each cache entry SHALL have a configurable stale time (default 30 seconds). Data fetched within the stale time SHALL be considered fresh and served without triggering a new fetch.

#### Scenario: Fresh data served from cache
- **WHEN** a `Query` is created with a key that has a cache entry fetched less than `staleTime` ago
- **THEN** the Query SHALL use the cached data without issuing a network request

#### Scenario: Stale data triggers refetch
- **WHEN** a `Query` is created with a key that has a cache entry fetched more than `staleTime` ago
- **THEN** the Query SHALL serve the cached data immediately AND trigger a background refetch

#### Scenario: Custom stale time
- **WHEN** a Query is configured with `.staleTime(Duration.ofMinutes(5))`
- **THEN** its cache entry SHALL use 5 minutes as the stale time instead of the default

### Requirement: Garbage Collection
Cache entries with no active Query observers SHALL be evicted after a configurable GC time (default 5 minutes).

#### Scenario: GC timer starts on last observer detach
- **WHEN** the last `Query` observing a cache entry is disposed
- **THEN** a GC timer SHALL start for that entry

#### Scenario: GC timer cancelled on new observer
- **WHEN** a new `Query` attaches to a cache entry before the GC timer fires
- **THEN** the GC timer SHALL be cancelled and the entry retained

#### Scenario: Entry evicted after GC time
- **WHEN** the GC timer fires without a new observer attaching
- **THEN** the cache entry SHALL be removed from the cache

#### Scenario: Custom GC time
- **WHEN** a Query is configured with `.gcTime(Duration.ofMinutes(10))`
- **THEN** its cache entry SHALL use 10 minutes as the GC time

### Requirement: In-Flight Request Deduplication
When multiple `Query` instances with the same key trigger a fetch simultaneously, only one HTTP request SHALL be made. All observers SHALL receive the result of the single request.

#### Scenario: Concurrent queries deduplicated
- **WHEN** two `Query` instances with the same key both trigger a fetch at the same time
- **THEN** only one `CompletableFuture` SHALL be created, and both queries SHALL observe its result

#### Scenario: Failed dedup request retried independently
- **WHEN** a deduplicated request fails
- **THEN** a subsequent fetch attempt SHALL create a new request (not reuse the failed future)

### Requirement: Cache Invalidation
`QueryCache` SHALL support invalidating cache entries, which marks them as stale and triggers an immediate refetch for any active Query observers.

#### Scenario: Invalidate by exact key
- **WHEN** `cache.invalidate(QueryKey.of("channels"))` is called
- **THEN** the cache entry for that exact key SHALL be marked stale, and any active Query with that key SHALL refetch immediately

#### Scenario: Invalidate all
- **WHEN** `cache.invalidateAll()` is called
- **THEN** all cache entries SHALL be marked stale, and all active Queries SHALL refetch immediately

#### Scenario: Invalidation of key with no active query
- **WHEN** a key is invalidated but no active Query observes it
- **THEN** the cached data SHALL be cleared but no fetch SHALL occur

### Requirement: Prefetch Support
`QueryCache` SHALL support prefetching data into the cache without requiring a UI-attached Query observer.

#### Scenario: Prefetch populates cache
- **WHEN** `cache.prefetch(key, fetcher)` is called
- **THEN** the fetcher SHALL execute, and the result SHALL be stored in the cache entry for that key

#### Scenario: Prefetched data served to subsequent Query
- **WHEN** a `Query` is later created with a key that was prefetched and is still fresh
- **THEN** the Query SHALL use the prefetched data without issuing a new request

#### Scenario: Prefetch failure does not cache error
- **WHEN** a prefetch fails
- **THEN** no error SHALL be cached, and a subsequent Query or prefetch SHALL retry
