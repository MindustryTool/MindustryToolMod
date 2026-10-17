package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static solim.modifier.Values.gap;
import static solim.modifier.Values.margin;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.visible;

import org.junit.jupiter.api.Test;

import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import solim.reactive.Dynamic;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class GapCoexistenceTest extends SolimEnv {

    @Test
    void childMarginAndContainerGapCoexistAdditivelyInRow() {
        Row r = new Row().props(gap(12f));
        Card card1 = new Card().props(margin(5f, 10f, 5f, 10f));
        Card card2 = new Card().props(margin(6f, 15f, 6f, 20f));
        Card card3 = new Card();

        r.children(() -> {
            AttachmentStack.add(card1);
            AttachmentStack.add(card2);
            AttachmentStack.add(card3);
        });

        Cell<?> cell1 = r.table().getCell(card1.element());
        Cell<?> cell2 = r.table().getCell(card2.element());
        Cell<?> cell3 = r.table().getCell(card3.element());

        assertEquals(10f, CellAccess.padLeft(cell1), 0.01f);
        assertEquals(10f, CellAccess.padRight(cell1), 0.01f);
        assertEquals(5f, CellAccess.padTop(cell1), 0.01f);
        assertEquals(5f, CellAccess.padBottom(cell1), 0.01f);

        assertEquals(27f, CellAccess.padLeft(cell2), 0.01f);
        assertEquals(20f, CellAccess.padRight(cell2), 0.01f);
        assertEquals(6f, CellAccess.padTop(cell2), 0.01f);
        assertEquals(6f, CellAccess.padBottom(cell2), 0.01f);

        assertEquals(12f, CellAccess.padLeft(cell3), 0.01f);
        assertEquals(0f, CellAccess.padRight(cell3), 0.01f);
        assertEquals(0f, CellAccess.padTop(cell3), 0.01f);
        assertEquals(0f, CellAccess.padBottom(cell3), 0.01f);
    }

    @Test
    void childMarginAndContainerGapCoexistAdditivelyInColumn() {
        Column col = new Column().props(gap(14f));
        Card card1 = new Card().props(margin(8f, 4f, 4f, 4f));
        Card card2 = new Card().props(margin(6f, 4f, 4f, 4f));
        Card card3 = new Card();

        col.children(() -> {
            AttachmentStack.add(card1);
            AttachmentStack.add(card2);
            AttachmentStack.add(card3);
        });

        Cell<?> cell1 = col.table().getCell(card1.element());
        Cell<?> cell2 = col.table().getCell(card2.element());
        Cell<?> cell3 = col.table().getCell(card3.element());

        assertEquals(8f, CellAccess.padTop(cell1), 0.01f);
        assertEquals(4f, CellAccess.padLeft(cell1), 0.01f);

        assertEquals(20f, CellAccess.padTop(cell2), 0.01f);
        assertEquals(4f, CellAccess.padLeft(cell2), 0.01f);

        assertEquals(14f, CellAccess.padTop(cell3), 0.01f);
        assertEquals(0f, CellAccess.padLeft(cell3), 0.01f);
    }

    @Test
    void visibilityCollapsePromotesNextVisibleSiblingToZeroLeadingGap() {
        Row row = new Row().props(gap(16f));
        Card cardA = new Card();
        Card cardB = new Card();
        Card cardC = new Card();

        row.children(() -> {
            AttachmentStack.add(cardA);
            AttachmentStack.add(cardB);
            AttachmentStack.add(cardC);
        });

        Cell<?> cellA = row.table().getCell(cardA.element());
        Cell<?> cellB = row.table().getCell(cardB.element());
        Cell<?> cellC = row.table().getCell(cardC.element());

        assertEquals(0f, CellAccess.padLeft(cellA), 0.01f);
        assertEquals(16f, CellAccess.padLeft(cellB), 0.01f);
        assertEquals(16f, CellAccess.padLeft(cellC), 0.01f);

        cardA.props(visible(false));
        assertEquals(0f, CellAccess.padLeft(cellA), 0.01f);

        assertEquals(0f, CellAccess.padLeft(cellB), 0.01f);

        assertEquals(16f, CellAccess.padLeft(cellC), 0.01f);

        cardB.props(visible(false));
        assertEquals(0f, CellAccess.padLeft(cellA), 0.01f);
        assertEquals(0f, CellAccess.padLeft(cellB), 0.01f);

        assertEquals(0f, CellAccess.padLeft(cellC), 0.01f);

        cardA.props(visible(true));
        assertEquals(0f, CellAccess.padLeft(cellA), 0.01f);
        assertEquals(0f, CellAccess.padLeft(cellB), 0.01f);
        assertEquals(16f, CellAccess.padLeft(cellC), 0.01f);
    }

    @Test
    void dynamicComponentCollapseAndExpandRespacesSiblingsWithoutGhostGaps() {
        Signal<Boolean> showItem = Signal.of(true);

        Row row = new Row().props(gap(20f));
        Card prefix = new Card();
        Dynamic<Boolean> dynamicCard = Dynamic.of(showItem, visible -> {
            if (visible) {
                new Card();
            }
        });
        Card suffix = new Card();

        row.children(() -> {
            AttachmentStack.add(prefix);
            AttachmentStack.add(dynamicCard);
            AttachmentStack.add(suffix);
        });

        Cell<?> prefixCell = row.table().getCell(prefix.element());
        Cell<?> dynCell = row.table().getCell(dynamicCard.element());
        Cell<?> suffixCell = row.table().getCell(suffix.element());

        assertEquals(0f, CellAccess.padLeft(prefixCell), 0.01f);
        assertEquals(20f, CellAccess.padLeft(dynCell), 0.01f);
        assertEquals(20f, CellAccess.padLeft(suffixCell), 0.01f);

        showItem.set(false);
        SignalDispatcher.flush();

        assertEquals(0f, CellAccess.padLeft(prefixCell), 0.01f);
        assertEquals(0f, CellAccess.padLeft(dynCell), 0.01f);

        assertEquals(20f, CellAccess.padLeft(suffixCell), 0.01f);

        prefix.props(visible(false));

        assertEquals(0f, CellAccess.padLeft(suffixCell), 0.01f);

        showItem.set(true);
        SignalDispatcher.flush();

        assertEquals(0f, CellAccess.padLeft(dynCell), 0.01f);
        assertEquals(20f, CellAccess.padLeft(suffixCell), 0.01f);
    }

    @Test
    void childMarginInsideRowColumnAndGridSetsParentCellPadding() {

        Row row = new Row();
        Card rowChild = new Card().props(margin(5f, 10f, 15f, 20f));
        row.children(() -> AttachmentStack.add(rowChild));
        Cell<?> rowCell = row.table().getCell(rowChild.element());
        assertEquals(5f, CellAccess.padTop(rowCell), 0.01f);
        assertEquals(10f, CellAccess.padLeft(rowCell), 0.01f);
        assertEquals(15f, CellAccess.padBottom(rowCell), 0.01f);
        assertEquals(20f, CellAccess.padRight(rowCell), 0.01f);

        Column col = new Column();
        Card colChild = new Card().props(margin(6f, 12f, 18f, 24f));
        col.children(() -> AttachmentStack.add(colChild));
        Cell<?> colCell = col.table().getCell(colChild.element());
        assertEquals(6f, CellAccess.padTop(colCell), 0.01f);
        assertEquals(12f, CellAccess.padLeft(colCell), 0.01f);
        assertEquals(18f, CellAccess.padBottom(colCell), 0.01f);
        assertEquals(24f, CellAccess.padRight(colCell), 0.01f);

        Grid grid = new Grid(2);
        Card gridChild = new Card().props(margin(7f, 14f, 21f, 28f));
        grid.children(() -> AttachmentStack.add(gridChild));
        Cell<?> gridCell = grid.table().getCell(gridChild.element());
        assertEquals(7f, CellAccess.padTop(gridCell), 0.01f);
        assertEquals(14f, CellAccess.padLeft(gridCell), 0.01f);
        assertEquals(21f, CellAccess.padBottom(gridCell), 0.01f);
        assertEquals(28f, CellAccess.padRight(gridCell), 0.01f);
    }

    @Test
    void marginAndPaddingCoexistOnContainer() {
        Row parent = new Row();
        Card card = new Card()
                .props(margin(8f, 12f, 16f, 20f), padding(4f, 6f, 8f, 10f));

        parent.children(() -> AttachmentStack.add(card));

        Cell<?> cell = parent.table().getCell(card.element());
        assertEquals(8f, CellAccess.padTop(cell), 0.01f);
        assertEquals(12f, CellAccess.padLeft(cell), 0.01f);
        assertEquals(16f, CellAccess.padBottom(cell), 0.01f);
        assertEquals(20f, CellAccess.padRight(cell), 0.01f);

        assertEquals(4f, card.container().getMarginTop(), 0.01f);
        assertEquals(6f, card.container().getMarginLeft(), 0.01f);
        assertEquals(8f, card.container().getMarginBottom(), 0.01f);
        assertEquals(10f, card.container().getMarginRight(), 0.01f);
    }
}
