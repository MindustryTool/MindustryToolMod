package solim.reactive;

import arc.util.Nullable;
import java.util.concurrent.CompletableFuture;
import solim.core.Disposable;

public final class CacheEntry<T> {
    private final QueryKey key;
    private @Nullable T data;
    private @Nullable Throwable error;
    private long updatedAtNanos;
    private boolean fetched = false;
    private long generation = 0L;
    private int activeObservers;
    private @Nullable CompletableFuture<T> inflight;
    private @Nullable Disposable gcTask;

    public CacheEntry(QueryKey key) {
        this.key = key;
    }

    public QueryKey getKey() {
        return key;
    }

    public synchronized @Nullable T getData() {
        return data;
    }

    public synchronized void setData(@Nullable T data) {
        this.data = data;
        this.fetched = true;
        this.error = null;
        this.updatedAtNanos = System.nanoTime();
    }

    public synchronized @Nullable Throwable getError() {
        return error;
    }

    public synchronized void setError(@Nullable Throwable error) {
        this.error = error;
    }

    public synchronized long getUpdatedAt() {
        return TimeUtil.nanosToMillis(updatedAtNanos);
    }

    public synchronized long generation() {
        return generation;
    }

    public synchronized boolean hasData() {
        return data != null;
    }

    public synchronized boolean isFetched() {
        return fetched;
    }

    public synchronized boolean isStale(long staleTimeMillis) {
        if (!fetched) {
            return true;
        }
        long elapsedNanos = System.nanoTime() - updatedAtNanos;
        return elapsedNanos > staleTimeMillis * 1_000_000L;
    }

    public synchronized int getActiveObservers() {
        return activeObservers;
    }

    public synchronized int incrementObservers() {
        cancelGc();
        return ++activeObservers;
    }

    public synchronized int decrementObservers() {
        if (activeObservers > 0) {
            activeObservers--;
        }
        return activeObservers;
    }

    public synchronized @Nullable CompletableFuture<T> getInflight() {
        return inflight;
    }

    public synchronized void setInflight(@Nullable CompletableFuture<T> inflight) {
        this.inflight = inflight;
    }

    public synchronized @Nullable Disposable getGcTask() {
        return gcTask;
    }

    public synchronized void setGcTask(@Nullable Disposable gcTask) {
        if (this.gcTask != null) {
            this.gcTask.dispose();
        }
        this.gcTask = gcTask;
    }

    public synchronized void cancelGc() {
        if (gcTask != null) {
            gcTask.dispose();
            gcTask = null;
        }
    }

    public synchronized void clear() {
        cancelGc();
        generation++;
        data = null;
        error = null;
        updatedAtNanos = 0L;
        fetched = false;
        inflight = null;
    }
}