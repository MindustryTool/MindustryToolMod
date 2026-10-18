package solim.display;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Threads;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import mindustry.Vars;
import solim.graphics.RoundedGenerator;

public final class ImageCache {

    private static final Map<String, TextureRegionDrawable> cache = new ConcurrentHashMap<>();

    private static final long CACHE_MAX_AGE = 30L * 24 * 60 * 60 * 1000;

    private static final ExecutorService DECODE_WORKER = Threads.unboundedExecutor("solim-img-decode", 2);

    private static volatile boolean cleanupDone = false;

    private static ImageLoader loader = new DefaultImageLoader();

    private ImageCache() {
    }

    public static void setLoader(@Nullable ImageLoader customLoader) {
        loader = customLoader != null ? customLoader : new DefaultImageLoader();
    }

    public static ImageLoader loader() {
        return loader;
    }

    public static void clearCache() {
        cache.clear();
    }

    public static String cacheKey(@Nullable String url, int radius) {
        return cacheKey(url, radius, 0f);
    }

    public static String cacheKey(@Nullable String url, int radius, float targetW) {
        if (url == null)
            return "";
        if (radius <= 0)
            return url;
        return targetW > 0f ? url + "@r=" + radius + "x" + (int) targetW : url + "@r=" + radius;
    }

    public static boolean isCached(String url) {
        return isCached(url, 0);
    }

    public static boolean isCached(String url, int radius) {
        return isCached(url, radius, 0f);
    }

    public static boolean isCached(String url, int radius, float targetW) {
        return url != null && cache.containsKey(cacheKey(url, radius, targetW));
    }

    public static @Nullable TextureRegionDrawable getCached(String url) {
        return getCached(url, 0);
    }

    public static @Nullable TextureRegionDrawable getCached(String url, int radius) {
        return getCached(url, radius, 0f);
    }

    public static @Nullable TextureRegionDrawable getCached(String url, int radius, float targetW) {
        return url != null ? cache.get(cacheKey(url, radius, targetW)) : null;
    }

    public static void putCache(String url, TextureRegion region) {
        putCache(url, 0, region);
    }

    public static void putCache(String url, int radius, TextureRegion region) {
        putCache(url, radius, 0f, region);
    }

    public static void putCache(String url, int radius, float targetW, TextureRegion region) {
        if (region != null) {
            putCache(url, radius, targetW, new TextureRegionDrawable(region));
        }
    }

    public static void putCache(String url, int radius, float targetW, TextureRegionDrawable drawable) {
        if (url != null && drawable != null) {
            cache.put(cacheKey(url, radius, targetW), drawable);
        }
    }

    public static TextureRegion toTexture(Pixmap pixmap) {
        try {
            Texture texture = new Texture(pixmap);
            texture.setFilter(TextureFilter.linear);
            return new TextureRegion(texture);
        } finally {
            pixmap.dispose();
        }
    }

    public static TextureRegion decodeTexture(byte[] bytes) {
        return decodeTexture(bytes, 0, 0f, 0f);
    }

    public static TextureRegion decodeTexture(byte[] bytes, int radius) {
        return decodeTexture(bytes, radius, 0f, 0f);
    }

    public static TextureRegion decodeTexture(byte[] bytes, int radius, float targetW, float targetH) {
        return toTexture(decodePixmap(bytes, radius, targetW, targetH));
    }

    public static Pixmap decodePixmap(byte[] bytes, int radius, float targetW, float targetH) {
        Pixmap pixmap = new Pixmap(bytes);
        if (radius > 0) {
            applyRoundedMask(pixmap, radius, targetW, targetH);
        }
        return pixmap;
    }

    public static void applyRoundedMask(Pixmap pixmap, int radius) {
        applyRoundedMask(pixmap, radius, 0f, 0f);
    }

    public static void applyRoundedMask(Pixmap pixmap, int radius, float targetW, float targetH) {
        if (pixmap == null || radius <= 0)
            return;
        int w = pixmap.width;
        int h = pixmap.height;

        int effectiveRadius = radius;
        if (targetW > 0f && w > 0) {
            float scale = (float) w / targetW;
            effectiveRadius = Math.round(radius * scale);
        }
        int r = Math.min(effectiveRadius, Math.min(w, h) / 2);
        if (r <= 0)
            return;

        for (int y = 0; y < r; y++) {
            float dy = r - 0.5f - y;
            for (int x = 0; x < r; x++) {
                float dx = r - 0.5f - x;
                float alpha = RoundedGenerator.computeAlpha(dx, dy, r);
                if (alpha >= 1f)
                    continue;

                applyAlpha(pixmap, x, y, alpha);
                applyAlpha(pixmap, w - 1 - x, y, alpha);
                applyAlpha(pixmap, x, h - 1 - y, alpha);
                applyAlpha(pixmap, w - 1 - x, h - 1 - y, alpha);
            }
        }
    }

