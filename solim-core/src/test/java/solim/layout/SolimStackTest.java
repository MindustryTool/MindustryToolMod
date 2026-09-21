package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import arc.func.Prov;

import org.junit.jupiter.api.Test;

import arc.scene.Element;
import solim.core.Component;
import solim.runtime.ParentStack;
import solim.test.SolimEnv;

class SolimStackTest extends SolimEnv {

	private static final class TestComponent implements Component {
		private final Element element = new Element();
		private int disposeCount;

		@Override
		public Element element() {
			return element;
		}

		@Override
		public void dispose() {
			disposeCount++;
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
	void layerAddsDirectChildWithoutRow() {
		SolimStack s = new SolimStack();
		TestComponent component = new TestComponent();

		s.layer(() -> component);

		assertEquals(1, s.stack().getChildren().size);
		assertSame(component.element(), s.stack().getChildren().get(0));
	}

	@Test
	void layerCanCaptureStackElement() {
		SolimStack s = new SolimStack();
		TestComponent component = new TestComponent();

		s.layer(parent -> {
			assertSame(s.stack(), parent);
			return component;
		});

		assertSame(component.element(), s.stack().getChildren().get(0));
	}

	@Test
	void nullLayerDoesNothing() {
		SolimStack s = new SolimStack();

		s.layer((Prov<Component>) null);
		s.layer(() -> null);

		assertTrue(s.stack().getChildren().isEmpty());
	}

	@Test
	void layerDisposesOwnedComponentOnce() {
		SolimStack s = new SolimStack();
		TestComponent component = new TestComponent();
		s.layer(() -> component);

		s.dispose();
		s.dispose();

		assertEquals(1, component.disposeCount);
	}

	@Test
	void layerRestoresParentStackAfterFailure() {
		SolimStack s = new SolimStack();
		int before = ParentStack.size();

		assertThrows(RuntimeException.class, () -> s.layer(() -> {
			throw new RuntimeException("layer failure");
		}));

		assertEquals(before, ParentStack.size());
		assertTrue(s.stack().getChildren().isEmpty());
	}

	@Test
	void legacyLayerAndChildrenMethodsAreRemoved() {
		for (Method method : SolimStack.class.getMethods()) {
			assertNotEquals("childrenComponent", method.getName(),
					"childrenComponent must be removed");
			assertNotEquals("children", method.getName(),
					"SolimStack must not expose children(...)");
			if ("layer".equals(method.getName())) {
				for (Class<?> param : method.getParameterTypes()) {
					assertNotEquals(Runnable.class, param, "layer(Runnable) must be removed");
				}
			}
		}
	}
}
