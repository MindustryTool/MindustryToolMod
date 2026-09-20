package solim.reactive;

import arc.Core;
import arc.util.Nullable;
import arc.util.Timer;
import arc.util.Timer.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.function.Supplier;
import solim.core.Disposable;

/**
 * Global cache for queries, managing entry caching, deduplication,
 * invalidation, and garbage collection.
 */
public final class QueryCache {

    public static final long DEFAULT_STALE_TIME_MS = 30_000L;
    public static final long DEFAULT_GC_TIME_MS = 300_000L;

    @FunctionalInterface
    public interface InvalidationListener {
        void onInvalidated();
    }

    @FunctionalInterface
    public interface Scheduler {
        @Nullable
        Disposable schedule(Runnable runnable, float delaySeconds);
    }

    private static QueryCache instance = new QueryCache();

    private final ConcurrentHashMap<QueryKey, CacheEntry<?>> entries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<QueryKey, List<InvalidationListener>> observers = new ConcurrentHashMap<>();
    private Scheduler scheduler = (runnable, delaySeconds) -> {
        try {
            if (Core.app == null) {
                return null;
            }
            Task task = Timer.schedule(runnable, delaySeconds);
            return new Disposable() {
                private boolean disposed = false;

                @Override
                public void dispose() {
                    if (!disposed) {
                        disposed = true;
                        task.cancel();
                    }
                }

                @Override
                public boolean isDisposed() {
                    return disposed;
                }
            };
        } catch (Throwable ignored) {
            return null;
        }
    };

    QueryCache() {
    }

    public static QueryCache getInstance() {
        return instance;
    }

    public static void setInstanceForTest(QueryCache testInstance) {
        instance = testInstance != null ? testInstance : new QueryCache();
    }

    void setSchedulerForTest(Scheduler scheduler) {
        this.scheduler = scheduler != null ? scheduler : (runnable, delaySeconds) -> null;
    }

    @SuppressWarnings("unchecked")
    public <T> CacheEntry<T> getOrCreateEntry(QueryKey key) {
        Objects.requireNonNull(key, "key cannot be null");
        return (CacheEntry<T>) entries.computeIfAbsent(key, CacheEntry::new);
    }

    @SuppressWarnings("unchecked")
    public <T> CacheEntry<T> getEntry(QueryKey key) {
        Objects.requireNonNull(key, "key cannot be null");
        return (CacheEntry<T>) entries.get(key);
    }

    public void attachObserver(QueryKey key, InvalidationListener listener) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");

        CacheEntry<?> entry = getOrCreateEntry(key);
        entry.incrementObservers();
        observers.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void detachObserver(QueryKey key, InvalidationListener listener, long gcTimeMillis) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");

        List<InvalidationListener> list = observers.get(key);
        if (list != null) {
            list.remove(listener);
            if (list.isEmpty()) {
                observers.remove(key, list);
            }
        }

