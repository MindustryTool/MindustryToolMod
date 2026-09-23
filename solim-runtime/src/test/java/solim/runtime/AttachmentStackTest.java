package solim.runtime;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.core.Component;

class AttachmentStackTest {

	@AfterEach
	void clear() {
		AttachmentStack.clear();
		AttachmentStack.setCellConfigurator((AttachmentStack.CellConfigurator) null);
	}

	@Test
	void pushPopCurrent() {
		Table root = new Table();
		AttachmentStack.push(root);
		assertEquals(root, AttachmentStack.current());
		assertEquals(1, AttachmentStack.size());
		AttachmentStack.pop();
		assertNull(AttachmentStack.current());
		assertEquals(0, AttachmentStack.size());
	}

	@Test
	void autoAttachChildren() {
		Table root = new Table();
		AttachmentStack.push(root);

		Element e1 = new Element();
		Element e2 = new Element();
		AttachmentStack.add(e1);
		AttachmentStack.add(e2);

		AttachmentStack.pop();

		assertEquals(2, root.getChildren().size);
		assertTrue(root.getChildren().contains(e1, true));
		assertTrue(root.getChildren().contains(e2, true));
	}

	@Test
	void nestedScopes() {
		assertEquals(0, AttachmentStack.size());
		AttachmentStack.push(new Table());
		assertEquals(1, AttachmentStack.size());
		{
			AttachmentStack.push(new Table());
			assertEquals(2, AttachmentStack.size());
			{
				AttachmentStack.push(new Table());
				assertEquals(3, AttachmentStack.size());
				AttachmentStack.pop();
			}
			assertEquals(2, AttachmentStack.size());
			AttachmentStack.pop();
		}
		assertEquals(1, AttachmentStack.size());
		AttachmentStack.pop();
		assertEquals(0, AttachmentStack.size());
	}

	@Test
	void isolateSupplier() {
		Table outer = new Table();
		AttachmentStack.push(outer);

		Element inner =
				AttachmentStack.isolate(() -> {
					assertNull(AttachmentStack.current());
					Table t = new Table();
					AttachmentStack.push(t);
					Element e = new Element();
					AttachmentStack.add(e);
					AttachmentStack.pop();
					return t;
				});

		assertNotNull(inner);
		assertEquals(outer, AttachmentStack.current());
		assertFalse(outer.getChildren().contains(inner, true));

		AttachmentStack.pop();
	}

	@Test
	void isolateRunnable() {
		Table outer = new Table();
		AttachmentStack.push(outer);

		final Table[] held = new Table[1];
		AttachmentStack.isolate(() -> {
			assertNull(AttachmentStack.current());
			Table t = new Table();
			AttachmentStack.push(t);
			Element e = new Element();
			AttachmentStack.add(e);
			AttachmentStack.pop();
			held[0] = t;
		});

		assertNotNull(held[0]);
		assertEquals(outer, AttachmentStack.current());
		assertFalse(outer.getChildren().contains(held[0], true));

		AttachmentStack.pop();
	}

	@Test
	void isolatePreservesOuterOnException() {
		Table outer = new Table();
		AttachmentStack.push(outer);

		assertThrows(
				RuntimeException.class,
				() ->
						AttachmentStack.isolate(
								() -> {
									throw new RuntimeException("fail");
								}));

		assertEquals(outer, AttachmentStack.current());
		assertEquals(1, AttachmentStack.size());

		AttachmentStack.pop();
	}

	@Test
	void isolateRunnablePreservesOuterOnException() {
		Table outer = new Table();
		AttachmentStack.push(outer);

		assertThrows(
				RuntimeException.class,
				() ->
						AttachmentStack.isolate(
								(Runnable)
										() -> {
											throw new RuntimeException("fail");
										}));

		assertEquals(outer, AttachmentStack.current());
		assertEquals(1, AttachmentStack.size());

		AttachmentStack.pop();
	}

