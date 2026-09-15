package solim.graphics;

import arc.Core;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.g2d.NinePatch;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.NinePatchDrawable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for white continuous-curvature rounded NinePatchDrawables.
 *
 * <p>Textures are pure white (#FFFFFF) with sub-pixel anti-aliasing. A single texture
 * per radius or stroke width is reused across all components and tinted dynamically via
 * vertex colors.
 */
public final class RoundedCache {

    private static final Map<Integer, NinePatchDrawable> solidCache = new ConcurrentHashMap<>();
    private static final Map<Long, NinePatchDrawable> borderCache = new ConcurrentHashMap<>();

    private RoundedCache() {
    }

    /**
     * Retrieves or generates a white solid rounded NinePatchDrawable for the given radius.
     */
    public static NinePatchDrawable getSolid(int radius) {
        int r = Math.max(1, radius);
        return solidCache.computeIfAbsent(r, RoundedCache::createSolidDrawable);
    }

    /**
     * Retrieves or generates a white hollow rounded border NinePatchDrawable.
     */
    public static NinePatchDrawable getBorder(int radius, float stroke) {
        int r = Math.max(1, radius);
        float s = Math.max(0.5f, stroke);
        long key = ((long) r << 32) | (Float.floatToIntBits(s) & 0xFFFFFFFFL);
        return borderCache.computeIfAbsent(key, k -> createBorderDrawable(r, s));
    }

    private static NinePatchDrawable createSolidDrawable(int radius) {
        Pixmap pixmap = RoundedGenerator.generateSolidPixmap(radius);
        return createNinePatch(pixmap, radius);
    }

    private static NinePatchDrawable createBorderDrawable(int radius, float stroke) {
        Pixmap pixmap = RoundedGenerator.generateBorderPixmap(radius, stroke);
        return createNinePatch(pixmap, radius);
    }

    private static NinePatchDrawable createNinePatch(Pixmap pixmap, int radius) {
        try {
            if (Core.gl != null) {
                Texture texture = new Texture(pixmap);
                texture.setFilter(TextureFilter.linear);
                TextureRegion region = new TextureRegion(texture);
                NinePatch patch = new NinePatch(region, radius, radius, radius, radius);
                return new NinePatchDrawable(patch);
            }
        } finally {
            pixmap.dispose();
        }

        // Fallback for headless testing environments without OpenGL context
        return new NinePatchDrawable();
    }

    /**
     * Clears all cached drawables and releases textures.
     */
    public static void clear() {
        for (NinePatchDrawable drawable : solidCache.values()) {
            disposeDrawable(drawable);
        }
        solidCache.clear();

        for (NinePatchDrawable drawable : borderCache.values()) {
            disposeDrawable(drawable);
        }
        borderCache.clear();
    }

    private static void disposeDrawable(NinePatchDrawable drawable) {
        if (drawable != null && drawable.getPatch() != null) {
            Texture texture = drawable.getPatch().getTexture();
            if (texture != null) {
                texture.dispose();
            }
        }
    }
}
