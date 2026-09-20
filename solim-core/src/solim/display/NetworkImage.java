package solim.display;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Http;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Scaling;
import arc.util.Threads;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import arc.scene.ui.Image;
import mindustry.Vars;
import mindustry.core.Version;
import solim.core.Disposable;
import solim.core.LeafComponent;
import solim.graphics.RoundedGenerator;
import solim.reactive.Effect;
import solim.reactive.Readable;

public final class NetworkImage extends LeafComponent<Image, NetworkImage> {

    @FunctionalInterface
    public interface ImageLoader {
        void load(String url, Cons<TextureRegion> onSuccess, Cons<Throwable> onError);

        default void load(String url, int radius, Cons<TextureRegion> onSuccess, Cons<Throwable> onError) {
            load(url, onSuccess, onError);
        }

        default void load(String url, int radius, float targetW, float targetH, Cons<TextureRegion> onSuccess,
                Cons<Throwable> onError) {
            load(url, radius, onSuccess, onError);
        }
    }

    private static final Map<String, TextureRegionDrawable> cache = new ConcurrentHashMap<>();
    private static final long CACHE_MAX_AGE = 30L * 24 * 60 * 60 * 1000;
    private static volatile boolean cleanupDone = false;

    /**
     * Shared application-lifetime worker. It is intentionally not shut down:
     * NetworkImage instances are short-lived, but the game process is the
     * lifetime owner of this bounded decode executor.
     */
    private static final ExecutorService DECODE_WORKER = Threads.unboundedExecutor("solim-img-decode", 2);

    private static ImageLoader loader = defaultLoader();

    private static ImageLoader defaultLoader() {
        return new ImageLoader() {
            @Override
            public void load(String url, Cons<TextureRegion> onSuccess, Cons<Throwable> onError) {
                load(url, 0, 0f, 0f, onSuccess, onError);
            }

            @Override
            public void load(String url, int radius, Cons<TextureRegion> onSuccess, Cons<Throwable> onError) {
                load(url, radius, 0f, 0f, onSuccess, onError);
            }

            @Override
            public void load(String url, int radius, float targetW, float targetH, Cons<TextureRegion> onSuccess,
                    Cons<Throwable> onError) {
                if (url == null || url.trim().isEmpty()) {
                    if (onError != null)
                        onError.get(new IllegalArgumentException("Empty URL"));
                    return;
                }
                try {
                    Http.get(url)
                            .timeout(30000)
                            .header("User-Agent", "MindustryTool/" + Version.buildString())
                            .error(onError != null ? onError : err -> Log.err("NetworkImage error", err))
                            .submit(response -> {
                                byte[] bytes = response.getResult();
                                if (bytes == null || bytes.length == 0) {
                                    if (onError != null)
                                        onError.get(new IllegalStateException("Empty response"));
                                    return;
                                }
                                writeToDisk(url, bytes);
                                Pixmap pixmap;
                                try {
                                    pixmap = decodePixmap(bytes, radius, targetW, targetH);
                                } catch (Throwable t) {
                                    if (onError != null)
                                        onError.get(t);
                                    return;
                                }
                                if (Core.app != null) {
                                    Core.app.post(() -> {
                                        try {
                                            onSuccess.get(toTexture(pixmap));
                                        } catch (Throwable t) {
                                            if (onError != null)
                                                onError.get(t);
                                        }
                                    });
                                } else {
                                    pixmap.dispose();
                                }
                            });
                } catch (Throwable t) {
                    if (onError != null)
                        onError.get(t);
                }
            }
        };
    }

    public static void setImageLoader(ImageLoader customLoader) {
        loader = customLoader != null ? customLoader : defaultLoader();
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
        if (url != null && region != null) {
            cache.put(cacheKey(url, radius, targetW), new TextureRegionDrawable(region));
        }
    }

    // --- Disk cache ---

    public static TextureRegion decodeTexture(byte[] bytes) {
        return decodeTexture(bytes, 0, 0f, 0f);
    }

    public static TextureRegion decodeTexture(byte[] bytes, int radius) {
        return decodeTexture(bytes, radius, 0f, 0f);
    }

    public static TextureRegion decodeTexture(byte[] bytes, int radius, float targetW, float targetH) {
        return toTexture(decodePixmap(bytes, radius, targetW, targetH));
    }

    /**
     * CPU-only PNG decode plus rounded masking. Safe to call on any thread.
     */
    public static Pixmap decodePixmap(byte[] bytes, int radius, float targetW, float targetH) {
        Pixmap pixmap = new Pixmap(bytes);
        if (radius > 0) {
            applyRoundedMask(pixmap, radius, targetW, targetH);
        }
        return pixmap;
    }

