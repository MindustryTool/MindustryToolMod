# query-primitive Specification

## Purpose

Async reactive primitive `Query<T>` — the async counterpart to `Computed` — that tracks reactive dependencies, auto-fetches when deps change, auto-marshals results to the main thread, and exposes results as `Readable<>` signals. Established by change `solim-query` to unify data fetching patterns.

## Requirements

### Requirement: Query Creation
`Query<T>` SHALL be created via `Query.of(QueryKey key, Supplier<CompletableFuture<T>> fetcher)`. It SHALL automatically register with the active `ComponentContext` for lifecycle ownership.

#### Scenario: Query created in component build
- **WHEN** `Query.of(key, fetcher)` is called inside a component's `build()` method
- **THEN** the Query SHALL be registered with the component's lifecycle and disposed when the component disposes

#### Scenario: Query eager fetch
- **WHEN** a Query is created
- **THEN** it SHALL immediately trigger its first fetch (unless disabled or cache has fresh data)

### Requirement: Reactive Dependency Tracking
`Query` SHALL automatically track reactive dependencies read inside the fetcher lambda, using the same `ReactiveContext` mechanism as `Computed` and `Effect`.

#### Scenario: Auto-refetch on dependency change
- **WHEN** a fetcher reads `channelId.get()` and `page.get()`, and `channelId` is later changed
- **THEN** the Query SHALL automatically trigger a new fetch with the updated `channelId` value

#### Scenario: Multiple dependency changes batched
- **WHEN** multiple tracked signals change within the same frame
- **THEN** the Query SHALL batch them and trigger only one refetch (via `SignalDispatcher` frame batching)

### Requirement: Async Result State
`Query<T>` SHALL expose its state via four `Readable<>` accessors:
- `data()` — `Readable<T>` containing the last successful result (or null if never fetched)
- `loading()` — `Readable<Boolean>` true only when no data has been fetched yet (initial load)
- `fetching()` — `Readable<Boolean>` true whenever any fetch is in-flight
- `error()` — `Readable<Throwable>` containing the last error (or null if last fetch succeeded)

#### Scenario: Initial state before first fetch completes
- **WHEN** a Query is created and the first fetch has not yet completed
- **THEN** `data()` SHALL return null, `loading()` SHALL return true, `fetching()` SHALL return true, `error()` SHALL return null

#### Scenario: Successful first fetch
- **WHEN** the first fetch completes successfully with result `R`
- **THEN** `data()` SHALL return `R`, `loading()` SHALL return false, `fetching()` SHALL return false, `error()` SHALL return null

#### Scenario: Failed first fetch
- **WHEN** the first fetch fails with error `E`
- **THEN** `data()` SHALL remain null, `loading()` SHALL return false, `fetching()` SHALL return false, `error()` SHALL return `E`

#### Scenario: Stale-while-revalidate during refetch
- **WHEN** a refetch is triggered after a successful first fetch
- **THEN** `data()` SHALL retain the previous value, `loading()` SHALL return false, `fetching()` SHALL return true

#### Scenario: Refetch success updates data
- **WHEN** a refetch completes successfully with new result `R2`
- **THEN** `data()` SHALL return `R2`, `fetching()` SHALL return false, `error()` SHALL return null

### Requirement: Main Thread Marshaling
All signal mutations resulting from async fetch completion SHALL be executed on the main thread via `Core.app.post()`. The Query consumer SHALL NOT need to handle thread marshaling.

#### Scenario: Background thread completion
- **WHEN** a `CompletableFuture` resolves on a background thread
- **THEN** the Query SHALL marshal the `data`/`loading`/`fetching`/`error` signal updates to the main thread before applying them

### Requirement: Race Condition Handling
`Query` SHALL discard results from stale fetches using an internal generation counter. Only the result of the most recently initiated fetch SHALL be applied.

#### Scenario: Stale fetch result discarded
- **WHEN** fetch A starts, then dependency changes trigger fetch B, then fetch A completes
- **THEN** fetch A's result SHALL be discarded and only fetch B's result SHALL be applied

