package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;
import solim.ui.Ui;

class RowTest {

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
	void createsTableWithDefaultName() {
		Row row = new Row();
		assertEquals("solim-row-table", row.table().name);
	}

	@Test
	void preservesChildOrder() {
		Row row = new Row();
		Element first = new Element();
		Element second = new Element();
		Element third = new Element();

		row.add(first);
		row.add(second);
		row.add(third);

		assertSame(first, row.table().getChildren().get(0));
		assertSame(second, row.table().getChildren().get(1));
		assertSame(third, row.table().getChildren().get(2));
	}

	@Test
	void gapSetsCellSpacing() {
		Row row = new Row();
		row.gap(8f);

		Element a = new Element();
		Element b = new Element();
		row.add(a);
		row.add(b);

		assertEquals(2, row.table().getChildren().size);
	}

	@Test
	void gapAppliesDirectionalSpacingToCells() {
		Row row = new Row();
		row.gap(8f);
		Element a = new Element();
		Element b = new Element();
		row.add(a);
		row.add(b);

		assertEquals(0f, arc.scene.ui.layout.CellAccess.padLeft(row.table().getCell(a)), 0.01f);
		assertEquals(8f, arc.scene.ui.layout.CellAccess.padLeft(row.table().getCell(b)), 0.01f);
		assertEquals(0f, arc.scene.ui.layout.CellAccess.padTop(row.table().getCell(b)), 0.01f);
	}

	@Test
	void paddingPreservesChildrenAndReturnsSelf() {
		Row row = new Row();
		Element child = new Element();
		row.add(child);

		assertSame(row, row.padding(12f));
		assertSame(row, row.padding(1f, 2f, 3f, 4f));
		assertEquals(1, row.table().getChildren().size);
		assertSame(child, row.table().getChildren().get(0));
	}

	@Test
	void visibleModifierChangesTableVisibility() {
		Row row = new Row();

		row.visible(false);
		assertFalse(row.table().visible);

		row.visible(true);
		assertTrue(row.table().visible);
	}

	@Test
	void reactiveVisibleUpdatesTableVisibility() {
		Signal<Boolean> vis = Signal.of(true);
		Row row = new Row();
		row.visible(vis);

		assertTrue(row.table().visible);

		vis.set(false);
		solim.runtime.SignalDispatcher.flush();
		assertFalse(row.table().visible);

		vis.set(true);
		solim.runtime.SignalDispatcher.flush();
		assertTrue(row.table().visible);
	}

	@Test
	void positionSetsTableCoordinates() {
		Row row = new Row();

		row.x(10f);
		assertEquals(10f, row.table().x, 0.01f);

		row.y(20f);
		assertEquals(20f, row.table().y, 0.01f);

		row.position(30f, 40f);
		assertEquals(30f, row.table().x, 0.01f);
		assertEquals(40f, row.table().y, 0.01f);
	}

	@Test
	void nameModifierUpdatesTableName() {
		Row row = new Row();
		row.name("my-row");
		assertEquals("my-row", row.table().name);
	}

	@Test
	void childrenRunnableAddsElements() {
		Row row = new Row();
		Element child = new Element();

		row.children(() -> {
			solim.runtime.ParentStack.add(child);
		});

		assertEquals(1, row.table().getChildren().size);
		assertSame(child, row.table().getChildren().get(0));
	}

	@Test
	void tableIsSameAsElement() {
		Row row = new Row();
		assertSame(row.table(), row.element());
	}

	@Test
	void sizeConstraintsReturnsNonNull() {
		Row row = new Row();
		assertNotNull(row.sizeConstraints());
	}

	@Test
	void childRowWithWidthActsAsSpacerInParentRow() {
		Row parent = new Row();
		Row child = new Row();
		parent.children(() -> {
			child.width(40f).minWidth(40f).children(() -> {});
		});

		assertEquals(1, parent.table().getChildren().size);
		assertSame(child.table(), parent.table().getChildren().get(0));
		arc.scene.ui.layout.Cell<?> cell = parent.table().getCell(child.table());
		assertNotNull(cell);
		assertEquals(40f, arc.scene.ui.layout.CellAccess.minWidth(cell), 0.01f);
	}

	@Test
	void testRowTopLeftAlignment() {
		arc.scene.ui.layout.Cell<?> testCell = new arc.scene.ui.layout.Cell<>();
		testCell.top().left();
		System.out.println("testCell after top left: " + arc.scene.ui.layout.CellAccess.align(testCell));
		testCell.center();
		System.out.println("testCell after center: " + arc.scene.ui.layout.CellAccess.align(testCell));
		Row row = new Row();
		row.top().left();
		row.table().defaults().top();
		Element e1 = new Element() {
			@Override public float getPrefWidth() { return 50f; }
			@Override public float getPrefHeight() { return 50f; }
		};
		Element e2 = new Element() {
			@Override public float getPrefWidth() { return 100f; }
			@Override public float getPrefHeight() { return 100f; }
		};
		row.children(() -> {
			solim.runtime.ParentStack.add(e1);
			solim.runtime.ParentStack.add(e2);
		});
		row.table().setSize(300f, 200f);
		row.table().layout();
		arc.scene.ui.layout.Cell<?> c1 = row.table().getCell(e1);
		arc.scene.ui.layout.Cell<?> c2 = row.table().getCell(e2);
		System.out.println("table align: " + row.table().getAlign());
		System.out.println("c1 align: " + arc.scene.ui.layout.CellAccess.align(c1) + ", y: " + e1.y + ", h: " + e1.getHeight() + ", x: " + e1.x);
		System.out.println("c2 align: " + arc.scene.ui.layout.CellAccess.align(c2) + ", y: " + e2.y + ", h: " + e2.getHeight() + ", x: " + e2.x);
	}

	@Test
	void testLandscapeLayoutWithScroll() {
		Element img = new Element() {
			@Override public float getPrefWidth() { return 120f; }
			@Override public float getPrefHeight() { return 80f; }
		};
		Element det = new Element() {
			@Override public float getPrefWidth() { return 200f; }
			@Override public float getPrefHeight() { return 250f; }
		};

		Scroll scroll = Ui.scroll().grow().children(() -> {
			Ui.row().grow().top().left().gap(8f).children(() -> {
				solim.runtime.ParentStack.add(img);
				solim.runtime.ParentStack.add(det);
			});
		});

		scroll.outer().setSize(600f, 400f);
		scroll.outer().layout();
		if (scroll.pane() != null) {
			scroll.pane().setSize(600f, 400f);
			scroll.pane().layout();
		}
		scroll.content().layout();

		Row row = (Row) scroll.content().getChildren().first().userObject;
		row.table().layout();

		System.out.println("scroll.outer: " + scroll.outer().getWidth() + "x" + scroll.outer().getHeight());
		System.out.println("scroll.content: " + scroll.content().getWidth() + "x" + scroll.content().getHeight());
		System.out.println("row.table: " + row.table().getWidth() + "x" + row.table().getHeight() + " at y=" + row.table().y);
		System.out.println("img: " + img.getWidth() + "x" + img.getHeight() + " at (" + img.x + ", " + img.y + ")");
		System.out.println("det: " + det.getWidth() + "x" + det.getHeight() + " at (" + det.x + ", " + det.y + ")");
	}
}
