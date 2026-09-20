## Context

With `Query<T>`, `Mutation<T, R>`, and `QueryCache` primitives established in `solim-core`, application UI components need a clean declarative way to render query states without hand-rolling `dynamic()` blocks and lifecycle guards.

Additionally, multiple features across the mod still use legacy data fetching patterns:
- `BrowserFilterDialog` uses instance boolean flags (`tagsLoaded`, `planetsLoaded`) for one-off loading.
- `MapDetailDialog` and `SchematicDetailDialog` manually invoke `MindustryTool.getUserBatch()` with manual `isDisposed()` checks and no cross-dialog caching.
- `ServerService` runs a global 15-minute `Timer.schedule` plus a `Vars.ui.join.shown()` hook with manual `Core.app.post()` marshaling.
- `Github` uses a custom `Memo<T>` atomic reference class for session caching and prefetching.

## Goals / Non-Goals

**Goals:**

- Implement `QueryView<T>` in `solim-core` extending `BaseComponent` with full `CellConfig`, `TableConfig`, and `ElementConfig` support.
- Expose `UI.query(Query<T>)` in `solim.UI` as an overloaded declarative factory.
- Provide built-in defaults for `.loading()` (centered spinner) and `.error()` (scarlet error text with auto-wired "Retry" button calling `query.refetch()`).
- Support both `.data(data -> ...)` and `.data((data, isFetching) -> ...)` for stale-while-revalidate UX.
- Migrate Phase 1 features:
  1. `BrowserFilterDialog` (tags and planets queries with 10m stale time)
  2. `MapDetailDialog` & `SchematicDetailDialog` (author profile lookup via cached user query)
  3. `ServerService` (server list query with 15m `refetchInterval`)
  4. `UpdateService` & `Github` (startup cache prefetching via `QueryCache.prefetch()`)

**Non-Goals:**

- Phase 2 features (Chat, PlayerConnect, Translation) — deferred to a subsequent change.
- Altering existing public API contracts of dialogs or services.

## Decisions

### 1. `QueryView<T>` State Resolution

`QueryView<T>` wraps an Arc `Table container = new Table()` and mounts child components dynamically using an internal `Effect`:

```
┌────────────────────────────────────────────────────────┐
│ State Resolution Order:                                │
│                                                        │
│ 1. query.loading() is true                             │
│    → Render loading (custom or default Loader.centered)│
│                                                        │
│ 2. query.error() != null && query.data() == null       │
│    → Render error (custom or default ErrorWithRetry)   │
│                                                        │
│ 3. query.data() != null                                │
│    → Render data(data, isFetching)                     │
│      (If error occurred during SWR refetch, old data   │
│       stays visible and error is non-fatal)            │
│                                                        │
│ 4. Fallback: empty container                           │
└────────────────────────────────────────────────────────┘
```

### 2. Default UI and Internationalization

When `.loading()` is omitted:
- Defaults to `Loader.centered()` (or equivalent centered spinner).

When `.error()` is omitted:
- Defaults to a column displaying the error message in scarlet text and a "Retry" button that calls `query.refetch()`.
- Reuses existing translation keys from `assets/bundles/bundle.properties` (`button.retry`, etc.) to adhere to the mandatory i18n rule.

### 3. Component Hierarchy and Lifecycle

`QueryView<T>` extends `BaseComponent` and implements `CellConfig`, `TableConfig`, and `ElementConfig`.
- Child components created during state switches are isolated via `ParentStack.isolate()` and properly disposed when state transitions occur, preventing memory leaks.
- Parent cell constraints (`minWidth`, `growX`, etc.) are updated via `PendingCellConfig`, matching `Dynamic<T>`.

### 4. Migration Details

#### BrowserFilterDialog
- Replace `cachedTags` / `cachedPlanets` signals and manual `tagsLoaded` / `planetsLoaded` boolean flags with:
  ```java
  Query<List<TagCategory>> tagsQuery = Query.of(
      QueryKey.of("tags", tagGroup),
      () -> MindustryTool.getTags(tagGroup)
  ).staleTime(Duration.ofMinutes(10));
  ```
- Render filter options using `query(tagsQuery).data(tags -> ...)`.

#### Detail Dialogs (Map & Schematic)
- Replace manual `whenComplete` author profile lookups with:
  ```java
  Query<UserData> authorQuery = Query.of(
      QueryKey.of("user", createdBy),
      () -> MindustryTool.getUserBatch(Collections.singletonList(createdBy))
          .thenApply(list -> list != null && !list.isEmpty() ? list.get(0) : null)
  );
  ```
- Multiple detail dialogs opened for items by the same author share the cache entry with 0 redundant network calls.

#### ServerService
- Replace standing `Timer.schedule` and `Vars.ui.join.shown()` with:
  ```java
  Query<List<Server>> serversQuery = Query.of(
      QueryKey.of("servers"),
      () -> MindustryTool.getServers(0, 100)
  ).refetchInterval(Duration.ofMinutes(15));
  ```

#### UpdateService & Github
- In `Main.java` or `Github.java`, replace `Github.prefetchAll()`'s manual future tracking with:
  ```java
  QueryCache.getInstance().prefetch(QueryKey.of("modHjson"), Github::getModHjson);
  QueryCache.getInstance().prefetch(QueryKey.of("releases"), Github::getReleases);
  ```

## Risks / Trade-offs

- **[Risk] State flicker if query quickly transitions loading → data** → Addressed by `QueryView` checking if cache entry already has fresh data; in that case, `data()` is rendered immediately on the first frame without any loading flicker.
- **[Risk] SWR refetch error replacing existing data** → By prioritizing `query.data() != null` over `query.error() != null`, existing data remains on screen even if a background refetch fails, maintaining a smooth user experience.
