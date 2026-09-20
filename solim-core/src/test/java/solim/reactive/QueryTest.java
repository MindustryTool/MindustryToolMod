package solim.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.runtime.SignalDispatcher;

import static org.junit.jupiter.api.Assertions.*;

class QueryTest {

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

		Query<String> query = Query.of(key, () -> future).retry(0);
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

		Query<String> query = Query.of(() -> {
			int p = page.get();
			callCount.incrementAndGet();
			return CompletableFuture.completedFuture("Page " + p);
		});

		assertEquals("Page 1", query.data().get());
		assertEquals(1, callCount.get());

		// Mutating page should invalidate the query's effect
		page.set(2);
		SignalDispatcher.flush();

		assertEquals("Page 2", query.data().get());
		assertEquals(2, callCount.get());
	}

	@Test
	void staleWhileRevalidate() {
		QueryKey key = QueryKey.of("swr");
		AtomicReference<CompletableFuture<String>> futureRef = new AtomicReference<>(CompletableFuture.completedFuture("v1"));
		Query<String> query = Query.of(key, futureRef::get);

		assertEquals("v1", query.data().get());
		assertFalse(query.loading().get());
		assertFalse(query.fetching().get());

		CompletableFuture<String> f2 = new CompletableFuture<>();
		futureRef.set(f2);
		cache.getOrCreateEntry(key).clear();
		query.refetch();

		// Previous data is retained!
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

		// Trigger second fetch while first is in-flight
		cache.getOrCreateEntry(key).clear();
		query.refetch();

		assertEquals("fast", query.data().get());

		// Now slow completes - it must be discarded!
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

		// Enable
		enabled.set(true);
		SignalDispatcher.flush();

		assertEquals(1, fetchCount.get(), "Enabling query should trigger fetch");
		assertEquals("data", query.data().get());

		// Disable again: data is retained
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

		// In-flight completing after disposal must be ignored
		inFlight.complete("ignored");
		assertNull(query.data().get());
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

		// Fresh query should not refetch on ensureFresh
		query.ensureFresh();
		assertEquals(1, fetchCount.get());

		// When cache entry is invalidated or becomes stale
		cache.getOrCreateEntry(key).clear();
		assertTrue(query.isStale());
		query.ensureFresh();
		assertEquals(2, fetchCount.get());

		// When query is in error state
		AtomicInteger errFetchCount = new AtomicInteger(0);
		AtomicReference<CompletableFuture<String>> futureRef = new AtomicReference<>(new CompletableFuture<>());
		Query<String> retryQuery = Query.of(QueryKey.of("err-retry"), () -> {
			errFetchCount.incrementAndGet();
			return futureRef.get();
		}).retry(0);

		futureRef.get().completeExceptionally(new RuntimeException("first fail"));
		assertTrue(retryQuery.isError());

		futureRef.set(CompletableFuture.completedFuture("recovered"));
		retryQuery.ensureFresh();
		assertEquals("recovered", retryQuery.data().get());
	}
}