	@Test
	void find() {
		Table root = new Table();
		root.name = "root";
		Table mid = new Table();
		mid.name = "mid";

		AttachmentStack.push(root);
		AttachmentStack.push(mid);

		assertEquals(root, AttachmentStack.find(t -> "root".equals(t.name)));
		assertEquals(mid, AttachmentStack.find(t -> "mid".equals(t.name)));
		assertNull(AttachmentStack.find(t -> "nonexistent".equals(t.name)));

		AttachmentStack.pop();
		AttachmentStack.pop();
	}

	@Test
	void attacherStrategy() {
		Table root = new Table();
		final boolean[] customAttacherCalled = new boolean[1];
		AttachmentStack.push(root, (table, child) -> {
			customAttacherCalled[0] = true;
			return table.add(child);
		});

		Element e = new Element();
		AttachmentStack.add(e);

		AttachmentStack.pop();

		assertEquals(1, root.getChildren().size);
		assertTrue(customAttacherCalled[0]);
	}

	@Test
	void addResolvesComponent() {
		Table root = new Table();
		AttachmentStack.push(root);
		Element e = new Element();
		Component comp = () -> e;
		AttachmentStack.add(comp);
		AttachmentStack.add(new Element());
		AttachmentStack.pop();
		assertEquals(2, root.getChildren().size);
		assertTrue(root.getChildren().contains(e, true));
	}

	@Test
	void cellConfiguratorReceivesComponentDirectlyWithoutUserObject() {
		Table root = new Table();
		AttachmentStack.push(root);

		Element element = new Element();
		assertNull(element.userObject);

		Component comp = () -> element;
		AttachmentStack.registerPendingComponent(comp, root);

		final Component[] receivedComp = new Component[1];
		final Element[] receivedElement = new Element[1];

		AttachmentStack.setCellConfigurator((cell, child, component) -> {
			receivedElement[0] = child;
			receivedComp[0] = component;
		});

		AttachmentStack.pop();

		assertSame(element, receivedElement[0]);
		assertSame(comp, receivedComp[0]);
		assertNull(element.userObject);
	}

	@Test
	void cellConfiguratorReceivesNullComponentForRawElement() {
		Table root = new Table();
		AttachmentStack.push(root);

		Element rawElement = new Element();
		final Component[] receivedComp = new Component[1];
		final Element[] receivedElement = new Element[1];

		AttachmentStack.setCellConfigurator((cell, child, component) -> {
			receivedElement[0] = child;
			receivedComp[0] = component;
		});

		AttachmentStack.add(rawElement);
		AttachmentStack.pop();

		assertSame(rawElement, receivedElement[0]);
		assertNull(receivedComp[0]);
	}

	@Test
	void isolateCompleteContextIsolation() {
		Table outer = new Table();
		Element outerEl = new Element();
		Component outerComp = () -> outerEl;

		boolean[] outerAttacherCalled = new boolean[1];
		AttachmentStack.push(outer, (table, child) -> {
			outerAttacherCalled[0] = true;
			return table.add(child);
		});
		AttachmentStack.registerPendingComponent(outerComp, outer);

		Table inner = AttachmentStack.isolate(() -> {
			assertNull(AttachmentStack.current());
			Table innerTable = new Table();
			boolean[] innerAttacherCalled = new boolean[1];
			AttachmentStack.push(innerTable, (t, c) -> {
				innerAttacherCalled[0] = true;
				return t.add(c);
			});

			Element innerEl = new Element();
			AttachmentStack.add(innerEl);
			AttachmentStack.pop();

			assertTrue(innerAttacherCalled[0]);
			assertFalse(outerAttacherCalled[0]);
			assertEquals(1, innerTable.getChildren().size);
			assertTrue(innerTable.getChildren().contains(innerEl, true));
			return innerTable;
		});

		assertNotNull(inner);
		assertEquals(outer, AttachmentStack.current());

		// Now finish outer
		AttachmentStack.pop();

		assertTrue(outerAttacherCalled[0]);
		assertEquals(1, outer.getChildren().size);
		assertTrue(outer.getChildren().contains(outerEl, true));
		assertFalse(outer.getChildren().contains(inner, true));
	}
}
