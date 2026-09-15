package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import arc.scene.ui.layout.Table;

class SolimStackTest {

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
	void createsStackWithDefaultName() {
		SolimStack s = new SolimStack();
		assertEquals("solim-stack-stack", s.stack().name);
	}

	@Test
	void addAddsChildToStack() {
		SolimStack s = new SolimStack();
		Element bg = new Element();
		Element fg = new Element();

		s.add(bg);
		s.add(fg);

		assertEquals(2, s.stack().getChildren().size);
		assertSame(bg, s.stack().getChildren().get(0));
		assertSame(fg, s.stack().getChildren().get(1));
	}

	@Test
	void nameModifierUpdatesStackName() {
		SolimStack s = new SolimStack();
		s.name("my-stack");
		assertEquals("my-stack", s.stack().name);
	}

	@Test
	void stackIsSameAsElement() {
		SolimStack s = new SolimStack();
		assertSame(s.stack(), s.element());
	}

	@Test
	void layerAddsRowChild() {
		SolimStack s = new SolimStack();
		s.layer(() -> new Row());

		assertEquals(1, s.stack().getChildren().size);
		assertTrue(s.stack().getChildren().get(0) instanceof Table);
	}
}
