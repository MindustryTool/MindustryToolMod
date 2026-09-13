package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.ui.layout.CellAccess;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;

class ColumnTest {

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
		Column col = new Column();
		assertEquals("solim-column-table", col.table().name);
	}

	@Test
	void preservesChildOrder() {
		Column col = new Column();
		Element first = new Element();
		Element second = new Element();
		Element third = new Element();

		col.add(first).row();
		col.add(second).row();
		col.add(third);

		assertSame(first, col.table().getChildren().get(0));
		assertSame(second, col.table().getChildren().get(1));
		assertSame(third, col.table().getChildren().get(2));
	}

	@Test
	void gapAppliesDirectionalSpacingToExistingCells() {
		Column col = new Column();
		Element a = new Element();
		Element b = new Element();
		col.add(a);
		col.add(b);

		col.gap(16f);

		assertEquals(0f, CellAccess.padTop(col.table().getCell(a)), 0.01f);
		assertEquals(16f, CellAccess.padTop(col.table().getCell(b)), 0.01f);
		assertEquals(0f, CellAccess.padLeft(col.table().getCell(b)), 0.01f);
		assertEquals(2, col.table().getChildren().size);
	}

	@Test
	void paddingPreservesChildrenAndReturnsSelf() {
		Column col = new Column();
		Element child = new Element();
		col.add(child);

		assertSame(col, col.padding(24f));
		assertSame(col, col.padding(1f, 2f, 3f, 4f));
		assertEquals(1, col.table().getChildren().size);
		assertSame(child, col.table().getChildren().get(0));
	}

	@Test
	void visibleModifierChangesTableVisibility() {
		Column col = new Column();

		col.visible(false);
		assertFalse(col.table().visible);

		col.visible(true);
		assertTrue(col.table().visible);
	}

	@Test
	void reactiveVisibleUpdatesTableVisibility() {
		Signal<Boolean> vis = Signal.of(true);
		Column col = new Column();
		col.visible(vis);

		assertTrue(col.table().visible);

		vis.set(false);
		solim.runtime.SignalDispatcher.flush();
		assertFalse(col.table().visible);

		vis.set(true);
		solim.runtime.SignalDispatcher.flush();
		assertTrue(col.table().visible);
	}

	@Test
	void positionSetsTableCoordinates() {
		Column col = new Column();

		col.x(10f);
		assertEquals(10f, col.table().x, 0.01f);

		col.y(20f);
		assertEquals(20f, col.table().y, 0.01f);

		col.position(30f, 40f);
		assertEquals(30f, col.table().x, 0.01f);
		assertEquals(40f, col.table().y, 0.01f);
	}

	@Test
	void nameModifierUpdatesTableName() {
		Column col = new Column();
		col.name("my-column");
		assertEquals("my-column", col.table().name);
	}

	@Test
	void childrenRunnableAddsElements() {
		Column col = new Column();
		Element child = new Element();

		col.children(() -> {
			solim.runtime.ParentStack.add(child);
		});

		assertEquals(1, col.table().getChildren().size);
		assertSame(child, col.table().getChildren().get(0));
	}

	@Test
	void fluentApiReturnsSameColumn() {
		Column col = new Column();

		assertSame(col, col.gap(8f));
		assertSame(col, col.padding(4f));
		assertSame(col, col.name("test"));
		assertSame(col, col.visible(true));
		assertSame(col, col.x(0f));
		assertSame(col, col.y(0f));
	}

	@Test
	void tableIsSameAsElement() {
		Column col = new Column();
		assertSame(col.table(), col.element());
	}

	@Test
	void sizeConstraintsReturnsNonNull() {
		Column col = new Column();
		assertNotNull(col.sizeConstraints());
	}
}
