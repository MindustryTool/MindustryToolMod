## Context

Solim's reactive system provides `Signal<T>`, `Computed<T>`, and `Effect` for synchronous reactivity. All three integrate with `ComponentContext.register()` for automatic lifecycle ownership and `ReactiveContext` for dependency tracking. However, the mod fetches data from HTTP APIs via `CompletableFuture<T>` (through `Request.sendAsync()`), which resolves on background threads. Bridging async results into Solim's main-thread-only reactive system currently requires manual boilerplate in every feature: `Core.app.post()` marshaling, separate `Signal<Boolean> loading`/`Signal<String> error`/`Signal<T> data` declarations, `isDisposed()` guards, and ad-hoc race condition handling.

The codebase audited 34 distinct async data patterns across browser, chat, player connect, auth, translation, and update features. 21 are read queries and 5 are write mutations. The remaining 8 are out-of-scope patterns (SSE streaming, raw socket pings, game action downloads).

There is no shared cache — `Github.Memo` provides in-flight deduplication for 2 endpoints, `BrowserFilterDialog` uses boolean flags, and `ChatStore` maintains per-channel message maps. Each is hand-rolled independently.

### Constraints

- **Java 8 runtime** — no `List.of()`, `Optional.isEmpty()`, etc.
- **Main-thread-only signals** — `Signal.set()` calls `SolimAssert.checkMainThread()`, throws if called on background thread.
- **Module isolation** — `:mod` cannot reference `:solim-runtime`. New code in `solim-core` can use `ComponentContext` and `ReactiveContext` from runtime (same as `Computed`/`Effect`).
- **Frame-batched effects** — `SignalDispatcher` flushes pending effects once per render frame via `Trigger.update`.

## Goals / Non-Goals

**Goals:**

- Provide `Query<T>` as the async counterpart to `Computed<T>` — a reactive primitive that tracks dependencies, auto-fetches when deps change, and exposes results as `Readable<>` signals.
- Provide `Mutation<T,R>` for write operations with optimistic update lifecycle hooks.
- Provide `QueryCache` as a global shared cache with structured keys, stale-time, GC, dedup, invalidation, and prefetch.
- Auto-marshal all async results to the main thread internally.
- Auto-register with `ComponentContext` for lifecycle ownership (auto-dispose when parent component disposes).
- Eliminate the `Core.app.post()` + loading/error signal boilerplate from all 21+ read patterns and 5+ write patterns.

**Non-Goals:**

- SSE/streaming support — fundamentally different from request/response. Stays hand-rolled.
- Infinite scroll / cursor-based pagination (`InfiniteQuery`) — decided to keep hand-rolled for now.
- Built-in debounce — stays at the signal/input level, Query reacts to signal changes.
- Replacing `NetworkImage`'s specialized 2-tier image cache — it has domain-specific disk caching and pixel processing.
- Global state management beyond async data — Query is not a general state store.

## Decisions

### 1. Query is an "Async Computed" — Internal Effect + Internal Signals

**Decision**: `Query<T>` internally uses an `Effect` to track reactive dependencies (like `Computed` does) and internal `Signal` fields for `data`, `loading`, `fetching`, and `error`. It does NOT subclass `Computed`.

**Why not subclass Computed?** `Computed` is synchronous and lazy — it recomputes on `get()`. `Query` is asynchronous and eager — it fires a `CompletableFuture` immediately and updates internal state when it resolves. The execution model is fundamentally different.

**Why internal Effect?** The `Effect` provides automatic dependency tracking via `ReactiveContext.push()/pop()`. When the fetcher lambda calls `channelId.get()`, the dependency is captured automatically. When `channelId` changes, the Effect re-runs, triggering a new fetch. This reuses existing reactive infrastructure rather than reimplementing it.

**Why internal Signals?** `data()`, `loading()`, `fetching()`, `error()` each return `Readable<T>` backed by internal `Signal<T>`. This integrates seamlessly with Solim components — `text(query.data().map(...))` works automatically.

**Alternative considered**: Making Query implement `Readable<QueryState<T>>` (a single combined state object). Rejected because it forces all consumers to destructure, and breaks the natural `text(query.data())` pattern.

### 2. Structured QueryKey for Cache Identity

**Decision**: `QueryKey` is an immutable value type wrapping `Object[]` with proper `equals()`/`hashCode()`. Created via `QueryKey.of("messages", channelId)`.

**Why structured?** Enables pattern-based invalidation (`cache.invalidate(key -> key.startsWith("messages"))`) and debuggable cache inspection. String-only keys would require manual namespacing conventions.

**Why Object[] not List?** Lighter weight, immutable after construction, and `Arrays.deepEquals()`/`Arrays.deepHashCode()` provide correct structural equality for nested arrays. Java 8 compatible.

### 3. Global Singleton QueryCache

**Decision**: `QueryCache` is a singleton accessed via `QueryCache.getInstance()`. Stores `CacheEntry<T>` objects keyed by `QueryKey`.

**Why singleton?** Enables cross-feature cache sharing (two dialogs fetching the same tags share one cache entry) and startup prefetching (`QueryCache.getInstance().prefetch(key, fetcher)`). Scoped caches were considered but add complexity without clear benefit in a single-mod context.

