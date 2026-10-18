package solim.display;

import static solim.modifier.Values.*;

import static org.junit.jupiter.api.Assertions.*;
import static solim.modifier.Values.position;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.visible;
import static solim.modifier.Values.x;
import static solim.modifier.Values.y;

import arc.graphics.Color;
import org.junit.jupiter.api.Test;
import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class SolimImageComponentTest extends SolimEnv {


	@Test
	void sizeSetsImageDimensions() {
		SolimImage img = new SolimImage();
		img.props(size(64f, 48f), prefWidth(64f), prefHeight(48f));
		assertEquals(64f, img.image().getWidth(), 0.01f);
		assertEquals(48f, img.image().getHeight(), 0.01f);
	}

	@Test
	void colorModifierUpdatesImageColor() {
		SolimImage img = new SolimImage();
		img.props(color(Color.red));
		assertEquals(Color.red, img.image().color);
	}

	@Test
	void reactiveColorUpdatesImageColor() {
		Signal<Color> colorSig = Signal.of(Color.green);
		SolimImage img = new SolimImage().props(color(colorSig));
		assertEquals(Color.green, img.image().color);

		colorSig.set(Color.blue);
		SignalDispatcher.flush();
		assertEquals(Color.blue, img.image().color);
	}

	@Test
	void visibleModifierChangesImageVisibility() {
		SolimImage img = new SolimImage();

		img.props(visible(false));
		assertFalse(img.image().visible);

		img.props(visible(true));
		assertTrue(img.image().visible);
	}

	@Test
	void positionSetsImageCoordinates() {
		SolimImage img = new SolimImage();

		img.props(x(10f));
		assertEquals(10f, img.image().x, 0.01f);

		img.props(y(20f));
		assertEquals(20f, img.image().y, 0.01f);

		img.props(position(30f, 40f));
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
