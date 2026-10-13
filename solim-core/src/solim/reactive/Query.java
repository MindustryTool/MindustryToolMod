package solim.reactive;

import arc.Core;
import arc.util.Nullable;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import arc.func.Prov;
import solim.core.Disposable;
import solim.core.OwnershipContext;

public final class Query<T> implements Readable<T>, Disposable {

    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final long DEFAULT_RETRY_DELAY_MS = 1_000L;
    private static final int MAX_RETRY_SHIFT = 10;
    private static final long MAX_RETRY_DELAY_MS = 60_000L;
    private static final int MAX_FETCHED_KEYS = 32;

    static final class Options {
        @Nullable
        Readable<Boolean> enabled;
        long staleTimeMs = QueryCache.DEFAULT_STALE_TIME_MS;
        long gcTimeMs = QueryCache.DEFAULT_GC_TIME_MS;
        int retryCount = DEFAULT_RETRY_COUNT;
        long retryDelayMs = DEFAULT_RETRY_DELAY_MS;
        @Nullable
        Duration refetchInterval;
    }

    private QueryKey key;
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

        boolean isInitiallyEnabled = options.enabled == null || Boolean.TRUE.equals(options.enabled.peek());
        this.loading.set(isInitiallyEnabled);

        OwnershipContext.register(this);
        this.cache.attachObserver(this.key, cacheListener);

        CacheEntry<T> entry = this.cache.getEntry(this.key);
        if (entry != null && entry.hasData()) {
            this.data.set(entry.getData());
            this.loading.set(false);
            this.hasData = true;
        }

        initEffect();
        startIntervalTimer();
    }

    public static <T> Query<T> of(QueryKey key, Prov<CompletableFuture<T>> fetcher) {
        return Query.<T>builder().key(key).fetch(fetcher).build();
    }

    public static <T> Query<T> of(QueryKey key, Readable<Boolean> enabled, Prov<CompletableFuture<T>> fetcher) {
        return Query.<T>builder().key(key).enabled(enabled).fetch(fetcher).build();
    }

    public static <T> Query<T> noKey(Prov<CompletableFuture<T>> fetcher) {
        return Query.<T>builder().fetch(fetcher).build();
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private final Options options = new Options();
        private @Nullable QueryKey key;
        private @Nullable Prov<QueryKey> keySupplier;
        private @Nullable Prov<CompletableFuture<T>> fetcher;

        public Builder<T> key(QueryKey key) {
            this.key = Objects.requireNonNull(key, "key cannot be null");
            this.keySupplier = null;
            return this;
        }

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

            QueryKey effectiveKey = key != null ? key : QueryKey.of(new Object());
            return new Query<>(effectiveKey, keySupplier, fetcher, options);
        }
    }

    private QueryKey keyForFetch() {
        return keySupplier != null ? keySupplier.get() : key;
    }

    private void initEffect() {
        effect = Effect.of(() -> {
            if (disposed)
                return;

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
        intervalTask = cache.scheduler().schedule(new Runnable() {
            @Override
            public void run() {
                if (disposed)
                    return;
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
        if (disposed)
            return;
        cancelRetryTimer();

        int gen = ++fetchGeneration;

        if (!hasData) {
            loading.set(true);
        }
        fetching.set(true);

        CompletableFuture<T> future;
        try {

            QueryKey fetchKey = keyForFetch();
            if (!fetchKey.equals(this.key)) {
                cache.detachObserver(this.key, cacheListener, gcTimeMs);
                this.key = fetchKey;
                cache.attachObserver(this.key, cacheListener);
            }
            rememberFetchedKey(fetchKey);
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
        if (disposed || gen != fetchGeneration)
            return;

        currentRetry = 0;
        data.set(result);
        hasData = true;
        loading.set(false);
        fetching.set(false);
        error.set(null);
    }

    private void handleError(int gen, Throwable throwable) {
        if (disposed || gen != fetchGeneration)
            return;

        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        if (cause != throwable) {
            cause.addSuppressed(throwable);
        }

        boolean isCancelled = cause instanceof CancellationException;

        if (!isCancelled && currentRetry < retryCount) {
            int attempt = currentRetry++;
            int shift = Math.min(attempt, MAX_RETRY_SHIFT);
            long multiplier = 1L << shift;
            long delayMs = retryDelayMs > MAX_RETRY_DELAY_MS / multiplier
                    ? MAX_RETRY_DELAY_MS
                    : retryDelayMs * multiplier;
            float delaySeconds = Math.max(0.001f, delayMs / 1000f);
            retryTask = cache.scheduler().schedule(this::doFetch, delaySeconds);
        } else {
            currentRetry = 0;
            error.set(cause);
            loading.set(false);
            fetching.set(false);
        }
    }

    public void refetch() {
        if (disposed)
            return;
        currentRetry = 0;
        doFetch();
    }

    public void mutate(@Nullable T newData) {
        if (disposed)
            return;
        data.set(newData);
        error.set(null);
        loading.set(false);
        fetching.set(false);
        hasData = true;
        cache.put(keyForFetch(), newData);
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
        if (disposed)
            return;
        disposed = true;
        fetchGeneration++;

        cancelRetryTimer();
        cancelIntervalTimer();

        if (effect != null) {
            effect.dispose();
            effect = null;
        }

        cache.detachObserver(key, cacheListener, gcTimeMs);

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

    private void rememberFetchedKey(QueryKey fetched) {
        if (fetchedKeys.contains(fetched)) {
            return;
        }
        fetchedKeys.add(fetched);
        while (fetchedKeys.size() > MAX_FETCHED_KEYS) {
            QueryKey oldest = fetchedKeys.iterator().next();
            fetchedKeys.remove(oldest);
            cache.evictIfUnobserved(oldest, gcTimeMs);
        }
    }

    private void postToMain(Runnable runnable) {
        if (Core.app != null) {
            Core.app.post(runnable);
        } else {
            Disposable scheduled = cache.scheduler().schedule(runnable, 0f);
            if (scheduled == null) {
                runnable.run();
            }
        }
    }
}
