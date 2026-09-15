package solim.display;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.Color;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;

class TextComponentTest {

	@BeforeAll
	static void checkArcContext() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
	}

	@Test
	void staticTextDisplaysCorrectly() {
		Text t = new Text("Hello");
		assertEquals("Hello", t.label().getText().toString());
		t.dispose();
	}

	@Test
	void emptyDefaultDisplaysEmptyString() {
		Text t = new Text();
		assertEquals("", t.label().getText().toString());
		t.dispose();
	}

	@Test
	void staticFactoryCreatesText() {
		Text t = Text.of("Hello");
		assertEquals("Hello", t.label().getText().toString());
		t.dispose();
	}

	@Test
	void signalUpdatesLabelText() {
		Signal<String> s = Signal.of("a");
		Text t = Text.of(s);

		assertEquals("a", t.label().getText().toString());

		s.set("b");
		assertEquals("b", t.label().getText().toString());

		s.set("c");
		assertEquals("c", t.label().getText().toString());
		t.dispose();
	}

	@Test
	void disposeStopsReactiveBinding() {
		Signal<String> s = Signal.of("a");
		Text t = Text.of(s);

		assertEquals("a", t.label().getText().toString());
		t.dispose();

		s.set("b");
		assertEquals("a", t.label().getText().toString());
	}

	@Test
	void colorModifierUpdatesLabelColor() {
		Text t = new Text("Hello");
		t.color(Color.red);
		assertEquals(Color.red, t.label().color);
		t.dispose();
	}

	@Test
	void reactiveColorUpdatesLabelColor() {
		Signal<Color> colorSig = Signal.of(Color.green);
		Text t = new Text("Hello").color(colorSig);
		assertEquals(Color.green, t.label().color);

		colorSig.set(Color.blue);
		assertEquals(Color.blue, t.label().color);
		t.dispose();
	}

	@Test
	void visibleModifierChangesLabelVisibility() {
		Text t = new Text("Hello");

		t.visible(false);
		assertFalse(t.label().visible);

		t.visible(true);
		assertTrue(t.label().visible);
		t.dispose();
	}

	@Test
	void reactiveVisibleUpdatesLabelVisibility() {
		Signal<Boolean> vis = Signal.of(true);
		Text t = new Text("Hello").visible(vis);
		assertTrue(t.label().visible);

		vis.set(false);
		assertFalse(t.label().visible);

		vis.set(true);
		assertTrue(t.label().visible);
		t.dispose();
	}

	@Test
	void positionSetsLabelCoordinates() {
		Text t = new Text("Hello");

		t.x(10f);
		assertEquals(10f, t.label().x, 0.01f);

		t.y(20f);
		assertEquals(20f, t.label().y, 0.01f);

		t.position(30f, 40f);
		assertEquals(30f, t.label().x, 0.01f);
		assertEquals(40f, t.label().y, 0.01f);
		t.dispose();
	}

	@Test
	void nameModifierUpdatesElementName() {
		Text t = new Text("Hello").name("my-text");
		assertEquals("my-text", t.element().name);
		t.dispose();
	}

	@Test
	void elementIsSameAsLabel() {
		Text t = new Text("Hello");
		assertSame(t.label(), t.element());
		t.dispose();
	}
}
