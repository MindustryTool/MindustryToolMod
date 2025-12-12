package mindustrytool.ui.image;

import arc.files.Fi;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ObjectMap;
import arc.util.*;
import mindustry.gen.Icon;
import mindustrytool.Main;

/** Loads images from cache or network. */
public final class NetworkImageLoader {
    private NetworkImageLoader() {}

    public static void load(String url, ObjectMap<String, TextureRegion> cache, Runnable onError) {
        cache.put(url, Icon.refresh.getRegion());
        if (!isImage(url)) return;
        Fi file = Main.imageDir.child(sanitize(url));
        if (file.exists()) loadFromFile(url, file, cache, onError);
        else loadFromHttp(url, file, cache, onError);
    }

    private static boolean isImage(String url) {
        return url.endsWith("png") || url.endsWith("jpg") || url.endsWith("jpeg");
    }

    private static String sanitize(String url) {
        return url.replace(":", "-").replace("/", "-").replace("?", "-").replace("&", "-");
    }

    private static void loadFromFile(String url, Fi file, ObjectMap<String, TextureRegion> cache, Runnable onError) {
        try { TextureCreator.create(file.readBytes(), url, cache, onError); }
        catch (Exception e) { onError.run(); file.delete(); Log.err(url, e); }
    }

    private static void loadFromHttp(String url, Fi file, ObjectMap<String, TextureRegion> cache, Runnable onError) {
        Http.get(url + "?format=jpeg", res -> {
            byte[] data = res.getResult();
            if (data.length == 0) return;
            try { file.writeBytes(data); } catch (Exception e) { Log.err(url, e); }
            TextureCreator.create(data, url, cache, onError);
        }, error -> {
            if (!(error instanceof Http.HttpStatusException) ||
                ((Http.HttpStatusException) error).status != Http.HttpStatus.NOT_FOUND)
                Log.err(url, error);
            onError.run();
        });
    }
}
