package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static solim.modifier.Values.gap;
import static solim.modifier.Values.marginTop;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.scene.ui.layout.CellAccess;
import solim.input.Button;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.runtime.SignalDispatcher;
import solim.test.ArcTestEnv;
import solim.test.SolimEnv;

public class PropsGapTimingTest extends SolimEnv {

    @BeforeEach
    public void initScene() {
        ArcTestEnv.newScene();
    }

    @Test
    void deferredPropsGapSpacesMixedHeightChildrenVertically() {
        Column column = new Column();
        column.props(gap(12f));

        Button tall = new Button().props(prefHeight(64f));
        Button medium = new Button().props(prefHeight(40f));
        Button shortOne = new Button().props(prefHeight(18f));

        column.children(() -> {
            column.table().add(tall.element());
            column.table().row();
            column.table().add(medium.element());
            column.table().row();
            column.table().add(shortOne.element());
        });

        assertEquals(0f, CellAccess.padTop(column.table().getCell(tall.element())), 0.01f,
                "the first child must not carry the gap");
        assertEquals(12f, CellAccess.padTop(column.table().getCell(medium.element())), 0.01f);
        assertEquals(12f, CellAccess.padTop(column.table().getCell(shortOne.element())), 0.01f,
                "the deferred respace must not derive the gap from the child's own height");
    }

    @Test
    void deferredPropsGapSpacesMixedWidthChildrenHorizontally() {
        Row row = new Row();
        row.props(gap(9f));

        Button wide = new Button().props(prefWidth(90f));
        Button narrow = new Button().props(prefWidth(30f));
        Button medium = new Button().props(prefWidth(55f));

        row.children(() -> {
            row.table().add(wide.element());
            row.table().add(narrow.element());
            row.table().add(medium.element());
        });

        assertEquals(0f, CellAccess.padLeft(row.table().getCell(wide.element())), 0.01f,
                "the first child must not carry the gap");
        assertEquals(9f, CellAccess.padLeft(row.table().getCell(narrow.element())), 0.01f);
        assertEquals(9f, CellAccess.padLeft(row.table().getCell(medium.element())), 0.01f,
                "the deferred respace must not derive the gap from the child's own width");
    }

    @Test
    void propsGapAppliesToChildrenAttachedByTheAttacher() {
        Column column = new Column();
        column.props(gap(14f));

        Button first = new Button().props(prefHeight(52f));
        Button second = new Button().props(prefHeight(20f));

        column.children(() -> {
            AttachmentStack.add(first.element());
            AttachmentStack.add(second.element());
        });

        assertEquals(0f, CellAccess.padTop(column.table().getCell(first.element())), 0.01f,
                "the first child must not carry the gap");
        assertEquals(14f, CellAccess.padTop(column.table().getCell(second.element())), 0.01f,
                "a child attached through the attacher must receive the recorded gap");
    }

    @Test
    void propsGapComposesWithChildMargin() {
        Column column = new Column();
        column.props(gap(10f));

        Button plain = new Button();
        Button inset = new Button().props(marginTop(6f));

        column.children(() -> {
            column.table().add(plain.element());
            column.table().row();
            column.table().add(inset.element());
        });

        assertEquals(0f, CellAccess.padTop(column.table().getCell(plain.element())), 0.01f);
        assertEquals(16f, CellAccess.padTop(column.table().getCell(inset.element())), 0.01f,
                "the gap must add to the child's own margin instead of replacing it");
    }

    @Test
    void propsGapTracksItsSignal() {
        Signal<Float> spacing = Signal.of(8f);

        Column column = new Column();
        column.props(gap(spacing));

        Button first = new Button();
        Button second = new Button();

        column.children(() -> {
            column.table().add(first.element());
            column.table().row();
            column.table().add(second.element());
        });

        assertEquals(8f, CellAccess.padTop(column.table().getCell(second.element())), 0.01f);

        spacing.set(21f);
        SignalDispatcher.flush();

        assertEquals(21f, CellAccess.padTop(column.table().getCell(second.element())), 0.01f,
                "a gap recorded through props must stay reactive");
    }
}
