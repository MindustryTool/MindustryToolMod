package solim.display;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.func.Cons;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Scaling;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import solim.runtime.SignalDispatcher;

class NetworkImageComponentTest {

    @BeforeAll
    static void initArc() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
    }

    @BeforeEach
    void setup() {
        NetworkImage.clearCache();
    }

    @Test
    void networkImageElementIsBackedImage() {
        NetworkImage.setImageLoader((url, success, error) -> {
        });
        NetworkImage img = new NetworkImage("https://example.com/test.png");
        assertSame(img.image(), img.element());
        img.dispose();
    }

    @Test
    void networkImagePlaceholder() {
        TextureRegion region = new TextureRegion();
        TextureRegionDrawable placeholder = new TextureRegionDrawable(region);
        NetworkImage.setImageLoader((url, success, error) -> {
        });
        NetworkImage img = new NetworkImage("https://example.com/test.png")
                .placeholder(placeholder);
        assertSame(placeholder, img.image().getDrawable());
        img.dispose();
    }

    @Test
    void networkImageSuccessLoadsAndCaches() {
        TextureRegion mockRegion = new TextureRegion();
        NetworkImage.setImageLoader((url, success, error) -> {
            success.get(mockRegion);
        });
        NetworkImage img = new NetworkImage("https://example.com/avatar.png");
        assertTrue(NetworkImage.isCached("https://example.com/avatar.png"));
        img.dispose();
    }

    @Test
    void networkImageFailureUsesFallback() {
        TextureRegion mockFallbackRegion = new TextureRegion();
        TextureRegionDrawable fallback = new TextureRegionDrawable(mockFallbackRegion);
        NetworkImage.setImageLoader((url, success, error) -> {
            error.get(new RuntimeException("404 Not Found"));
        });
        NetworkImage img = new NetworkImage("https://example.com/missing.png")
                .fallback(fallback);
        assertSame(fallback, img.image().getDrawable());
        img.dispose();
    }

    @Test
    void networkImageReactiveUrl() {
        TextureRegion regionA = new TextureRegion();
        TextureRegion regionB = new TextureRegion();
        NetworkImage.setImageLoader((url, success, error) -> {
            if ("https://example.com/a.png".equals(url)) {
                success.get(regionA);
            } else if ("https://example.com/b.png".equals(url)) {
                success.get(regionB);
            }
        });
        Signal<String> urlSignal = Signal.of("https://example.com/a.png");
        NetworkImage img = new NetworkImage(urlSignal);
        assertTrue(NetworkImage.isCached("https://example.com/a.png"));
        urlSignal.set("https://example.com/b.png");
        SignalDispatcher.flush();
        assertTrue(NetworkImage.isCached("https://example.com/b.png"));
        img.dispose();
    }

    @Test
    void networkImageSizingAndScaling() {
        NetworkImage img = new NetworkImage()
                .size(64f, 48f)
                .scaling(Scaling.fill);
        assertEquals(64f, img.cellConfig().prefWidth.get());
        assertEquals(48f, img.cellConfig().prefHeight.get());
        assertEquals(Scaling.fill, img.getScaling());
        img.dispose();
    }

    @Test
    void networkImageDisposeStopsReactiveUrlUpdates() {
        TextureRegion regionA = new TextureRegion();
        NetworkImage.setImageLoader((url, success, error) -> success.get(regionA));
        Signal<String> urlSignal = Signal.of("https://example.com/a.png");
        NetworkImage img = new NetworkImage(urlSignal);
        assertTrue(NetworkImage.isCached("https://example.com/a.png"));

        img.dispose();
        urlSignal.set("https://example.com/b.png");

        // After disposal the new URL must not trigger a load.
        assertFalse(NetworkImage.isCached("https://example.com/b.png"));
    }

    @Test
    void networkImageRoundedRadiusTracking() {
        NetworkImage img = new NetworkImage().rounded(12);
        assertEquals(12, img.getCornerRadius());
        img.dispose();
    }

    @Test
    void networkImageRoundedCacheSeparation() {
        TextureRegion regionSquare = new TextureRegion();
        TextureRegion regionRounded = new TextureRegion();

        NetworkImage.setImageLoader(new NetworkImage.ImageLoader() {
            @Override
            public void load(String url, Cons<TextureRegion> onSuccess, Cons<Throwable> onError) {
                load(url, 0, onSuccess, onError);
            }

            @Override
            public void load(String url, int radius, Cons<TextureRegion> onSuccess, Cons<Throwable> onError) {
                if (radius > 0) {
                    onSuccess.get(regionRounded);
                } else {
                    onSuccess.get(regionSquare);
                }
            }
        });

        String url = "https://example.com/user.png";
        NetworkImage img1 = new NetworkImage(url);
        NetworkImage img2 = new NetworkImage(url).rounded(8);

        assertTrue(NetworkImage.isCached(url, 0));
        assertTrue(NetworkImage.isCached(url, 8));
        assertSame(regionSquare, NetworkImage.getCached(url, 0).getRegion());
        assertSame(regionRounded, NetworkImage.getCached(url, 8).getRegion());

        img1.dispose();
        img2.dispose();
    }

    @Test
    void networkImageApplyRoundedMask() {
        int width = 30;
        int height = 30;
        int radius = 8;
        Pixmap pixmap = new Pixmap(width, height);
        int opaqueWhite = Color.rgba8888(1f, 1f, 1f, 1f);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixmap.set(x, y, opaqueWhite);
            }
        }

        NetworkImage.applyRoundedMask(pixmap, radius);

        // Outer corner tips must be fully transparent (alpha == 0)
        assertEquals(0, pixmap.get(0, 0) & 0xFF, "Top-left tip must be transparent");
        assertEquals(0, pixmap.get(width - 1, 0) & 0xFF, "Top-right tip must be transparent");
        assertEquals(0, pixmap.get(0, height - 1) & 0xFF, "Bottom-left tip must be transparent");
        assertEquals(0, pixmap.get(width - 1, height - 1) & 0xFF, "Bottom-right tip must be transparent");

        // Center must remain fully opaque (alpha == 255)
        assertEquals(255, pixmap.get(width / 2, height / 2) & 0xFF, "Center must remain fully opaque");

        // Flat edge centers must remain fully opaque
        assertEquals(255, pixmap.get(width / 2, 0) & 0xFF, "Top middle edge must remain opaque");
        assertEquals(255, pixmap.get(0, height / 2) & 0xFF, "Left middle edge must remain opaque");

        // Near the boundary inside corner (e.g. x = radius - 1, y = radius - 1), alpha
        // should be > 0
        int cornerInteriorAlpha = pixmap.get(radius - 1, radius - 1) & 0xFF;
        assertTrue(cornerInteriorAlpha > 0, "Inside corner should preserve opacity");

        pixmap.dispose();
    }

    @Test
    void networkImageApplyRoundedMaskWithTargetScaling() {
        // Pixmap is 60x60, but display size is 30x30 (2x scale factor)
        // Radius is 8 in display units -> effective radius should be 16 in pixmap units
        int width = 60;
        int height = 60;
        int displayRadius = 8;
        Pixmap pixmap = new Pixmap(width, height);
        int opaqueWhite = Color.rgba8888(1f, 1f, 1f, 1f);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixmap.set(x, y, opaqueWhite);
            }
        }

        NetworkImage.applyRoundedMask(pixmap, displayRadius, 30f, 30f);

        // Corner tip at (0, 0) must be transparent
        assertEquals(0, pixmap.get(0, 0) & 0xFF);

        // At (2, 2), which is outside the corner curve for R=16, alpha must be < 255
        int scaledCornerAlpha = pixmap.get(2, 2) & 0xFF;
        assertTrue(scaledCornerAlpha < 255, "Should be within the scaled corner curvature");

        // Center (30, 30) must remain fully opaque
        assertEquals(255, pixmap.get(30, 30) & 0xFF);

        pixmap.dispose();
    }
}
