package solim.reactive;

import arc.Core;
import arc.util.Nullable;
import arc.util.Timer;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import solim.core.Disposable;
import solim.runtime.ComponentContext;

/**
 * Asynchronous reactive query primitive with dependency tracking, caching,
 * stale-while-revalidate, retry, and lifecycle management.
 *
 * @param <T> query data type
 */
public final class Query<T> implements Readable<T>, Disposable {

	private static final int DEFAULT_RETRY_COUNT = 3;
	private static final long DEFAULT_RETRY_DELAY_MS = 1_000L;

	private final QueryKey key;
	private final Supplier<CompletableFuture<T>> fetcher;
	private final @Nullable Supplier<QueryKey> keySupplier;
	private final Set<QueryKey> fetchedKeys = new LinkedHashSet<>();
	private final QueryCache cache;

	private final Signal<T> data = Signal.of(null);
	private final Signal<Boolean> loading = Signal.of(true);
	private final Signal<Boolean> fetching = Signal.of(false);
	private final Signal<Throwable> error = Signal.of(null);

	private @Nullable Readable<Boolean> enabled;
	private long staleTimeMs = QueryCache.DEFAULT_STALE_TIME_MS;
	private long gcTimeMs = QueryCache.DEFAULT_GC_TIME_MS;
	private int retryCount = DEFAULT_RETRY_COUNT;
	private long retryDelayMs = DEFAULT_RETRY_DELAY_MS;
	private @Nullable Duration refetchInterval;

	private int fetchGeneration = 0;
	private int currentRetry = 0;
	private boolean hasData = false;
	private boolean disposed = false;
	private boolean initialRun = true;

	private @Nullable Effect effect;
	private @Nullable Disposable retryTask;
	private @Nullable Disposable intervalTask;
	private final QueryCache.InvalidationListener cacheListener = this::refetch;

	private Query(@Nullable QueryKey key, @Nullable Supplier<QueryKey> keySupplier,
			@Nullable Readable<Boolean> enabled, Supplier<CompletableFuture<T>> fetcher) {
		this.fetcher = Objects.requireNonNull(fetcher, "fetcher cannot be null");
		this.keySupplier = keySupplier;
		this.key = keySupplier != null
				? Objects.requireNonNull(keySupplier.get(), "key supplier must return a key")
				: Objects.requireNonNull(key, "key cannot be null");
		this.enabled = enabled;
		this.cache = QueryCache.getInstance();

		ComponentContext.register(this);
		this.cache.attachObserver(this.key, cacheListener);

		// Check cache for existing data on initialization
		CacheEntry<T> entry = this.cache.getEntry(this.key);
		if (entry != null && entry.hasData()) {
			this.data.set(entry.getData());
			this.loading.set(false);
			this.hasData = true;
		}

		initEffect();
	}

	public static <T> Query<T> of(QueryKey key, Supplier<CompletableFuture<T>> fetcher) {
		return new Query<>(key, null, null, fetcher);
	}

	public static <T> Query<T> of(QueryKey key, Readable<Boolean> enabled, Supplier<CompletableFuture<T>> fetcher) {
		return new Query<>(key, null, enabled, fetcher);
	}

	public static <T> Query<T> of(Supplier<CompletableFuture<T>> fetcher) {
		return new Query<>(QueryKey.of(new Object()), null, null, fetcher);
	}

	public static <T> Query<T> of(Readable<Boolean> enabled, Supplier<CompletableFuture<T>> fetcher) {
		return new Query<>(QueryKey.of(new Object()), null, enabled, fetcher);
	}

