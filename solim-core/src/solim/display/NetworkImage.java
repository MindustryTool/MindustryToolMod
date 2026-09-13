package solim.display;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Http;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Scaling;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import arc.scene.ui.Image;
import mindustry.Vars;
import mindustry.core.Version;
import solim.core.Component;
import solim.core.Disposable;
import solim.runtime.ComponentContext;
import solim.layout.LayoutModifiers;
import solim.layout.SizeConstraints;
import solim.modifier.ElementModifiers;
import solim.signal.Effect;
import solim.signal.Readable;

public final class NetworkImage implements Component, LayoutModifiers<NetworkImage> {

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
                                if (Core.app != null) {
                                    Core.app.post(() -> {
                                        try {
                                            onSuccess.get(decodeTexture(bytes, radius, targetW, targetH));
                                        } catch (Throwable t) {
                                            if (onError != null)
                                                onError.get(t);
                                        }
                                    });
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
        Pixmap pixmap = new Pixmap(bytes);
        if (radius > 0) {
            applyRoundedMask(pixmap, radius, targetW, targetH);
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(TextureFilter.linear);
        TextureRegion region = new TextureRegion(texture);
        pixmap.dispose();
        return region;
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
                float alpha = solim.graphics.RoundedGenerator.computeAlpha(dx, dy, r);
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

    private final Image image;
    private final SizeConstraints constraints = new SizeConstraints();
    private @Nullable Drawable placeholder;
    private @Nullable Drawable fallback;
    private @Nullable Disposable binding;
    private @Nullable String currentUrl;
    private boolean failed = false;
    private Scaling scaling = Scaling.fit;
    private int cornerRadius = 0;
    private float padTop, padLeft, padBottom, padRight;
    private float marginTop, marginLeft, marginBottom, marginRight;

    public NetworkImage() {
        this.image = new Image((Drawable) null);
        this.image.setScaling(Scaling.fit);
        ComponentContext.register(this);
    }

    public NetworkImage(String url) {
        this();
        url(url);
    }

    public NetworkImage(Readable<String> url) {
        this();
        url(url);
    }

    @Override
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

    @Override
    public NetworkImage rounded(int radius, @Nullable Color color) {
        rounded(radius);
        if (color != null)
            color(color);
        return this;
    }

    @Override
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
        if (image.getDrawable() == null && placeholder != null) {
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
        image.setScaling(scaling);
        return this;
    }

    public Scaling getScaling() {
        return scaling;
    }

    @Override
    public NetworkImage size(float width, float height) {
        LayoutModifiers.super.size(width, height);
        if (cornerRadius > 0 && currentUrl != null) {
            loadUrl(currentUrl);
        }
        return this;
    }

    @Override
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
        if (url == null || url.trim().isEmpty()) {
            this.failed = true;
            applyDrawable(fallback != null ? fallback : placeholder);
            return;
        }

        float targetW = (constraints.prefWidth != null && constraints.prefWidth.get() != null)
                ? constraints.prefWidth.get()
                : 0f;
        float targetH = (constraints.prefHeight != null && constraints.prefHeight.get() != null)
                ? constraints.prefHeight.get()
                : 0f;

        String key = cacheKey(url, cornerRadius, targetW);
        TextureRegionDrawable cached = cache.get(key);
        if (cached != null) {
            applyDrawable(cached);
            return;
        }

        if (placeholder != null) {
            applyDrawable(placeholder);
        }

        scheduleCleanup();

        if (loadFromDisk(url, targetW, targetH))
            return;

        loader.load(url, cornerRadius, targetW, targetH, region -> {
            TextureRegionDrawable drawable = new TextureRegionDrawable(region);
            cache.put(key, drawable);
            if (url.equals(this.currentUrl)) {
                this.failed = false;
                applyDrawable(drawable);
            }
        }, error -> {
            if (url.equals(this.currentUrl)) {
                this.failed = true;
                applyDrawable(fallback != null ? fallback : placeholder);
            }
        });
    }

    private boolean loadFromDisk(String url, float targetW, float targetH) {
        try {
            Fi dir = cacheDir();
            if (dir == null)
                return false;
            Fi file = dir.child(cacheName(url));
            if (!file.exists() || file.isDirectory())
                return false;
            long age = System.currentTimeMillis() - file.lastModified();
            if (age > CACHE_MAX_AGE) {
                file.delete();
                return false;
            }
            byte[] bytes = file.readBytes();
            if (bytes == null || bytes.length == 0) {
                file.delete();
                return false;
            }
            Core.app.post(() -> {
                try {
                    TextureRegionDrawable drawable = new TextureRegionDrawable(
                            decodeTexture(bytes, cornerRadius, targetW, targetH));
                    cache.put(cacheKey(url, cornerRadius, targetW), drawable);
                    if (url.equals(this.currentUrl)) {
                        this.failed = false;
                        applyDrawable(drawable);
                    }
                } catch (Throwable t) {
                    try {
                        file.delete();
                    } catch (Throwable ignored) {
                    }
                    if (url.equals(this.currentUrl)) {
                        this.failed = true;
                        applyDrawable(fallback != null ? fallback : placeholder);
                    }
                }
            });
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void applyDrawable(@Nullable Drawable drawable) {
        if (drawable != null) {
            image.setDrawable(drawable);
        }
        image.invalidateHierarchy();
    }

    public NetworkImage color(Color color) {
        image.setColor(color);
        return this;
    }

    public NetworkImage color(Readable<Color> color) {
        if (color != null) {
            Effect e = Effect.of(() -> {
                Color c = color.get();
                if (c != null)
                    image.setColor(c);
            });
            ComponentContext.register(e);
        }
        return this;
    }

    @Override
    public SizeConstraints sizeConstraints() {
        return constraints;
    }

    @Override
    public NetworkImage name(String name) {
        ElementModifiers.name(image, name);
        return this;
    }

    public Image image() {
        applySpacing();
        return image;
    }

    @Override
    public Element element() {
        applySpacing();
        return image;
    }

    public NetworkImage padding(float p) {
        this.padTop = this.padLeft = this.padBottom = this.padRight = p;
        applySpacing();
        return this;
    }

    public NetworkImage margin(float m) {
        this.marginTop = this.marginLeft = this.marginBottom = this.marginRight = m;
        applySpacing();
        return this;
    }

    public void applySpacing() {
        if (image.parent instanceof Table) {
            Cell<?> cell = ((Table) image.parent).getCell(image);
            if (cell != null) {
                cell.pad(padTop + marginTop, padLeft + marginLeft, padBottom + marginBottom, padRight + marginRight);
            }
        }
    }

    @Override
    public void dispose() {
        if (binding != null) {
            binding.dispose();
            binding = null;
        }
    }
}
