package mindustrytool.ui.component;

import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Scl;
import arc.struct.ObjectMap;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustrytool.ui.image.NetworkImageLoader;

public class NetworkImage extends Image {
    public Color borderColor = Pal.gray;
    public float scaling = 16f, thickness = 1f;
    private volatile boolean isError = false;
    private volatile boolean loadStarted = false;
    private final String url;
    private TextureRegion lastTexture;
    private static final Object cacheLock = new Object();
    private static final ObjectMap<String, TextureRegion> cache = new ObjectMap<>(128);

    public NetworkImage(String url) { 
        super(Tex.clear); 
        this.url = url; 
        setScaling(Scaling.fit);
        // Start loading immediately in constructor, not in draw()
        startLoad();
    }
    
    private void startLoad() {
        if (loadStarted || isError) return;
        synchronized (cacheLock) {
            if (cache.containsKey(url)) return;
        }
        loadStarted = true;
        try {
            NetworkImageLoader.load(url, cache, cacheLock, () -> isError = true);
        } catch (Exception error) { 
            Log.err(url, error); 
            isError = true; 
        }
    }

    @Override public void draw() {
        // Get texture from cache (thread-safe read)
        TextureRegion next;
        synchronized (cacheLock) {
            next = cache.get(url);
        }
        
        if (next != null && lastTexture != next) {
            lastTexture = next;
            setDrawable(next);
        }
        
        // Draw image
        super.draw();
        
        // Draw border
        Draw.color(borderColor);
        Lines.stroke(Scl.scl(thickness));
        Lines.rect(x, y, width, height);
        Draw.reset();
    }
    
    public static void clearCache() {
        synchronized (cacheLock) {
            // Dispose textures to free GPU memory
            for (TextureRegion region : cache.values()) {
                if (region != null && region.texture != null) {
                    try { region.texture.dispose(); } catch (Exception ignored) {}
                }
            }
            cache.clear();
        }
        Log.info("[NetworkImage] Cache cleared");
    }
}
