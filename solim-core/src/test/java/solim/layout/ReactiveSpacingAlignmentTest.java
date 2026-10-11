package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import solim.input.Button;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.runtime.SignalDispatcher;
import solim.test.ArcTestEnv;
import solim.test.SolimEnv;

public class ReactiveSpacingAlignmentTest extends SolimEnv {

    @BeforeEach
    public void initScene() {
        ArcTestEnv.newScene();
    }

    @Test
    void testReactivePaddingOnTableContainer() {
        Card card = new Card();
        Signal<Float> pad = Signal.of(8f);

        card.padding(pad);
        assertEquals(8f, card.table().getMarginTop(), 0.01f);
        assertEquals(8f, card.table().getMarginLeft(), 0.01f);
        assertEquals(8f, card.table().getMarginBottom(), 0.01f);
        assertEquals(8f, card.table().getMarginRight(), 0.01f);

        pad.set(16f);
        SignalDispatcher.flush();

        assertEquals(16f, card.table().getMarginTop(), 0.01f);
        assertEquals(16f, card.table().getMarginLeft(), 0.01f);
        assertEquals(16f, card.table().getMarginBottom(), 0.01f);
        assertEquals(16f, card.table().getMarginRight(), 0.01f);
    }

    @Test
    void testReactiveMarginOnComponentInParentCell() {
        Signal<Float> topMargin = Signal.of(6f);
        Signal<Float> leftMargin = Signal.of(10f);

        Button btn = new Button();
        btn.marginTop(topMargin).marginLeft(leftMargin);

        Column parent = new Column();
        parent.children(() -> {
            AttachmentStack.add(btn.element());
        });

        Cell<?> cell = parent.table().getCell(btn.element());
        assertEquals(6f, CellAccess.padTop(cell), 0.01f);
        assertEquals(10f, CellAccess.padLeft(cell), 0.01f);

        topMargin.set(14f);
        leftMargin.set(22f);
        SignalDispatcher.flush();

        assertEquals(14f, CellAccess.padTop(cell), 0.01f);
        assertEquals(22f, CellAccess.padLeft(cell), 0.01f);
    }

    @Test
    void testReactiveGapUpdatesSpacingAcrossSiblings() {
        Signal<Float> gapSignal = Signal.of(10f);
        Column col = new Column().gap(gapSignal);

        Button b1 = new Button();
        Button b2 = new Button();
        Button b3 = new Button();

        col.children(() -> {
            col.table().add(b1.element());
            col.table().row();
            col.table().add(b2.element());
            col.table().row();
            col.table().add(b3.element());
        });

        Cell<?> c1 = col.table().getCell(b1.element());
        Cell<?> c2 = col.table().getCell(b2.element());
        Cell<?> c3 = col.table().getCell(b3.element());

        // Initial: first element has 0 gap, subsequent have 10
        assertEquals(0f, CellAccess.padTop(c1), 0.01f);
        assertEquals(10f, CellAccess.padTop(c2), 0.01f);
        assertEquals(10f, CellAccess.padTop(c3), 0.01f);

        // Update gap reactively
        gapSignal.set(25f);
        SignalDispatcher.flush();

        assertEquals(0f, CellAccess.padTop(c1), 0.01f);
        assertEquals(25f, CellAccess.padTop(c2), 0.01f);
        assertEquals(25f, CellAccess.padTop(c3), 0.01f);
    }

    @Test
    void testDisposalCleansUpReactiveEffects() {
        Signal<Float> gapSignal = Signal.of(12f);
        Column col = new Column().gap(gapSignal);

        Button b1 = new Button();
        Button b2 = new Button();
        col.children(() -> {
            col.table().add(b1.element());
            col.table().row();
            col.table().add(b2.element());
        });

        Cell<?> c2 = col.table().getCell(b2.element());
        assertEquals(12f, CellAccess.padTop(c2), 0.01f);

        // Dispose column
        col.dispose();

        // Updating signal should no longer update padding
        gapSignal.set(30f);
        SignalDispatcher.flush();

        assertEquals(12f, CellAccess.padTop(c2), 0.01f, "Disposed container should not receive reactive updates");
    }
}
