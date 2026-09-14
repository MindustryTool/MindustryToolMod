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
import solim.runtime.ParentStack;
import solim.runtime.SignalDispatcher;

class GridTest {

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
		Grid g = new Grid();
		assertEquals("solim-grid-table", g.table().name);
	}

	@Test
	void defaultColumnCountIsOne() {
		Grid g = new Grid();
		Element a = new Element();
		Element b = new Element();
		g.add(a);
		g.add(b);
		assertEquals(2, g.table().getChildren().size);
	}

	@Test
	void wrapsChildrenAfterColumnCount() {
		Grid g = new Grid(2);
		Element a = new Element();
		Element b = new Element();
		Element c = new Element();

		g.add(a);
		g.add(b);
		g.add(c);

		assertEquals(3, g.table().getChildren().size);
	}

	@Test
	void gapAppliesDirectionalSpacing() {
		Grid g = new Grid(2);
		g.gap(16f);
		Element a = new Element();
		Element b = new Element();
		Element c = new Element();
		g.add(a);
		g.add(b);
		g.add(c);

		assertEquals(0f, CellAccess.padLeft(g.table().getCell(a)), 0.01f);
		assertEquals(0f, CellAccess.padTop(g.table().getCell(a)), 0.01f);
		assertEquals(16f, CellAccess.padLeft(g.table().getCell(b)), 0.01f);
		assertEquals(0f, CellAccess.padTop(g.table().getCell(b)), 0.01f);
		assertEquals(0f, CellAccess.padLeft(g.table().getCell(c)), 0.01f);
		assertEquals(16f, CellAccess.padTop(g.table().getCell(c)), 0.01f);
	}

	@Test
	void columnsReflowsExistingChildren() {
		Grid g = new Grid(2);
		Element a = new Element();
		Element b = new Element();
		Element c = new Element();
		g.add(a);
		g.add(b);
		g.add(c);
		assertEquals(3, g.table().getChildren().size);

		g.columns(4);
		assertEquals(3, g.table().getChildren().size);
		assertSame(a, g.table().getChildren().get(0));
		assertSame(b, g.table().getChildren().get(1));
		assertSame(c, g.table().getChildren().get(2));
	}

	@Test
	void negativeColumnCountNormalizedToOne() {
		Grid g = new Grid(-1);
		Element a = new Element();
		Element b = new Element();
		g.add(a);
		g.add(b);
		assertEquals(2, g.table().getChildren().size);
	}

	@Test
	void zeroColumnCountNormalizedToOne() {
		Grid g = new Grid(0);
		Element a = new Element();
		Element b = new Element();
		g.add(a);
		g.add(b);
		assertEquals(2, g.table().getChildren().size);
	}

	@Test
	void childrenRunnableAddsElementsInOrder() {
		Grid g = new Grid(2);
		Element first = new Element();
		Element second = new Element();

		g.children(() -> {
			ParentStack.add(first);
			ParentStack.add(second);
		});

		assertEquals(2, g.table().getChildren().size);
		assertSame(first, g.table().getChildren().get(0));
		assertSame(second, g.table().getChildren().get(1));
	}

	@Test
	void reactiveGapUpdatesCellSpacing() {
		Signal<Float> gap = Signal.of(8f);
		Grid g = new Grid(2);
		g.gap(gap);
		Element a = new Element();
		Element b = new Element();
		g.add(a);
		g.add(b);

		assertEquals(8f, CellAccess.padLeft(g.table().getCell(b)), 0.01f);

		gap.set(16f);
		SignalDispatcher.flush();
		assertEquals(16f, CellAccess.padLeft(g.table().getCell(b)), 0.01f);
	}

	@Test
	void tableIsSameAsElement() {
		Grid g = new Grid();
		assertSame(g.table(), g.element());
	}

	@Test
	void sizeConstraintsReturnsNonNull() {
		Grid g = new Grid();
		assertNotNull(g.sizeConstraints());
	}

	@Test
	void fluentApiReturnsSameGrid() {
		Grid g = new Grid();
		assertSame(g, g.columns(3));
		assertSame(g, g.gap(8f));
		assertSame(g, g.name("test"));
	}
}
