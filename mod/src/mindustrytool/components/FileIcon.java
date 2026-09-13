package mindustrytool.components;

import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Log;
import arc.util.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import mindustry.gen.Icon;
import mindustrytool.Main;

public class FileIcon {

    private static ConcurrentHashMap<String, TextureRegionDrawable> iconCache = new ConcurrentHashMap<>();

    public static TextureRegionDrawable of(String name) {
        return of(name, fallbackIcon());
    }

    public static TextureRegionDrawable of(String name, @Nullable TextureRegionDrawable fallback) {
        if (iconCache.containsKey(name)) {
            return iconCache.get(name);
        }

        try {
            if (Main.self == null || Main.self.root == null) {
                return fallback != null ? fallback : fallbackIcon();
            }
            var file = Main.self.root.child("icons").child(name);

            if (!file.exists()) {
                return fallback != null ? fallback : fallbackIcon();
            }
            var texture = new TextureRegion(new Texture(file));
            var drawable = new TextureRegionDrawable(texture);
            iconCache.put(name, drawable);

            return drawable;
        } catch (Exception e) {
            Log.err(e.getMessage());
            var fb = fallback != null ? fallback : fallbackIcon();
            iconCache.put(name, fb);
            return fb;
        }
    }

    private static TextureRegionDrawable fallbackIcon() {
        return Icon.book;
    }
}
