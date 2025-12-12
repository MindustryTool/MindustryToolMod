package mindustrytool.ui.image;

import arc.Core;
import arc.graphics.Texture;
import arc.graphics.Pixmap;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ObjectMap;
import arc.util.Log;

public class ImageCache {
    private static final Object cacheLock = new Object();
    private static final ObjectMap<String, TextureRegion> cache = new ObjectMap<>(256);
    private static final TextureRegion PLACEHOLDER = Core.atlas.find("nomap");

    public static TextureRegion get(String id) {
        synchronized (cacheLock) {
            return cache.get(id, PLACEHOLDER);
        }
    }
    
    public static boolean has(String id) {
        synchronized (cacheLock) {
            return cache.containsKey(id);
        }
    }
    
    public static TextureRegion createAndCache(String id, Pixmap pix) {
        try {
            TextureRegion region = new TextureRegion(new Texture(pix));
            synchronized (cacheLock) {
                cache.put(id, region);
            }
            pix.dispose();
            return region;
        } catch (Exception e) { Log.err("Texture error: " + id, e); pix.dispose(); return PLACEHOLDER; }
    }
    
    public static void clear() {
        synchronized (cacheLock) {
            // Dispose textures to free GPU memory
            for (TextureRegion region : cache.values()) {
                if (region != null && region.texture != null) {
                    try { region.texture.dispose(); } catch (Exception ignored) {}
                }
            }
            cache.clear();
        }
        Log.info("[ImageCache] Cache cleared");
    }
}