    public static boolean loadCached(String url, int radius, float targetW, float targetH,
            Cons<TextureRegion> onSuccess, @Nullable Runnable onCorrupt) {
        final Fi file = freshCached(url);
        if (file == null)
            return false;
        decodeFile(file, radius, targetW, targetH, onSuccess, error -> {
            deleteQuietly(file);
            if (onCorrupt != null) {
                onCorrupt.run();
            }
        });
        return true;
    }

    public static void decodeFile(Fi file, int radius, float targetW, float targetH,
            Cons<TextureRegion> onSuccess, Cons<Throwable> onError) {
        try {
            DECODE_WORKER.execute(() -> {
                byte[] bytes;
                try {
                    bytes = file.readBytes();
                } catch (Throwable t) {
                    postError(onError, t);
                    return;
                }
                if (bytes == null || bytes.length == 0) {
                    postError(onError, new IllegalStateException("Empty image file: " + file.name()));
                    return;
                }
                Pixmap pixmap;
                try {
                    pixmap = decodePixmap(bytes, radius, targetW, targetH);
                } catch (Throwable t) {
                    postError(onError, t);
                    return;
                }
                postSuccess(pixmap, onSuccess);
            });
        } catch (Throwable t) {
            postError(onError, t);
        }
    }

    static void writeToDisk(String url, byte[] bytes) {
        try {
            Fi dir = cacheDir();
            if (dir == null || bytes == null || bytes.length == 0)
                return;
            dir.mkdirs();
            dir.child(cacheName(url)).writeBytes(bytes);
        } catch (Throwable t) {
            Log.debug("ImageCache: disk write failed: " + t.getMessage());
        }
    }

    public static void scheduleCleanup() {
        if (cleanupDone)
            return;
        cleanupDone = true;
        try {
            Thread thread = new Thread(() -> {
                try {
                    Fi dir = cacheDir();
                    if (dir == null || !dir.exists())
                        return;
                    Fi[] files = dir.list();
                    if (files == null)
                        return;
                    long now = System.currentTimeMillis();
                    for (Fi file : files) {
                        if (file != null && !file.isDirectory() && now - file.lastModified() > CACHE_MAX_AGE) {
                            file.delete();
                        }
                    }
                } catch (Throwable ignored) {
                }
            }, "solim-img-cache-cleanup");
            thread.setDaemon(true);
            thread.start();
        } catch (Throwable ignored) {
        }
    }

    public static String cacheName(String url) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Throwable t) {
            return Integer.toHexString(url.hashCode());
        }
    }

    private static @Nullable Fi freshCached(String url) {
        try {
            Fi dir = cacheDir();
            if (dir == null)
                return null;
            Fi file = dir.child(cacheName(url));
            if (!file.exists() || file.isDirectory())
                return null;
            long age = System.currentTimeMillis() - file.lastModified();
            if (age > CACHE_MAX_AGE) {
                file.delete();
                return null;
            }
            return file;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void postSuccess(Pixmap pixmap, Cons<TextureRegion> onSuccess) {
        if (Core.app == null) {
            pixmap.dispose();
            return;
        }
        Core.app.post(() -> onSuccess.get(toTexture(pixmap)));
    }

    private static void postError(@Nullable Cons<Throwable> onError, Throwable error) {
        if (Core.app == null) {
            return;
        }
        Core.app.post(() -> {
            if (onError != null) {
                onError.get(error);
            }
        });
    }

    private static void deleteQuietly(@Nullable Fi file) {
        try {
            if (file != null) {
                file.delete();
            }
        } catch (Throwable ignored) {
        }
    }

    static @Nullable Fi cacheDir() {
        try {
            return Vars.dataDirectory.child("solim").child("cache").child("networkImage");
        } catch (Throwable t) {
            return null;
        }
    }

    private static void applyAlpha(Pixmap pixmap, int x, int y, float alpha) {
        int pixel = pixmap.get(x, y);
        int currentAlpha = pixel & 0xFF;
        int newAlpha = Math.round(currentAlpha * alpha);
        pixmap.set(x, y, (pixel & 0xFFFFFF00) | (newAlpha & 0xFF));
    }
}
