package mindustrytool.components;

import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import mindustry.gen.Icon;
import mindustrytool.Main;

public class FileIcon {

    private static ConcurrentHashMap<String, TextureRegionDrawable> iconCache = new ConcurrentHashMap<>();

    public static TextureRegionDrawable of(String name) {
        if (iconCache.containsKey(name)) {
            return iconCache.get(name);
        }

        try {
            if (Main.self == null || Main.self.root == null) {
                return fallbackIcon();
            }
            var file = Main.self.root.child("icons").child(name);

            if (!file.exists()) {
                return fallbackIcon();
            }
            var texture = new TextureRegion(new Texture(file));
            var drawable = new TextureRegionDrawable(texture);
            iconCache.put(name, drawable);

            return drawable;
        } catch (Exception e) {
            Log.err(e.getMessage());
            var fallback = fallbackIcon();
            iconCache.put(name, fallback);
            return fallback;
        }
    }

    private static TextureRegionDrawable fallbackIcon() {
        return Icon.book != null ? Icon.book : new TextureRegionDrawable();
    }
}
