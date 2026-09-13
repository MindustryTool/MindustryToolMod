package solim.modifier;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.input.Button;
import solim.layout.Card;
import solim.layout.Column;
import solim.layout.Container;
import solim.layout.Divider;
import solim.layout.Grid;
import solim.layout.Row;
import solim.layout.Scroll;
import solim.layout.SolimStack;
import solim.layout.Spacer;
import solim.layout.Wrap;

class ElementModifiersTest {

    @BeforeAll
    static void checkArcContext() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
    }

    @Test
    void elementModifiersGapOnTable() {
        Table table = new Table();
        Element a = new Element();
        Element b = new Element();
        table.add(a);
        table.add(b);
        ElementModifiers.gap(table, 16f);
        Cell<?> cellA = table.getCell(a);
        Cell<?> cellB = table.getCell(b);
        assertEquals(0f, CellAccess.padLeft(cellA), 0.01f);
        assertEquals(0f, CellAccess.padTop(cellA), 0.01f);
        assertEquals(16f, CellAccess.padLeft(cellB), 0.01f);
        assertEquals(0f, CellAccess.padTop(cellB), 0.01f);

        ElementModifiers.gap(table, 24f);
        assertEquals(0f, CellAccess.padLeft(cellA), 0.01f);
        assertEquals(24f, CellAccess.padLeft(cellB), 0.01f);
    }

    @Test
    void elementModifiersGapOnElementOverload() {
        Table table = new Table();
        Element a = new Element();
        Element b = new Element();
        table.add(a);
        table.add(b);
        ElementModifiers.gap((Element) table, 20f);
        Cell<?> cellA = table.getCell(a);
        Cell<?> cellB = table.getCell(b);
        assertEquals(0f, CellAccess.padLeft(cellA), 0.01f);
        assertEquals(20f, CellAccess.padLeft(cellB), 0.01f);

        Element element = new Element();
        assertDoesNotThrow(() -> ElementModifiers.gap(element, 20f));
        assertDoesNotThrow(() -> ElementModifiers.gap((Table) null, 20f));
        assertDoesNotThrow(() -> ElementModifiers.gap((Element) null, 20f));
    }

    @Test
    void componentGapDelegation() {
        Row row = new Row().gap(14f);
        Element ra = new Element();
        Element rb = new Element();
        row.add(ra);
        row.add(rb);
        assertEquals(0f, CellAccess.padLeft(row.table().getCell(ra)), 0.01f);
        assertEquals(14f, CellAccess.padLeft(row.table().getCell(rb)), 0.01f);
        assertEquals(0f, CellAccess.padTop(row.table().getCell(rb)), 0.01f);

        Column column = new Column().gap(18f);
        Element ca = new Element();
        Element cb = new Element();
        column.add(ca);
        column.add(cb);
        assertEquals(0f, CellAccess.padTop(column.table().getCell(ca)), 0.01f);
        assertEquals(18f, CellAccess.padTop(column.table().getCell(cb)), 0.01f);
        assertEquals(0f, CellAccess.padLeft(column.table().getCell(cb)), 0.01f);

        Grid grid = new Grid(2).gap(10f);
        Element ga = new Element();
        Element gb = new Element();
        Element gc = new Element();
        grid.add(ga);
        grid.add(gb);
        grid.add(gc);
        assertEquals(0f, CellAccess.padLeft(grid.table().getCell(ga)), 0.01f);
        assertEquals(10f, CellAccess.padLeft(grid.table().getCell(gb)), 0.01f);
        assertEquals(10f, CellAccess.padTop(grid.table().getCell(gc)), 0.01f);

        Wrap wrap = new Wrap().gap(12f);
        Element wa = new Element();
        Element wb = new Element();
        wrap.add(wa);
        wrap.add(wb);
        assertEquals(0f, CellAccess.padLeft(wrap.table().getCell(wa)), 0.01f);
        assertEquals(12f, CellAccess.padRight(wrap.table().getCell(wa)), 0.01f);
        assertEquals(12f, CellAccess.padBottom(wrap.table().getCell(wa)), 0.01f);
        assertEquals(0f, CellAccess.padLeft(wrap.table().getCell(wb)), 0.01f);
        assertEquals(12f, CellAccess.padRight(wrap.table().getCell(wb)), 0.01f);
        assertEquals(12f, CellAccess.padBottom(wrap.table().getCell(wb)), 0.01f);

        Card card = new Card().gap(16f);
        Element cda = new Element();
        Element cdb = new Element();
        card.container().add(cda);
        card.container().add(cdb);
        card.respace();
        assertEquals(0f, CellAccess.padTop(card.container().getCell(cda)), 0.01f);
        assertEquals(16f, CellAccess.padTop(card.container().getCell(cdb)), 0.01f);

        Button button = new Button().gap(8f);
        Element bta = new Element();
        Element btb = new Element();
        button.sizedButton().add(bta);
        button.sizedButton().add(btb);
        button.respace();
        assertEquals(0f, CellAccess.padLeft(button.sizedButton().getCell(bta)), 0.01f);
        assertEquals(8f, CellAccess.padLeft(button.sizedButton().getCell(btb)), 0.01f);
        assertEquals(0f, CellAccess.padTop(button.sizedButton().getCell(btb)), 0.01f);
    }

    @Test
    void elementModifiersNameOnElement() {
        Element element = new Element();
        ElementModifiers.name(element, "test-element");
        assertEquals("test-element", element.name);

        assertDoesNotThrow(() -> ElementModifiers.name(null, "ignored"));
    }

    @Test
    void elementModifiersNullSafe() {
        assertDoesNotThrow(() -> ElementModifiers.width(null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.height(null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.size(null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.size(null, 10f, 10f));
        assertDoesNotThrow(() -> ElementModifiers.x(null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.y(null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.position(null, 10f, 10f));
        assertDoesNotThrow(() -> ElementModifiers.visible(null, true));
        assertDoesNotThrow(() -> ElementModifiers.align(null, 0));
        assertDoesNotThrow(() -> ElementModifiers.top(null));
        assertDoesNotThrow(() -> ElementModifiers.bottom(null));
        assertDoesNotThrow(() -> ElementModifiers.left(null));
        assertDoesNotThrow(() -> ElementModifiers.right(null));
        assertDoesNotThrow(() -> ElementModifiers.center(null));
        assertDoesNotThrow(() -> ElementModifiers.margin((Table) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.padding((Table) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.margin((Element) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.margin((Element) null, 1f, 2f, 3f, 4f));
        assertDoesNotThrow(() -> ElementModifiers.marginTop((Element) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.marginBottom((Element) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.marginLeft((Element) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.marginRight((Element) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.padding((Element) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.padding((Element) null, 1f, 2f, 3f, 4f));
        assertDoesNotThrow(() -> ElementModifiers.paddingTop((Element) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.paddingBottom((Element) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.paddingLeft((Element) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.paddingRight((Element) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.paddingX((Table) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.paddingY((Table) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.marginX((Table) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.marginY((Table) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.paddingX((Element) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.paddingY((Element) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.marginX((Element) null, 10f));
        assertDoesNotThrow(() -> ElementModifiers.marginY((Element) null, 10f));
    }

    @Test
    void elementModifiersPaddingAndMarginOnElementInTable() {
        Table table = new Table();
        Element element = new Element();
        table.add(element);

        ElementModifiers.padding(element, 10f);
        assertEquals(10f, CellAccess.padTop(table.getCell(element)), 0.01f);
        assertEquals(10f, CellAccess.padLeft(table.getCell(element)), 0.01f);
        assertEquals(10f, CellAccess.padBottom(table.getCell(element)), 0.01f);
        assertEquals(10f, CellAccess.padRight(table.getCell(element)), 0.01f);

        ElementModifiers.padding(element, 1f, 2f, 3f, 4f);
        assertEquals(1f, CellAccess.padTop(table.getCell(element)), 0.01f);
        assertEquals(2f, CellAccess.padLeft(table.getCell(element)), 0.01f);
        assertEquals(3f, CellAccess.padBottom(table.getCell(element)), 0.01f);
        assertEquals(4f, CellAccess.padRight(table.getCell(element)), 0.01f);

        ElementModifiers.margin(element, 8f);
        assertEquals(8f, CellAccess.padTop(table.getCell(element)), 0.01f);
        assertEquals(8f, CellAccess.padLeft(table.getCell(element)), 0.01f);
        assertEquals(8f, CellAccess.padBottom(table.getCell(element)), 0.01f);
        assertEquals(8f, CellAccess.padRight(table.getCell(element)), 0.01f);

        ElementModifiers.marginTop(element, 12f);
        assertEquals(12f, CellAccess.padTop(table.getCell(element)), 0.01f);
        ElementModifiers.marginLeft(element, 14f);
        assertEquals(14f, CellAccess.padLeft(table.getCell(element)), 0.01f);
        ElementModifiers.marginBottom(element, 16f);
        assertEquals(16f, CellAccess.padBottom(table.getCell(element)), 0.01f);
        ElementModifiers.marginRight(element, 18f);
        assertEquals(18f, CellAccess.padRight(table.getCell(element)), 0.01f);
    }

    @Test
    void elementModifiersTwoAxisPaddingAndMargin() {
        Table table = new Table();
        ElementModifiers.paddingX(table, 12f);
        ElementModifiers.paddingY(table, 6f);
        assertEquals(12f, table.getMarginLeft(), 0.01f);
        assertEquals(12f, table.getMarginRight(), 0.01f);
        assertEquals(6f, table.getMarginTop(), 0.01f);
        assertEquals(6f, table.getMarginBottom(), 0.01f);

        ElementModifiers.marginX(table, 14f);
        ElementModifiers.marginY(table, 8f);
        assertEquals(14f, table.getMarginLeft(), 0.01f);
        assertEquals(14f, table.getMarginRight(), 0.01f);
        assertEquals(8f, table.getMarginTop(), 0.01f);
        assertEquals(8f, table.getMarginBottom(), 0.01f);

        Element element = new Element();
        table.add(element);

        ElementModifiers.paddingX(element, 15f);
        ElementModifiers.paddingY(element, 7f);
        assertEquals(15f, CellAccess.padLeft(table.getCell(element)), 0.01f);
        assertEquals(15f, CellAccess.padRight(table.getCell(element)), 0.01f);
        assertEquals(7f, CellAccess.padTop(table.getCell(element)), 0.01f);
        assertEquals(7f, CellAccess.padBottom(table.getCell(element)), 0.01f);

        ElementModifiers.marginX(element, 20f);
        ElementModifiers.marginY(element, 10f);
        assertEquals(20f, CellAccess.padLeft(table.getCell(element)), 0.01f);
        assertEquals(20f, CellAccess.padRight(table.getCell(element)), 0.01f);
        assertEquals(10f, CellAccess.padTop(table.getCell(element)), 0.01f);
        assertEquals(10f, CellAccess.padBottom(table.getCell(element)), 0.01f);
    }

    @Test
    void componentNameDelegationAndChaining() {
        Row row = new Row().name("my-row").gap(8f);
        assertEquals("my-row", row.element().name);

        Column column = new Column().name("my-column").gap(8f);
        assertEquals("my-column", column.element().name);

        Card card = new Card().name("my-card").gap(8f);
        assertEquals("my-card", card.element().name);

        Scroll scroll = new Scroll().name("my-scroll");
        assertEquals("my-scroll", scroll.element().name);

        Grid grid = new Grid().name("my-grid").gap(8f);
        assertEquals("my-grid", grid.element().name);

        Container container = new Container().name("my-container");
        assertEquals("my-container", container.element().name);

        Divider divider = new Divider().name("my-divider");
        assertEquals("my-divider", divider.element().name);

        Spacer spacer = new Spacer().name("my-spacer");
        assertEquals("my-spacer", spacer.element().name);

        SolimStack stack = new SolimStack().name("my-stack");
        assertEquals("my-stack", stack.element().name);

        Wrap wrap = new Wrap().name("my-wrap").gap(8f);
        assertEquals("my-wrap", wrap.element().name);

        Button button = new Button().name("my-button").gap(8f);
        assertEquals("my-button", button.element().name);
    }
}
