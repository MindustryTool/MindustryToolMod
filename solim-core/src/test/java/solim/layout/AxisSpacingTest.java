package solim.layout;

import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import arc.Core;
import arc.mock.MockApplication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.display.SolimImage;
import solim.input.Button;
import solim.runtime.SignalDispatcher;
import solim.reactive.Signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import arc.mock.MockGraphics;

public class AxisSpacingTest {

    @BeforeAll
    static void setup() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
    }

    @Test
    void columnAndRowAxisSpacing() {
        Column col = new Column().paddingX(16f).paddingY(8f);
        Table colTable = col.table();
        assertEquals(16f, colTable.getMarginLeft(), 0.01f);
        assertEquals(16f, colTable.getMarginRight(), 0.01f);
        assertEquals(8f, colTable.getMarginTop(), 0.01f);
        assertEquals(8f, colTable.getMarginBottom(), 0.01f);

        col.paddingX(10f).paddingY(5f);
        assertEquals(10f, colTable.getMarginLeft(), 0.01f);
        assertEquals(10f, colTable.getMarginRight(), 0.01f);
        assertEquals(5f, colTable.getMarginTop(), 0.01f);
        assertEquals(5f, colTable.getMarginBottom(), 0.01f);

        Row row = new Row().paddingX(20f).paddingY(12f);
        Table rowTable = row.table();
        assertEquals(20f, rowTable.getMarginLeft(), 0.01f);
        assertEquals(20f, rowTable.getMarginRight(), 0.01f);
        assertEquals(12f, rowTable.getMarginTop(), 0.01f);
        assertEquals(12f, rowTable.getMarginBottom(), 0.01f);

        row.paddingX(14f).paddingY(7f);
        assertEquals(14f, rowTable.getMarginLeft(), 0.01f);
        assertEquals(14f, rowTable.getMarginRight(), 0.01f);
        assertEquals(7f, rowTable.getMarginTop(), 0.01f);
        assertEquals(7f, rowTable.getMarginBottom(), 0.01f);
    }

    @Test
    void cardAxisPadding() {
        Card card = new Card().paddingX(18f).paddingY(9f);
        Table cardContainer = card.container();
        assertEquals(18f, cardContainer.getMarginLeft(), 0.01f);
        assertEquals(18f, cardContainer.getMarginRight(), 0.01f);
        assertEquals(9f, cardContainer.getMarginTop(), 0.01f);
        assertEquals(9f, cardContainer.getMarginBottom(), 0.01f);
    }

    @Test
    void solimImageAxisSpacing() {
        Table parent = new Table();
        SolimImage img = new SolimImage();
        parent.add(img.element());

        img.paddingX(10f).paddingY(5f).marginX(8f).marginY(4f);

        Cell<?> cell = parent.getCell(img.element());
        // padding (10, 5) + margin (8, 4) = 18 horizontal, 9 vertical
        assertEquals(18f, CellAccess.padLeft(cell), 0.01f);
        assertEquals(18f, CellAccess.padRight(cell), 0.01f);
        assertEquals(9f, CellAccess.padTop(cell), 0.01f);
        assertEquals(9f, CellAccess.padBottom(cell), 0.01f);
    }

    @Test
    void buttonAxisPaddingStaticAndReactive() {
        Button btn = new Button().paddingX(12f).paddingY(6f);
        arc.scene.ui.Button arcBtn = (arc.scene.ui.Button) btn.element();
        assertEquals(12f, arcBtn.getMarginLeft(), 0.01f);
        assertEquals(12f, arcBtn.getMarginRight(), 0.01f);
        assertEquals(6f, arcBtn.getMarginTop(), 0.01f);
        assertEquals(6f, arcBtn.getMarginBottom(), 0.01f);

        Signal<Float> padXSignal = Signal.of(15f);
        Signal<Float> padYSignal = Signal.of(9f);
        Button reactiveBtn = new Button().paddingX(padXSignal).paddingY(padYSignal);
        SignalDispatcher.flush();
        arc.scene.ui.Button arcReactiveBtn = (arc.scene.ui.Button) reactiveBtn.element();
        assertEquals(15f, arcReactiveBtn.getMarginLeft(), 0.01f);
        assertEquals(15f, arcReactiveBtn.getMarginRight(), 0.01f);
        assertEquals(9f, arcReactiveBtn.getMarginTop(), 0.01f);
        assertEquals(9f, arcReactiveBtn.getMarginBottom(), 0.01f);

        padXSignal.set(24f);
        padYSignal.set(16f);
        SignalDispatcher.flush();
        assertEquals(24f, arcReactiveBtn.getMarginLeft(), 0.01f);
        assertEquals(24f, arcReactiveBtn.getMarginRight(), 0.01f);
        assertEquals(16f, arcReactiveBtn.getMarginTop(), 0.01f);
        assertEquals(16f, arcReactiveBtn.getMarginBottom(), 0.01f);
    }

    @Test
    void layoutModifiersReactiveMarginAxis() {
        Table parent = new Table();
        Grid grid = new Grid(2);
        Cell<?> cell = parent.add(grid.element());

        Signal<Float> mx = Signal.of(11f);
        Signal<Float> my = Signal.of(7f);

        grid.marginX(mx).marginY(my);
        grid.cellConfig().applyToCell(cell);
        SignalDispatcher.flush();

        assertEquals(11f, CellAccess.padLeft(cell), 0.01f);
        assertEquals(11f, CellAccess.padRight(cell), 0.01f);
        assertEquals(7f, CellAccess.padTop(cell), 0.01f);
        assertEquals(7f, CellAccess.padBottom(cell), 0.01f);

        mx.set(22f);
        my.set(14f);
        SignalDispatcher.flush();

        assertEquals(22f, CellAccess.padLeft(cell), 0.01f);
        assertEquals(22f, CellAccess.padRight(cell), 0.01f);
        assertEquals(14f, CellAccess.padTop(cell), 0.01f);
        assertEquals(14f, CellAccess.padBottom(cell), 0.01f);
    }
}
