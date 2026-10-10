package solim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import arc.graphics.Color;
import solim.layout.Row;
import solim.runtime.AttachmentStack;
import solim.test.SolimEnv;

class ColumnAttachmentExplorationTest extends SolimEnv {

    @AfterEach
    void tearDown() {
        AttachmentStack.clear();
    }

    @Test
    void columnWithoutChildrenIsNotAttachedToParentRow() {
        Row row = UI.row().children(() -> {
            UI.text("1");
            UI.column().width(10f); // Notice: children() is NOT called!
            UI.text("2");
        });

        // Column was instantiated but never attached because attachment only happens in .children()!
        // So row only has 2 children instead of 3.
        assertEquals(2, row.table().getChildren().size);
    }

    @Test
    void columnWithChildrenIsAttachedToParentRow() {
        Row row = UI.row().children(() -> {
            UI.text("1");
            UI.column(() -> {}).width(10f); // children() IS called
            UI.text("2");
        });

        // Row has all 3 children: Text("1"), Column table, Text("2")
        assertEquals(3, row.table().getChildren().size);
    }

    @Test
    void spacerIsAttachedToParentRow() {
        Row row = UI.row().children(() -> {
            UI.text("1");
            UI.spacer(); // UI.spacer() calls AttachmentStack.attachToParent immediately!
            UI.text("2");
        });

        // Spacer is immediately attached
        assertEquals(3, row.table().getChildren().size);
    }
}
