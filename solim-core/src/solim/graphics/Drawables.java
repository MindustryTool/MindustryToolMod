package solim.graphics;

import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Drawables {

    private static final Map<TextureRegionDrawable, TextureRegionDrawable> scalableCache = new ConcurrentHashMap<>();

    private Drawables() {
    }

    public static @Nullable Drawable scalable(@Nullable Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        if (drawable instanceof TextureRegionDrawable) {
            TextureRegionDrawable trd = (TextureRegionDrawable) drawable;
            if (trd.getClass() != TextureRegionDrawable.class && trd.getRegion() != null) {
                TextureRegionDrawable cached = scalableCache.get(trd);
                if (cached == null) {
                    cached = new TextureRegionDrawable(trd.getRegion());
                    cached.setMinWidth(trd.getMinWidth());
                    cached.setMinHeight(trd.getMinHeight());
                    scalableCache.put(trd, cached);
                }
                return cached;
            }
        }
        return drawable;
    }
}
