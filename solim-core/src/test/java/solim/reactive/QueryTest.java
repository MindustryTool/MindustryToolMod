package solim.reactive;

import solim.test.CollectingOwner;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.core.Disposable;
import solim.core.OwnershipContext;
import solim.runtime.SignalDispatcher;

import static org.junit.jupiter.api.Assertions.*;
import solim.test.OwnershipEnv;

class QueryTest extends OwnershipEnv {

    private QueryCache cache;

    @BeforeEach
    void setUp() {
        cache = new QueryCache();
        QueryCache.setInstanceForTest(cache);
        SignalDispatcher.resetForTests();
    }

    @Test
    void initialAndSuccessState() {
        QueryKey key = QueryKey.of("test-initial");
        CompletableFuture<String> future = new CompletableFuture<>();

        Query<String> query = Query.of(key, () -> future);

        assertTrue(query.loading().get(), "Should be loading initially");
        assertTrue(query.fetching().get(), "Should be fetching initially");
        assertNull(query.data().get(), "Data should be null initially");
        assertNull(query.error().get(), "Error should be null initially");

        future.complete("Hello Solim");

        assertEquals("Hello Solim", query.data().get());
        assertEquals("Hello Solim", query.get());
        assertEquals("Hello Solim", query.peek());
        assertFalse(query.loading().get());
        assertFalse(query.fetching().get());
        assertNull(query.error().get());
    }

    @Test
    void errorState() {
        QueryKey key = QueryKey.of("test-error");
        CompletableFuture<String> future = new CompletableFuture<>();
        RuntimeException failure = new RuntimeException("Network down");

        Query<String> query = Query.<String>builder()
                .key(key)
                .fetch(() -> future)
                .retry(0)
                .build();
        future.completeExceptionally(failure);

        assertNull(query.data().get());
        assertFalse(query.loading().get());
        assertFalse(query.fetching().get());
        assertSame(failure, query.error().get());
    }

    @Test
    void reactiveDependencyTracking() {
        Signal<Integer> page = Signal.of(1);
        AtomicInteger callCount = new AtomicInteger(0);

        Query<String> query = Query.noKey(() -> {
            int p = page.get();
            callCount.incrementAndGet();
            return CompletableFuture.completedFuture("Page " + p);
        });

        assertEquals("Page 1", query.data().get());
        assertEquals(1, callCount.get());

        page.set(2);
        SignalDispatcher.flush();

        assertEquals("Page 2", query.data().get());
        assertEquals(2, callCount.get());
    }

    @Test
    void staleWhileRevalidate() {
        QueryKey key = QueryKey.of("swr");
        AtomicReference<CompletableFuture<String>> futureRef = new AtomicReference<>(
                CompletableFuture.completedFuture("v1"));
        Query<String> query = Query.of(key, futureRef::get);

        assertEquals("v1", query.data().get());
        assertFalse(query.loading().get());
        assertFalse(query.fetching().get());

        CompletableFuture<String> f2 = new CompletableFuture<>();
        futureRef.set(f2);
        cache.getOrCreateEntry(key).clear();
        query.refetch();

        assertEquals("v1", query.data().get(), "Data should be retained during refetch");
        assertFalse(query.loading().get(), "loading() should remain false during refetch");
        assertTrue(query.fetching().get(), "fetching() should be true during refetch");

        f2.complete("v2");
        assertEquals("v2", query.data().get());
        assertFalse(query.fetching().get());
    }

    @Test
    void raceConditionDiscardsStaleResults() {
        QueryKey key = QueryKey.of("race");
        CompletableFuture<String> slow = new CompletableFuture<>();
        CompletableFuture<String> fast = CompletableFuture.completedFuture("fast");

        AtomicInteger count = new AtomicInteger(0);
        Query<String> query = Query.of(key, () -> {
            return count.incrementAndGet() == 1 ? slow : fast;
        });

        cache.getOrCreateEntry(key).clear();
        query.refetch();

        assertEquals("fast", query.data().get());

        slow.complete("slow");
        assertEquals("fast", query.data().get(), "Slow stale response should not overwrite fast response");
    }

    @Test
    void enabledCondition() {
        QueryKey key = QueryKey.of("enabled-test");
        Signal<Boolean> enabled = Signal.of(false);
        AtomicInteger fetchCount = new AtomicInteger(0);

        Query<String> query = Query.of(key, enabled, () -> {
            fetchCount.incrementAndGet();
            return CompletableFuture.completedFuture("data");
        });

        assertEquals(0, fetchCount.get(), "Disabled query must not fetch");
        assertNull(query.data().get());

        enabled.set(true);
        SignalDispatcher.flush();

        assertEquals(1, fetchCount.get(), "Enabling query should trigger fetch");
        assertEquals("data", query.data().get());

        enabled.set(false);
        SignalDispatcher.flush();
        assertEquals("data", query.data().get(), "Data should be retained when disabled");
    }

