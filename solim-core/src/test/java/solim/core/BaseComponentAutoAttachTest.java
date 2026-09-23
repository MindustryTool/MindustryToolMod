package solim.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import solim.layout.Column;
import solim.layout.Divider;
import solim.layout.Row;
import solim.layout.Spacer;
import solim.layout.Direction;
import solim.runtime.OwnershipContext;
import solim.runtime.AttachmentStack;
import solim.test.SolimEnv;

class BaseComponentAutoAttachTest extends SolimEnv {


    static class Probe extends BaseComponent {
        @Override
        protected Element build() {
            return new Element();
        }
    }

    @AfterEach
    void clear() {
        AttachmentStack.clear();
        OwnershipContext.clear();
    }

    @Test
    void constructsInScopeAutoAttaches() {
        Table root = new Table();
        AttachmentStack.push(root);
        Probe probe = new Probe();
        AttachmentStack.pop();
        assertEquals(1, root.getChildren().size);
        assertTrue(root.getChildren().contains(probe.element(), true));
    }

    @Test
    void nestedScopeAttachesToInnerParent() {
        Table root = new Table();
        AttachmentStack.push(root);
        final Probe[] held = new Probe[1];
        Column col = new Column().children(() -> {
            held[0] = new Probe();
        });
        AttachmentStack.pop();
        assertEquals(1, root.getChildren().size);
        assertEquals(1, col.table().getChildren().size);
        assertSame(held[0].element(), col.table().getChildren().get(0));
    }

    @Test
    void constructsOutsideScopeAttachesNothing() {
        assertNull(AttachmentStack.current());
        Probe probe = new Probe();
        assertNull(probe.element().parent);
        assertEquals(0, AttachmentStack.size());
    }

    @Test
    void explicitComponentCallDoesNotDuplicate() {
        Table root = new Table();
        AttachmentStack.push(root);
        Probe probe = new Probe();
        AttachmentStack.attachToParent(AttachmentStack.isolate(probe::element));
        AttachmentStack.pop();
        assertEquals(1, root.getChildren().size);
        assertTrue(root.getChildren().contains(probe.element(), true));
    }

    @Test
    void preservesChildOrderWhenInterleavedWithDivider() {
        final Probe[] messageList = new Probe[1];
        final Element[] divider = new Element[1];
        final Probe[] inputView = new Probe[1];

        Column col = new Column().grow().gap(4f).children(() -> {
            messageList[0] = new Probe();
            Divider div = new Divider(Direction.X);
            AttachmentStack.attachToParent(div.element());
            divider[0] = div.element();
            inputView[0] = new Probe();
        });

        assertEquals(3, col.table().getChildren().size);
        assertSame(messageList[0].element(), col.table().getChildren().get(0), "Message list should be at index 0");
        assertSame(divider[0], col.table().getChildren().get(1), "Divider should be at index 1");
        assertSame(inputView[0].element(), col.table().getChildren().get(2), "Input view should be at index 2");
    }

    @Test
    void preservesChildOrderWithMultipleInterleavedElements() {
        final Probe[] comp1 = new Probe[1];
        final Element[] el1 = new Element[1];
        final Probe[] comp2 = new Probe[1];
        final Element[] el2 = new Element[1];

        Column col = new Column().children(() -> {
            Spacer spacer = new Spacer();
            AttachmentStack.attachToParent(spacer.element());
            el1[0] = spacer.element();
            comp1[0] = new Probe();
            Divider dividerComp = new Divider(Direction.X);
            AttachmentStack.attachToParent(dividerComp.element());
            el2[0] = dividerComp.element();
            comp2[0] = new Probe();
        });

        assertEquals(4, col.table().getChildren().size);
        assertSame(el1[0], col.table().getChildren().get(0), "Spacer should be at index 0");
        assertSame(comp1[0].element(), col.table().getChildren().get(1), "Comp 1 should be at index 1");
        assertSame(el2[0], col.table().getChildren().get(2), "Divider should be at index 2");
        assertSame(comp2[0].element(), col.table().getChildren().get(3), "Comp 2 should be at index 3");
    }

    @Test
    void preservesChildOrderWithNestedLayout() {
        final Probe[] comp1 = new Probe[1];
        final Element[] rowElem = new Element[1];
        final Probe[] comp2 = new Probe[1];

        Column col = new Column().children(() -> {
            comp1[0] = new Probe();
            Row innerRow = new Row().children(() -> {
                AttachmentStack.attachToParent(new Spacer().element());
            });
            rowElem[0] = innerRow.table();
            comp2[0] = new Probe();
        });

        assertEquals(3, col.table().getChildren().size);
        assertSame(comp1[0].element(), col.table().getChildren().get(0), "Comp 1 should be at index 0");
        assertSame(rowElem[0], col.table().getChildren().get(1), "Row should be at index 1");
        assertSame(comp2[0].element(), col.table().getChildren().get(2), "Comp 2 should be at index 2");
    }
}
