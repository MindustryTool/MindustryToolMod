package solim.layout;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import arc.scene.style.TextureRegionDrawable;

class WrapTest {

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
		Wrap w = new Wrap();
		assertEquals("solim-wrap-table", w.table().name);
	}

	@Test
	void addAddsChildToTable() {
		Wrap w = new Wrap();
		Element first = new Element();
		Element second = new Element();

		w.add(first);
		w.add(second);

		assertEquals(2, w.table().getChildren().size);
		assertSame(first, w.table().getChildren().get(0));
		assertSame(second, w.table().getChildren().get(1));
	}

	@Test
	void nameModifierUpdatesTableName() {
		Wrap w = new Wrap();
		w.name("my-wrap");
		assertEquals("my-wrap", w.table().name);
	}

	@Test
	void tableIsSameAsElement() {
		Wrap w = new Wrap();
		assertSame(w.table(), w.element());
	}

	@Test
	void gapSetsDefaultCellPadding() {
		Wrap w = new Wrap();
		w.gap(8f);
		assertEquals(8f, w.gap());
		Cell<?> def = w.table().defaults();
		assertEquals(8f, CellAccess.padRight(def));
		assertEquals(8f, CellAccess.padBottom(def));
	}

	@Test
	void leftAlignsTable() {
		Wrap w = new Wrap();
		w.left();
		// Just verify it doesn't throw
	}

	@Test
	void centerAlignsTable() {
		Wrap w = new Wrap();
		w.center();
		// Just verify it doesn't throw
	}

	@Test
	void rightAlignsTable() {
		Wrap w = new Wrap();
		w.right();
		// Just verify it doesn't throw
	}

	@Test
	void backgroundSetsDrawable() {
		Wrap w = new Wrap();
		assertNull(w.table().getBackground());
		w.background(new TextureRegionDrawable());
		assertNotNull(w.table().getBackground());
	}

	@Test
	void paddingSetsTablePadding() {
		Wrap w = new Wrap();
		w.padding(12f);
		// Just verify it doesn't throw
	}

	@Test
	void childrenAddsElementsToTable() {
		Wrap w = new Wrap();
		w.add(new Element());
		w.add(new Element());
		assertEquals(2, w.table().getChildren().size);
	}

	@Test
	void defaultGapIs4() {
		Wrap w = new Wrap();
		assertEquals(4f, w.gap());
	}
}
