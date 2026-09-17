package solim.signal;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.Events;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import java.util.concurrent.atomic.AtomicInteger;
import mindustry.game.EventType.ResizeEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SignalsTest {

	static class ResizableMockGraphics extends MockGraphics {
		int width = 1000;
		int height = 500;

		@Override
		public int getWidth() {
			return width;
		}

		@Override
		public int getHeight() {
			return height;
		}

		@Override
		public boolean isPortrait() {
			return height > width;
		}
	}

	private static ResizableMockGraphics mockGraphics;

	@BeforeAll
	static void initCore() {
		if (Core.app == null) {
			Core.app = new MockApplication();
		}
		mockGraphics = new ResizableMockGraphics();
		Core.graphics = mockGraphics;
		Signals.init();
	}

	@BeforeEach
	void resetDimensions() {
		mockGraphics.width = 1000;
		mockGraphics.height = 500;
		Events.fire(new ResizeEvent());
	}

	@Test
	void testInitialStateIsLandscape() {
		assertFalse(Signals.isPortrait().peek());
	}

	@Test
	void testOrientationChangeOnResizeEvent() {
		// Change dimensions to portrait
		mockGraphics.width = 500;
		mockGraphics.height = 1000;
		Events.fire(new ResizeEvent());

		assertTrue(Signals.isPortrait().peek());

		// Change dimensions back to landscape
		mockGraphics.width = 1200;
		mockGraphics.height = 800;
		Events.fire(new ResizeEvent());

		assertFalse(Signals.isPortrait().peek());
	}

	@Test
	void testDeduplicationWhenOrientationUnchanged() {
		AtomicInteger fireCount = new AtomicInteger(0);
		if (Signals.isPortrait() instanceof Signal) {
			((Signal<Boolean>) Signals.isPortrait()).subscribe(val -> fireCount.incrementAndGet());
		}

		// Initial is landscape (false). Fire resize that remains landscape.
		mockGraphics.width = 1600;
		mockGraphics.height = 900;
		Events.fire(new ResizeEvent());

		assertEquals(0, fireCount.get());

		// Switch to portrait
		mockGraphics.width = 400;
		mockGraphics.height = 800;
		Events.fire(new ResizeEvent());

		assertEquals(1, fireCount.get());
	}

	@Test
	void testMultipleSubscribers() {
		AtomicInteger sub1 = new AtomicInteger(0);
		AtomicInteger sub2 = new AtomicInteger(0);

		if (Signals.isPortrait() instanceof Signal) {
			((Signal<Boolean>) Signals.isPortrait()).subscribe(val -> sub1.incrementAndGet());
			((Signal<Boolean>) Signals.isPortrait()).subscribe(val -> sub2.incrementAndGet());
		}

		mockGraphics.width = 300;
		mockGraphics.height = 600;
		Events.fire(new ResizeEvent());

		assertEquals(1, sub1.get());
		assertEquals(1, sub2.get());
	}
}