        CacheEntry<?> entry = getEntry(key);
        if (entry != null) {
            int remaining = entry.decrementObservers();
            if (remaining <= 0) {
                if (gcTimeMillis <= 0) {
                    entries.remove(key);
                    observers.remove(key);
                    entry.clear();
                } else {
                    float delaySeconds = Math.max(0.001f, gcTimeMillis / 1000f);
                    Disposable task = scheduler.schedule(() -> {
                        synchronized (entry) {
                            if (entry.getActiveObservers() <= 0) {
                                entries.remove(key);
                                observers.remove(key);
                                entry.clear();
                            }
                        }
                    }, delaySeconds);
                    entry.setGcTask(task);
                }
            }
        }
    }

    public void invalidate(QueryKey key) {
        Objects.requireNonNull(key, "key cannot be null");
        CacheEntry<?> entry = entries.get(key);
        if (entry != null) {
            entry.clear();
        }

        List<InvalidationListener> list = observers.get(key);
        if (list != null && !list.isEmpty()) {
            List<InvalidationListener> copy = new ArrayList<>(list);
            for (InvalidationListener l : copy) {
                l.onInvalidated();
            }
        }
    }

    public void invalidate(Predicate<QueryKey> predicate) {
        Objects.requireNonNull(predicate, "predicate cannot be null");
        List<QueryKey> matched = new ArrayList<>();
        for (QueryKey key : entries.keySet()) {
            if (predicate.test(key)) {
                matched.add(key);
            }
        }
        for (QueryKey key : observers.keySet()) {
            if (!matched.contains(key) && predicate.test(key)) {
                matched.add(key);
            }
        }
        for (QueryKey key : matched) {
            invalidate(key);
        }
    }

    public void invalidateAll() {
        invalidate(key -> true);
    }

    public <T> CompletableFuture<T> prefetch(QueryKey key, Supplier<CompletableFuture<T>> fetcher) {
        CacheEntry<T> entry = getOrCreateEntry(key);
        synchronized (entry) {
            CompletableFuture<T> inflight = entry.getInflight();
            if (inflight != null && !inflight.isDone()) {
                return inflight;
            }
            if (entry.hasData() && !entry.isStale(DEFAULT_STALE_TIME_MS)) {
                return CompletableFuture.completedFuture(entry.getData());
            }

            CompletableFuture<T> future = fetcher.get();
            entry.setInflight(future);
            future.whenComplete((result, throwable) -> {
                synchronized (entry) {
                    entry.setInflight(null);
                    if (throwable == null) {
                        entry.setData(result);
                    }
                }
            });
            return future;
        }
    }

    public <T> CompletableFuture<T> fetchCached(QueryKey key, Supplier<CompletableFuture<T>> fetcher) {
        return fetchCached(key, DEFAULT_STALE_TIME_MS, fetcher);
    }

    public <T> CompletableFuture<T> fetchCached(QueryKey key, long staleTimeMs,
            Supplier<CompletableFuture<T>> fetcher) {
        CacheEntry<T> entry = getOrCreateEntry(key);
        synchronized (entry) {
            CompletableFuture<T> inflight = entry.getInflight();
            if (inflight != null && !inflight.isDone()) {
                return inflight;
            }
            if (entry.hasData() && !entry.isStale(staleTimeMs)) {
                return CompletableFuture.completedFuture(entry.getData());
            }

            CompletableFuture<T> future = fetcher.get();
            entry.setInflight(future);
            future.whenComplete((result, throwable) -> {
                synchronized (entry) {
                    entry.setInflight(null);
                    if (throwable != null) {
                        entry.setError(throwable);
                    } else {
                        entry.setData(result);
                    }
                }
            });
            return future;
        }
    }

    public <T> CompletableFuture<T> fetchOrJoin(QueryKey key, Supplier<CompletableFuture<T>> fetcher) {
        CacheEntry<T> entry = getOrCreateEntry(key);
        synchronized (entry) {
            CompletableFuture<T> inflight = entry.getInflight();
            if (inflight != null && !inflight.isDone()) {
                return inflight;
            }

            CompletableFuture<T> future = fetcher.get();
            entry.setInflight(future);
            future.whenComplete((result, throwable) -> {
                synchronized (entry) {
                    entry.setInflight(null);
                    if (throwable != null) {
                        entry.setError(throwable);
                    } else {
                        entry.setData(result);
                    }
                }
            });
            return future;
        }
    }

    public <T> void put(QueryKey key, @Nullable T data) {
        CacheEntry<T> entry = getOrCreateEntry(key);
        synchronized (entry) {
            entry.setData(data);
        }
    }

    public <T> T get(QueryKey key) {
        CacheEntry<T> entry = getEntry(key);
        return entry != null ? entry.getData() : null;
    }

    public void clear() {
        for (Map.Entry<QueryKey, CacheEntry<?>> entry : entries.entrySet()) {
            entry.getValue().clear();
        }
        entries.clear();
        observers.clear();
    }

    public int entryCount() {
        return entries.size();
    }

    public int observerCount(QueryKey key) {
        List<InvalidationListener> list = observers.get(key);
        return list != null ? list.size() : 0;
    }
}
