package solim.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import arc.Core;
import arc.Events;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import mindustry.game.EventType.ResizeEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.signal.Computed;
import solim.signal.Signal;

class UnitsTest {

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
	}

	private static ResizableMockGraphics mockGraphics;

	@BeforeAll
	static void initCore() {
		if (Core.app == null) {
			Core.app = new MockApplication();
		}
		mockGraphics = new ResizableMockGraphics();
		Core.graphics = mockGraphics;
	}

	@BeforeEach
	void resetDimensions() {
		mockGraphics.width = 1000;
		mockGraphics.height = 500;
		Units.update();
	}

	@Test
	void testInitialUnits() {
		assertEquals(10f, Units.dvw.get(), 0.001f);
		assertEquals(5f, Units.dvh.get(), 0.001f);
		assertEquals(1000f, Units.width().get(), 0.001f);
		assertEquals(500f, Units.height().get(), 0.001f);
	}

	@Test
	void testPercentageHelpers() {
		Computed<Float> halfWidth = Units.dvw(50f);
		Computed<Float> eightyHeight = Units.dvh(80f);

		assertEquals(500f, halfWidth.get(), 0.001f);
		assertEquals(400f, eightyHeight.get(), 0.001f);
	}

	@Test
	void testDynamicPercentageHelper() {
		Signal<Float> pct = Signal.of(20f);
		Computed<Float> dynamicW = Units.dvw(pct);

		assertEquals(200f, dynamicW.get(), 0.001f);

		pct.set(40f);
		assertEquals(400f, dynamicW.get(), 0.001f);
	}

	@Test
	void testAutoUpdateOnResizeEvent() {
		Computed<Float> halfWidth = Units.dvw(50f);
		assertEquals(500f, halfWidth.get(), 0.001f);

		// Simulate resize to 1600 x 900
		mockGraphics.width = 1600;
		mockGraphics.height = 900;
		Events.fire(new ResizeEvent());

		// dvw and dvh should automatically update
		assertEquals(16f, Units.dvw.get(), 0.001f);
		assertEquals(9f, Units.dvh.get(), 0.001f);
		assertEquals(800f, halfWidth.get(), 0.001f);
		assertEquals(1600f, Units.width().get(), 0.001f);
		assertEquals(900f, Units.height().get(), 0.001f);
	}

	@Test
	void testUiFacadeMethods() {
		Computed<Float> w = Ui.dvw(25f);
		Computed<Float> h = Ui.dvh(10f);

		assertEquals(250f, w.get(), 0.001f);
		assertEquals(50f, h.get(), 0.001f);
	}
}
