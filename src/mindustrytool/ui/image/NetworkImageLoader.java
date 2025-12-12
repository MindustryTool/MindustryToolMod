package mindustrytool.ui.image;

import arc.files.Fi;
import arc.util.*;
import mindustry.gen.Icon;
import mindustrytool.Main;
import mindustrytool.core.util.ThreadSafeCache;
import arc.graphics.g2d.TextureRegion;

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
        try { TextureCreator.create(file.readBytes(), url, cache, () -> { file.delete(); onError.run(); }); }
        catch (Exception e) { file.delete(); onError.run(); Log.err(url, e); }
    }

    private static void loadHttp(String url, Fi file, ThreadSafeCache<String, TextureRegion> cache, Runnable onError) {
        Http.get(url + "?format=jpeg", res -> {
            byte[] d = res.getResult(); if (d.length == 0) return;
            try { file.writeBytes(d); } catch (Exception ignored) {}
            TextureCreator.create(d, url, cache, onError);
        }, e -> onError.run());
    }
}
