package solim.core;

import solim.test.CollectingOwner;
import static org.junit.jupiter.api.Assertions.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.minWidth;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.display.SolimImage;
import solim.runtime.AttachmentStack;
import solim.test.SolimEnv;

class EagerComponentTest extends SolimEnv {

    @AfterEach
    void tearDown() {
        AttachmentStack.clear();
        OwnershipContext.clear();
    }

    @Test
    void bindsSolimTokenAutomatically() {
        Element el = new Element();
        SolimImage leaf = new SolimImage();
        assertNotNull(SolimToken.get(leaf.element()));
        assertSame(leaf, SolimToken.getComponent(leaf.element()));
        assertSame(leaf.cellConfig(), SolimToken.get(leaf.element()).cellConfig);
        assertNotNull(el);
    }

    @Test
    void autoRegistersPendingAttachmentWithAttachmentStack() {
        Table root = new Table();
        AttachmentStack.push(root);

        SolimImage leaf = new SolimImage();
        assertEquals(0, root.getChildren().size, "Should not attach immediately before pop");

        AttachmentStack.pop();
        assertEquals(1, root.getChildren().size, "Should attach upon parent pop");
        assertSame(leaf.element(), root.getChildren().first());
    }

    @Test
    void appliesCellConstraintsDirectlyOnAttachmentStackAttach() {
        Table root = new Table();
        AttachmentStack.push(root);

        SolimImage leaf = new SolimImage();
        leaf.props(minWidth(80f), growX());

        AttachmentStack.pop();

        Cell<?> cell = root.getCell(leaf.element());
        assertNotNull(cell);
        assertEquals(80f, CellAccess.minWidth(cell), 0.01f);
        assertEquals(1, CellAccess.expandX(cell));
        assertEquals(1f, CellAccess.fillX(cell), 0.01f);
    }

    @Test
    void registersChildWithActiveOwnershipContext() {
        CollectingOwner registered = new CollectingOwner();
        OwnershipContext.push(registered);

        SolimImage leaf = new SolimImage();
        OwnershipContext.pop();

        assertTrue(registered.collected().contains(leaf),
                "An eager component must register with the active OwnershipContext");
    }

    @Test
    void managesDisposablesInLifoOrder() {
        SolimImage leaf = new SolimImage();
        List<Integer> order = new ArrayList<>();

        leaf.adopt(DisposableAction.of(() -> order.add(1)));
        leaf.adopt(DisposableAction.of(() -> order.add(2)));
        leaf.adopt(DisposableAction.of(() -> order.add(3)));

        assertFalse(leaf.isDisposed());
        leaf.dispose();

        assertTrue(leaf.isDisposed());
        assertEquals(3, order.size());
        assertEquals(3, order.get(0).intValue());
        assertEquals(2, order.get(1).intValue());
        assertEquals(1, order.get(2).intValue());
    }

    @Test
    void disposalIsIdempotentAndForbidsElementAccess() {
        SolimImage leaf = new SolimImage();
        AtomicBoolean disposed = new AtomicBoolean(false);
        leaf.adopt(DisposableAction.of(() -> disposed.set(true)));

        leaf.dispose();
        assertTrue(leaf.isDisposed());
        assertTrue(disposed.get());

        assertDoesNotThrow(leaf::dispose);

        assertThrows(IllegalStateException.class, leaf::element);
    }

    @Test
    void nameConfigurationUpdatesBothModelAndElement() {
        SolimImage leaf = new SolimImage();
        Element el = leaf.element();

        assertTrue(leaf.name().startsWith("solim-solimimage-image"));
        assertEquals(leaf.name(), el.name);
    }
}
