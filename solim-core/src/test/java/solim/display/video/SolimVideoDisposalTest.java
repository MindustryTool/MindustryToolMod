package solim.display.video;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.Core;
import arc.files.Fi;
import arc.func.Cons;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Slider.SliderStyle;
import solim.core.Component;
import solim.reactive.Signal;
import solim.test.SolimEnv;

/**
 * Disposal proof for {@link SolimVideo}.
 *
 * <p>A video widget owns more than its Arc stack: five overlay controls, two replaceable reactive
 * bindings, and a decoder that holds GL resources. Disposal has to reach all three, and the
 * asynchronous load callbacks have to notice that they are late so a player cannot be resurrected
 * after the component is gone.
 */
class SolimVideoDisposalTest extends SolimEnv {

	private final List<SolimVideo> videos = new ArrayList<>();

	@BeforeEach
	void stubStylesAndIcons() {
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
	void disposeDisposesEveryOverlayControl() {
		SolimVideo.setVideoLoader((url, success, error) -> {
		});

		SolimVideo video = track(new SolimVideo());
		List<Component> controls = new ArrayList<>(video.children());
		assertEquals(5, controls.size(), "A video owns its play, scrub, time, replay and mute controls");

		video.dispose();
		assertTrue(video.isDisposed());
		for (Component control : controls) {
			assertTrue(control.isDisposed(),
					"Overlay control must dispose with the video: " + control.getClass().getSimpleName());
		}
		assertDoesNotThrow(video::dispose, "Double dispose must be safe");
	}

	@Test
	void controlsBindingStopsOnDispose() {
		SolimVideo.setVideoLoader((url, success, error) -> {
		});

		Signal<Boolean> controls = Signal.of(true);
		SolimVideo video = track(new SolimVideo().controls(controls));
		flushEffects();
		assertTrue(video.overlay().visible);

		controls.set(false);
		flushEffects();
		assertFalse(video.overlay().visible, "Live controls binding must drive the overlay");

		video.dispose();
		assertEquals(0, controls.observerCount(), "Disposal must drain the controls observers");

		controls.set(true);
		flushEffects();
		assertFalse(video.overlay().visible, "Disposed controls binding must freeze");
	}

	@Test
	void urlBindingStopsOnDispose() {
		List<String> requested = new ArrayList<>();
		SolimVideo.setVideoLoader((url, success, error) -> {
			requested.add(url);
			error.get(new RuntimeException("offline"));
		});

		Signal<String> url = Signal.of("clip://a");
		SolimVideo video = track(new SolimVideo(url));
		assertEquals(1, requested.size());

		url.set("clip://b");
		flushEffects();
		assertEquals(2, requested.size(), "Live url binding must reload the player");

		video.dispose();
		assertEquals(0, url.observerCount(), "Disposal must drain the url observers");

		url.set("clip://c");
		flushEffects();
		assertEquals(2, requested.size(), "Disposed url binding must stop loading");
	}

	@Test
	void disposeReleasesThePlayer() {
		SolimVideo.setVideoLoader((url, success, error) -> error.get(new RuntimeException("offline")));

		SolimVideo video = track(new SolimVideo("clip://a"));
		flushEffects();
		assertNull(video.player());

		video.dispose();
		assertNull(video.player(), "Disposal must release the decoder");
		assertDoesNotThrow(video::dispose, "Double dispose must be safe");
	}

	@Test
	void lateLoadCallbackAfterDisposeDoesNotResurrectThePlayer() {
		AtomicReference<Cons<Fi>> success = new AtomicReference<>();
		SolimVideo.setVideoLoader((url, onSuccess, error) -> success.set(onSuccess));

		SolimVideo video = track(new SolimVideo("clip://a"));
		flushEffects();

		video.dispose();

		File file = new File(System.getProperty("java.io.tmpdir"), "solim-video-disposal.mp4");
		try {
			assertTrue(file.createNewFile() || file.exists());
		} catch (IOException e) {
			throw new AssertionError("Could not stage a playable-looking file", e);
		}
		success.get().get(new Fi(file));

		assertNull(video.player(), "A load callback that arrives after disposal must be ignored");
	}
}