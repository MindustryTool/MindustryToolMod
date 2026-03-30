package mindustrytool.features.display.pathfinding;

import arc.struct.LongMap;
import arc.struct.LongSeq;

public class PathfindingCacheManager {
    private final LongMap<PathfindingCache> cache = new LongMap<>();
    private final LongSeq keysToRemove = new LongSeq();

    public PathfindingCache get(long key) {
        return cache.get(key);
    }

    public void put(long key, PathfindingCache value) {
        cache.put(key, value);
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