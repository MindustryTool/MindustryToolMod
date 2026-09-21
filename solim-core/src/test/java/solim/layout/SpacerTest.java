package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import solim.core.SolimToken;
import solim.test.SolimEnv;

class SpacerTest extends SolimEnv {


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

	@Test
	void spacerReturnsComponentForChaining() {
		Spacer s = new Spacer();
		assertSame(s, s.name("gap"));
		assertEquals("gap", s.element().name);
	}
}
