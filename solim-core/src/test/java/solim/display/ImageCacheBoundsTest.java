package solim.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.graphics.Texture;
import arc.graphics.g2d.TextureRegion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class ImageCacheBoundsTest extends SolimEnv {

    @BeforeEach
    void resetCache() {
        SolimImage.clearCache();
    }

    @AfterEach
    void clearCache() {
        SolimImage.clearCache();
    }

    @Test
    void cacheStaysAtItsBoundAfterOverInsert() {
        for (int i = 0; i < ImageCache.MAX_CACHE_ENTRIES + 64; i++) {
            SolimImage.putCache(url(i), new TextureRegion());
        }

        assertEquals(ImageCache.MAX_CACHE_ENTRIES, ImageCache.cacheSize());
    }

    @Test
    void evictionDropsTheLeastRecentlyUsedEntry() {
        for (int i = 0; i < ImageCache.MAX_CACHE_ENTRIES; i++) {
            SolimImage.putCache(url(i), new TextureRegion());
        }
        SolimImage.putCache(url(0), new TextureRegion());

        for (int i = ImageCache.MAX_CACHE_ENTRIES; i < ImageCache.MAX_CACHE_ENTRIES + 4; i++) {
            SolimImage.putCache(url(i), new TextureRegion());
        }

        assertTrue(SolimImage.isCached(url(0)), "Recently used entries must survive eviction");
        assertFalse(SolimImage.isCached(url(1)), "Least recently used entries must be evicted");
    }

    @Test
    void evictionKeepsEvictedTextureAlive() {
        TrackingTexture texture = new TrackingTexture();
        SolimImage.putCache(url(0), regionOf(texture));

        for (int i = 1; i <= ImageCache.MAX_CACHE_ENTRIES; i++) {
            SolimImage.putCache(url(i), new TextureRegion());
        }

        assertFalse(SolimImage.isCached(url(0)));
        assertFalse(texture.disposed, "Eviction must drop the reference without disposing the texture");
    }

    private static TextureRegion regionOf(Texture texture) {
        TextureRegion region = new TextureRegion();
        region.texture = texture;
        return region;
    }

    private static String url(int index) {
        return "https://example.test/image-" + index;
    }

    private static final class TrackingTexture extends Texture {
        private boolean disposed = false;

        private TrackingTexture() {
            super();
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
