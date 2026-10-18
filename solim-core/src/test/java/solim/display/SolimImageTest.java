package solim.display;

import static solim.modifier.Values.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Scaling;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import mindustry.Vars;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class SolimImageTest extends SolimEnv {

    private static final String URL = "https://example.com/avatar.png";

    private final CapturingLoader loader = new CapturingLoader();
    private Fi previousDataDirectory;

    static final class CapturingLoader implements ImageLoader {
        final List<Cons<TextureRegion>> successes = new ArrayList<>();
        final List<Integer> radii = new ArrayList<>();
        volatile CountDownLatch onLoad;

        @Override
        public void load(String url, int radius, float targetW, float targetH,
                Cons<TextureRegion> onSuccess, Cons<Throwable> onError) {
            successes.add(onSuccess);
            radii.add(radius);
            CountDownLatch latch = onLoad;
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    @BeforeEach
    void setUp() {
        SolimImage.clearCache();
        SolimImage.setImageLoader(loader);
        previousDataDirectory = Vars.dataDirectory;
        Vars.dataDirectory = null;
    }

    @AfterEach
    void tearDown() {
        SolimImage.setImageLoader(null);
        SolimImage.clearCache();
        Vars.dataDirectory = previousDataDirectory;
    }

    @Test
    void placeholderShownWhileLoading() {
        TextureRegionDrawable placeholder = new TextureRegionDrawable(new TextureRegion());

        SolimImage.setImageLoader((url, radius, targetW, targetH, success, error) -> {
        });

        SolimImage image = new SolimImage(URL).props(placeholder(placeholder));

        assertSame(placeholder, image.image().getDrawable());
        image.dispose();
    }

    @Test
    void successLoadsImageAndCaches() {
        TextureRegion region = new TextureRegion();
        SolimImage.setImageLoader((url, radius, targetW, targetH, success, error) -> success.get(region));

        SolimImage image = new SolimImage(URL);
        assertNotNull(image.image().getDrawable());
        assertTrue(SolimImage.isCached(URL));

        SolimImage.setImageLoader((url, radius, targetW, targetH, success, error) -> fail("Cached entry must skip the loader"));

        SolimImage cached = new SolimImage(URL);
        assertSame(image.image().getDrawable(), cached.image().getDrawable());

        image.dispose();
        cached.dispose();
    }

    @Test
    void failureUsesFallback() {
        TextureRegionDrawable fallback = new TextureRegionDrawable(new TextureRegion());
        SolimImage.setImageLoader((url, radius, targetW, targetH, success, error) -> error.get(new RuntimeException("404")));

        SolimImage image = new SolimImage(URL).props(fallback(fallback));

        assertSame(fallback, image.image().getDrawable());
        image.dispose();
    }

    @Test
    void blankSourceUsesFallbackOrPlaceholder() {
        TextureRegionDrawable placeholder = new TextureRegionDrawable(new TextureRegion());
        TextureRegionDrawable fallback = new TextureRegionDrawable(new TextureRegion());

        SolimImage.setImageLoader((url, radius, targetW, targetH, success, error) -> fail("Blank source must not fetch"));

        SolimImage withFallback = new SolimImage((String) null)
                .props(placeholder(placeholder), fallback(fallback));
        assertSame(fallback, withFallback.image().getDrawable());

        SolimImage withPlaceholder = new SolimImage((String) null).props(placeholder(placeholder));
        assertSame(placeholder, withPlaceholder.image().getDrawable());

        withFallback.dispose();
        withPlaceholder.dispose();
    }

    @Test
    void reactiveSourceSwitchesBetweenUrls() {
        TextureRegion regionA = new TextureRegion();
        TextureRegion regionB = new TextureRegion();

        SolimImage.setImageLoader((url, radius, targetW, targetH, success, error) -> {
            if ("https://example.com/a.png".equals(url)) {
                success.get(regionA);
            } else if ("https://example.com/b.png".equals(url)) {
                success.get(regionB);
            }
        });

        Signal<ImageSource> source = Signal.of(ImageSource.of("https://example.com/a.png"));
        SolimImage image = new SolimImage().src(source);
        assertSame(regionA, ((TextureRegionDrawable) image.image().getDrawable()).getRegion());

        source.set(ImageSource.of("https://example.com/b.png"));
        SignalDispatcher.flush();
        assertSame(regionB, ((TextureRegionDrawable) image.image().getDrawable()).getRegion());

        image.dispose();
    }

    @Test
    void staleGenerationDoesNotOverrideNewerResult() {
        TextureRegion oldRegion = new TextureRegion();
        TextureRegion newRegion = new TextureRegion();

        SolimImage image = new SolimImage().props(src(URL));
        image.props(imageRounded(8));

        assertEquals(2, loader.successes.size());

        loader.successes.get(1).get(newRegion);
        loader.successes.get(0).get(oldRegion);

        TextureRegionDrawable applied = (TextureRegionDrawable) image.image().getDrawable();
        assertSame(newRegion, applied.getRegion(), "Older generation must not override newer");
        assertTrue(SolimImage.isCached(URL, 0));
        assertTrue(SolimImage.isCached(URL, 8));

        image.dispose();
    }

    @Test
    void cacheKeyUsesRadiusCapturedAtScheduleTime() {
        TextureRegion region = new TextureRegion();

        SolimImage image = new SolimImage().props(src(URL));
        image.props(imageRounded(8));

        assertEquals(2, loader.radii.size());
        assertEquals(0, loader.radii.get(0));
        assertEquals(8, loader.radii.get(1));

        loader.successes.get(0).get(region);

        assertTrue(SolimImage.isCached(URL, 0), "Captured radius must key the cache entry");
        assertFalse(SolimImage.isCached(URL, 8), "Must not key under the changed radius");

        image.dispose();
    }

    @Test
    void corruptDiskCacheRetriesNetwork() throws Exception {
        Fi base = Fi.get(System.getProperty("java.io.tmpdir") + "/solim-image-" + System.nanoTime());
        try {
            Vars.dataDirectory = base;
            Fi dir = base.child("solim").child("cache").child("networkImage");
            dir.mkdirs();
            Fi cached = dir.child(ImageCache.cacheName(URL));
            cached.writeBytes(new byte[] { 1, 2, 3 });

            loader.onLoad = new CountDownLatch(1);

            SolimImage image = new SolimImage(URL);

            assertTrue(loader.onLoad.await(5, TimeUnit.SECONDS), "Corrupt cache must retry the network loader");
            assertEquals(1, loader.successes.size());
            assertFalse(cached.exists(), "Corrupt cache file must be deleted");

            image.dispose();
        } finally {
            base.deleteDirectory();
        }
    }

    @Test
    void sizingAndScalingModifiers() {
        SolimImage image = new SolimImage()
                .props(size(64f, 48f))
                .props(scaling(Scaling.fill));

        assertEquals(64f, image.cellConfig().prefWidth.get());
        assertEquals(48f, image.cellConfig().prefHeight.get());
        assertEquals(Scaling.fill, image.getScaling());
        image.dispose();
    }
}
