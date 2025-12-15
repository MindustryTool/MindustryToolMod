package mindustrytool.plugins.browser;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.TextureRegion;

public class ImageCache {
    private static final TextureRegion PLACEHOLDER = Core.atlas.find("nomap");
    private static final ThreadSafeCache<String, TextureRegion> cache = new ThreadSafeCache<>(256, PLACEHOLDER);

    public static TextureRegion get(String id) { return cache.get(id); }
    public static boolean has(String id) { return cache.has(id); }

    public static TextureRegion createAndCache(String id, Pixmap pix) {
        try {
            TextureRegion r = new TextureRegion(new Texture(pix));
            cache.put(id, r); 
            pix.dispose(); 
            return r;
        } catch (Exception e) { 
            pix.dispose(); 
            return PLACEHOLDER; 
        }
    }

    public static void clear() { cache.clear(null); }
}
