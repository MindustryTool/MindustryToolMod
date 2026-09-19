## Why

Every async data fetch in the mod repeats the same boilerplate: fire a `CompletableFuture`, marshal the result to the main thread via `Core.app.post()`, manage separate loading/error/data signals, guard against disposed components, and handle race conditions. This pattern appears 30+ times across features (browser, chat, player connect, auth, translation). There is no shared cache, no request deduplication, and no automatic lifecycle management — each feature hand-rolls its own state machine.

Solim has reactive primitives for synchronous values (`Signal`, `Computed`, `Effect`) but nothing for the async case. A `Query` primitive — inspired by TanStack React Query — would eliminate this boilerplate, unify data fetching patterns, and bring automatic caching, deduplication, and lifecycle ownership to Solim's reactive system.

## What Changes

- **New `QueryKey` type** in `solim-core`: Structured, composable cache identity keys (e.g. `key("messages", channelId)`).
- **New `QueryCache` singleton** in `solim-core`: Global in-memory cache with entry-level stale time, garbage collection, in-flight request deduplication, prefix-based invalidation, and prefetch support.
- **New `Query<T>` reactive primitive** in `solim-core`: Async counterpart to `Computed` — tracks reactive dependencies, auto-fetches when deps change, auto-marshals results to main thread, exposes `data()`/`loading()`/`fetching()`/`error()` as `Readable<>`, supports `enabled()`, `refetchInterval()`, configurable retry with backoff, stale-while-revalidate, and auto-disposes with owning component via `ComponentContext.register()`.
- **New `Mutation<T,R>` reactive primitive** in `solim-core`: For write operations (send, upload, delete). Tracks `isPending()`/`error()`/`result()`, supports `onMutate`/`onSuccess`/`onError` lifecycle hooks for optimistic updates with rollback.
- **New `UI.query()` and `UI.mutation()` facade methods** in `:solim` module.
- **Refactor `BrowserState`** in `:mod` to use `Query` internally, collapsing ~150 lines of fetch/loading/error machinery.

## Capabilities

### New Capabilities
- `query-cache`: Global cache singleton with structured keys, entry-level stale time, GC, dedup, invalidation, and prefetch.
- `query-primitive`: Async reactive primitive (`Query<T>`) with dependency tracking, auto-refetch, thread marshaling, retry, stale-while-revalidate, enabled/disabled, and interval polling.
- `mutation-primitive`: Async action primitive (`Mutation<T,R>`) with pending/error/result tracking and lifecycle hooks for optimistic updates.

### Modified Capabilities
- `solim-reactivity`: Adding `Query` and `Mutation` as new reactive primitives alongside Signal/Computed/Effect, and `QueryKey`/`QueryCache` as supporting infrastructure.
- `browsers`: Refactoring `BrowserState` to use `Query` internally for its fetch machinery.

## Impact

- **`solim-core` module** (`solim-core/src/solim/reactive/`): New files for `QueryKey`, `QueryCache`, `Query`, `Mutation`, and `QueryState`. No changes to existing `Signal`/`Computed`/`Effect`.
- **`solim` module** (`solim/src/solim/UI.java`): New facade methods for `query()` and `mutation()`.
- **`mod` module** (`mod/src/mindustrytool/features/browser/common/BrowserState.java`): Refactored to delegate fetch lifecycle to `Query`.
- **Dependencies**: No new external dependencies. `Query` uses `CompletableFuture` (Java 8), `Core.app.post()` (Arc), and `ComponentContext.register()` (solim-runtime) — all already available in `solim-core`.
- **Threading**: `QueryCache` must be thread-safe for prefetch from any thread, but signal mutations remain main-thread-only (enforced by existing `SolimAssert.checkMainThread()`).
- **Testing**: New unit tests for `Query`, `Mutation`, `QueryCache`, and `QueryKey` in `solim-core/src/test/`.
