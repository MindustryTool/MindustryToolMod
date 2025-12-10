package mindustrytool.ui.image;

import arc.Core;
import arc.graphics.Texture;
import arc.graphics.Pixmap;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ObjectMap;
import arc.util.Log;

public class ImageCache {
    private static final ObjectMap<String, TextureRegion> cache = new ObjectMap<>(256);
    private static final TextureRegion PLACEHOLDER = Core.atlas.find("nomap");

    public static TextureRegion get(String id) { return cache.get(id, PLACEHOLDER); }
    public static boolean has(String id) { return cache.containsKey(id); }
    
    public static TextureRegion createAndCache(String id, Pixmap pix) {
        try {
            TextureRegion region = new TextureRegion(new Texture(pix));
            cache.put(id, region);
            pix.dispose();
            return region;
        } catch (Exception e) { Log.err("Texture error: " + id, e); pix.dispose(); return PLACEHOLDER; }
    }
}
