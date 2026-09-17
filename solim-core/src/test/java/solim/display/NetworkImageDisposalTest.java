package solim.display;

import static org.junit.jupiter.api.Assertions.*;

import arc.Application;
import arc.Core;
import arc.Graphics;
import arc.func.Cons;
import arc.graphics.GL20;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockApplication;
import arc.mock.MockGL20;
import arc.mock.MockGraphics;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.runtime.SignalDispatcher;
import solim.reactive.Signal;

class NetworkImageDisposalTest {

	static Application prevApp;
	static Graphics prevGraphics;
	static GL20 prevGl;
	static GL20 prevGl20;

	@BeforeAll
	static void initArc() {
		prevApp = Core.app;
		prevGraphics = Core.graphics;
		prevGl = Core.gl;
		prevGl20 = Core.gl20;
		if (Core.app == null) {
			Core.app = new MockApplication();
		}
		if (Core.graphics == null) {
			Core.graphics = new MockGraphics();
		}
		if (Core.gl == null) {
			Core.gl = new MockGL20();
			Core.gl20 = (MockGL20) Core.gl;
		}
	}

	@AfterAll
	static void tearDownArc() {
		Core.app = prevApp;
		Core.graphics = prevGraphics;
		Core.gl = prevGl;
		Core.gl20 = prevGl20;
		prevApp = null;
		prevGraphics = null;
		prevGl = null;
		prevGl20 = null;
	}

	@BeforeEach
	void setUp() {
		SignalDispatcher.resetForTests();
		NetworkImage.clearCache();
	}

	@AfterEach
	void tearDown() {
		SignalDispatcher.resetForTests();
		NetworkImage.clearCache();
		NetworkImage.setImageLoader(null);
	}

	static final class CountingLoader implements NetworkImage.ImageLoader {
		final Map<String, Integer> loads = new HashMap<>();

		@Override
		public void load(String url, Cons<TextureRegion> onSuccess, Cons<Throwable> onError) {
			loads.put(url, loads.containsKey(url) ? loads.get(url) + 1 : 1);
		}

		int loadsOf(String url) {
			return loads.containsKey(url) ? loads.get(url) : 0;
		}
	}

	@Test
	void staticReUrlDisposesSignalBinding() {
		CountingLoader loader = new CountingLoader();
		NetworkImage.setImageLoader(loader);
		Signal<String> url = Signal.of("deep://a");
		NetworkImage image = new NetworkImage(url);
		assertEquals(1, loader.loadsOf("deep://a"));
		assertFalse(image.isDisposed());

		url.set("deep://b");
		SignalDispatcher.flush();
		assertEquals(1, loader.loadsOf("deep://b"));

		image.url("deep://c");
		assertEquals(1, loader.loadsOf("deep://c"));

		url.set("deep://d");
		SignalDispatcher.flush();
		assertEquals(0, loader.loadsOf("deep://d"), "Re-url must dispose the previous signal binding");

		image.dispose();
		assertTrue(image.isDisposed());
		assertDoesNotThrow(image::dispose, "Double dispose must be safe");
	}

	@Test
	void disposeStopsSignalReloads() {
		CountingLoader loader = new CountingLoader();
		NetworkImage.setImageLoader(loader);
		Signal<String> url = Signal.of("deep://e");
		NetworkImage image = new NetworkImage(url);
		assertEquals(1, loader.loadsOf("deep://e"));

		image.dispose();
		assertTrue(image.isDisposed());

		url.set("deep://f");
		SignalDispatcher.flush();
		assertEquals(0, loader.loadsOf("deep://f"), "Disposed NetworkImage must stop reloading");

		assertDoesNotThrow(image::dispose, "Double dispose must be safe");
		assertTrue(image.isDisposed());
	}
}