### Requirement: Enabled/Disabled Queries
`Query` SHALL support an `.enabled(Readable<Boolean>)` configuration. When disabled, the Query SHALL not fetch and SHALL retain its current state.

#### Scenario: Disabled query does not fetch
- **WHEN** a Query is created with `.enabled(Readable.of(false))`
- **THEN** no fetch SHALL be triggered

#### Scenario: Enabled transition triggers fetch
- **WHEN** an enabled signal transitions from false to true
- **THEN** the Query SHALL trigger a fetch

#### Scenario: Disabled transition does not clear data
- **WHEN** an enabled signal transitions from true to false while data is present
- **THEN** the existing `data()` SHALL be retained

### Requirement: Refetch Interval
`Query` SHALL support `.refetchInterval(Duration)` for periodic automatic refetching while the Query is active and enabled.

#### Scenario: Periodic refetch fires
- **WHEN** a Query is configured with `.refetchInterval(Duration.ofMinutes(15))`
- **THEN** the Query SHALL automatically refetch every 15 minutes while active

#### Scenario: Interval stops on dispose
- **WHEN** a Query with a refetch interval is disposed
- **THEN** the periodic timer SHALL be cancelled

#### Scenario: Interval paused when disabled
- **WHEN** a Query with a refetch interval has its enabled signal set to false
- **THEN** the periodic timer SHALL be paused until re-enabled

### Requirement: Retry with Backoff
`Query` SHALL retry failed fetches with exponential backoff. Default: 3 retries with base delay of 1 second (1s, 2s, 4s). Configurable via `.retry(int count)`.

#### Scenario: Automatic retry on failure
- **WHEN** a fetch fails and retry count has not been exhausted
- **THEN** the Query SHALL schedule a retry after the backoff delay

#### Scenario: Retry exhausted sets error
- **WHEN** all retry attempts fail
- **THEN** `error()` SHALL contain the last failure's throwable, and no more automatic retries SHALL occur

#### Scenario: Retry counter resets on success
- **WHEN** a fetch succeeds after retries
- **THEN** the retry counter SHALL reset to 0 for the next fetch cycle

#### Scenario: Disable retry
- **WHEN** a Query is configured with `.retry(0)`
- **THEN** no automatic retry SHALL occur on failure

### Requirement: Manual Refetch
`Query` SHALL expose a `refetch()` method for manually triggering a new fetch.

#### Scenario: Manual refetch
- **WHEN** `query.refetch()` is called
- **THEN** a new fetch SHALL be triggered regardless of stale time, and the result SHALL update the cache entry

### Requirement: Disposal
`Query` SHALL implement `Disposable`. On disposal, it SHALL cancel any in-flight fetch, unregister from the cache entry, stop any refetch interval timer, and dispose its internal Effect.

#### Scenario: Dispose cancels in-flight
- **WHEN** a Query is disposed while a fetch is in-flight
- **THEN** the in-flight result SHALL be discarded (not applied to signals)

#### Scenario: Dispose unregisters from cache
- **WHEN** a Query is disposed
- **THEN** the cache entry's observer count SHALL decrease, potentially starting the GC timer

#### Scenario: Auto-dispose on component dispose
- **WHEN** the parent Solim component is disposed
- **THEN** the Query SHALL be automatically disposed via `ComponentContext` ownership

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

### Requirement: Dialog-Scoped Query Ownership
Queries created for a dialog, including via dialog-owned state holders constructed inside the dialog's component scope, SHALL be disposed automatically when the dialog component disposes, detaching their `QueryCache` observers.

#### Scenario: Open-close cycle releases observers
- **WHEN** a browser dialog is opened and closed
- **THEN** the `QueryCache` observer count for its query key SHALL return to its prior baseline and unobserved entries SHALL become GC-eligible

#### Scenario: State holder created outside scope disposes explicitly
- **WHEN** a state holder owning a `Query` is created outside any component scope
- **THEN** the holder SHALL implement `Disposable` and dispose its `Query` explicitly on teardown
