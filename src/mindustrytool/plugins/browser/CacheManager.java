package mindustrytool.plugins.browser;

import arc.struct.ObjectMap;
import arc.struct.Seq;

/**
 * Thread-safe cache manager with capacity limits.
 * @param <K> Key type
 * @param <V> Value type
 */
public class CacheManager<K, V> {
    private final ObjectMap<K, V> cache;
    private final int maxSize;

    public CacheManager(int capacity) {
        this.cache = new ObjectMap<>(capacity);
        this.maxSize = capacity * 2;
    }

    public V get(K key) { return cache.get(key); }
    public boolean has(K key) { return cache.containsKey(key); }

    public void put(K key, V value) {
        if (cache.size >= maxSize) trim();
        cache.put(key, value);
    }

    private void trim() {
        Seq<K> keys = cache.keys().toSeq();
        for (int i = 0; i < Math.min(keys.size, cache.size - maxSize / 2); i++) {
            cache.remove(keys.get(i));
        }
    }

    public void clear() { cache.clear(); }
    public int size() { return cache.size; }
}
