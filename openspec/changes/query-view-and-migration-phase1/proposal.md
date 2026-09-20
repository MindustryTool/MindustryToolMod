## Why

Now that `Query<T>`, `Mutation<T, R>`, and `QueryCache` primitives are established, components still need boilerplate `dynamic()` blocks to handle loading spinners, error cards, and refetch indicators. Furthermore, existing features across the mod still use legacy, ad-hoc data fetching patterns (manual boolean flags in `BrowserFilterDialog`, un-cached author lookups in detail dialogs, standing timers in `ServerService`, and manual memoization in `Github`).

Introducing a declarative `QueryView<T>` component and migrating Phase 1 features (Browser Dialogs & Core Services) will unify data fetching, eliminate repetitive loading/error boilerplate, and provide automatic caching and deduplication across the mod.

## What Changes

- **New `QueryView<T>` component** in `solim-core`: A declarative reactive component that automatically switches between `.loading()`, `.error()`, and `.data()` states.
  - Defaults: Centered spinner for loading; scarlet error text with an auto-wired `"Retry"` button calling `query.refetch()` when error occurs.
  - SWR awareness: Exposes both `.data(data -> ...)` and `.data((data, isFetching) -> ...)`.
- **Public facade in `solim.UI`**: `UI.query(Query<T> query)` returning `QueryView<T>`.
- **Migration of Browser Dialogs** (`:mod`):
  - `BrowserFilterDialog`: Replace `tagsLoaded` and `planetsLoaded` boolean flags with cached queries for tags and planets (`staleTime = 10m`).
  - `MapDetailDialog` & `SchematicDetailDialog`: Replace manual author user batch lookups with `Query.of(QueryKey.of("user", authorId), ...)` rendered via `QueryView`.
- **Migration of Core Services** (`:mod`):
  - `ServerService`: Migrate server list fetching to a `Query` with `refetchInterval(Duration.ofMinutes(15))`, eliminating manual `Timer.schedule` and `Vars.ui.join.shown()` listeners.
  - `UpdateService` & `Github`: Migrate startup warming to `QueryCache.getInstance().prefetch()` and replace `Github.Memo` with queries.

## Capabilities

### New Capabilities
- `query-view`: Declarative UI component (`QueryView<T>`) and `UI.query(Query<T>)` that automatically renders loading, error (with auto-wired retry), and data states with SWR refetch awareness.

### Modified Capabilities
- `browsers`: Migrating BrowserFilterDialog (tags & planets) and DetailDialogs (MapDetailDialog & SchematicDetailDialog author lookup) to use Query and QueryView.
- `services`: Migrating ServerService (15m polling query) and UpdateService/Github (startup prefetch).

## Impact

- **`solim-core`**: Adds `QueryView.java` in `solim.reactive` and unit tests in `solim-core/src/test/`.
- **`solim`**: Exposes `UI.query(Query<T>)` in `solim.UI`.
- **`mod`**: Refactors `BrowserFilterDialog`, `MapDetailDialog`, `SchematicDetailDialog`, `ServerService`, `UpdateService`, and `Github`.
- **Dependencies**: No new dependencies.
- **Breaking changes**: None. Public APIs and dialog behavior are preserved.
