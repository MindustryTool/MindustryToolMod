package solim.display;

import static solim.modifier.Values.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.func.Cons;
import arc.graphics.g2d.TextureRegion;
import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;
import solim.test.OwnershipEnv;

class SolimImageDisposalTest extends OwnershipEnv {

	@BeforeEach
	void setUp() {
		SignalDispatcher.resetForTests();
		SolimImage.clearCache();
	}

	@AfterEach
	void tearDown() {
		SignalDispatcher.resetForTests();
		SolimImage.clearCache();
		SolimImage.setImageLoader(null);
	}

	static final class CountingLoader implements ImageLoader {
		final Map<String, Integer> loads = new HashMap<>();

		@Override
		public void load(String url, int radius, float targetW, float targetH, Cons<TextureRegion> onSuccess, Cons<Throwable> onError) {
			loads.put(url, loads.containsKey(url) ? loads.get(url) + 1 : 1);
		}

		int loadsOf(String url) {
			return loads.containsKey(url) ? loads.get(url) : 0;
		}
	}

	@Test
	void disposeStopsSignalReloads() {
		CountingLoader loader = new CountingLoader();
		SolimImage.setImageLoader(loader);
		Signal<ImageSource> source = Signal.of(ImageSource.of("deep://e"));
		SolimImage image = new SolimImage(source);
		assertEquals(1, loader.loadsOf("deep://e"));

		image.dispose();
		assertTrue(image.isDisposed());

		source.set(ImageSource.of("deep://f"));
		SignalDispatcher.flush();
		assertEquals(0, loader.loadsOf("deep://f"), "Disposed SolimImage must stop reloading");

		assertDoesNotThrow(image::dispose, "Double dispose must be safe");
		assertTrue(image.isDisposed());
	}
}
