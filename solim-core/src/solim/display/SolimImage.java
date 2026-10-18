package solim.display;

import arc.files.Fi;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Scaling;
import java.util.Objects;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.SolimToken;
import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.Configurable;
import solim.modifier.ImageCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;

public final class SolimImage extends Component
        implements Configurable<SolimImage>, ImageCapability, CellCapability, AlignmentCapability {

    private final Image element;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<SolimImage> pending;
    private @Nullable Disposable sourceBinding;
    private @Nullable ImageSource source;
    private @Nullable Drawable placeholder;
    private @Nullable Drawable fallback;
    private Scaling scaling = Scaling.fit;
    private int cornerRadius;
    private long loadGeneration;
    private boolean failed;

    public SolimImage() {
        this((Drawable) null, Scaling.fit);
    }

    public SolimImage(@Nullable Drawable drawable) {
        this(drawable, Scaling.fit);
    }

    public SolimImage(@Nullable Drawable drawable, Scaling scaling) {
        this(new Image(drawable, scaling), scaling);
    }

    public SolimImage(@Nullable String url) {
        this();
        src(url);
    }

    public SolimImage(@Nullable ImageSource source) {
        this();
        src(source);
    }

    public SolimImage(@Nullable Readable<ImageSource> source) {
        this();
        src(source);
    }

    private SolimImage(Image element, Scaling scaling) {
        super(element);
        this.element = element;
        this.scaling = scaling;
        SolimToken.bind(element, this, constraints);
    }

    public static SolimImage of(Drawable drawable) {
        return new SolimImage(drawable);
    }

    public static SolimImage of(Signal<Drawable> drawable) {
        SolimImage image = new SolimImage();
        image.drawable(drawable);
        return image;
    }

    public static void setImageLoader(@Nullable ImageLoader loader) {
        ImageCache.setLoader(loader);
    }

    public static void clearCache() {
        ImageCache.clearCache();
    }

    public static String cacheKey(@Nullable String url, int radius) {
        return ImageCache.cacheKey(url, radius);
    }

    public static String cacheKey(@Nullable String url, int radius, float targetW) {
        return ImageCache.cacheKey(url, radius, targetW);
    }

    public static boolean isCached(String url) {
        return ImageCache.isCached(url);
    }

    public static boolean isCached(String url, int radius) {
        return ImageCache.isCached(url, radius);
    }

    public static boolean isCached(String url, int radius, float targetW) {
        return ImageCache.isCached(url, radius, targetW);
    }

    public static @Nullable TextureRegionDrawable getCached(String url) {
        return ImageCache.getCached(url);
    }

    public static @Nullable TextureRegionDrawable getCached(String url, int radius) {
        return ImageCache.getCached(url, radius);
    }

    public static @Nullable TextureRegionDrawable getCached(String url, int radius, float targetW) {
        return ImageCache.getCached(url, radius, targetW);
    }

    public static void putCache(String url, TextureRegion region) {
        ImageCache.putCache(url, region);
    }

    public static void putCache(String url, int radius, TextureRegion region) {
        ImageCache.putCache(url, radius, region);
    }

    public static void putCache(String url, int radius, float targetW, TextureRegion region) {
        ImageCache.putCache(url, radius, targetW, region);
    }

    public SolimImage src(@Nullable ImageSource source) {
        disposeSourceBinding();
        applySource(source);
        return this;
    }

    public SolimImage src(@Nullable Readable<ImageSource> source) {
        disposeSourceBinding();
        if (source == null) {
            applySource(null);
            return this;
        }
        withinOwnership(() -> sourceBinding = Effect.of(() -> {
            ImageSource next = source.get();
            if (!Objects.equals(next, this.source)) {
                applySource(next);
            }
        }));
        return this;
    }

    public SolimImage src(@Nullable String url) {
        return src(ImageSource.of(url));
    }

    public SolimImage src(@Nullable Drawable drawable) {
        return src(ImageSource.of(drawable));
    }

    public SolimImage drawable(@Nullable Drawable drawable) {
        disposeSourceBinding();
        source = null;
        failed = false;
        loadGeneration++;
        applyDrawable(drawable);
        return this;
    }

    public SolimImage drawable(@Nullable Readable<Drawable> drawable) {
        if (drawable != null) {
            withinOwnership(() -> Effect.of(() -> drawable(drawable.get())));
        }
        return this;
    }

    public SolimImage icon(@Nullable Drawable drawable) {
        return drawable(drawable);
    }

    public SolimImage icon(@Nullable Readable<Drawable> drawable) {
        return drawable(drawable);
    }

    public SolimImage placeholder(@Nullable Drawable placeholder) {
        this.placeholder = placeholder;
        if (element.getDrawable() == null && placeholder != null) {
            applyDrawable(placeholder);
        }
        return this;
    }

    public SolimImage fallback(@Nullable Drawable fallback) {
        this.fallback = fallback;
        if (failed && fallback != null) {
            applyDrawable(fallback);
        }
        return this;
    }

    public SolimImage rounded(int radius) {
        setRounded(radius);
        return this;
    }

    public void setRounded(int radius) {
        int next = Math.max(0, radius);
        if (cornerRadius != next) {
            cornerRadius = next;
            if (source != null) {
                applySource(source);
            }
        }
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public void setPlaceholder(@Nullable Drawable placeholder) {
        placeholder(placeholder);
    }

    public void setFallback(@Nullable Drawable fallback) {
        fallback(fallback);
    }

    public void setScaling(Scaling scaling) {
        this.scaling = scaling;
        element.setScaling(scaling);
    }

    public Scaling getScaling() {
        return scaling;
    }

    public Image image() {
        return element;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public SolimImage name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public SolimImage self() {
        return this;
    }

    @Override
    public @Nullable Table table() {
        return null;
    }

    @Override
    public @Nullable Props<SolimImage> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<SolimImage> props) {
        this.pending = props;
    }

    @Override
    protected void onDispose() {
        loadGeneration++;
        disposeSourceBinding();
    }

    private void disposeSourceBinding() {
        if (sourceBinding != null) {
            sourceBinding.dispose();
            sourceBinding = null;
        }
    }

    private void applySource(@Nullable ImageSource source) {
        this.source = source;
        this.failed = false;
        final long gen = ++loadGeneration;

        if (source == null) {
            fail(gen);
            return;
        }

        Drawable immediate = source.drawable();
        if (immediate != null) {
            applyDrawable(immediate);
            return;
        }

        String url = source.url();
        Fi file = source.file();
        if (file == null && (url == null || url.trim().isEmpty())) {
            fail(gen);
            return;
        }

        final String key = file != null ? file.path() : url;
        final int radius = cornerRadius;
        final float targetW = prefWidth();
        final float targetH = prefHeight();

        TextureRegionDrawable cached = ImageCache.getCached(key, radius, targetW);
        if (cached != null) {
            applyDrawable(cached);
            return;
        }

        if (placeholder != null) {
            applyDrawable(placeholder);
        }

        if (file != null) {
            ImageCache.decodeFile(file, radius, targetW, targetH,
                    region -> applyResult(key, radius, targetW, gen, region),
                    error -> fail(gen));
            return;
        }

        ImageCache.scheduleCleanup();
        if (ImageCache.loadCached(url, radius, targetW, targetH,
                region -> applyResult(key, radius, targetW, gen, region),
                () -> {
                    if (isCurrent(gen, key)) {
                        httpLoad(url, radius, targetW, targetH, gen);
                    }
                })) {
            return;
        }
        httpLoad(url, radius, targetW, targetH, gen);
    }

    private void httpLoad(String url, int radius, float targetW, float targetH, long gen) {
        ImageCache.loader().load(url, radius, targetW, targetH,
                region -> applyResult(url, radius, targetW, gen, region),
                error -> fail(gen));
    }

    private void applyResult(String key, int radius, float targetW, long gen, TextureRegion region) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(region);
        ImageCache.putCache(key, radius, targetW, drawable);
        if (isCurrent(gen, key)) {
            failed = false;
            applyDrawable(drawable);
        }
    }

    private void fail(long gen) {
        if (gen != loadGeneration) {
            return;
        }
        failed = true;
        applyDrawable(fallback != null ? fallback : placeholder);
    }

    private boolean isCurrent(long gen, String key) {
        String current = currentKey();
        return gen == loadGeneration && current != null && current.equals(key);
    }

    private @Nullable String currentKey() {
        ImageSource current = source;
        if (current == null) {
            return null;
        }
        Fi file = current.file();
        return file != null ? file.path() : current.url();
    }

    private float prefWidth() {
        Readable<Float> width = constraints.prefWidth;
        if (width == null) {
            return 0f;
        }
        Float value = width.get();
        return value != null ? value : 0f;
    }

    private float prefHeight() {
        Readable<Float> height = constraints.prefHeight;
        if (height == null) {
            return 0f;
        }
        Float value = height.get();
        return value != null ? value : 0f;
    }

    private void applyDrawable(@Nullable Drawable drawable) {
        if (drawable != null) {
            element.setDrawable(drawable);
        }
        element.invalidateHierarchy();
    }
}
