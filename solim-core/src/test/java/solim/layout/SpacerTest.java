package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.core.SolimToken;

class SpacerTest {

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
	void elementIsExpanding() {
		Spacer s = new Spacer();
		assertTrue(SolimToken.isExpanding(s.element()));
	}

	@Test
	void nameModifierUpdatesElementName() {
		Spacer s = new Spacer();
		s.name("my-spacer");
		assertEquals("my-spacer", s.element().name);
	}
}
