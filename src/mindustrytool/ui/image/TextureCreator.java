package mindustrytool.ui.image;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.TextureRegion;
import mindustrytool.core.util.ThreadSafeCache;

public final class TextureCreator {
    private TextureCreator() {}

    public static void create(byte[] data, String url, ThreadSafeCache<String, TextureRegion> cache, Runnable onError) {
        try {
            Pixmap pix = new Pixmap(data);
            Core.app.post(() -> {
                try {
                    Texture tex = new Texture(pix);
                    tex.setFilter(Texture.TextureFilter.linear);
                    cache.put(url, new TextureRegion(tex));
                    pix.dispose();
                } catch (Exception e) { pix.dispose(); onError.run(); }
            });
        } catch (Exception e) { onError.run(); }
    }
}
