package mindustrytool.ui.image;

import arc.Core;
import arc.graphics.*;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ObjectMap;
import arc.util.Log;

/** Reusable texture creation utility. */
public final class TextureCreator {
    private TextureCreator() {}

    public static void create(byte[] data, String url, ObjectMap<String, TextureRegion> cache, Object cacheLock, Runnable onError) {
        try {
            Pixmap pix = new Pixmap(data);
            Core.app.post(() -> {
                try {
                    Texture tex = new Texture(pix);
                    tex.setFilter(TextureFilter.linear);
                    synchronized (cacheLock) {
                        cache.put(url, new TextureRegion(tex));
                    }
                    pix.dispose();
                } catch (Exception e) { Log.err(url, e); onError.run(); }
            });
        } catch (Exception e) { Log.err(url, e); onError.run(); }
    }
}
