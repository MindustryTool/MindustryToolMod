package solim.core;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionRootTest {

	private static class DummyComponent extends Component {
		DummyComponent() {
			super(new Element());
		}
	}

	@BeforeEach
	void setUp() {
		OwnershipContext.clear();
		SessionRoot.get().clear();
	}

	@AfterEach
	void tearDown() {
		OwnershipContext.clear();
		SessionRoot.get().clear();
	}

	@Test
	void unparentedComponentAdoptsIntoSessionRoot() {
		assertEquals(0, SessionRoot.get().children().size());

		DummyComponent comp = new DummyComponent();

		assertTrue(SessionRoot.get().contains(comp),
				"Unparented component should be adopted into SessionRoot");
		assertEquals(1, SessionRoot.get().children().size());
	}

	@Test
	void disposingUnparentedComponentRemovesItFromSessionRoot() {
		DummyComponent comp = new DummyComponent();
		assertTrue(SessionRoot.get().contains(comp));

		comp.dispose();

		assertFalse(SessionRoot.get().contains(comp),
				"Disposed component should be removed from SessionRoot to prevent leaks");
		assertEquals(0, SessionRoot.get().children().size());
	}

	@Test
	void reparentingUnparentedComponentRemovesItFromSessionRoot() {
		DummyComponent comp = new DummyComponent();
		assertTrue(SessionRoot.get().contains(comp));

		DummyComponent parent = new DummyComponent();
		parent.adopt(comp);

		assertFalse(SessionRoot.get().contains(comp),
				"Reparented component should no longer reside in SessionRoot");
		assertEquals(parent, comp.owner());
		assertTrue(parent.children().contains(comp));
	}

	@Test
	void verifiesOwnershipLineage() {
		DummyComponent top = new DummyComponent();
		assertTrue(SessionRoot.get().contains(top));

		DummyComponent child = new DummyComponent();
		top.adopt(child);
		assertEquals(top, child.owner());

		top.dispose();
		assertFalse(SessionRoot.get().contains(top));
	}
}
