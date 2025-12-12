package mindustrytool.core.util;

import arc.struct.ObjectMap;

public class ThreadSafeCache<K, V> {
    private final Object lock = new Object();
    private final ObjectMap<K, V> cache;
    private final V placeholder;

    public ThreadSafeCache(int cap, V placeholder) { this.cache = new ObjectMap<>(cap); this.placeholder = placeholder; }
    public V get(K key) { synchronized (lock) { return cache.get(key, placeholder); } }
    public void put(K key, V val) { synchronized (lock) { cache.put(key, val); } }
    public boolean has(K key) { synchronized (lock) { return cache.containsKey(key); } }

    public void clear(java.util.function.Consumer<V> disposer) {
        synchronized (lock) {
            if (disposer != null) for (V v : cache.values()) try { disposer.accept(v); } catch (Exception ignored) {}
            cache.clear();
        }
    }
}
