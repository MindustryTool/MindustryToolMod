package solim.reactive;

import arc.Core;
import arc.util.Nullable;
import arc.util.Timer;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import arc.func.Prov;
import solim.core.Disposable;
import solim.runtime.OwnershipContext;

/**
 * Asynchronous reactive query primitive with dependency tracking, caching,
 * stale-while-revalidate, retry, and lifecycle management.
 *
 * <p>Create simple queries with {@link #of(QueryKey, Prov)} or
 * {@link #noKey(Prov)}; use {@link #builder()} for advanced
 * configuration. All options must be supplied before {@code build()} —
 * configuration is fixed for the query's lifetime.
 *
 * @param <T> query data type
 */
public final class Query<T> implements Readable<T>, Disposable {

	private static final int DEFAULT_RETRY_COUNT = 3;
	private static final long DEFAULT_RETRY_DELAY_MS = 1_000L;

	/**
	 * Package-private configuration bundle. Accumulated by {@link Builder}
	 * and consumed once by the {@link Query} constructor.
	 */
	static final class Options {
		@Nullable Readable<Boolean> enabled;
		long staleTimeMs = QueryCache.DEFAULT_STALE_TIME_MS;
		long gcTimeMs = QueryCache.DEFAULT_GC_TIME_MS;
		int retryCount = DEFAULT_RETRY_COUNT;
		long retryDelayMs = DEFAULT_RETRY_DELAY_MS;
		@Nullable Duration refetchInterval;
	}

	private final QueryKey key;
	private final Prov<CompletableFuture<T>> fetcher;
	private final @Nullable Prov<QueryKey> keySupplier;
	private final Set<QueryKey> fetchedKeys = new LinkedHashSet<>();
	private final QueryCache cache;

	private final Signal<T> data = Signal.of(null);
	private final Signal<Boolean> loading = Signal.of(true);
	private final Signal<Boolean> fetching = Signal.of(false);
	private final Signal<Throwable> error = Signal.of(null);

	private final @Nullable Readable<Boolean> enabled;
	private final long staleTimeMs;
	private final long gcTimeMs;
	private final int retryCount;
	private final long retryDelayMs;
	private final @Nullable Duration refetchInterval;

	private int fetchGeneration = 0;
	private int currentRetry = 0;
	private boolean hasData = false;
	private boolean disposed = false;
	private boolean initialRun = true;

	private @Nullable Effect effect;
	private @Nullable Disposable retryTask;
	private @Nullable Disposable intervalTask;
	private final QueryCache.InvalidationListener cacheListener = this::refetch;

	private Query(@Nullable QueryKey key, @Nullable Prov<QueryKey> keySupplier,
			Prov<CompletableFuture<T>> fetcher, Options options) {
		this.fetcher = Objects.requireNonNull(fetcher, "fetcher cannot be null");
		this.keySupplier = keySupplier;
		this.key = keySupplier != null
				? Objects.requireNonNull(keySupplier.get(), "key Prov must return a key")
				: Objects.requireNonNull(key, "key cannot be null");
		this.enabled = options.enabled;
		this.staleTimeMs = options.staleTimeMs;
		this.gcTimeMs = options.gcTimeMs;
		this.retryCount = options.retryCount;
		this.retryDelayMs = options.retryDelayMs;
		this.refetchInterval = options.refetchInterval;
		this.cache = QueryCache.getInstance();

		OwnershipContext.register(this);
		this.cache.attachObserver(this.key, cacheListener);

		// Check cache for existing data on initialization
		CacheEntry<T> entry = this.cache.getEntry(this.key);
		if (entry != null && entry.hasData()) {
			this.data.set(entry.getData());
			this.loading.set(false);
			this.hasData = true;
		}

		initEffect();
		startIntervalTimer();
	}

	/**
	 * Creates a simple query with a static cache key. Equivalent to
	 * {@code Query.builder().key(key).fetch(fetcher).build()}.
	 */
	public static <T> Query<T> of(QueryKey key, Prov<CompletableFuture<T>> fetcher) {
		return Query.<T>builder().key(key).fetch(fetcher).build();
	}

