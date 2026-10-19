package solim.display.video;

import static solim.modifier.Values.*;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.g2d.Font;
import arc.graphics.g2d.Font.FontData;
import arc.graphics.g2d.Font.Glyph;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.Slider.SliderStyle;
import arc.struct.SnapshotSeq;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.core.Units;
import solim.reactive.Signal;
import solim.test.SolimEnv;

class SolimVideoTest extends SolimEnv {

	private static final int GLYPH_WIDTH = 8;

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

		SolimVideo video = track(new SolimVideo().props(fallback(fallback)));
		video.props(src((String) null));
		flushEffects();

		assertSame(fallback, ((Image) video.stack().getChildren().first()).getDrawable());
	}

	@Test
	void loadErrorShowsFallback() {
		TextureRegionDrawable fallback = new TextureRegionDrawable(new TextureRegion());
		SolimVideo.setVideoLoader((url, success, error) -> error.get(new RuntimeException("404")));

		SolimVideo video = track(new SolimVideo("https://example.com/missing.mp4").props(fallback(fallback)));
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

		video.props(controls(false));
		flushEffects();
		assertFalse(video.overlay().visible);

		video.props(controls(true));
		flushEffects();
		assertTrue(video.overlay().visible);
	}

	@Test
	void reactiveControlsToggleHidesOverlay() {
		SolimVideo.setVideoLoader((url, success, error) -> {
		});

		Signal<Boolean> controls = Signal.of(true);
		SolimVideo video = track(new SolimVideo().props(controls(controls)));
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
		SolimVideo video = track(new SolimVideo(url.map(u -> VideoSource.of(u))));
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
		assertDoesNotThrow(video::dispose);
	}

	@Test
	void twoWayMutedSignalWritesBack() {
		Signal<Boolean> muted = Signal.of(false);
		SolimVideo video = track(new SolimVideo().props(muted(muted)));
		assertFalse(muted.get());

		video.setMuted(true);
		flushEffects();
		assertTrue(muted.get(), "Internal muted state change must write back to caller's signal");

		video.dispose();
	}

	@Test
	void appliedOnceAutoplayTakesEffect() {
		SolimVideo video = track(new SolimVideo().props(autoplay(true)));
		assertTrue(video.isAutoplay());
		video.dispose();
	}

	@Test
	void overlayControlsFitTheVideoStage() {
		installMeasuredLabelFont();
		SolimVideo video = track(new SolimVideo().props(controls(true)));
		flushEffects();

		float stageWidth = Units.unit(80);
		float stageHeight = Units.unit(45);
		video.stack().setSize(stageWidth, stageHeight);
		video.stack().validate();

		assertControlsLaidOut(video.overlay(), stageWidth, stageHeight);
	}

	private static void assertControlsLaidOut(Group group, float stageWidth, float stageHeight) {
		SnapshotSeq<Element> children = group.getChildren();
		for (int i = 0; i < children.size; i++) {
			Element child = children.get(i);
			assertTrue(child.x >= 0f && child.y >= 0f && child.x + child.getWidth() <= stageWidth
					&& child.y + child.getHeight() <= stageHeight,
					child.getClass().getSimpleName() + " escapes the video stage at " + child.x + "," + child.y
							+ " sized " + child.getWidth() + "x" + child.getHeight());
			if (child instanceof Label) {
				Label label = (Label) child;
				assertTrue(label.getWidth() >= label.getText().length() * GLYPH_WIDTH,
						"the readout \"" + label.getText() + "\" needs " + label.getText().length() * GLYPH_WIDTH
								+ "px but was given " + label.getWidth());
			}
			for (int j = i + 1; j < children.size; j++) {
				Element sibling = children.get(j);
				assertFalse(overlaps(child, sibling), child.getClass().getSimpleName() + " overlaps "
						+ sibling.getClass().getSimpleName());
			}
			if (child instanceof Group) {
				assertControlsLaidOut((Group) child, stageWidth, stageHeight);
			}
		}
	}

	private static boolean overlaps(Element a, Element b) {
		return a.x < b.x + b.getWidth() && b.x < a.x + a.getWidth()
				&& a.y < b.y + b.getHeight() && b.y < a.y + a.getHeight();
	}

	private static void installMeasuredLabelFont() {
		Core.scene.addStyle(LabelStyle.class, new LabelStyle(measuredFont(), null));
	}

	private static Font measuredFont() {
		FontData data = new FontData() {
			@Override
			public boolean hasGlyph(char ch) {
				return true;
			}

			@Override
			public Glyph getGlyph(char ch) {
				Glyph glyph = new Glyph();
				glyph.id = ch;
				glyph.width = GLYPH_WIDTH;
				glyph.height = GLYPH_WIDTH;
				glyph.xadvance = GLYPH_WIDTH;
				return glyph;
			}
		};
		return new Font(data, new TextureRegion(), false);
	}
}