    @Test
    void disposalCleanup() {
        QueryKey key = QueryKey.of("dispose-test");
        CompletableFuture<String> inFlight = new CompletableFuture<>();
        Query<String> query = Query.of(key, () -> inFlight);

        assertFalse(query.isDisposed());
        assertEquals(1, cache.observerCount(key));

        query.dispose();
        assertTrue(query.isDisposed());
        assertEquals(0, cache.observerCount(key));

        inFlight.complete("ignored");
        assertNull(query.data().get());
    }

    @Test
    void disposalReleasesObserversAndEvictsWithZeroGcTime() {
        QueryKey key = QueryKey.of("dispose-release");
        int baselineObservers = cache.observerCount(key);

        Query<String> query = Query.<String>builder()
                .key(key)
                .fetch(() -> CompletableFuture.completedFuture("x"))
                .gcTime(Duration.ZERO)
                .build();
        assertEquals(baselineObservers + 1, cache.observerCount(key));
        assertNotNull(cache.getEntry(key));

        query.dispose();

        assertEquals(baselineObservers, cache.observerCount(key),
                "Disposal must release the cache observer");
        assertNull(cache.getEntry(key), "gcTime ZERO must evict the entry immediately on dispose");
    }

    @Test
    void queryRegistersWithAmbientOwnershipContext() {
        QueryKey key = QueryKey.of("ambient-owned");
        CollectingOwner owner = new CollectingOwner();
        OwnershipContext.push(owner);
        Query<String> query;
        try {
            query = Query.of(key, () -> CompletableFuture.completedFuture("x"));
        } finally {
            OwnershipContext.pop();
        }

        assertTrue(owner.collected().contains(query),
                "Query created in component scope must register for disposal");
        assertEquals(1, cache.observerCount(key));

        owner.dispose();
        assertTrue(query.isDisposed(), "Disposing the owner must dispose the query");
        assertEquals(0, cache.observerCount(key));
    }

    @Test
    void queryCreatedOutsideScopeRequiresExplicitDispose() {
        assertEquals(0, OwnershipContext.size());
        QueryKey key = QueryKey.of("outside-scope");
        Query<String> query = Query.of(key, () -> CompletableFuture.completedFuture("x"));

        assertEquals(1, cache.observerCount(key),
                "Outside component scope nothing owns the query; it must be disposed explicitly");

        query.dispose();
        assertEquals(0, cache.observerCount(key));
    }

    @Test
    void dynamicKeyFetchesDistinctParameterStates() {
        Signal<Integer> page = Signal.of(0);
        AtomicInteger callCount = new AtomicInteger(0);
        Map<Integer, CompletableFuture<String>> futures = new HashMap<>();

        Query<String> query = Query.<String>builder()
                .key(() -> QueryKey.of("page", page.get()))
                .fetch(() -> {
                    int p = page.get();
                    callCount.incrementAndGet();
                    return futures.computeIfAbsent(p, k -> new CompletableFuture<>());
                })
                .build();

        futures.get(0).complete("page0");
        assertEquals("page0", query.data().get());

        page.set(1);
        SignalDispatcher.flush();
        page.set(2);
        SignalDispatcher.flush();

        assertTrue(query.fetching().get(), "Page 2 request should still be in flight");
        futures.get(2).complete("page2");
        assertEquals("page2", query.data().get(), "Final navigation target must win");

        futures.get(1).complete("page1");
        assertEquals("page2", query.data().get(), "Stale response for a different parameter state must be discarded");
        assertEquals(3, callCount.get(), "Each distinct parameter state must invoke the fetcher once");
    }

    @Test
    void dynamicKeyJoinsOnlyIdenticalRequests() {
        Signal<Integer> page = Signal.of(0);
        AtomicInteger callCount = new AtomicInteger(0);
        Map<Integer, CompletableFuture<String>> futures = new HashMap<>();

        Query<String> query = Query.<String>builder()
                .key(() -> QueryKey.of("page", page.get()))
                .fetch(() -> {
                    int p = page.get();
                    callCount.incrementAndGet();
                    return futures.computeIfAbsent(p, k -> new CompletableFuture<>());
                })
                .build();

        page.set(1);
        SignalDispatcher.flush();
        page.set(0);
        SignalDispatcher.flush();

        assertEquals(2, callCount.get(), "Returning to a page with an identical in-flight request must join it");

        futures.get(0).complete("page0");
        assertEquals("page0", query.data().get());

        futures.get(1).complete("page1");
        assertEquals("page0", query.data().get(), "Late response for superseded parameters must not win");
    }

