package solim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static solim.modifier.Values.height;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.width;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import solim.display.Text;
import solim.layout.Card;
import solim.layout.Column;
import solim.layout.Grid;
import solim.layout.Row;
import solim.layout.Wrap;
import solim.runtime.AttachmentStack;
import solim.test.SolimEnv;

class ContainerAutoAttachmentTest extends SolimEnv {

    @AfterEach
    void tearDown() {
        AttachmentStack.clear();
    }

    @Test
    void columnWithoutChildrenAutoAttachesToParentRow() {
        Column[] colRef = new Column[1];
        Row row = UI.row().children(() -> {
            UI.text("1");
            colRef[0] = UI.column().props(width(10f), prefWidth(10f));
            UI.text("2");
        });

        assertEquals(3, row.table().getChildren().size);
        assertSame(colRef[0].table(), row.table().getChildren().get(1));
    }

    @Test
    void rowWithoutChildrenAutoAttachesToParentColumn() {
        Row[] rowRef = new Row[1];
        Column col = UI.column().children(() -> {
            UI.text("Top");
            rowRef[0] = UI.row().props(height(15f), prefHeight(15f));
            UI.text("Bottom");
        });

        assertEquals(3, col.table().getChildren().size);
        assertSame(rowRef[0].table(), col.table().getChildren().get(1));
    }

    @Test
    void cardGridWrapAutoAttachWhenChildrenOmitted() {
        Card[] cardRef = new Card[1];
        Grid[] gridRef = new Grid[1];
        Wrap[] wrapRef = new Wrap[1];

        Column col = UI.column().children(() -> {
            cardRef[0] = UI.card();
            gridRef[0] = UI.grid(2);
            wrapRef[0] = UI.wrap();
        });

        assertEquals(3, col.table().getChildren().size);
        assertSame(cardRef[0].table(), col.table().getChildren().get(0));
        assertSame(gridRef[0].table(), col.table().getChildren().get(1));
        assertSame(wrapRef[0].table(), col.table().getChildren().get(2));
    }

    @Test
    void sequentialSiblingOrderPreservedBetweenLeavesAndContainers() {
        Text[] t1 = new Text[1];
        Column[] c1 = new Column[1];
        Text[] t2 = new Text[1];
        Row[] r1 = new Row[1];
        Text[] t3 = new Text[1];

        Row row = UI.row().children(() -> {
            t1[0] = UI.text("A");
            c1[0] = UI.column().props(width(5f), prefWidth(5f));
            t2[0] = UI.text("B");
            r1[0] = UI.row().props(width(10f), prefWidth(10f));
            t3[0] = UI.text("C");
        });

        assertEquals(5, row.table().getChildren().size);
        assertSame(t1[0].label(), row.table().getChildren().get(0));
        assertSame(c1[0].table(), row.table().getChildren().get(1));
        assertSame(t2[0].label(), row.table().getChildren().get(2));
        assertSame(r1[0].table(), row.table().getChildren().get(3));
        assertSame(t3[0].label(), row.table().getChildren().get(4));
    }

    @Test
    void containerWithChildrenDoesNotAttachTwice() {
        Row[] innerRow = new Row[1];
        Column outerCol = UI.column().children(() -> {
            UI.text("Header");
            innerRow[0] = UI.row().children(() -> {
                UI.text("Inside 1");
                UI.text("Inside 2");
            });
            UI.text("Footer");
        });

        assertEquals(3, outerCol.table().getChildren().size);
        assertSame(innerRow[0].table(), outerCol.table().getChildren().get(1));
        assertEquals(2, innerRow[0].table().getChildren().size);
    }
}
