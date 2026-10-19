package mindustrytool.features.background;

import static mindustrytool.features.background.BackgroundLifecycle.Action.NONE;
import static mindustrytool.features.background.BackgroundLifecycle.Action.RELOAD;
import static mindustrytool.features.background.BackgroundLifecycle.Action.RELEASE_VIDEO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import solim.test.ArcTestEnv;

class BackgroundMediaTest extends ArcTestEnv {

    @Test
    void detectsMediaFormatFromPath() {
        assertEquals(BackgroundMediaType.NONE, BackgroundMediaType.of(null));
        assertEquals(BackgroundMediaType.NONE, BackgroundMediaType.of(""));
        assertEquals(BackgroundMediaType.NONE, BackgroundMediaType.of("   "));

        assertEquals(BackgroundMediaType.VIDEO, BackgroundMediaType.of("clip.mp4"));
        assertEquals(BackgroundMediaType.VIDEO, BackgroundMediaType.of("CLIP.MP4"));
        assertEquals(BackgroundMediaType.IMAGE, BackgroundMediaType.of("wall.png"));
        assertEquals(BackgroundMediaType.IMAGE, BackgroundMediaType.of("wall.jpeg"));
        assertEquals(BackgroundMediaType.IMAGE, BackgroundMediaType.of("mp4"));

        assertTrue(BackgroundMediaType.VIDEO.isVideo());
        assertFalse(BackgroundMediaType.IMAGE.isVideo());
    }

    @Test
    void cropFillsViewportWithoutDistortion() {
        float width = BackgroundScaling.drawWidth(1920f, 1080f, 1280f, 720f, true);
        float height = BackgroundScaling.drawHeight(1920f, 1080f, 1280f, 720f, true);

        assertEquals(1280f, width, 0.001f);
        assertEquals(720f, height, 0.001f);
        assertEquals(1920f / 1080f, width / height, 0.001f);
    }

    @Test
    void cropOversizesOnUltrawideViewportSoNoGapRemains() {
        float width = BackgroundScaling.drawWidth(1920f, 1080f, 2560f, 1080f, true);
        float height = BackgroundScaling.drawHeight(1920f, 1080f, 2560f, 1080f, true);

        assertEquals(2560f, width, 0.001f);
        assertTrue(height >= 1080f, "cropped background must cover the viewport height");
    }

    @Test
    void stretchMatchesScreenSizeExactly() {
        assertEquals(2560f, BackgroundScaling.drawWidth(1920f, 1080f, 2560f, 1080f, false));
        assertEquals(1080f, BackgroundScaling.drawHeight(1920f, 1080f, 2560f, 1080f, false));
    }

    @Test
    void cropStaysFiniteForDegenerateTextureSize() {
        float width = BackgroundScaling.drawWidth(0f, 0f, 100f, 100f, true);
        float height = BackgroundScaling.drawHeight(0f, 0f, 100f, 100f, true);

        assertFalse(Float.isNaN(width) || Float.isInfinite(width));
        assertFalse(Float.isNaN(height) || Float.isInfinite(height));
    }

    @Test
    void releasesVideoOutsideMenuAndReloadsOnReturn() {
        assertEquals(NONE, BackgroundLifecycle.next(true, true, true, true));
        assertEquals(RELEASE_VIDEO, BackgroundLifecycle.next(true, false, true, true));
        assertEquals(NONE, BackgroundLifecycle.next(true, false, false, false));
        assertEquals(RELOAD, BackgroundLifecycle.next(true, true, false, false));
        assertEquals(NONE, BackgroundLifecycle.next(true, true, true, true));
    }

    @Test
    void doesNothingWhileDisabledOrWithoutAVideoDecoder() {
        assertEquals(NONE, BackgroundLifecycle.next(false, true, false, false));
        assertEquals(NONE, BackgroundLifecycle.next(false, false, true, true));
        assertEquals(NONE, BackgroundLifecycle.next(true, false, true, false));
    }
}
