package mindustrytool.presentation.builder;

import arc.Core;
import arc.graphics.*;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.g2d.TextureRegion;
import arc.struct.ObjectMap;
import arc.util.*;
import mindustry.gen.*;
import mindustrytool.Main;

public class NetworkImageLoader {
    public static void load(String url, ObjectMap<String, TextureRegion> cache, Runnable onError) {
        cache.put(url, Icon.refresh.getRegion());
        if (!url.endsWith("png") && !url.endsWith("jpg") && !url.endsWith("jpeg")) return;
        var file = Main.imageDir.child(url.replace(":", "-").replace("/", "-").replace("?", "-").replace("&", "-"));
        if (file.exists()) loadFromFile(url, file, cache, onError);
        else loadFromHttp(url, file, cache, onError);
    }

    private static void loadFromFile(String url, arc.files.Fi file, ObjectMap<String, TextureRegion> cache, Runnable onError) {
        try {
            byte[] result = file.readBytes();
            Pixmap pix = new Pixmap(result);
            Core.app.post(() -> { try { var tex = new Texture(pix); tex.setFilter(TextureFilter.linear); cache.put(url, new TextureRegion(tex)); pix.dispose(); } catch (Exception e) { Log.err(url, e); onError.run(); } });
        } catch (Exception e) { onError.run(); file.delete(); Log.err(url, e); }
    }

    private static void loadFromHttp(String url, arc.files.Fi file, ObjectMap<String, TextureRegion> cache, Runnable onError) {
        Http.get(url + "?format=jpeg", res -> {
            byte[] result = res.getResult();
            if (result.length == 0) return;
            try { file.writeBytes(result); } catch (Exception error) { Log.err(url, error); }
            Core.app.post(() -> { try { Pixmap pix = new Pixmap(result); var tex = new Texture(pix); tex.setFilter(TextureFilter.linear); cache.put(url, new TextureRegion(tex)); pix.dispose(); } catch (Exception e) { Log.err(url, e); onError.run(); } });
        }, error -> { if (!(error instanceof Http.HttpStatusException requestError) || requestError.status != Http.HttpStatus.NOT_FOUND) Log.err(url, error); onError.run(); });
    }
}
