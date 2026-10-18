package solim.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import arc.Core;
import arc.files.Fi;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Slider.SliderStyle;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.display.video.SolimVideo;
import solim.display.video.VideoSource;
import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class MediaSourceTest extends SolimEnv {

    private final List<SolimVideo> videos = new ArrayList<>();

    @BeforeEach
    void stubVideoScene() {
        newScene();
        try {
            Core.scene.getStyle(SliderStyle.class);
        } catch (IllegalArgumentException missing) {
            Core.scene.addStyle(SliderStyle.class, new SliderStyle());
        }
        TextureRegionDrawable stub = new TextureRegionDrawable(new TextureRegion());
        SolimVideo.setDefaultIcons(stub, stub, stub, stub, stub);
    }

    @AfterEach
    void restoreVideo() {
        for (SolimVideo video : videos) {
            video.dispose();
        }
        videos.clear();
        SolimVideo.setVideoLoader(null);
        SolimVideo.setDefaultIcons(null, null, null, null, null);
        SolimImage.setImageLoader(null);
        SolimImage.clearCache();
    }

    @Test
    void drawableImageSourceResolvesSynchronously() {
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion());
        ImageSource source = ImageSource.of(drawable);

        assertSame(drawable, source.drawable());
        assertNull(source.url());
        assertNull(source.file());
        assertFalse(source.isAsync());
    }

    @Test
    void textureRegionImageSourceWrapsRegion() {
        ImageSource source = ImageSource.of(new TextureRegion());

        assertNotNull(source.drawable());
        assertTrue(source.drawable() instanceof TextureRegionDrawable);
        assertFalse(source.isAsync());
    }

    @Test
    void urlImageSourceRequiresAsyncLoading() {
        ImageSource source = ImageSource.of("https://example.com/a.png");

        assertNull(source.drawable());
        assertEquals("https://example.com/a.png", source.url());
        assertNull(source.file());
        assertTrue(source.isAsync());
    }

    @Test
    void fileImageSourceExposesHandle() {
        Fi file = Fi.get("some/local.png");
        ImageSource source = ImageSource.of(file);

        assertSame(file, source.file());
        assertNull(source.url());
        assertTrue(source.isAsync());
    }

    @Test
    void imageSourceEqualityIsValueBased() {
        assertEquals(ImageSource.of("a.png"), ImageSource.of("a.png"));
        assertNotEquals(ImageSource.of("a.png"), ImageSource.of("b.png"));
        assertNotEquals(ImageSource.of("a.png"), ImageSource.of(Fi.get("a.png")));
    }

    @Test
    void urlVideoSourceExposesUrl() {
        VideoSource source = VideoSource.of("https://example.com/clip.mp4");

        assertEquals("https://example.com/clip.mp4", source.url());
        assertNull(source.file());
        assertFalse(source.isLocal());
    }

    @Test
    void fileVideoSourceBypassesNetwork() {
        Fi file = Fi.get("some/clip.mp4");
        VideoSource source = VideoSource.of(file);

        assertSame(file, source.file());
        assertNull(source.url());
        assertTrue(source.isLocal());
    }

    @Test
    void videoSourceEqualityIsValueBased() {
        assertEquals(VideoSource.of("clip.mp4"), VideoSource.of("clip.mp4"));
        assertNotEquals(VideoSource.of("clip.mp4"), VideoSource.of("other.mp4"));
    }

    @Test
    void imageComponentSwitchesBetweenSourcesReactively() {
        TextureRegion regionA = new TextureRegion();
        TextureRegion regionB = new TextureRegion();
        SolimImage.setImageLoader((url, radius, targetW, targetH, success, error) ->
                success.get("https://example.com/a.png".equals(url) ? regionA : regionB));

        Signal<ImageSource> source = Signal.of(ImageSource.of("https://example.com/a.png"));
        SolimImage image = new SolimImage().src(source);
        assertSame(regionA, ((TextureRegionDrawable) image.image().getDrawable()).getRegion());

        source.set(ImageSource.of("https://example.com/b.png"));
        SignalDispatcher.flush();
        assertSame(regionB, ((TextureRegionDrawable) image.image().getDrawable()).getRegion());

        image.dispose();
    }

    @Test
    void videoComponentSwitchesBetweenSourcesReactively() {
        List<String> requested = new ArrayList<>();
        SolimVideo.setVideoLoader((url, success, error) -> {
            requested.add(url);
            error.get(new RuntimeException("offline"));
        });

        Signal<VideoSource> source = Signal.of(VideoSource.of("https://example.com/a.mp4"));
        SolimVideo video = new SolimVideo(source);
        videos.add(video);
        assertEquals(1, requested.size());
        assertEquals("https://example.com/a.mp4", requested.get(0));

        source.set(VideoSource.of("https://example.com/b.mp4"));
        SignalDispatcher.flush();
        assertEquals(2, requested.size());
        assertEquals("https://example.com/b.mp4", requested.get(1));
    }

    @Test
    void localVideoSourceSkipsNetworkLoader() {
        Fi file = Fi.get(System.getProperty("java.io.tmpdir") + "/solim-media-" + System.nanoTime() + ".mp4");
        SolimVideo.setVideoLoader((url, success, error) -> fail("A local file source must not hit the loader"));

        SolimVideo video = new SolimVideo(VideoSource.of(file));
        videos.add(video);
        SignalDispatcher.flush();
        assertFalse(video.isDisposed());
    }
}
