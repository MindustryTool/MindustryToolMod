package mindustrytool.ui.image;

import arc.Core;
import arc.files.Fi;
import arc.graphics.*;
import arc.graphics.g2d.TextureRegion;
import arc.util.Http;
import mindustry.gen.Icon;
import mindustrytool.Main;
import mindustrytool.core.util.ThreadSafeCache;

public final class NetworkImageLoader {
    private NetworkImageLoader() {}

    public static void load(String url, ThreadSafeCache<String, TextureRegion> cache, Runnable onError) {
        cache.put(url, Icon.refresh.getRegion());
        if (!url.endsWith("png") && !url.endsWith("jpg") && !url.endsWith("jpeg")) return;
        Fi file = Main.imageDir.child(url.replace(":", "-").replace("/", "-").replace("?", "-").replace("&", "-"));
        if (file.exists()) loadFile(url, file, cache, onError);
        else loadHttp(url, file, cache, onError);
    }

    private static void loadFile(String url, Fi file, ThreadSafeCache<String, TextureRegion> cache, Runnable onError) {
        try { createTexture(file.readBytes(), url, cache, () -> { file.delete(); onError.run(); }); }
        catch (Exception e) { file.delete(); onError.run(); }
    }

    private static void loadHttp(String url, Fi file, ThreadSafeCache<String, TextureRegion> cache, Runnable onError) {
        Http.get(url + "?format=jpeg", res -> {
            byte[] d = res.getResult(); if (d.length == 0) return;
            try { file.writeBytes(d); } catch (Exception ignored) {}
            createTexture(d, url, cache, onError);
        }, e -> onError.run());
    }

    private static void createTexture(byte[] data, String url, ThreadSafeCache<String, TextureRegion> cache, Runnable onError) {
        try {
            Pixmap pix = new Pixmap(data);
            Core.app.post(() -> {
                try { Texture t = new Texture(pix); t.setFilter(Texture.TextureFilter.linear); cache.put(url, new TextureRegion(t)); pix.dispose(); }
                catch (Exception e) { pix.dispose(); onError.run(); }
            });
        } catch (Exception e) { onError.run(); }
    }
}
