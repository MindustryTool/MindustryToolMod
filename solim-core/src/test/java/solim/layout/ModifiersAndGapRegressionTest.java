package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static solim.modifier.Values.gap;
import static solim.modifier.Values.height;
import static solim.modifier.Values.opacity;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.visible;
import static solim.modifier.Values.width;

import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.Test;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.test.SolimCoreEnv;
import solim.test.TestScheduler;

public class ModifiersAndGapRegressionTest extends SolimCoreEnv {

    @Test
    void elementModifiersAppliedCorrectly() {
        Column col = new Column();
        col.props(width(120f), prefWidth(120f), height(60f), prefHeight(60f), visible(false), opacity(0.75f)).touchable(Touchable.disabled);

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
        row.props(width(widthSig), prefWidth(widthSig), visible(visSig));

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
        AttachmentStack.push(parent);

        AttachmentStack.setCellConfigurator((cell, child, comp) -> {
            if (child.name != null && child.name.equals("c1")) {
                cell.pad(15f);
            }
        });

        Element c1 = new Element();
        c1.name = "c1";
        AttachmentStack.add(c1);

        Element c2 = new Element();
        c2.name = "c2";
        AttachmentStack.add(c2);

        AttachmentStack.pop();

        assertEquals(15f, CellAccess.padTop(parent.getCell(c1)), 0.01f);
        assertEquals(0f, CellAccess.padTop(parent.getCell(c2)), 0.01f,
                "Cell configuration on Child 1 must not bleed into Child 2");
    }

    @Test
    void gapContainerEmptyAndSingleChild() {
        Column emptyCol = new Column().props(gap(12f));
        assertEquals(0, emptyCol.table().getChildren().size);

        Column singleCol = new Column().props(gap(12f));
        Element singleEl = new Element();
        singleCol.table().add(singleEl);

        assertEquals(0f, CellAccess.padTop(singleCol.table().getCell(singleEl)), 0.01f);
    }

    @Test
    void gapContainerMultipleChildrenAndVisibilityTransitions() {
        Column col = new Column().props(gap(10f));
        Table table = col.table();

        Element c1 = new Element();
        Element c2 = new Element();
        Element c3 = new Element();

        table.add(c1);
        table.add(c2);
        table.add(c3);

        c1.visible = true;
        c2.visible = true;
        c3.visible = true;
        col.respace();

        assertEquals(0f, CellAccess.padTop(table.getCell(c1)), 0.01f, "First visible child has no gap");
        assertEquals(10f, CellAccess.padTop(table.getCell(c2)), 0.01f, "Second visible child has gap");
        assertEquals(10f, CellAccess.padTop(table.getCell(c3)), 0.01f, "Third visible child has gap");

        c2.visible = false;
        col.respace();

        assertEquals(0f, CellAccess.padTop(table.getCell(c1)), 0.01f);
        assertEquals(0f, CellAccess.padTop(table.getCell(c2)), 0.01f, "Hidden child has 0 gap");
        assertEquals(10f, CellAccess.padTop(table.getCell(c3)), 0.01f,
                "c3 becomes second visible child and receives gap");

        c1.visible = false;
        col.respace();

        assertEquals(0f, CellAccess.padTop(table.getCell(c1)), 0.01f);
        assertEquals(0f, CellAccess.padTop(table.getCell(c2)), 0.01f);
        assertEquals(0f, CellAccess.padTop(table.getCell(c3)), 0.01f,
                "c3 is now the first visible child, receives 0 gap");

        c3.visible = false;
        col.respace();

        assertEquals(0f, CellAccess.padTop(table.getCell(c1)), 0.01f);
        assertEquals(0f, CellAccess.padTop(table.getCell(c2)), 0.01f);
        assertEquals(0f, CellAccess.padTop(table.getCell(c3)), 0.01f);

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
