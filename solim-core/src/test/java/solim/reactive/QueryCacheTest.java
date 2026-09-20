package solim.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.core.Disposable;

import static org.junit.jupiter.api.Assertions.*;

class QueryCacheTest {

	private QueryCache cache;

	@BeforeEach
	void setUp() {
		cache = new QueryCache();
		QueryCache.setInstanceForTest(cache);
	}

	@Test
	void queryKeyStructuralEqualityAndHashing() {
		QueryKey k1 = QueryKey.of("channels", "general");
		QueryKey k2 = QueryKey.of("channels", "general");
		QueryKey k3 = QueryKey.of("channels", "random");
		QueryKey k4 = QueryKey.of("channels");

		assertEquals(k1, k2);
		assertEquals(k1.hashCode(), k2.hashCode());
		assertNotEquals(k1, k3);
		assertNotEquals(k1, k4);

		assertEquals(2, k1.size());
		assertEquals("channels", k1.get(0));
		assertEquals("general", k1.get(1));

		assertTrue(k1.startsWith(QueryKey.of("channels")));
		assertTrue(k1.startsWith(QueryKey.of("channels", "general")));
		assertFalse(k1.startsWith(QueryKey.of("channels", "random")));
		assertFalse(k4.startsWith(k1));

		assertEquals("[channels, general]", k1.toString());
	}

	@Test
	void entryCreationAndRetrieval() {
		QueryKey key = QueryKey.of("test", 1);
		assertNull(cache.getEntry(key));

		CacheEntry<String> entry = cache.getOrCreateEntry(key);
		assertNotNull(entry);
		assertSame(entry, cache.getEntry(key));
		assertSame(entry, cache.getOrCreateEntry(key));
		assertEquals(key, entry.getKey());
		assertFalse(entry.hasData());
	}

	@Test
	void inFlightDeduplication() {
		QueryKey key = QueryKey.of("dedup");
		AtomicInteger callCount = new AtomicInteger(0);
		CompletableFuture<String> future = new CompletableFuture<>();

		CompletableFuture<String> f1 = cache.fetchOrJoin(key, () -> {
			callCount.incrementAndGet();
			return future;
		});

		CompletableFuture<String> f2 = cache.fetchOrJoin(key, () -> {
			callCount.incrementAndGet();
			return CompletableFuture.completedFuture("second");
		});

		assertEquals(1, callCount.get(), "Concurrent fetch should be deduplicated to 1 call");
		assertSame(f1, f2);

		future.complete("done");
		assertEquals("done", f1.join());
		assertEquals("done", f2.join());

		CacheEntry<String> entry = cache.getEntry(key);
		assertNotNull(entry);
		assertEquals("done", entry.getData());
		assertNull(entry.getInflight());

		// Subsequent fetch after completion starts new request
		CompletableFuture<String> f3 = cache.fetchOrJoin(key, () -> {
			callCount.incrementAndGet();
			return CompletableFuture.completedFuture("third");
		});
		assertEquals(2, callCount.get());
		assertEquals("third", f3.join());
	}

	@Test
	void staleTimeChecks() {
		QueryKey key = QueryKey.of("stale");
		CacheEntry<String> entry = cache.getOrCreateEntry(key);

		assertTrue(entry.isStale(1000L));
		entry.setData("value");
		assertFalse(entry.isStale(10_000L));
		assertTrue(entry.isStale(-1L));
	}

	@Test
	void observerAttachmentAndInvalidation() {
		QueryKey key = QueryKey.of("users", 42);
		CacheEntry<String> entry = cache.getOrCreateEntry(key);
		entry.setData("Alice");

		AtomicInteger invalidations = new AtomicInteger(0);
		QueryCache.InvalidationListener listener = invalidations::incrementAndGet;

		cache.attachObserver(key, listener);
		assertEquals(1, entry.getActiveObservers());
		assertEquals(1, cache.observerCount(key));

		cache.invalidate(key);
		assertEquals(1, invalidations.get());
		assertNull(entry.getData(), "Invalidation should clear data");
		assertFalse(entry.hasData());

		cache.detachObserver(key, listener, 0);
		assertEquals(0, entry.getActiveObservers());
	}

