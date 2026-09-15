package solim.display;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.Color;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;

class BadgeComponentTest {

	@BeforeAll
	static void checkArcContext() {
		Assumptions.assumeTrue(Core.scene != null, "Arc Core.scene is null; skipping skin-dependent tests");
	}

	@Test
	void badgeCreatesWithDefaultNameAndLabelChild() {
		Badge b = new Badge("MOD");
		assertEquals("solim-badge-table", b.table().name);
		assertSame(b.table(), b.element());
		assertSame(b.text().label(), b.table().getChildren().get(0));
		assertEquals("MOD", b.text().label().getText().toString());
		b.dispose();
	}

	@Test
	void badgeStaticText() {
		Badge b = new Badge("VIP");
		assertEquals("VIP", b.text().label().getText().toString());
		b.dispose();
	}

	@Test
	void badgeReactiveText() {
		Signal<String> tag = Signal.of("USER");
		Badge b = new Badge(tag);
		assertEquals("USER", b.text().label().getText().toString());
		tag.set("ADMIN");
		assertEquals("ADMIN", b.text().label().getText().toString());
		b.dispose();
	}

	@Test
	void badgeStaticFactory() {
		Badge b = Badge.of("TAG");
		assertEquals("TAG", b.text().label().getText().toString());
		b.dispose();
	}

	@Test
	void badgeStaticFactoryReactive() {
		Signal<String> s = Signal.of("X");
		Badge b = Badge.of(s);
		assertEquals("X", b.text().label().getText().toString());
		b.dispose();
	}

	@Test
	void badgeOfCount() {
		Signal<Integer> unread = Signal.of(3);
		Badge b = Badge.ofCount(unread);
		assertTrue(b.element().visible);
		assertEquals("3", b.text().label().getText().toString());
		unread.set(0);
		assertFalse(b.element().visible);
		unread.set(10);
		assertTrue(b.element().visible);
		assertEquals("10", b.text().label().getText().toString());
		b.dispose();
	}

	@Test
	void badgeHideOnZeroDisabled() {
		Signal<Integer> unread = Signal.of(0);
		Badge b = Badge.ofCount(unread);
		b.hideOnZero(false);
		unread.set(0);
		assertTrue(b.element().visible);
		b.dispose();
	}

	@Test
	void badgeColorModifier() {
		Badge b = new Badge("TAG");
		b.color(Color.royal);
		assertEquals(Color.royal, b.table().color);
		b.dispose();
	}

	@Test
	void badgeTextColorModifier() {
		Badge b = new Badge("TAG");
		b.textColor(Color.white);
		assertEquals(Color.white, b.text().label().color);
		b.dispose();
	}

	@Test
	void badgeNameModifier() {
		Badge b = new Badge("TAG");
		b.name("my-badge");
		assertEquals("my-badge", b.table().name);
		b.dispose();
	}

	@Test
	void badgeDisposeStopsCountVisibilityUpdates() {
		Signal<Integer> unread = Signal.of(3);
		Badge b = Badge.ofCount(unread);
		assertTrue(b.element().visible);

		b.dispose();
		unread.set(0);

		// Binding disposed: visibility must stay as it was before disposal.
		assertTrue(b.element().visible);
	}

	@Test
	void badgeFluentApiReturnsSameInstance() {
		Badge b = new Badge("TAG");
		assertSame(b, b.hideOnZero(false));
		assertSame(b, b.name("x"));
		b.dispose();
	}
}
