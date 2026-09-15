package solim.runtime;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.core.Component;

class ParentStackTest {

	@AfterEach
	void clear() {
		ParentStack.clear();
	}

	@Test
	void pushPopCurrent() {
		Table root = new Table();
		ParentStack.push(root);
		assertEquals(root, ParentStack.current());
		assertEquals(1, ParentStack.size());
		ParentStack.pop();
		assertNull(ParentStack.current());
		assertEquals(0, ParentStack.size());
	}

	@Test
	void autoAttachChildren() {
		Table root = new Table();
		ParentStack.push(root);

		Element e1 = new Element();
		Element e2 = new Element();
		ParentStack.add(e1);
		ParentStack.add(e2);

		ParentStack.pop();

		assertEquals(2, root.getChildren().size);
		assertTrue(root.getChildren().contains(e1, true));
		assertTrue(root.getChildren().contains(e2, true));
	}

	@Test
	void nestedScopes() {
		assertEquals(0, ParentStack.size());
		ParentStack.push(new Table());
		assertEquals(1, ParentStack.size());
		{
			ParentStack.push(new Table());
			assertEquals(2, ParentStack.size());
			{
				ParentStack.push(new Table());
				assertEquals(3, ParentStack.size());
				ParentStack.pop();
			}
			assertEquals(2, ParentStack.size());
			ParentStack.pop();
		}
		assertEquals(1, ParentStack.size());
		ParentStack.pop();
		assertEquals(0, ParentStack.size());
	}

	@Test
	void isolateSupplier() {
		Table outer = new Table();
		ParentStack.push(outer);

		Element inner =
				ParentStack.isolate(() -> {
					assertNull(ParentStack.current());
					Table t = new Table();
					ParentStack.push(t);
					Element e = new Element();
					ParentStack.add(e);
					ParentStack.pop();
					return t;
				});

		assertNotNull(inner);
		assertEquals(outer, ParentStack.current());
		assertFalse(outer.getChildren().contains(inner, true));

		ParentStack.pop();
	}

	@Test
	void isolateRunnable() {
		Table outer = new Table();
		ParentStack.push(outer);

		final Table[] held = new Table[1];
		ParentStack.isolate(() -> {
			assertNull(ParentStack.current());
			Table t = new Table();
			ParentStack.push(t);
			Element e = new Element();
			ParentStack.add(e);
			ParentStack.pop();
			held[0] = t;
		});

		assertNotNull(held[0]);
		assertEquals(outer, ParentStack.current());
		assertFalse(outer.getChildren().contains(held[0], true));

		ParentStack.pop();
	}

	@Test
	void isolatePreservesOuterOnException() {
		Table outer = new Table();
		ParentStack.push(outer);

		assertThrows(
				RuntimeException.class,
				() ->
						ParentStack.isolate(
								() -> {
									throw new RuntimeException("fail");
								}));

		assertEquals(outer, ParentStack.current());
		assertEquals(1, ParentStack.size());

		ParentStack.pop();
	}

	@Test
	void isolateRunnablePreservesOuterOnException() {
		Table outer = new Table();
		ParentStack.push(outer);

		assertThrows(
				RuntimeException.class,
				() ->
						ParentStack.isolate(
								(Runnable)
										() -> {
											throw new RuntimeException("fail");
										}));

		assertEquals(outer, ParentStack.current());
		assertEquals(1, ParentStack.size());

		ParentStack.pop();
	}

	@Test
	void find() {
		Table root = new Table();
		root.name = "root";
		Table mid = new Table();
		mid.name = "mid";

		ParentStack.push(root);
		ParentStack.push(mid);

		assertEquals(root, ParentStack.find(t -> "root".equals(t.name)));
		assertEquals(mid, ParentStack.find(t -> "mid".equals(t.name)));
		assertNull(ParentStack.find(t -> "nonexistent".equals(t.name)));

		ParentStack.pop();
		ParentStack.pop();
	}

	@Test
	void attacherStrategy() {
		Table root = new Table();
		final boolean[] customAttacherCalled = new boolean[1];
		ParentStack.push(root, (table, child) -> {
			customAttacherCalled[0] = true;
			return table.add(child);
		});

		Element e = new Element();
		ParentStack.add(e);

		ParentStack.pop();

		assertEquals(1, root.getChildren().size);
		assertTrue(customAttacherCalled[0]);
	}

	@Test
	void addResolvesComponent() {
		Table root = new Table();
		ParentStack.push(root);
		Element e = new Element();
		Component comp = () -> e;
		ParentStack.add(comp);
		ParentStack.add(new Element());
		ParentStack.pop();
		assertEquals(2, root.getChildren().size);
		assertTrue(root.getChildren().contains(e, true));
	}
}
