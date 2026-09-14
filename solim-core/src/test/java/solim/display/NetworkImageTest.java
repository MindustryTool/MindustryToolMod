package solim.display;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Scaling;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;
import solim.runtime.SignalDispatcher;

class NetworkImageTest {

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
	void placeholderShownInitially() {
		TextureRegion region = new TextureRegion();
		TextureRegionDrawable placeholder = new TextureRegionDrawable(region);

		NetworkImage.setImageLoader((url, success, error) -> {});

		NetworkImage img = new NetworkImage("https://example.com/test.png")
				.placeholder(placeholder);

		assertSame(placeholder, img.image().getDrawable());
		img.dispose();
	}

	@Test
	void successLoadsImageAndCaches() {
		TextureRegion mockRegion = new TextureRegion();

		NetworkImage.setImageLoader((url, success, error) -> {
			success.get(mockRegion);
		});

		NetworkImage img = new NetworkImage("https://example.com/avatar.png");
		assertNotNull(img.image().getDrawable());
		assertTrue(NetworkImage.isCached("https://example.com/avatar.png"));

		// Second image loads from cache immediately even if loader is empty
		NetworkImage.setImageLoader((url, success, error) -> {
			fail("Loader should not be called when cached");
		});

		NetworkImage img2 = new NetworkImage("https://example.com/avatar.png");
		assertNotNull(img2.image().getDrawable());
		assertSame(img.image().getDrawable(), img2.image().getDrawable());

		img.dispose();
		img2.dispose();
	}

	@Test
	void failureUsesFallback() {
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
	void reactiveUrlUpdatesImage() {
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
	void sizingAndScalingModifiers() {
		NetworkImage img = new NetworkImage()
				.size(64f, 48f)
				.scaling(Scaling.fill);

		assertEquals(64f, img.sizeConstraints().prefWidth.get());
		assertEquals(48f, img.sizeConstraints().prefHeight.get());
		assertEquals(Scaling.fill, img.getScaling());
		img.dispose();
	}

	@Test
	void explicitSizeOverridesLoadedTextureDimensions() {
		TextureRegion largeRegion = new TextureRegion();
		largeRegion.width = 512;
		largeRegion.height = 512;

		NetworkImage.setImageLoader((url, success, error) -> success.get(largeRegion));

		NetworkImage img = new NetworkImage("https://example.com/large-avatar.png")
				.size(32f, 32f);

		assertEquals(32f, img.sizeConstraints().prefWidth.get(), "Pref width must remain locked to explicit 32f");
		assertEquals(32f, img.sizeConstraints().prefHeight.get(), "Pref height must remain locked to explicit 32f");
		img.dispose();
	}
}
