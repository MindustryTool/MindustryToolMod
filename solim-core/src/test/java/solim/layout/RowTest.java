package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;

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
}
