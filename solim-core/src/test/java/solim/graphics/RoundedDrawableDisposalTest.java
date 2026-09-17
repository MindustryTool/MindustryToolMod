package solim.graphics;

import static org.junit.jupiter.api.Assertions.*;

import arc.graphics.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.runtime.SignalDispatcher;
import solim.reactive.Signal;

class RoundedDrawableDisposalTest {

	@BeforeEach
	void setUp() {
		SignalDispatcher.resetForTests();
	}

	@AfterEach
	void tearDown() {
		SignalDispatcher.resetForTests();
	}

	@Test
	void reactiveBindingsDieOnDispose() {
		Signal<Integer> radius = Signal.of(4);
		Signal<Color> fill = Signal.of(Color.red.cpy());
		Signal<Color> border = Signal.of(Color.white.cpy());
		RoundedDrawable drawable = new RoundedDrawable(2).radius(radius).fillColor(fill).border(1f, border);
		assertFalse(drawable.isDisposed());

		radius.set(8);
		fill.set(Color.blue.cpy());
		SignalDispatcher.flush();
		assertEquals(8, drawable.getRadius());

		drawable.dispose();
		assertTrue(drawable.isDisposed());

		radius.set(12);
		fill.set(Color.green.cpy());
		border.set(Color.black.cpy());
		SignalDispatcher.flush();
		assertEquals(8, drawable.getRadius(), "Radius must stop after dispose");

		assertDoesNotThrow(drawable::dispose, "Double dispose must be safe");
		assertTrue(drawable.isDisposed());
	}

	@Test
	void staticDrawableDisposesSafely() {
		RoundedDrawable drawable = RoundedDrawable.of(4, Color.white.cpy());
		assertFalse(drawable.isDisposed());

		drawable.dispose();
		assertTrue(drawable.isDisposed());

		assertDoesNotThrow(drawable::dispose, "Double dispose must be safe");
	}
}