	/**
	 * Creates a query whose cache key is recomputed before every fetch from
	 * reactive state, so each parameter combination (e.g. browser pagination,
	 * search terms) owns its own cache entry and in-flight request. The supplier
	 * must read the same reactive state the fetcher uses to build its request;
	 * it is evaluated inside the query effect so dependency tracking stays
	 * intact even when an identical in-flight request is joined.
	 *
	 * <p>Cache helpers operating on the fixed key ({@link #mutate(Object)},
	 * {@link #isStale()}, {@link #getKey()}) address the initial key.
	 */
	public static <T> Query<T> ofDynamic(Readable<Boolean> enabled, Supplier<QueryKey> keySupplier,
			Supplier<CompletableFuture<T>> fetcher) {
		return new Query<>(null, keySupplier, enabled, fetcher);
	}

	private QueryKey keyForFetch() {
		return keySupplier != null ? keySupplier.get() : key;
	}

	private void initEffect() {
		effect = Effect.of(() -> {
			if (disposed) return;

			boolean isEnabled = enabled == null || Boolean.TRUE.equals(enabled.get());
			if (!isEnabled) {
				fetchGeneration++;
				fetching.set(false);
				return;
			}

			if (initialRun) {
				initialRun = false;
				CacheEntry<T> entry = cache.getEntry(keyForFetch());
				if (entry != null && entry.hasData() && !entry.isStale(staleTimeMs)) {
					// Cached data is fresh on first run; do not trigger network fetch
					return;
				}
			}

			doFetch();
		});
	}

	public Query<T> enabled(Readable<Boolean> enabled) {
		this.enabled = enabled;
		if (enabled != null && !Boolean.TRUE.equals(enabled.peek())) {
			fetchGeneration++;
			fetching.set(false);
		}
		if (effect != null) {
			effect.invalidate();
		}
		return this;
	}

	public Query<T> staleTime(Duration staleTime) {
		this.staleTimeMs = staleTime != null ? staleTime.toMillis() : QueryCache.DEFAULT_STALE_TIME_MS;
		return this;
	}

	public Query<T> gcTime(Duration gcTime) {
		this.gcTimeMs = gcTime != null ? gcTime.toMillis() : QueryCache.DEFAULT_GC_TIME_MS;
		return this;
	}

	public Query<T> retry(int count) {
		this.retryCount = Math.max(0, count);
		return this;
	}

	public Query<T> retryDelay(Duration delay) {
		this.retryDelayMs = delay != null ? Math.max(0, delay.toMillis()) : DEFAULT_RETRY_DELAY_MS;
		return this;
	}

	public Query<T> refetchInterval(Duration interval) {
		this.refetchInterval = interval;
		startIntervalTimer();
		return this;
	}

	private void startIntervalTimer() {
		cancelIntervalTimer();
		if (disposed || refetchInterval == null || refetchInterval.isZero() || refetchInterval.isNegative()) {
			return;
		}

		float seconds = Math.max(0.1f, refetchInterval.toMillis() / 1000f);
		intervalTask = scheduleTimer(new Runnable() {
			@Override
			public void run() {
				if (disposed) return;
				boolean isEnabled = enabled == null || Boolean.TRUE.equals(enabled.peek());
				if (isEnabled) {
					refetch();
				}
				if (!disposed && refetchInterval != null) {
					startIntervalTimer();
				}
			}
		}, seconds);
	}

	private void cancelIntervalTimer() {
		if (intervalTask != null) {
			intervalTask.dispose();
			intervalTask = null;
		}
	}

	private void cancelRetryTimer() {
		if (retryTask != null) {
			retryTask.dispose();
			retryTask = null;
		}
	}