    /**
     * GL texture upload. Must be called on the main thread.
     */
    private static TextureRegion toTexture(Pixmap pixmap) {
        try {
            Texture texture = new Texture(pixmap);
            texture.setFilter(TextureFilter.linear);
            return new TextureRegion(texture);
        } finally {
            pixmap.dispose();
        }
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

    private static void applyAlpha(Pixmap pixmap, int x, int y, float alpha) {
        int pixel = pixmap.get(x, y);
        int currentAlpha = pixel & 0xFF;
        int newAlpha = Math.round(currentAlpha * alpha);
        pixmap.set(x, y, (pixel & 0xFFFFFF00) | (newAlpha & 0xFF));
    }

    private static @Nullable Fi cacheDir() {
        try {
            return Vars.dataDirectory.child("solim").child("cache").child("networkImage");
        } catch (Throwable t) {
            return null;
        }
    }

    private static String cacheName(String url) {
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

    private static void writeToDisk(String url, byte[] bytes) {
        try {
            Fi dir = cacheDir();
            if (dir == null || bytes == null || bytes.length == 0)
                return;
            dir.mkdirs();
            dir.child(cacheName(url)).writeBytes(bytes);
        } catch (Throwable t) {
            Log.debug("NetworkImage: disk write failed: " + t.getMessage());
        }
    }

    private static void scheduleCleanup() {
        if (cleanupDone)
            return;
        cleanupDone = true;
        try {
            new Thread(() -> {
                try {
                    Fi dir = cacheDir();
                    if (dir == null || !dir.exists())
                        return;
                    Fi[] files = dir.list();
                    if (files == null)
                        return;
                    long now = System.currentTimeMillis();
                    for (Fi f : files) {
                        if (f != null && !f.isDirectory() && now - f.lastModified() > CACHE_MAX_AGE) {
                            f.delete();
                        }
                    }
                } catch (Throwable ignored) {
                }
            }, "solim-img-cache-cleanup").start();
        } catch (Throwable ignored) {
        }
    }

    // --- Instance ---

    private @Nullable Drawable placeholder;
    private @Nullable Drawable fallback;
    private @Nullable Disposable binding;
    private @Nullable String currentUrl;
    private boolean failed = false;
    private Scaling scaling = Scaling.fit;
    private int cornerRadius = 0;
    private long loadGeneration = 0L;

    public NetworkImage() {
        super(new Image((Drawable) null));
        this.element.setScaling(Scaling.fit);
    }

    public NetworkImage(String url) {
        this();
        url(url);
    }

    public NetworkImage(Readable<String> url) {
        this();
        url(url);
    }

    public NetworkImage origin(int align) {
        this.element.setOrigin(align);
        return this;
    }

    public NetworkImage rounded(int radius) {
        int r = Math.max(0, radius);
        if (this.cornerRadius != r) {
            this.cornerRadius = r;
            if (currentUrl != null) {
                loadUrl(currentUrl);
            }
        }
        return this;
    }

    public NetworkImage rounded(int radius, @Nullable Color color) {
        rounded(radius);
        if (color != null)
            color(color);
        return this;
    }

    public NetworkImage rounded(int radius, @Nullable Readable<Color> color) {
        rounded(radius);
        if (color != null)
            color(color);
        return this;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public NetworkImage placeholder(@Nullable Drawable placeholder) {
        this.placeholder = placeholder;
        if (element.getDrawable() == null && placeholder != null) {
            applyDrawable(placeholder);
        }
        return this;
    }

    public NetworkImage fallback(@Nullable Drawable fallback) {
        this.fallback = fallback;
        if (failed && fallback != null) {
            applyDrawable(fallback);
        }
        return this;
    }

    public NetworkImage scaling(Scaling scaling) {
        this.scaling = scaling;
        element.setScaling(scaling);
        return this;
    }

    public Scaling getScaling() {
        return scaling;
    }

    public NetworkImage size(float width, float height) {
        width(width);
        height(height);
        if (cornerRadius > 0 && currentUrl != null) {
            loadUrl(currentUrl);
        }
        return this;
    }

    public NetworkImage size(float size) {
        return size(size, size);
    }

    public NetworkImage url(@Nullable String url) {
        if (binding != null) {
            binding.dispose();
            binding = null;
        }
        loadUrl(url);
        return this;
    }

    public NetworkImage url(@Nullable Readable<String> url) {
        if (binding != null) {
            binding.dispose();
            binding = null;
        }
        if (url != null) {
            binding = Effect.of(() -> loadUrl(url.get()));
        }
        return this;
    }

    private void loadUrl(@Nullable String url) {
        this.currentUrl = url;
        this.failed = false;
        final long gen = ++loadGeneration;
        if (url == null || url.trim().isEmpty()) {
            this.failed = true;
            applyDrawable(fallback != null ? fallback : placeholder);
            return;
        }

        final int radius = cornerRadius;
        final float targetW = (constraints.prefWidth != null && constraints.prefWidth.get() != null)
                ? constraints.prefWidth.get()
                : 0f;
        final float targetH = (constraints.prefHeight != null && constraints.prefHeight.get() != null)
                ? constraints.prefHeight.get()
                : 0f;

        String key = cacheKey(url, radius, targetW);
        TextureRegionDrawable cached = cache.get(key);
        if (cached != null) {
            applyDrawable(cached);
            return;
        }

        if (placeholder != null) {
            applyDrawable(placeholder);
        }

        scheduleCleanup();

        if (loadFromDisk(url, radius, targetW, targetH, gen))
            return;

        loader.load(url, radius, targetW, targetH, region -> {
            TextureRegionDrawable drawable = new TextureRegionDrawable(region);
            cache.put(cacheKey(url, radius, targetW), drawable);
            if (isCurrent(gen, url)) {
                this.failed = false;
                applyDrawable(drawable);
            }
        }, error -> {
            if (isCurrent(gen, url)) {
                this.failed = true;
                applyDrawable(fallback != null ? fallback : placeholder);
            }
        });
    }

    private boolean isCurrent(long gen, String url) {
        return gen == loadGeneration && url.equals(currentUrl);
    }

    private boolean loadFromDisk(String url, int radius, float targetW, float targetH, long gen) {
        final Fi file;
        try {
            Fi dir = cacheDir();
            if (dir == null)
                return false;
            file = dir.child(cacheName(url));
            if (!file.exists() || file.isDirectory())
                return false;
            long age = System.currentTimeMillis() - file.lastModified();
            if (age > CACHE_MAX_AGE) {
                file.delete();
                return false;
            }
        } catch (Throwable t) {
            return false;
        }
        try {
            DECODE_WORKER.execute(() -> {
                byte[] bytes;
                try {
                    bytes = file.readBytes();
                } catch (Throwable t) {
                    postNetworkRetry(url, radius, targetW, targetH, gen);
                    return;
                }
                if (bytes == null || bytes.length == 0) {
                    deleteQuietly(file);
                    postNetworkRetry(url, radius, targetW, targetH, gen);
                    return;
                }
                Pixmap pixmap;
                try {
                    pixmap = decodePixmap(bytes, radius, targetW, targetH);
                } catch (Throwable t) {
                    deleteQuietly(file);
                    postNetworkRetry(url, radius, targetW, targetH, gen);
                    return;
                }
                if (Core.app == null) {
                    pixmap.dispose();
                    return;
                }
                Core.app.post(() -> {
                    TextureRegionDrawable drawable;
                    try {
                        drawable = new TextureRegionDrawable(toTexture(pixmap));
                    } catch (Throwable t) {
                        deleteQuietly(file);
                        if (isCurrent(gen, url)) {
                            this.failed = true;
                            applyDrawable(fallback != null ? fallback : placeholder);
                        }
                        return;
                    }
                    cache.put(cacheKey(url, radius, targetW), drawable);
                    if (isCurrent(gen, url)) {
                        this.failed = false;
                        applyDrawable(drawable);
                    }
                });
            });
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void postNetworkRetry(String url, int radius, float targetW, float targetH, long gen) {
        if (Core.app == null) {
            return;
        }
        Core.app.post(() -> retryNetwork(url, radius, targetW, targetH, gen));
    }

    private void retryNetwork(String url, int radius, float targetW, float targetH, long gen) {
        if (!isCurrent(gen, url)) {
            return;
        }
        loader.load(url, radius, targetW, targetH, region -> {
            TextureRegionDrawable drawable = new TextureRegionDrawable(region);
            cache.put(cacheKey(url, radius, targetW), drawable);
            if (isCurrent(gen, url)) {
                this.failed = false;
                applyDrawable(drawable);
            }
        }, error -> {
            if (isCurrent(gen, url)) {
                this.failed = true;
                applyDrawable(fallback != null ? fallback : placeholder);
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

    private void applyDrawable(@Nullable Drawable drawable) {
        if (drawable != null) {
            element.setDrawable(drawable);
        }
        element.invalidateHierarchy();
    }

    public NetworkImage color(Color color) {
        element.setColor(color);
        return this;
    }

    public NetworkImage color(Readable<Color> color) {
        if (color != null) {
            Effect.of(() -> {
                Color c = color.get();
                if (c != null)
                    element.setColor(c);
            });
        }
        return this;
    }

    public Image image() {
        return element;
    }

    public NetworkImage top() {
        constraints.alignTop();
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null)
                cell.top();
        }
        return this;
    }

    public NetworkImage bottom() {
        constraints.alignBottom();
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null)
                cell.bottom();
        }
        return this;
    }

    public NetworkImage left() {
        constraints.alignLeft();
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null)
                cell.left();
        }
        return this;
    }

    public NetworkImage right() {
        constraints.alignRight();
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null)
                cell.right();
        }
        return this;
    }

    public NetworkImage center() {
        constraints.alignCenter();
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null)
                cell.center();
        }
        return this;
    }

    @Override
    public void dispose() {
        if (isDisposed()) {
            return;
        }
        if (binding != null) {
            binding.dispose();
            binding = null;
        }
        super.dispose();
    }
}