	/**
	 * Creates a simple query with a static cache key and an enablement gate.
	 * Equivalent to {@code Query.builder().key(key).enabled(enabled).fetch(fetcher).build()}.
	 */
	public static <T> Query<T> of(QueryKey key, Readable<Boolean> enabled, Prov<CompletableFuture<T>> fetcher) {
		return Query.<T>builder().key(key).enabled(enabled).fetch(fetcher).build();
	}

	/**
	 * Creates a stateless query under an anonymous key. The key cannot be
	 * targeted by {@link QueryCache#invalidate(QueryKey)}, so this is only
	 * appropriate for fetches with no cache identity (e.g. one-off loads).
	 */
	public static <T> Query<T> noKey(Prov<CompletableFuture<T>> fetcher) {
		return Query.<T>builder().fetch(fetcher).build();
	}

	public static <T> Builder<T> builder() {
		return new Builder<>();
	}

	/**
	 * Builder for advanced query configuration. All options are applied at
	 * {@link #build()} time, before the query's fetch effect starts — late or
	 * out-of-order configuration is impossible.
	 *
	 * @param <T> query data type
	 */
	public static final class Builder<T> {
		private final Options options = new Options();
		private @Nullable QueryKey key;
		private @Nullable Prov<QueryKey> keySupplier;
		private @Nullable Prov<CompletableFuture<T>> fetcher;

		/**
		 * Sets a static cache key. Clears any previously supplied key Prov.
		 */
		public Builder<T> key(QueryKey key) {
			this.key = Objects.requireNonNull(key, "key cannot be null");
			this.keySupplier = null;
			return this;
		}

		/**
		 * Sets a dynamic cache key recomputed before every fetch from reactive
		 * state, so each parameter combination (e.g. browser pagination, search
		 * terms) owns its own cache entry and in-flight request. The Prov
		 * must read the same reactive state the fetcher uses to build its
		 * request; it is evaluated inside the query effect so dependency
		 * tracking stays intact even when an identical in-flight request is
		 * joined.
		 *
		 * <p>Cache helpers operating on the fixed key ({@link Query#mutate},
		 * {@link Query#isStale()}, {@link Query#getKey()}) address the initial
		 * key. Clears any previously supplied static key.
		 */
		public Builder<T> key(Prov<QueryKey> keySupplier) {
			this.keySupplier = Objects.requireNonNull(keySupplier, "key Prov cannot be null");
			this.key = null;
			return this;
		}

		public Builder<T> enabled(Readable<Boolean> enabled) {
			this.options.enabled = Objects.requireNonNull(enabled, "enabled cannot be null");
			return this;
		}

		public Builder<T> fetch(Prov<CompletableFuture<T>> fetcher) {
			this.fetcher = Objects.requireNonNull(fetcher, "fetcher cannot be null");
			return this;
		}

		public Builder<T> staleTime(Duration staleTime) {
			this.options.staleTimeMs = staleTime != null ? staleTime.toMillis() : QueryCache.DEFAULT_STALE_TIME_MS;
			return this;
		}

		public Builder<T> gcTime(Duration gcTime) {
			this.options.gcTimeMs = gcTime != null ? gcTime.toMillis() : QueryCache.DEFAULT_GC_TIME_MS;
			return this;
		}

		public Builder<T> retry(int count) {
			this.options.retryCount = Math.max(0, count);
			return this;
		}

		public Builder<T> retryDelay(Duration delay) {
			this.options.retryDelayMs = delay != null
					? Math.max(0, delay.toMillis())
					: DEFAULT_RETRY_DELAY_MS;
			return this;
		}

		public Builder<T> refetchInterval(Duration interval) {
			this.options.refetchInterval = interval;
			return this;
		}

		public Query<T> build() {
			if (fetcher == null) {
				throw new IllegalStateException("fetch must be set before build()");
			}
			// No key supplied: anonymous identity, same semantics as noKey(...)
			QueryKey effectiveKey = key != null ? key : QueryKey.of(new Object());
			return new Query<>(effectiveKey, keySupplier, fetcher, options);
		}
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
