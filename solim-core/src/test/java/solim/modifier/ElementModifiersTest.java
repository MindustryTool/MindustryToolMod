package solim.modifier;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.Color;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.graphics.RoundedDrawable;
import solim.input.Button;
import solim.layout.Card;
import solim.layout.Column;
import solim.layout.Divider;
import solim.layout.Grid;
import solim.layout.Row;
import solim.layout.Scroll;
import solim.layout.SolimStack;
import solim.layout.Spacer;
import solim.layout.Wrap;
import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;

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
    void elementConfigModifiersOnRow() {
        Row row = new Row();
        row.width(100f).height(50f).x(10f).y(20f).visible(false).opacity(0.8f);

        assertEquals(100f, row.element().getWidth(), 0.01f);
        assertEquals(50f, row.element().getHeight(), 0.01f);
        assertEquals(10f, row.element().x, 0.01f);
        assertEquals(20f, row.element().y, 0.01f);
        assertFalse(row.element().visible);
        assertEquals(0.8f, row.element().color.a, 0.01f);

        row.size(80f);
        assertEquals(80f, row.element().getWidth(), 0.01f);
        assertEquals(80f, row.element().getHeight(), 0.01f);

        row.position(30f, 40f);
        assertEquals(30f, row.element().x, 0.01f);
        assertEquals(40f, row.element().y, 0.01f);
    }

    @Test
    void tableConfigPaddingOnRow() {
        Row row = new Row();
        row.padding(10f);
        assertEquals(10f, row.table().getMarginLeft(), 0.01f);
        assertEquals(10f, row.table().getMarginRight(), 0.01f);
        assertEquals(10f, row.table().getMarginTop(), 0.01f);
        assertEquals(10f, row.table().getMarginBottom(), 0.01f);

        row.padding(1f, 2f, 3f, 4f);
        assertEquals(1f, row.table().getMarginTop(), 0.01f);
        assertEquals(2f, row.table().getMarginLeft(), 0.01f);
        assertEquals(3f, row.table().getMarginBottom(), 0.01f);
        assertEquals(4f, row.table().getMarginRight(), 0.01f);

        row.paddingX(12f).paddingY(6f);
        assertEquals(12f, row.table().getMarginLeft(), 0.01f);
        assertEquals(12f, row.table().getMarginRight(), 0.01f);
        assertEquals(6f, row.table().getMarginTop(), 0.01f);
        assertEquals(6f, row.table().getMarginBottom(), 0.01f);
    }

    @Test
    void cellConfigMarginOnRow() {
        Table parent = new Table();
        Row row = new Row();
        row.margin(8f);
        Cell<?> cell = parent.add(row.element());
        row.cellConfig().applyToCell(cell);
        assertEquals(8f, CellAccess.padLeft(cell), 0.01f);
        assertEquals(8f, CellAccess.padRight(cell), 0.01f);
        assertEquals(8f, CellAccess.padTop(cell), 0.01f);
        assertEquals(8f, CellAccess.padBottom(cell), 0.01f);

        row.marginX(14f).marginY(8f);
        row.cellConfig().applyToCell(cell);
        assertEquals(14f, CellAccess.padLeft(cell), 0.01f);
        assertEquals(14f, CellAccess.padRight(cell), 0.01f);
        assertEquals(8f, CellAccess.padTop(cell), 0.01f);
        assertEquals(8f, CellAccess.padBottom(cell), 0.01f);

        row.marginTop(12f).marginLeft(14f).marginBottom(16f).marginRight(18f);
        row.cellConfig().applyToCell(cell);
        assertEquals(12f, CellAccess.padTop(cell), 0.01f);
        assertEquals(14f, CellAccess.padLeft(cell), 0.01f);
        assertEquals(16f, CellAccess.padBottom(cell), 0.01f);
        assertEquals(18f, CellAccess.padRight(cell), 0.01f);
    }

    @Test
    void elementConfigStylingOnRow() {
        Row row = new Row();
        row.rounded(12, Color.royal).border(2f, Color.gold);

        assertTrue(row.table().getBackground() instanceof RoundedDrawable);
        RoundedDrawable rd = (RoundedDrawable) row.table().getBackground();
        assertEquals(12, rd.getRadius());
        assertEquals(Color.royal, rd.getFillColor());
        assertEquals(2f, rd.getStroke(), 0.01f);
        assertEquals(Color.gold, rd.getBorderColor());
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

    @Test
    void colorDrawableDrawsCornerBased() {
        RoundedHelper.ColorDrawable cd = new RoundedHelper.ColorDrawable(Color.red);
        assertDoesNotThrow(() -> cd.draw(10f, 20f, 100f, 50f));
        assertDoesNotThrow(() -> cd.draw(10f, 20f, 50f, 25f, 100f, 50f, 1f, 1f, 0f));
    }

    @Test
    void backgroundColorModifierOnColumn() {
        Column column = new Column().backgroundColor(Color.crimson);
        assertTrue(column.table().getBackground() instanceof RoundedDrawable);
        RoundedDrawable rd = (RoundedDrawable) column.table().getBackground();
        assertEquals(Color.crimson, rd.getFillColor(), "backgroundColor modifier must set fillColor on RoundedDrawable");
    }

    @Test
    void cellSizeConstraintsAppliedToAttachedElement() {
        Row parent = new Row();
        Button button = new Button();
        parent.table().add(button.element());

        Cell<?> cell = parent.table().getCell(button.element());
        assertNotNull(cell);

        button.minWidth(64f).minHeight(32f).maxWidth(128f).maxHeight(96f);

        assertEquals(64f, CellAccess.minWidth(cell), 0.01f);
        assertEquals(32f, CellAccess.minHeight(cell), 0.01f);
        assertEquals(128f, CellAccess.maxWidth(cell), 0.01f);
        assertEquals(96f, CellAccess.maxHeight(cell), 0.01f);
    }

    @Test
    void reactiveCellSizeConstraintsAppliedToAttachedElement() {
        Row parent = new Row();
        Button button = new Button();
        parent.table().add(button.element());

        Cell<?> cell = parent.table().getCell(button.element());
        assertNotNull(cell);

        Signal<Float> minWidthSig = Signal.of(50f);
        Signal<Float> minHeightSig = Signal.of(30f);
        Signal<Float> maxWidthSig = Signal.of(100f);
        Signal<Float> maxHeightSig = Signal.of(80f);
        button.minWidth(minWidthSig).minHeight(minHeightSig).maxWidth(maxWidthSig).maxHeight(maxHeightSig);

        assertEquals(50f, CellAccess.minWidth(cell), 0.01f);
        assertEquals(30f, CellAccess.minHeight(cell), 0.01f);
        assertEquals(100f, CellAccess.maxWidth(cell), 0.01f);
        assertEquals(80f, CellAccess.maxHeight(cell), 0.01f);

        minWidthSig.set(75f);
        minHeightSig.set(45f);
        maxWidthSig.set(150f);
        maxHeightSig.set(120f);
        SignalDispatcher.flush();

        assertEquals(75f, CellAccess.minWidth(cell), 0.01f);
        assertEquals(45f, CellAccess.minHeight(cell), 0.01f);
        assertEquals(150f, CellAccess.maxWidth(cell), 0.01f);
        assertEquals(120f, CellAccess.maxHeight(cell), 0.01f);
    }
}
