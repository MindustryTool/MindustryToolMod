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
import solim.modifier.ElementModifiers;
import solim.runtime.SignalDispatcher;
import solim.signal.Signal;
import solim.ui.Dynamic;
import solim.ui.Ui;

class GapCoexistenceTest {

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
	void childMarginAndContainerGapCoexistAdditivelyInRow() {
		Row r = Ui.row().gap(12f);
		Card card1 = Ui.card().margin(5f, 10f, 5f, 10f); // top 5, left 10, bottom 5, right 10
		Card card2 = Ui.card().margin(6f, 15f, 6f, 20f); // top 6, left 15, bottom 6, right 20
		Card card3 = Ui.card(); // no margin

		r.children(() -> {
			solim.runtime.ParentStack.add(card1);
			solim.runtime.ParentStack.add(card2);
			solim.runtime.ParentStack.add(card3);
		});

		Cell<?> cell1 = r.table().getCell(card1.element());
		Cell<?> cell2 = r.table().getCell(card2.element());
		Cell<?> cell3 = r.table().getCell(card3.element());

		// First visible element receives 0 gap + child's own margin
		assertEquals(10f, CellAccess.padLeft(cell1), 0.01f);
		assertEquals(10f, CellAccess.padRight(cell1), 0.01f);
		assertEquals(5f, CellAccess.padTop(cell1), 0.01f);
		assertEquals(5f, CellAccess.padBottom(cell1), 0.01f);

		// Subsequent visible element receives gap (12) + child's own margin (15) = 27
		assertEquals(27f, CellAccess.padLeft(cell2), 0.01f);
		assertEquals(20f, CellAccess.padRight(cell2), 0.01f);
		assertEquals(6f, CellAccess.padTop(cell2), 0.01f);
		assertEquals(6f, CellAccess.padBottom(cell2), 0.01f);

		// Third element receives gap (12) + 0 margin = 12
		assertEquals(12f, CellAccess.padLeft(cell3), 0.01f);
		assertEquals(0f, CellAccess.padRight(cell3), 0.01f);
		assertEquals(0f, CellAccess.padTop(cell3), 0.01f);
		assertEquals(0f, CellAccess.padBottom(cell3), 0.01f);
	}

	@Test
	void childMarginAndContainerGapCoexistAdditivelyInColumn() {
		Column col = Ui.column().gap(14f);
		Card card1 = Ui.card().margin(8f, 4f, 4f, 4f);
		Card card2 = Ui.card().margin(6f, 4f, 4f, 4f);
		Card card3 = Ui.card();

		col.children(() -> {
			solim.runtime.ParentStack.add(card1);
			solim.runtime.ParentStack.add(card2);
			solim.runtime.ParentStack.add(card3);
		});

		Cell<?> cell1 = col.table().getCell(card1.element());
		Cell<?> cell2 = col.table().getCell(card2.element());
		Cell<?> cell3 = col.table().getCell(card3.element());

		// First visible element receives 0 gap + top margin = 8
		assertEquals(8f, CellAccess.padTop(cell1), 0.01f);
		assertEquals(4f, CellAccess.padLeft(cell1), 0.01f);

		// Second visible element receives gap (14) + top margin (6) = 20
		assertEquals(20f, CellAccess.padTop(cell2), 0.01f);
		assertEquals(4f, CellAccess.padLeft(cell2), 0.01f);

		// Third visible element receives gap (14) + 0 top margin = 14
		assertEquals(14f, CellAccess.padTop(cell3), 0.01f);
		assertEquals(0f, CellAccess.padLeft(cell3), 0.01f);
	}