    @Test
    void dynamicKeyReactivitySurvivesInFlightJoin() {

        Signal<Integer> page = Signal.of(0);
        AtomicInteger callCount = new AtomicInteger(0);
        Map<Integer, CompletableFuture<String>> futures = new HashMap<>();

        Query.<String>builder()
                .key(() -> QueryKey.of("page", page.get()))
                .fetch(() -> {
                    int p = page.get();
                    callCount.incrementAndGet();
                    return futures.computeIfAbsent(p, k -> new CompletableFuture<>());
                })
                .build();

        page.set(1);
        SignalDispatcher.flush();
        page.set(0);
        SignalDispatcher.flush();

        page.set(2);
        SignalDispatcher.flush();
        assertEquals(3, callCount.get(), "Parameter changes must keep triggering fetches after a join");
    }

    @Test
    void ensureFreshBehavior() {
        QueryKey key = QueryKey.of("ensure-fresh-test");
        AtomicInteger fetchCount = new AtomicInteger(0);

        Query<String> query = Query.of(key, () -> {
            fetchCount.incrementAndGet();
            return CompletableFuture.completedFuture("result");
        });

        assertEquals(1, fetchCount.get());
        assertTrue(query.hasData());
        assertFalse(query.isStale());
        assertFalse(query.isError());

        query.ensureFresh();
        assertEquals(1, fetchCount.get());

        cache.getOrCreateEntry(key).clear();
        assertTrue(query.isStale());
        query.ensureFresh();
        assertEquals(2, fetchCount.get());

        AtomicInteger errFetchCount = new AtomicInteger(0);
        AtomicReference<CompletableFuture<String>> futureRef = new AtomicReference<>(new CompletableFuture<>());
        Query<String> retryQuery = Query.<String>builder()
                .key(QueryKey.of("err-retry"))
                .fetch(() -> {
                    errFetchCount.incrementAndGet();
                    return futureRef.get();
                })
                .retry(0)
                .build();

        futureRef.get().completeExceptionally(new RuntimeException("first fail"));
        assertTrue(retryQuery.isError());

        futureRef.set(CompletableFuture.completedFuture("recovered"));
        retryQuery.ensureFresh();
        assertEquals("recovered", retryQuery.data().get());
    }

    @Test
    void builderStaticKeyMatchesOf() {
        QueryKey key = QueryKey.of("builder-static");
        Query<String> built = Query.<String>builder()
                .key(key)
                .fetch(() -> CompletableFuture.completedFuture("built"))
                .build();
        Query<String> simple = Query.of(QueryKey.of("builder-static-2"), () -> CompletableFuture.completedFuture("of"));

        assertEquals("built", built.data().get());
        assertEquals("of", simple.data().get());
        assertEquals(key, built.getKey());
    }

    @Test
    void builderAllOptionsEffectiveBeforeFirstFetch() {

        QueryKey key = QueryKey.of("builder-options");
        cache.getOrCreateEntry(key).clear();

        AtomicInteger attempts = new AtomicInteger(0);
        Query<String> query = Query.<String>builder()
                .key(key)
                .fetch(() -> {
                    attempts.incrementAndGet();
                    CompletableFuture<String> f = new CompletableFuture<>();
                    f.completeExceptionally(new RuntimeException("down"));
                    return f;
                })
                .staleTime(Duration.ZERO)
                .retry(0)
                .retryDelay(Duration.ofMillis(1))
                .build();

        assertTrue(query.isError(), "retry(0) must surface the error on the first fetch");
        assertEquals(1, attempts.get(), "No retry may run after a retry(0) first fetch");
        assertTrue(query.isStale(), "staleTime(ZERO) must make the entry stale immediately");
    }

    @Test
    void builderWithoutKeyUsesAnonymousKey() {
        Query<String> query = Query.<String>builder()
                .fetch(() -> CompletableFuture.completedFuture("anon"))
                .build();

        assertEquals("anon", query.data().get());
        assertNotNull(query.getKey());
        assertEquals(1, cache.observerCount(query.getKey()));
    }

    @Test
    void noKeyCreatesAnonymousKeyQuery() {
        Query<String> query = Query.noKey(() -> CompletableFuture.completedFuture("stateless"));

        assertEquals("stateless", query.data().get());
        assertNotNull(query.getKey(), "noKey must still assign an anonymous identity key");
    }

    @Test
    void builderDisabledNeverFetches() {
        Signal<Boolean> enabled = Signal.of(false);
        AtomicInteger fetchCount = new AtomicInteger(0);

        Query<String> query = Query.<String>builder()
                .key(QueryKey.of("builder-disabled"))
                .enabled(enabled)
                .fetch(() -> {
                    fetchCount.incrementAndGet();
                    return CompletableFuture.completedFuture("data");
                })
                .build();

        assertEquals(0, fetchCount.get(), "Disabled builder query must not fetch");

        enabled.set(true);
        SignalDispatcher.flush();
        assertEquals(1, fetchCount.get(), "Enabling must trigger the first fetch");
        assertEquals("data", query.data().get());
    }

