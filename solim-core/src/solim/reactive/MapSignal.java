package solim.reactive;

import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import solim.core.ReactiveObserver;
import solim.core.ReactiveSource;
import solim.runtime.ReactiveContext;
import solim.runtime.SolimAssert;

/**
 * Reactive map primitive with fine-grained key-level observation.
 * Implements {@link Readable} for whole-map reactivity and provides
 * key projections via {@link #readable(Object)} that only invalidate
 * when their specific key changes. Never registers with
 * {@link OwnershipContext}, so store-held instances survive UI unmounts.
 */
public final class MapSignal<K, V> implements Readable<Map<K, V>>, ReactiveSource {
    private final Map<K, V> data = new HashMap<>();
    private final Set<ReactiveObserver> mapObservers = new LinkedHashSet<>();
    private final Map<K, KeyReadable<K, V>> readableCache = new HashMap<>();

    public MapSignal() {
    }

    public MapSignal(@Nullable Map<K, V> initial) {
        if (initial != null && !initial.isEmpty()) {
            data.putAll(initial);
        }
    }

    public static <K, V> MapSignal<K, V> of() {
        return new MapSignal<>();
    }

    public static <K, V> MapSignal<K, V> of(@Nullable Map<K, V> initial) {
        return new MapSignal<>(initial);
    }

    @Override
    public Map<K, V> get() {
        ReactiveContext.trackWithWarning(this, "[Solim Reactivity Warning] MapSignal.get() was called during build()! "
                + "This severs reactivity. Pass the MapSignal/Readable directly to the component "
                + "or use .map(). If an untracked read is intentional, use .peek().");
        return Collections.unmodifiableMap(new HashMap<>(data));
    }

    @Override
    public Map<K, V> peek() {
        return Collections.unmodifiableMap(new HashMap<>(data));
    }

    /**
     * Returns the number of entries, tracking whole-map reactivity.
     */
    public int size() {
        ReactiveContext.trackWithWarning(this, "[Solim Reactivity Warning] MapSignal.size() was called during build()! "
                + "This severs reactivity. If an untracked read is intentional, use peek().size().");
        return data.size();
    }

    /**
     * Returns true when empty, tracking whole-map reactivity.
     */
    public boolean isEmpty() {
        ReactiveContext.trackWithWarning(this, "[Solim Reactivity Warning] MapSignal.isEmpty() was called during build()! "
                + "This severs reactivity. If an untracked read is intentional, use peek().isEmpty().");
        return data.isEmpty();
    }

    /**
     * Returns a lightweight key projection that only invalidates when
     * {@code key} changes. Never registers with {@link OwnershipContext}.
     */
    public Readable<V> readable(@Nullable K key) {
        return key == null ? Readable.of(null) : getOrCreateReadable(key);
    }

    private KeyReadable<K, V> getOrCreateReadable(K key) {
        KeyReadable<K, V> existing = readableCache.get(key);
        return existing != null ? existing : createReadable(key);
    }

    private KeyReadable<K, V> createReadable(K key) {
        KeyReadable<K, V> created = new KeyReadable<>(this, key);
        readableCache.put(key, created);
        return created;
    }

    /**
     * Associates {@code value} with {@code key}, notifying only observers
     * of {@code key} and whole-map observers when the value actually changed.
     */
    public void put(@Nullable K key, @Nullable V value) {
        SolimAssert.checkMainThread();
        boolean contained = data.containsKey(key);
        V old = data.get(key);
        if (contained && Objects.equals(old, value)) {
            return;
        }
        data.put(key, value);
        if (key != null) {
            invalidateKeyObservers(key);
        }
        invalidateMapObservers();
    }