	private void doFetch() {
		if (disposed) return;
		cancelRetryTimer();

		int gen = ++fetchGeneration;

		if (!hasData) {
			loading.set(true);
		}
		fetching.set(true);

		CompletableFuture<T> future;
		try {
			// Forced fetch: dependency changes on the same key (e.g. browser
			// pagination) and explicit refetch() must always hit the network.
			// Freshness via per-query staleTimeMs is owned by initEffect() and
			// ensureFresh(), which skip doFetch() entirely when data is fresh.
			// With dynamic keys the key is recomputed here (inside the effect,
			// keeping dependency tracking intact) so joining only ever happens
			// between requests with identical parameters.
			QueryKey fetchKey = keyForFetch();
			fetchedKeys.add(fetchKey);
			future = cache.fetchOrJoin(fetchKey, fetcher);
		} catch (Throwable t) {
			handleError(gen, t);
			return;
		}

		future.whenComplete((result, throwable) -> {
			postToMain(() -> {
				if (disposed || gen != fetchGeneration) {
					return;
				}
				if (throwable != null) {
					handleError(gen, throwable);
				} else {
					handleSuccess(gen, result);
				}
			});
		});
	}

	private void handleSuccess(int gen, T result) {
		if (disposed || gen != fetchGeneration) return;

		currentRetry = 0;
		data.set(result);
		hasData = true;
		loading.set(false);
		fetching.set(false);
		error.set(null);
	}

	private void handleError(int gen, Throwable throwable) {
		if (disposed || gen != fetchGeneration) return;

		Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;

		if (currentRetry < retryCount) {
			int attempt = currentRetry++;
			long delayMs = retryDelayMs * (1L << attempt);
			float delaySeconds = Math.max(0.001f, delayMs / 1000f);
			retryTask = scheduleTimer(this::doFetch, delaySeconds);
		} else {
			currentRetry = 0;
			error.set(cause);
			loading.set(false);
			fetching.set(false);
		}
	}

	public void refetch() {
		if (disposed) return;
		currentRetry = 0;
		doFetch();
	}

	public void mutate(@Nullable T newData) {
		if (disposed) return;
		data.set(newData);
		error.set(null);
		loading.set(false);
		fetching.set(false);
		hasData = true;
		cache.put(key, newData);
	}

	public Readable<T> data() {
		return data;
	}

	public Readable<Boolean> loading() {
		return loading;
	}

	public Readable<Boolean> fetching() {
		return fetching;
	}

	public Readable<Throwable> error() {
		return error;
	}

	public QueryKey getKey() {
		return key;
	}

	public boolean hasData() {
		return hasData;
	}

	public boolean isStale() {
		CacheEntry<T> entry = cache.getEntry(key);
		return entry == null || !entry.hasData() || entry.isStale(staleTimeMs);
	}

	public boolean isError() {
		return error.peek() != null;
	}

	public boolean isFetching() {
		return Boolean.TRUE.equals(fetching.peek());
	}

	public void ensureFresh() {
		if (disposed || isFetching()) {
			return;
		}
		boolean isEnabled = enabled == null || Boolean.TRUE.equals(enabled.peek());
		if (!isEnabled) {
			return;
		}
		if (!hasData || isError() || isStale()) {
			refetch();
		}
	}

	@Override
	public T get() {
		return data.get();
	}

	@Override
	public T peek() {
		return data.peek();
	}

	@Override
	public void dispose() {
		if (disposed) return;
		disposed = true;
		fetchGeneration++;

		cancelRetryTimer();
		cancelIntervalTimer();

		if (effect != null) {
			effect.dispose();
			effect = null;
		}

		cache.detachObserver(key, cacheListener, gcTimeMs);

		// Parameter-keyed entries have no registered observers; schedule their
		// eviction so browsing does not leak one cache entry per visited page.
		for (QueryKey fetched : fetchedKeys) {
			if (!fetched.equals(key)) {
				cache.evictIfUnobserved(fetched, gcTimeMs);
			}
		}
		fetchedKeys.clear();
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}

	private static void postToMain(Runnable runnable) {
		if (Core.app != null) {
			Core.app.post(runnable);
		} else {
			runnable.run();
		}
	}

	private static @Nullable Disposable scheduleTimer(Runnable runnable, float delaySeconds) {
		try {
			if (Core.app == null) {
				return null;
			}
			Timer.Task task = Timer.schedule(runnable, delaySeconds);
            
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
	}
}
