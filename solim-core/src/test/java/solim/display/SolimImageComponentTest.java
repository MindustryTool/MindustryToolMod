package solim.display;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.Color;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;
import solim.runtime.SignalDispatcher;

class SolimImageComponentTest {

	@BeforeAll
	static void initArc() {
		if (Core.app == null) {
			Core.app = new MockApplication();
		}
		if (Core.graphics == null) {
			Core.graphics = new MockGraphics();
		}
	}

	@Test
	void sizeSetsImageDimensions() {
		SolimImage img = new SolimImage();
		img.size(64f, 48f);
		assertEquals(64f, img.image().getWidth(), 0.01f);
		assertEquals(48f, img.image().getHeight(), 0.01f);
	}

	@Test
	void colorModifierUpdatesImageColor() {
		SolimImage img = new SolimImage();
		img.color(Color.red);
		assertEquals(Color.red, img.image().color);
	}

	@Test
	void reactiveColorUpdatesImageColor() {
		Signal<Color> colorSig = Signal.of(Color.green);
		SolimImage img = new SolimImage().color(colorSig);
		assertEquals(Color.green, img.image().color);

		colorSig.set(Color.blue);
		SignalDispatcher.flush();
		assertEquals(Color.blue, img.image().color);
	}

	@Test
	void visibleModifierChangesImageVisibility() {
		SolimImage img = new SolimImage();

		img.visible(false);
		assertFalse(img.image().visible);

		img.visible(true);
		assertTrue(img.image().visible);
	}

	@Test
	void positionSetsImageCoordinates() {
		SolimImage img = new SolimImage();

		img.x(10f);
		assertEquals(10f, img.image().x, 0.01f);

		img.y(20f);
		assertEquals(20f, img.image().y, 0.01f);

		img.position(30f, 40f);
		assertEquals(30f, img.image().x, 0.01f);
		assertEquals(40f, img.image().y, 0.01f);
	}

	@Test
	void nameModifierUpdatesElementName() {
		SolimImage img = new SolimImage().name("my-image");
		assertEquals("my-image", img.element().name);
	}

	@Test
	void elementIsSameAsImage() {
		SolimImage img = new SolimImage();
		assertSame(img.image(), img.element());
	}
}