    /**
     * Batch insertion that notifies only changed keys and whole-map
     * observers once per batch.
     */
    public void putAll(@Nullable Map<K, V> entries) {
        SolimAssert.checkMainThread();
        if (entries == null || entries.isEmpty()) {
            return;
        }
        List<K> changed = new ArrayList<>();
        for (Map.Entry<K, V> entry : entries.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            boolean contained = data.containsKey(key);
            V old = data.get(key);
            if (contained && Objects.equals(old, value)) {
                continue;
            }
            data.put(key, value);
            changed.add(key);
        }
        if (changed.isEmpty()) {
            return;
        }
        for (K key : changed) {
            if (key != null) {
                invalidateKeyObservers(key);
            }
        }
        invalidateMapObservers();
    }

    /**
     * Removes {@code key}, invalidating its observers with null and
     * whole-map observers. Returns the previous value or null.
     */
    public @Nullable V remove(@Nullable K key) {
        SolimAssert.checkMainThread();
        if (!data.containsKey(key)) {
            return null;
        }
        V old = data.remove(key);
        if (key != null) {
            invalidateKeyObservers(key);
        }
        invalidateMapObservers();
        return old;
    }

    /**
     * Clears all entries, invalidating every key observer and
     * whole-map observers. No-op when already empty.
     */
    public void clear() {
        SolimAssert.checkMainThread();
        if (data.isEmpty()) {
            return;
        }
        data.clear();
        List<KeyReadable<K, V>> cached = new ArrayList<>(readableCache.values());
        for (KeyReadable<K, V> readable : cached) {
            readable.invalidateObservers();
        }
        invalidateMapObservers();
    }

    @Override
    public void addObserver(ReactiveObserver observer) {
        mapObservers.add(observer);
    }

    @Override
    public void removeObserver(ReactiveObserver observer) {
        mapObservers.remove(observer);
    }

    private void invalidateKeyObservers(K key) {
        KeyReadable<K, V> readable = readableCache.get(key);
        if (readable != null) {
            readable.invalidateObservers();
        }
    }

    private void invalidateMapObservers() {
        Set<ReactiveObserver> copy = new LinkedHashSet<>(mapObservers);
        for (ReactiveObserver observer : copy) {
            try {
                observer.invalidate();
            } catch (Throwable e) {
                Log.err("[MapSignal] observer invalidate error", e);
            }
        }
    }

    int mapObserverCount() {
        return mapObservers.size();
    }

    int keyObserverCount(@Nullable K key) {
        KeyReadable<K, V> readable = key == null ? null : readableCache.get(key);
        return readable == null ? 0 : readable.observerCount();
    }

    int cachedReadableCount() {
        return readableCache.size();
    }

    /**
     * Lightweight per-key projection. Tracks only its own key and never
     * registers with {@link OwnershipContext}.
     */
    public static final class KeyReadable<K, V> implements Readable<V>, ReactiveSource {
        private final MapSignal<K, V> parent;
        private final K key;
        private final Set<ReactiveObserver> observers = new LinkedHashSet<>();

        KeyReadable(MapSignal<K, V> parent, K key) {
            this.parent = parent;
            this.key = key;
        }

        @Override
        public @Nullable V get() {
            ReactiveContext.trackWithWarning(this, "[Solim Reactivity Warning] MapSignal.readable(key).get() was called during build()! "
                    + "This severs reactivity. Pass the Readable directly to the component or use .map(). "
                    + "If an untracked read is intentional, use .peek().");
            return parent.data.get(key);
        }

        @Override
        public @Nullable V peek() {
            return parent.data.get(key);
        }

        @Override
        public void addObserver(ReactiveObserver observer) {
            observers.add(observer);
        }

        @Override
        public void removeObserver(ReactiveObserver observer) {
            observers.remove(observer);
            if (observers.isEmpty() && !parent.data.containsKey(key)) {
                parent.readableCache.remove(key);
            }
        }

        void invalidateObservers() {
            Set<ReactiveObserver> copy = new LinkedHashSet<>(observers);
            for (ReactiveObserver observer : copy) {
                try {
                    observer.invalidate();
                } catch (Throwable e) {
                    Log.err("[MapSignal] key observer invalidate error", e);
                }
            }
        }

        int observerCount() {
            return observers.size();
        }
    }
}