	@Test
	void invalidateByPredicateAndAll() {
		QueryKey k1 = QueryKey.of("chat", "room1");
		QueryKey k2 = QueryKey.of("chat", "room2");
		QueryKey k3 = QueryKey.of("settings");

		cache.getOrCreateEntry(k1).setData("r1");
		cache.getOrCreateEntry(k2).setData("r2");
		cache.getOrCreateEntry(k3).setData("s");

		AtomicInteger chatInvalidated = new AtomicInteger(0);
		cache.attachObserver(k1, chatInvalidated::incrementAndGet);
		cache.attachObserver(k2, chatInvalidated::incrementAndGet);

		cache.invalidate(key -> key.startsWith(QueryKey.of("chat")));
		assertEquals(2, chatInvalidated.get());
		assertNull(cache.getEntry(k1).getData());
		assertNull(cache.getEntry(k2).getData());
		assertEquals("s", cache.getEntry(k3).getData());

		cache.invalidateAll();
		assertNull(cache.getEntry(k3).getData());
	}

	@Test
	void prefetchPopulatesCacheAndIgnoresErrors() {
		QueryKey key = QueryKey.of("prefetch");
		CompletableFuture<String> res = cache.prefetch(key, () -> CompletableFuture.completedFuture("warm"));
		assertEquals("warm", res.join());

		CacheEntry<String> entry = cache.getEntry(key);
		assertNotNull(entry);
		assertEquals("warm", entry.getData());

		// Prefetch error does not cache error
		QueryKey errKey = QueryKey.of("prefetch-err");
		CompletableFuture<String> errFuture = new CompletableFuture<>();
		errFuture.completeExceptionally(new RuntimeException("boom"));

		cache.prefetch(errKey, () -> errFuture);
		CacheEntry<String> errEntry = cache.getEntry(errKey);
		assertNotNull(errEntry);
		assertNull(errEntry.getError(), "Prefetch failure should not cache error");
		assertNull(errEntry.getData());
	}

	@Test
	void garbageCollectionEviction() {
		QueryKey key = QueryKey.of("gc");
		AtomicReference<Runnable> scheduledTask = new AtomicReference<>();
		cache.setSchedulerForTest((runnable, delay) -> {
			scheduledTask.set(runnable);
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

		QueryCache.InvalidationListener listener = () -> {};
		cache.attachObserver(key, listener);
		cache.getOrCreateEntry(key).setData("temp");

		// Detach with GC time
		cache.detachObserver(key, listener, 5000L);
		assertNotNull(scheduledTask.get(), "GC task should be scheduled");
		assertNotNull(cache.getEntry(key), "Entry should still exist before GC runs");

		// Run scheduled GC
		scheduledTask.get().run();
		assertNull(cache.getEntry(key), "Entry should be evicted after GC task runs");
	}

	@Test
	void gcCancelledIfNewObserverAttaches() {
		QueryKey key = QueryKey.of("gc-cancel");
		AtomicReference<Runnable> scheduledTask = new AtomicReference<>();
		AtomicBoolean cancelled = new AtomicBoolean(false);

		cache.setSchedulerForTest((runnable, delay) -> {
			scheduledTask.set(runnable);
			return new Disposable() {
				private boolean disposed = false;

				@Override
				public void dispose() {
					cancelled.set(true);
					disposed = true;
				}

				@Override
				public boolean isDisposed() {
					return disposed;
				}
			};
		});

		QueryCache.InvalidationListener listener1 = () -> {};
		QueryCache.InvalidationListener listener2 = () -> {};

		cache.attachObserver(key, listener1);
		cache.detachObserver(key, listener1, 5000L);

		// Re-attach before GC runs
		cache.attachObserver(key, listener2);
		assertTrue(cancelled.get(), "GC task should be cancelled when new observer attaches");

		// Even if the scheduled runnable somehow ran, it should not evict since activeObservers > 0
		scheduledTask.get().run();
		assertNotNull(cache.getEntry(key), "Entry should not be evicted while active observer is attached");
	}
}