	@Test
	void visibilityCollapsePromotesNextVisibleSiblingToZeroLeadingGap() {
		Row row = Ui.row().gap(16f);
		Card cardA = Ui.card();
		Card cardB = Ui.card();
		Card cardC = Ui.card();

		row.children(() -> {
			solim.runtime.ParentStack.add(cardA);
			solim.runtime.ParentStack.add(cardB);
			solim.runtime.ParentStack.add(cardC);
		});

		Cell<?> cellA = row.table().getCell(cardA.element());
		Cell<?> cellB = row.table().getCell(cardB.element());
		Cell<?> cellC = row.table().getCell(cardC.element());

		// Initial: A is first (0), B is second (16), C is third (16)
		assertEquals(0f, CellAccess.padLeft(cellA), 0.01f);
		assertEquals(16f, CellAccess.padLeft(cellB), 0.01f);
		assertEquals(16f, CellAccess.padLeft(cellC), 0.01f);

		// Collapse cardA
		ElementModifiers.visible(cardA.element(), false);
		assertEquals(0f, CellAccess.padLeft(cellA), 0.01f);
		// cardB is promoted to first visible: padLeft becomes 0
		assertEquals(0f, CellAccess.padLeft(cellB), 0.01f);
		// cardC is now second visible: padLeft remains 16
		assertEquals(16f, CellAccess.padLeft(cellC), 0.01f);

		// Collapse cardB as well
		ElementModifiers.visible(cardB.element(), false);
		assertEquals(0f, CellAccess.padLeft(cellA), 0.01f);
		assertEquals(0f, CellAccess.padLeft(cellB), 0.01f);
		// cardC is now first visible: padLeft becomes 0
		assertEquals(0f, CellAccess.padLeft(cellC), 0.01f);

		// Re-expand cardA: cardA is first visible (0), cardC is second visible (16)
		ElementModifiers.visible(cardA.element(), true);
		assertEquals(0f, CellAccess.padLeft(cellA), 0.01f);
		assertEquals(0f, CellAccess.padLeft(cellB), 0.01f); // still invisible
		assertEquals(16f, CellAccess.padLeft(cellC), 0.01f);
	}

	@Test
	void dynamicComponentCollapseAndExpandRespacesSiblingsWithoutGhostGaps() {
		Signal<Boolean> showItem = Signal.of(true);

		Row row = Ui.row().gap(20f);
		Card prefix = Ui.card();
		Dynamic<Boolean> dynamicCard = Dynamic.of(showItem, visible -> {
			if (visible) {
				return Ui.card();
			}
			return null;
		});
		Card suffix = Ui.card();

		row.children(() -> {
			solim.runtime.ParentStack.add(prefix);
			solim.runtime.ParentStack.add(dynamicCard);
			solim.runtime.ParentStack.add(suffix);
		});

		Cell<?> prefixCell = row.table().getCell(prefix.element());
		Cell<?> dynCell = row.table().getCell(dynamicCard.element());
		Cell<?> suffixCell = row.table().getCell(suffix.element());

		// Initial: prefix (0), dyn (20), suffix (20)
		assertEquals(0f, CellAccess.padLeft(prefixCell), 0.01f);
		assertEquals(20f, CellAccess.padLeft(dynCell), 0.01f);
		assertEquals(20f, CellAccess.padLeft(suffixCell), 0.01f);

		// Collapse dynamic item: null return zeroes its cell pad and collapses
		showItem.set(false);
		SignalDispatcher.flush();

		assertEquals(0f, CellAccess.padLeft(prefixCell), 0.01f);
		assertEquals(0f, CellAccess.padLeft(dynCell), 0.01f);
		// suffix is now the 2nd visible element, gets single gap (20) without ghost gap
		assertEquals(20f, CellAccess.padLeft(suffixCell), 0.01f);

		// Collapse prefix as well
		ElementModifiers.visible(prefix.element(), false);
		// Now suffix is the ONLY visible element in row -> gets 0 leading gap!
		assertEquals(0f, CellAccess.padLeft(suffixCell), 0.01f);

		// Re-expand dynamic item
		showItem.set(true);
		SignalDispatcher.flush();
		// Dynamic item is now first visible (0), suffix is 2nd visible (20)
		assertEquals(0f, CellAccess.padLeft(dynCell), 0.01f);
		assertEquals(20f, CellAccess.padLeft(suffixCell), 0.01f);
	}
}
