package mindustrytool.features.pathfinding;

import arc.struct.LongMap;
import arc.struct.LongSeq;
import arc.util.Nullable;

public class PathfindingCacheManager {
    public static final int MAX_ENTRIES = 4096;

    private final LongMap<PathfindingCache> cache = new LongMap<>();
    private final LongSeq keysToRemove = new LongSeq();

    public @Nullable PathfindingCache get(long key) {
        return cache.get(key);
    }

    public void put(long key, PathfindingCache value) {
        if (value == null) {
            return;
        }
        if (cache.size >= MAX_ENTRIES && !cache.containsKey(key)) {
            evictOldest();
        }
        cache.put(key, value);
    }

    private void evictOldest() {
        long oldestKey = 0L;
        float oldestTime = Float.MAX_VALUE;
        boolean found = false;
        for (LongMap.Entry<PathfindingCache> entry : cache.entries()) {
            if (entry.value != null && entry.value.lastUsedTime < oldestTime) {
                oldestTime = entry.value.lastUsedTime;
                oldestKey = entry.key;
                found = true;
            }
        }
        if (found) {
            cache.remove(oldestKey);
        }
    }

    public void clear() {
        cache.clear();
    }

    public void cleanup(float currentTime, float maxAge) {
        keysToRemove.clear();
        for (LongMap.Entry<PathfindingCache> entry : cache.entries()) {
            if ((currentTime - entry.value.lastUsedTime) > maxAge) {
                keysToRemove.add(entry.key);
            }
        }
        for (int i = 0; i < keysToRemove.size; i++) {
            cache.remove(keysToRemove.get(i));
        }
    }
}