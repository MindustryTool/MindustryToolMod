package solim.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import mindustry.Vars;
import solim.test.SolimEnv;

/**
 * Regression coverage for the asynchronous NetworkImage decode path: the most
 * recent load must win regardless of worker completion order, decode parameters
 * must stay consistent per load, and corrupt disk entries must retry the network.
 */
class NetworkImageDecodeDeterminismTest extends SolimEnv {

    private static final String URL = "https://example.com/card.png";

    private final CapturingLoader loader = new CapturingLoader();
    private Fi previousDataDirectory;

    static final class CapturingLoader implements ImageLoader {
        final List<Cons<TextureRegion>> successes = new ArrayList<>();
        final List<Integer> radii = new ArrayList<>();
        volatile CountDownLatch onLoad;

        @Override
        public void load(String url, int radius, float targetW, float targetH,
                Cons<TextureRegion> onSuccess, Cons<Throwable> onError) {
            record(radius, onSuccess, onError);
        }

        private void record(int radius, Cons<TextureRegion> onSuccess, Cons<Throwable> onError) {
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
        NetworkImage.clearCache();
        NetworkImage.setImageLoader(loader);
        previousDataDirectory = Vars.dataDirectory;
        Vars.dataDirectory = null;
    }

    @AfterEach
    void tearDown() {
        NetworkImage.setImageLoader(null);
        NetworkImage.clearCache();
        Vars.dataDirectory = previousDataDirectory;
    }

    @Test
    void staleGenerationDoesNotOverrideNewerResult() {
        TextureRegion oldRegion = new TextureRegion();
        TextureRegion newRegion = new TextureRegion();

        NetworkImage image = new NetworkImage();
        image.url(URL);
        image.rounded(8);

        assertEquals(2, loader.successes.size());

        loader.successes.get(1).get(newRegion);
        loader.successes.get(0).get(oldRegion);

        TextureRegionDrawable applied = (TextureRegionDrawable) image.image().getDrawable();
        assertSame(newRegion, applied.getRegion(), "older generation must not override newer");
        assertTrue(NetworkImage.isCached(URL, 0));
        assertTrue(NetworkImage.isCached(URL, 8));

        image.dispose();
    }

    @Test
    void cacheKeyUsesRadiusCapturedAtScheduleTime() {
        TextureRegion region = new TextureRegion();

        NetworkImage image = new NetworkImage();
        image.url(URL);
        image.rounded(8);

        assertEquals(2, loader.radii.size());
        assertEquals(0, loader.radii.get(0));
        assertEquals(8, loader.radii.get(1));

        loader.successes.get(0).get(region);

        assertTrue(NetworkImage.isCached(URL, 0), "captured radius must key the cache entry");
        assertFalse(NetworkImage.isCached(URL, 8), "must not key under the changed radius");

        image.dispose();
    }

    @Test
    void corruptDiskCacheRetriesNetwork() throws Exception {
        Fi base = Fi.get(System.getProperty("java.io.tmpdir") + "/solim-netimage-" + System.nanoTime());
        try {
            Vars.dataDirectory = base;
            Fi dir = base.child("solim").child("cache").child("networkImage");
            dir.mkdirs();
            Fi cached = dir.child(cacheName(URL));
            cached.writeBytes(new byte[] { 1, 2, 3 });

            loader.onLoad = new CountDownLatch(1);

            NetworkImage image = new NetworkImage(URL);

            assertTrue(loader.onLoad.await(5, TimeUnit.SECONDS), "corrupt cache must retry the network loader");
            assertEquals(1, loader.successes.size());
            assertFalse(cached.exists(), "corrupt cache file must be deleted");

            image.dispose();
        } finally {
            base.deleteDirectory();
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
}
