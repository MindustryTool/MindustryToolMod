package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.Slider.SliderStyle;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Signal;
import solim.test.SolimEnv;

class SolimVideoTest extends SolimEnv {

	private final List<SolimVideo> videos = new ArrayList<>();

	@BeforeEach
	void stubIcons() {
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
	void restoreStatics() {
		for (SolimVideo video : videos) {
			video.dispose();
		}
		videos.clear();
		SolimVideo.setVideoLoader(null);
		SolimVideo.setDefaultIcons(null, null, null, null, null);
	}

	private SolimVideo track(SolimVideo video) {
		videos.add(video);
		return video;
	}

	@Test
	void formatTimeRendersMinutesAndSeconds() {
		assertEquals("00:00", SolimVideo.formatTime(0));
		assertEquals("00:05", SolimVideo.formatTime(5.2));
		assertEquals("01:05", SolimVideo.formatTime(65));
		assertEquals("10:00", SolimVideo.formatTime(600));
		assertEquals("00:00", SolimVideo.formatTime(-3));
		assertEquals("00:00", SolimVideo.formatTime(Double.NaN));
	}

	@Test
	void nullUrlShowsFallback() {
		TextureRegionDrawable fallback = new TextureRegionDrawable(new TextureRegion());
		SolimVideo.setVideoLoader((url, success, error) -> fail("Loader must not run for null URLs"));

		SolimVideo video = track(new SolimVideo().fallback(fallback));
		video.url((String) null);
		flushEffects();

		assertSame(fallback, ((Image) video.stack().getChildren().first()).getDrawable());
	}

	@Test
	void loadErrorShowsFallback() {
		TextureRegionDrawable fallback = new TextureRegionDrawable(new TextureRegion());
		SolimVideo.setVideoLoader((url, success, error) -> error.get(new RuntimeException("404")));

		SolimVideo video = track(new SolimVideo("https://example.com/missing.mp4").fallback(fallback));
		flushEffects();

		assertSame(fallback, ((Image) video.stack().getChildren().first()).getDrawable());
		assertNull(video.player());
	}

	@Test
	void controlsToggleHidesOverlay() {
		SolimVideo.setVideoLoader((url, success, error) -> {
		});

		SolimVideo video = track(new SolimVideo());
		flushEffects();
		assertTrue(video.overlay().visible);

		video.controls(false);
		flushEffects();
		assertFalse(video.overlay().visible);

		video.controls(true);
		flushEffects();
		assertTrue(video.overlay().visible);
	}

	@Test
	void reactiveControlsToggleHidesOverlay() {
		SolimVideo.setVideoLoader((url, success, error) -> {
		});

		Signal<Boolean> controls = Signal.of(true);
		SolimVideo video = track(new SolimVideo().controls(controls));
		flushEffects();
		assertTrue(video.overlay().visible);

		controls.set(false);
		flushEffects();
		assertFalse(video.overlay().visible);
	}

	@Test
	void reactiveUrlSwitchReloads() {
		List<String> requested = new ArrayList<>();
		SolimVideo.setVideoLoader((url, success, error) -> {
			requested.add(url);
			error.get(new RuntimeException("offline"));
		});

		Signal<String> url = Signal.of("https://example.com/a.mp4");
		SolimVideo video = track(new SolimVideo(url));
		assertEquals(1, requested.size());

		url.set("https://example.com/b.mp4");
		flushEffects();
		assertEquals(2, requested.size());
		assertEquals("https://example.com/b.mp4", requested.get(1));
		assertNull(video.player());
		video.dispose();
	}

	@Test
	void disposeReleasesPlayerReference() {
		SolimVideo.setVideoLoader((url, success, error) -> error.get(new RuntimeException("offline")));

		SolimVideo video = track(new SolimVideo("https://example.com/clip.mp4"));
		flushEffects();
		video.dispose();
		assertTrue(video.isDisposed());
		assertNull(video.player());
		assertDoesNotThrow(video::dispose);
	}
}
