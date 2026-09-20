## Context

The initial phase of Query migration introduced `Query<T>` and `QueryView<T>` to handle asynchronous data fetching, caching, retry, and lifecycle management. However, several consumers were implemented as "slapped-in" bridges:
1. `ChatChannels`: Maintained a legacy `channels` signal alongside `channelsQuery`, but failed to sync them. `ChatChannelListView` rendered the empty legacy signal despite a successful query fetch, and `activeChannelId` was never selected on initial load.
2. `PlayerConnectFeature`: Maintained a duplicate `rooms` signal synced via `Effect.of(...)`, violating single-source-of-truth rules.
3. `BrowserState`: Maintained duplicate `items`, `loading`, and `error` signals synced via 3 separate `Effect.of(...)` blocks, and dialogs used nested `dynamic()` calls instead of `QueryView`.
4. `Query` lacked mount awareness: Queries created at mod startup that failed (e.g. before network was ready) or expired their `staleTime` never refetched when views mounted, forcing users to click "Refresh" manually.

## Goals / Non-Goals

**Goals:**
- Implement mount-time freshness detection (`ensureFresh()`, `isStale()`, `isError()`) in `Query` and trigger it in `QueryView.build()`.
- Eliminate duplicate state and `Effect.of` bridges in `ChatChannels`, `PlayerConnectFeature`, and `BrowserState`.
- Make `ChatChannels` auto-select the first channel when channels load, and have `ChatChannelListView` directly consume query data.
- Refactor `SchematicBrowserDialog` and `MapBrowserDialog` to use declarative `query(state.query())` rather than nested `dynamic()` trees.

**Non-Goals:**
- Modifying the underlying HTTP client (`Request`) or API endpoints.
- Introducing pagination primitives into `Query` itself (paged queries remain parameterized via signals/keys).

## Decisions

### Decision 1: Mount-time freshness check via `Query.ensureFresh()`
- **Choice**: Add `ensureFresh()` to `Query`, and call it inside `QueryView.build()`.
- **Behavior**:
  - If the query is currently fetching or disposed: no-op.
  - If the query has no data, has errored, or `isStale()`: triggers `refetch()`.
  - If the query has cached data: renders cached data immediately; if stale, refetches in background (`stale-while-revalidate`).
  - If cached data is fresh (within `staleTime`): renders cached data without network request.
- **Alternatives considered**:
  - *Require `enabled(dialog.shown())`*: Requires every view to pass dialog visibility signals into queries. Fragile and verbose.
  - *Manual `shown(query::refetch)`*: Reintroduces imperative lifecycle handling in every dialog.

### Decision 2: Single Source of Truth in Features
- **Choice**:
  - `ChatChannels`: Remove the `channels` signal. `all()` returns `channelsQuery.data().map(list -> list != null ? list : Collections.emptyList())`. When data arrives, automatically set `activeChannelId` to the first channel if null or invalid.
  - `PlayerConnectFeature`: Remove the `rooms` signal and the bridge `Effect.of`. `getRooms()` returns `roomsQuery.data().map(...)`. SSE events directly mutate `roomsQuery`.
- **Alternatives considered**:
  - *Keep legacy signals and fix effect bridges*: Violates AGENTS.md ("One Source of Truth"). Led directly to the Chat 0-item rendering bug.

### Decision 3: Declarative `QueryView` in Browser Dialogs
- **Choice**: `BrowserState` exposes `query()` returning `Query<List<T>>`. `SchematicBrowserDialog` and `MapBrowserDialog` use `query(state.query()).loading(...).error(...).data(...)`.
- **Alternatives considered**:
  - *Keep `BrowserState.items()` as a computed Readable*: Keeps nested `dynamic()` trees in dialogs instead of using Solim's declarative `QueryView`.

## Risks / Trade-offs

- **[Risk]** Mount-time refetch could cause excessive network requests when dialogs open frequently.
  - **Mitigation**: Respect `staleTime`. If data was fetched within `staleTime`, `ensureFresh()` does nothing.
- **[Risk]** Auto-selecting the first channel might override an existing user selection.
  - **Mitigation**: Only select the first channel if `activeChannelId.get()` is null or not found in the loaded list.
