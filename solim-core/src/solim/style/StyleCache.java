package solim.style;

import arc.util.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Thread-safe flyweight cache interning static button style configurations.
 * Identical static styles share a single {@link SolimButtonStyle} instance
 * with zero redundant allocations. Dynamic styles containing signals bypass
 * the cache entirely.
 */
public final class StyleCache {

    private static final ConcurrentHashMap<String, SolimButtonStyle> CACHE = new ConcurrentHashMap<>();

    private StyleCache() {
    }

    public static @Nullable SolimButtonStyle get(String key) {
        if (key == null) {
            return null;
        }
        return CACHE.get(key);
    }

    public static SolimButtonStyle getOrCreate(String key, Supplier<SolimButtonStyle> factory) {
        return CACHE.computeIfAbsent(key, k -> factory.get());
    }

    public static void clear() {
        CACHE.clear();
    }

    public static int size() {
        return CACHE.size();
    }
}
