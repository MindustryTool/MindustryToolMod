package mindustrytool.features.social.auth;

import arc.struct.ObjectMap;
import arc.graphics.Texture;

/** Simple image cache for auth plugin */
public final class ImageCache {
    private static final ObjectMap<String, Texture> cache = new ObjectMap<>();
    
    private ImageCache() {}
    
    public static Texture get(String key) { return cache.get(key); }
    public static void put(String key, Texture tex) { cache.put(key, tex); }
    public static boolean has(String key) { return cache.containsKey(key); }
    
    public static void clear() {
        for (Texture tex : cache.values()) {
            if (tex != null) tex.dispose();
        }
        cache.clear();
    }
}