    @Test
    void builderRequiresFetch() {
        assertThrows(IllegalStateException.class, () -> Query.<String>builder()
                .key(QueryKey.of("builder-missing-fetch"))
                .build());
    }

    @Test
    void cachedNullDoesNotSuppressInitialFetchOrEnsureFresh() {
        QueryKey key = QueryKey.of("cached-null-key");
        CacheEntry<String> entry = cache.getOrCreateEntry(key);
        entry.setData(null);
        assertTrue(entry.isFetched());
        assertFalse(entry.hasData());

        AtomicInteger fetchCount = new AtomicInteger(0);
        Query<String> query = Query.<String>builder()
                .key(key)
                .fetch(() -> {
                    fetchCount.incrementAndGet();
                    return CompletableFuture.completedFuture("fresh-val");
                })
                .build();

        assertEquals(1, fetchCount.get(), "Query should not suppress initial fetch on cached null");
        assertEquals("fresh-val", query.data().get());

        query.mutate(null);
        assertEquals(1, fetchCount.get());

        query.ensureFresh();
        assertEquals(2, fetchCount.get(), "ensureFresh should fetch when cached data is null");
    }

    @Test
    void dynamicKeySupplierRebindsObserver() {
        Signal<String> dynamicId = Signal.of("id1");
        AtomicInteger fetchCount = new AtomicInteger(0);

        Query<String> query = Query.<String>builder()
                .key(() -> QueryKey.of("user", dynamicId.get()))
                .fetch(() -> {
                    fetchCount.incrementAndGet();
                    return CompletableFuture.completedFuture("data-" + dynamicId.peek());
                })
                .build();

        assertEquals(1, fetchCount.get());
        assertEquals("data-id1", query.data().get());

        QueryKey key1 = QueryKey.of("user", "id1");
        QueryKey key2 = QueryKey.of("user", "id2");

        assertEquals(1, cache.observerCount(key1));
        assertEquals(0, cache.observerCount(key2));

        dynamicId.set("id2");
        SignalDispatcher.flush();

        assertEquals(2, fetchCount.get());
        assertEquals("data-id2", query.data().get());
        assertEquals(0, cache.observerCount(key1), "Old key must be detached");
        assertEquals(1, cache.observerCount(key2), "New key must be attached");

        cache.invalidate(key1);
        assertEquals(2, fetchCount.get(), "Invalidating old key must not trigger refetch");

        cache.invalidate(key2);
        assertEquals(3, fetchCount.get(), "Invalidating active key must trigger refetch");
    }

    @Test
    void querySharesCacheSchedulerForRetry() {
        QueryKey key = QueryKey.of("shared-scheduler-retry");
        AtomicReference<Runnable> scheduledRetry = new AtomicReference<>();
        cache.setSchedulerForTest((runnable, delay) -> {
            scheduledRetry.set(runnable);
            return new Disposable() {
                private boolean disposed = false;

                @Override
                public void dispose() {
                    disposed = true;
                }

                @Override
                public boolean isDisposed() {
                    return disposed;
                }
            };
        });

        CompletableFuture<String> failing = new CompletableFuture<>();
        failing.completeExceptionally(new RuntimeException("transient error"));

        Query<String> query = Query.<String>builder()
                .key(key)
                .fetch(() -> failing)
                .retry(1)
                .retryDelay(Duration.ofMillis(500))
                .build();

        assertNotNull(scheduledRetry.get(), "Retry task should be scheduled using cache's scheduler");
        assertTrue(query.fetching().get());
        query.dispose();
    }

    @Test
    void cancellationSkipsRetryAndPreservesSuppressedCause() {
        QueryKey key = QueryKey.of("cancelled-no-retry");
        AtomicInteger retryCount = new AtomicInteger(0);
        cache.setSchedulerForTest((runnable, delay) -> {
            retryCount.incrementAndGet();
            return null;
        });

        CompletableFuture<String> future = new CompletableFuture<>();
        Query<String> query = Query.<String>builder()
                .key(key)
                .fetch(() -> future)
                .retry(3)
                .retryDelay(Duration.ofMillis(100))
                .build();

        future.cancel(true);

        assertEquals(0, retryCount.get(), "Cancellation must not schedule a retry");
        assertTrue(query.isError());
        assertTrue(query.error().get() instanceof CancellationException);
        assertFalse(query.fetching().get());
        assertFalse(query.loading().get());
        query.dispose();
    }
}

