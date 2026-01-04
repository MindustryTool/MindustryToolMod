package mindustrytool.features.content.browser;

import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.Image;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;

public class NetworkImage extends Image {
    public Color borderColor = Pal.gray;
    public float thickness = 1f;
    private volatile boolean isError, loadStarted;
    private final String url;
    private TextureRegion lastTexture;
    private static final ThreadSafeCache<String, TextureRegion> cache = new ThreadSafeCache<>(128, null);

    public NetworkImage(String url) {
        super(Tex.clear);
        this.url = url;
        setScaling(Scaling.fit);
        startLoad();
    }

    private void startLoad() {
        if (loadStarted || isError || cache.has(url))
            return;
        loadStarted = true;
        try {
            NetworkImageLoader.load(url, cache, () -> isError = true);
        } catch (Exception e) {
            Log.err(url, e);
            isError = true;
        }
    }

    @Override
    public void draw() {
        TextureRegion next = cache.get(url);
        if (next != null && lastTexture != next) {
            lastTexture = next;
            setDrawable(next);
        }
        super.draw();
    }

    public static void clearCache() {
        cache.clear(null);
    }
}