**Thread safety**: The cache map itself is a `ConcurrentHashMap`. Reads (cache hits) can happen from any thread. Writes (cache updates, signal mutations) are always marshaled to the main thread via `Core.app.post()`.

**GC strategy**: When the last `Query` observer detaches from a `CacheEntry`, a `Timer.schedule` starts a GC countdown (default 5 minutes). If a new `Query` attaches before the timer fires, the timer is cancelled. Otherwise, the entry is evicted.

### 4. Stale-While-Revalidate with loading vs fetching

**Decision**: Two separate boolean signals distinguish first load from refetch:
- `loading()` — `true` only when no data has been successfully fetched yet (initial load).
- `fetching()` — `true` whenever any fetch is in-flight (including refetch).

During a refetch, `data()` retains the previous successful value. Consumers show old data while the new fetch is in progress.

**Why?** Prevents UI flashing to empty/spinner on every parameter change. The browser search experience stays smooth — old results visible until new results arrive.

### 5. Retry with Exponential Backoff

**Decision**: Default 3 retry attempts with exponential backoff (1s, 2s, 4s). Configurable per-query via `.retry(count)` and `.retryDelay(baseDuration)`. Retry counter resets on successful fetch.

**Why default-on?** Game mods operate over real player networks with intermittent connectivity. Failing silently on first timeout is poor UX. 3 retries with backoff is aggressive enough to recover from transient failures without hammering the server.

### 6. Auto Thread Marshaling

**Decision**: `Query` internally wraps `CompletableFuture.whenComplete()` with `Core.app.post()` before setting any internal signals. This is invisible to consumers.

**Why internal?** This is the #1 source of boilerplate (30+ `Core.app.post()` calls across the codebase) and the #1 source of bugs (forgetting it causes `IllegalStateException` from `SolimAssert.checkMainThread()`). Making it automatic eliminates both problems.

### 7. Race Condition Handling via Generation Counter

**Decision**: Each fetch increments an internal `int fetchGeneration`. When the `CompletableFuture` resolves, the callback checks `if (gen != fetchGeneration) return` — discarding stale results from superseded fetches.

**Why?** When a user types "hel" → "hello" rapidly, two fetches fire. Without the generation check, the slower "hel" response could overwrite the "hello" response. This is a standard pattern (already used in `ChatPresence`).

### 8. Mutation Lifecycle Hooks

**Decision**: `Mutation<T,R>` supports three hooks:
- `onMutate(T input) → R context` — called synchronously before the async operation. Returns a context object for rollback.
- `onSuccess(result, R context)` — called on main thread after successful completion.
- `onError(Throwable error, R context)` — called on main thread after failure.

**Why?** The chat optimistic send pattern (`ChatInputView`) inserts a temp message before the server confirms. `onMutate` captures the temp ID, `onSuccess` confirms it, `onError` rolls back. Without hooks, this pattern remains hand-rolled.

### 9. refetchInterval for Polling

**Decision**: `Query` supports `.refetchInterval(Duration)` which schedules periodic refetch using `arc.util.Timer.schedule()`. The timer is active only while the Query has observers (is mounted). Disposed when the Query disposes.

**Why?** 5 polling patterns found in the codebase (servers 15min, auth 5min, rooms 30s, etc.). Without this, polling stays in `Timer.schedule` callbacks disconnected from the query lifecycle.

### 10. Enabled/Disabled Queries

**Decision**: `.enabled(Readable<Boolean>)` controls whether the Query is active. When `enabled` is false, the Query does not fetch and stays in its current state. When it transitions to true, it fetches. The enabled signal is tracked reactively — the Query watches it like any other dependency.

**Why?** Auth-gated fetches (only fetch profile when logged in) and dependent queries (only fetch messages after channel is selected) are common patterns. Without `enabled`, consumers need external `dynamic()` wrappers or manual guards.

## Risks / Trade-offs

**[Risk] Cache memory pressure** → The GC timer (default 5min) limits unbounded growth. Configurable per-query. `QueryCache` could add a max-entries limit in the future if needed, but the mod's data volume is small enough that this is unlikely.

**[Risk] Stale data shown to user** → Stale-while-revalidate means users briefly see old data during refetch. This is intentional for UX (prevents flashing), and the `fetching()` signal allows showing a subtle refresh indicator if desired.

**[Risk] Thread safety of QueryCache** → `ConcurrentHashMap` for reads, `Core.app.post()` for writes. The prefetch path must handle the case where data arrives before any Query observer attaches — the cache entry stores the result, and when a Query later attaches, it picks up the cached data on the main thread.

**[Risk] Circular dependency tracking** → If a Query's fetcher reads another Query's data (dependent queries), the Effect's dependency tracking handles this naturally since `query.data().get()` registers a dependency. However, two queries that depend on each other would create an infinite refetch loop. Mitigation: same cycle detection as `Computed` (throw `IllegalStateException`), plus the Effect's `running` guard prevents re-entrance.

**[Risk] BrowserState refactor breaks existing behavior** → Mitigated by keeping the refactor minimal: BrowserState's public API (signals, pagination methods) stays identical. Only the internal fetch machinery changes. Existing dialog code that uses `state.items()`, `state.loading()`, etc. continues to work unchanged.
