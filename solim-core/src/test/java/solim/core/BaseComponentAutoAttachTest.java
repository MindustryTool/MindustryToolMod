package solim.core;

import solim.runtime.ComponentContext;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.runtime.ParentStack;
import solim.ui.Ui;

class BaseComponentAutoAttachTest {

	@BeforeAll
	static void initArc() {
		if (Core.app == null) {
			Core.app = new MockApplication();
		}
		if (Core.graphics == null) {
			Core.graphics = new MockGraphics();
		}
	}

	static class Probe extends BaseComponent {
		@Override
		protected Element build() {
			return new Element();
		}
	}

	@AfterEach
	void clear() {
		ParentStack.clear();
		ComponentContext.clear();
	}

	@Test
	void constructsInScopeAutoAttaches() {
		Table root = new Table();
		ParentStack.push(root);
		Probe probe = new Probe();
		ParentStack.pop();
		assertEquals(1, root.getChildren().size);
		assertTrue(root.getChildren().contains(probe.element(), true));
	}

	@Test
	void nestedScopeAttachesToInnerParent() {
		Table root = new Table();
		ParentStack.push(root);
		final Probe[] held = new Probe[1];
		var col = Ui.column().children(() -> {
			held[0] = new Probe();
		});
		ParentStack.pop();
		assertEquals(1, root.getChildren().size);
		assertEquals(1, col.table().getChildren().size);
		assertSame(held[0].element(), col.table().getChildren().get(0));
	}

	@Test
	void constructsOutsideScopeAttachesNothing() {
		assertNull(ParentStack.current());
		Probe probe = new Probe();
		assertNull(probe.element().parent);
		assertEquals(0, ParentStack.size());
	}

	@Test
	void explicitComponentCallDoesNotDuplicate() {
		Table root = new Table();
		ParentStack.push(root);
		Probe probe = new Probe();
		ParentStack.attachToParent(ParentStack.isolate(probe::element));
		ParentStack.pop();
		assertEquals(1, root.getChildren().size);
		assertTrue(root.getChildren().contains(probe.element(), true));
	}

	@Test
	void preservesChildOrderWhenInterleavedWithDivider() {
		final Probe[] messageList = new Probe[1];
		final Element[] divider = new Element[1];
		final Probe[] inputView = new Probe[1];

		var col = Ui.column().grow().gap(4f).children(() -> {
			messageList[0] = new Probe();
			var div = Ui.divider();
			divider[0] = div.element();
			inputView[0] = new Probe();
		});

		assertEquals(3, col.table().getChildren().size);
		assertSame(messageList[0].element(), col.table().getChildren().get(0), "Message list should be at index 0");
		assertSame(divider[0], col.table().getChildren().get(1), "Divider should be at index 1");
		assertSame(inputView[0].element(), col.table().getChildren().get(2), "Input view should be at index 2");
	}

	@Test
	void preservesChildOrderWithMultipleInterleavedElements() {
		final Probe[] comp1 = new Probe[1];
		final Element[] el1 = new Element[1];
		final Probe[] comp2 = new Probe[1];
		final Element[] el2 = new Element[1];

		var col = Ui.column().children(() -> {
			el1[0] = Ui.spacer();
			comp1[0] = new Probe();
			el2[0] = Ui.divider().element();
			comp2[0] = new Probe();
		});

		assertEquals(4, col.table().getChildren().size);
		assertSame(el1[0], col.table().getChildren().get(0), "Spacer should be at index 0");
		assertSame(comp1[0].element(), col.table().getChildren().get(1), "Comp 1 should be at index 1");
		assertSame(el2[0], col.table().getChildren().get(2), "Divider should be at index 2");
		assertSame(comp2[0].element(), col.table().getChildren().get(3), "Comp 2 should be at index 3");
	}

	@Test
	void preservesChildOrderWithNestedLayout() {
		final Probe[] comp1 = new Probe[1];
		final Element[] rowElem = new Element[1];
		final Probe[] comp2 = new Probe[1];

		var col = Ui.column().children(() -> {
			comp1[0] = new Probe();
			var innerRow = Ui.row().children(() -> {
				Ui.spacer();
			});
			rowElem[0] = innerRow.table();
			comp2[0] = new Probe();
		});

		assertEquals(3, col.table().getChildren().size);
		assertSame(comp1[0].element(), col.table().getChildren().get(0), "Comp 1 should be at index 0");
		assertSame(rowElem[0], col.table().getChildren().get(1), "Row should be at index 1");
		assertSame(comp2[0].element(), col.table().getChildren().get(2), "Comp 2 should be at index 2");
	}
}
