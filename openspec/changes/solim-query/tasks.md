## 1. Query Key and Cache Infrastructure

- [ ] 1.1 Implement `QueryKey` in `solim-core/src/solim/reactive/QueryKey.java` with structural equality, hashing, and immutability
- [ ] 1.2 Implement `CacheEntry<T>` in `solim-core/src/solim/reactive/CacheEntry.java` tracking data, error, timestamps, active observers, in-flight futures, and GC timer handles
- [ ] 1.3 Implement `QueryCache` in `solim-core/src/solim/reactive/QueryCache.java` with thread-safe lookup, stale-time checks, in-flight dedup, invalidation, GC eviction, and prefetching
- [ ] 1.4 Create comprehensive unit tests for `QueryKey` and `QueryCache` in `solim-core/src/test/java/solim/reactive/QueryCacheTest.java`

## 2. Query Reactive Primitive

- [ ] 2.1 Implement `Query<T>` in `solim-core/src/solim/reactive/Query.java` implementing `Readable<T>` and `Disposable`
- [ ] 2.2 Implement internal `Effect` for reactive dependency tracking and automatic re-evaluation
- [ ] 2.3 Implement internal state signals and accessors: `data()`, `loading()`, `fetching()`, `error()`
- [ ] 2.4 Implement thread marshaling via `Core.app.post()` and generation counter to discard stale async results
- [ ] 2.5 Integrate `Query` with `QueryCache` for stale-while-revalidate and shared cache entries
- [ ] 2.6 Implement `.enabled(Readable<Boolean>)` for conditional query activation
- [ ] 2.7 Implement `.refetchInterval(Duration)` for periodic automatic polling
- [ ] 2.8 Implement retry mechanism with exponential backoff (`.retry(int)`, `.retryDelay(Duration)`)
- [ ] 2.9 Implement manual `refetch()` and lifecycle cleanup via `ComponentContext.register(this)`
- [ ] 2.10 Create unit tests for `Query<T>` in `solim-core/src/test/java/solim/reactive/QueryTest.java`

## 3. Mutation Reactive Primitive

- [ ] 3.1 Implement `Mutation<T, R>` in `solim-core/src/solim/reactive/Mutation.java` implementing `Disposable`
- [ ] 3.2 Implement internal signals: `isPending()`, `error()`, and `result()`
- [ ] 3.3 Implement `mutate(T input)` with main thread marshaling and generation counter
- [ ] 3.4 Implement lifecycle hooks: `.onMutate(Function<T, C>)`, `.onSuccess(BiConsumer<R, C>)`, `.onError(BiConsumer<Throwable, C>)`
- [ ] 3.5 Implement `reset()` and disposal cleanup via `ComponentContext.register(this)`
- [ ] 3.6 Create unit tests for `Mutation<T, R>` in `solim-core/src/test/java/solim/reactive/MutationTest.java`

## 4. Solim Public Facade

- [ ] 4.1 Add `UI.query(...)` factory methods to `solim/src/solim/UI.java`
- [ ] 4.2 Add `UI.mutation(...)` factory methods to `solim/src/solim/UI.java`

## 5. BrowserState Refactor and Verification

- [ ] 5.1 Refactor `BrowserState<T>` in `mod/src/mindustrytool/features/browser/common/BrowserState.java` to use `Query` internally while preserving its public API
- [ ] 5.2 Verify compatibility with `MapBrowserDialog` and `SchematicBrowserDialog`
- [ ] 5.3 Run all tests and verify project builds cleanly
