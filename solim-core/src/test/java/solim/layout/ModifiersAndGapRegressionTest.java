package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.Test;
import solim.reactive.Signal;
import solim.runtime.ParentStack;
import solim.test.SolimEnv;
import solim.test.TestScheduler;

public class ModifiersAndGapRegressionTest extends SolimEnv {

	// ==========================================
	// Phase 14: Modifier Pipeline & Cell Config
	// ==========================================

	@Test
	void elementModifiersAppliedCorrectly() {
		Column col = new Column();
		col.width(120f).height(60f).visible(false).opacity(0.75f).touchable(Touchable.disabled);

		Element el = col.element();
		assertEquals(120f, el.getWidth(), 0.01f);
		assertEquals(60f, el.getHeight(), 0.01f);
		assertFalse(el.visible);
		assertEquals(0.75f, el.color.a, 0.01f);
		assertEquals(Touchable.disabled, el.touchable);
	}

	@Test
	void reactiveModifiersUpdateOnSignalChange() {
		Signal<Float> widthSig = Signal.of(100f);
		Signal<Boolean> visSig = Signal.of(true);

		Row row = new Row();
		row.width(widthSig).visible(visSig);

		assertEquals(100f, row.element().getWidth(), 0.01f);
		assertTrue(row.element().visible);

		widthSig.set(250f);
		visSig.set(false);
		TestScheduler.flush();

		assertEquals(250f, row.element().getWidth(), 0.01f);
		assertFalse(row.element().visible);

		row.dispose();
	}

	@Test
	void pendingCellConfigurationAppliedOnAttachmentWithSiblingIsolation() {
		Table parent = new Table();
		ParentStack.push(parent);

		// Child 1 with custom pad
		ParentStack.setCellConfigurator((cell, child, comp) -> {
			if (child.name != null && child.name.equals("c1")) {
				cell.pad(15f);
			}
		});

		Element c1 = new Element();
		c1.name = "c1";
		ParentStack.add(c1);

		// Child 2 without pad
		Element c2 = new Element();
		c2.name = "c2";
		ParentStack.add(c2);

		ParentStack.pop();

		assertEquals(15f, CellAccess.padTop(parent.getCell(c1)), 0.01f);
		assertEquals(0f, CellAccess.padTop(parent.getCell(c2)), 0.01f,
				"Cell configuration on Child 1 must not bleed into Child 2");
	}

	// ==========================================
	// Phase 15: GapContainer Dynamic Spacing
	// ==========================================

	@Test
	void gapContainerEmptyAndSingleChild() {
		Column emptyCol = new Column().gap(12f);
		assertEquals(0, emptyCol.table().getChildren().size);

		Column singleCol = new Column().gap(12f);
		Element singleEl = new Element();
		singleCol.table().add(singleEl);

		// With a single child, no gap should be applied before or after it
		assertEquals(0f, CellAccess.padTop(singleCol.table().getCell(singleEl)), 0.01f);
	}

	@Test
	void gapContainerMultipleChildrenAndVisibilityTransitions() {
		Column col = new Column().gap(10f);
		Table table = col.table();

		Element c1 = new Element();
		Element c2 = new Element();
		Element c3 = new Element();

		table.add(c1);
		table.add(c2);
		table.add(c3);

		// Initial state: all visible
		c1.visible = true;
		c2.visible = true;
		c3.visible = true;
		col.respace();

		assertEquals(0f, CellAccess.padTop(table.getCell(c1)), 0.01f, "First visible child has no gap");
		assertEquals(10f, CellAccess.padTop(table.getCell(c2)), 0.01f, "Second visible child has gap");
		assertEquals(10f, CellAccess.padTop(table.getCell(c3)), 0.01f, "Third visible child has gap");

		// Transition 1: Middle child (c2) hidden
		c2.visible = false;
		col.respace();

		assertEquals(0f, CellAccess.padTop(table.getCell(c1)), 0.01f);
		assertEquals(0f, CellAccess.padTop(table.getCell(c2)), 0.01f, "Hidden child has 0 gap");
		assertEquals(10f, CellAccess.padTop(table.getCell(c3)), 0.01f, "c3 becomes second visible child and receives gap");

		// Transition 2: First child (c1) also hidden -> only c3 visible
		c1.visible = false;
		col.respace();

		assertEquals(0f, CellAccess.padTop(table.getCell(c1)), 0.01f);
		assertEquals(0f, CellAccess.padTop(table.getCell(c2)), 0.01f);
		assertEquals(0f, CellAccess.padTop(table.getCell(c3)), 0.01f, "c3 is now the first visible child, receives 0 gap");

		// Transition 3: All hidden
		c3.visible = false;
		col.respace();

		assertEquals(0f, CellAccess.padTop(table.getCell(c1)), 0.01f);
		assertEquals(0f, CellAccess.padTop(table.getCell(c2)), 0.01f);
		assertEquals(0f, CellAccess.padTop(table.getCell(c3)), 0.01f);

		// Transition 4: Restore all visible
		c1.visible = true;
		c2.visible = true;
		c3.visible = true;
		col.respace();

		assertEquals(0f, CellAccess.padTop(table.getCell(c1)), 0.01f);
		assertEquals(10f, CellAccess.padTop(table.getCell(c2)), 0.01f);
		assertEquals(10f, CellAccess.padTop(table.getCell(c3)), 0.01f);

		col.dispose();
	}
}
