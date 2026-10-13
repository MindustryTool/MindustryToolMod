package solim.reactive;

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
import solim.core.SolimAssert;

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

    public int size() {
        ReactiveContext.trackWithWarning(this, "[Solim Reactivity Warning] MapSignal.size() was called during build()! "
                + "This severs reactivity. If an untracked read is intentional, use peek().size().");
        return data.size();
    }

    public boolean isEmpty() {
        ReactiveContext.trackWithWarning(this,
                "[Solim Reactivity Warning] MapSignal.isEmpty() was called during build()! "
                        + "This severs reactivity. If an untracked read is intentional, use peek().isEmpty().");
        return data.isEmpty();
    }

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
            observer.invalidate();
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
            ReactiveContext.trackWithWarning(this,
                    "[Solim Reactivity Warning] MapSignal.readable(key).get() was called during build()! "
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
                observer.invalidate();
            }
        }

        int observerCount() {
            return observers.size();
        }
    }
}
