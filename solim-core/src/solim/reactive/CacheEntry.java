package solim.reactive;

import arc.util.Nullable;
import java.util.concurrent.CompletableFuture;
import solim.core.Disposable;

/**
 * Represents a single cached query state in {@link QueryCache}.
 *
 * @param <T> the query result type
 */
public final class CacheEntry<T> {
	private final QueryKey key;
	private @Nullable T data;
	private @Nullable Throwable error;
	private long updatedAt;
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
		this.error = null;
		this.updatedAt = System.currentTimeMillis();
	}

	public synchronized @Nullable Throwable getError() {
		return error;
	}

	public synchronized void setError(@Nullable Throwable error) {
		this.error = error;
	}

	public synchronized long getUpdatedAt() {
		return updatedAt;
	}

	public synchronized boolean hasData() {
		return updatedAt > 0;
	}

	public synchronized boolean isStale(long staleTimeMillis) {
		if (updatedAt <= 0) return true;
		return (System.currentTimeMillis() - updatedAt) > staleTimeMillis;
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
		data = null;
		error = null;
		updatedAt = 0;
		inflight = null;
	}
}
