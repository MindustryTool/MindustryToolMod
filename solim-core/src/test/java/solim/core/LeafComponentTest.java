package solim.core;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;

class LeafComponentTest {

    @BeforeAll
    static void initArc() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
    }

    @AfterEach
    void tearDown() {
        ParentStack.clear();
        ComponentContext.clear();
    }

    static class TestLeaf extends LeafComponent<Element, TestLeaf> {
        TestLeaf() {
            super(new Element());
        }

        TestLeaf(Element el) {
            super(el);
        }
    }

    @Test
    void bindsSolimTokenAutomatically() {
        Element el = new Element();
        TestLeaf leaf = new TestLeaf(el);

        SolimToken token = SolimToken.get(el);
        assertNotNull(token);
        assertSame(leaf, token.component);
        assertSame(leaf.cellConfig(), token.cellConfig);
        assertSame(leaf, SolimToken.getComponent(el));
    }

    @Test
    void rejectsNullElement() {
        assertThrows(IllegalArgumentException.class, () -> new TestLeaf(null));
    }

    @Test
    void autoRegistersPendingAttachmentWithParentStack() {
        Table root = new Table();
        ParentStack.push(root);

        TestLeaf leaf = new TestLeaf();
        assertEquals(0, root.getChildren().size, "Should not attach immediately before pop");

        ParentStack.pop();
        assertEquals(1, root.getChildren().size, "Should attach upon parent pop");
        assertSame(leaf.element(), root.getChildren().first());
    }

    @Test
    void appliesCellConstraintsDirectlyOnParentStackAttach() {
        Table root = new Table();
        ParentStack.push(root);

        TestLeaf leaf = new TestLeaf();
        leaf.minWidth(80f).growX();

        ParentStack.pop();

        Cell<?> cell = root.getCell(leaf.element());
        assertNotNull(cell);
        assertEquals(80f, CellAccess.minWidth(cell), 0.01f);
        assertEquals(1, CellAccess.expandX(cell));
        assertEquals(1f, CellAccess.fillX(cell), 0.01f);
    }

    @Test
    void registersChildWithActiveComponentContext() {
        List<Disposable> registered = new ArrayList<>();
        ComponentContext.push(registered::add);

        TestLeaf leaf = new TestLeaf();
        ComponentContext.pop();

        assertTrue(registered.contains(leaf), "LeafComponent must register with active ComponentContext");
    }

    @Test
    void managesDisposablesInLifoOrder() {
        TestLeaf leaf = new TestLeaf();
        List<Integer> order = new ArrayList<>();

        leaf.own(() -> order.add(1));
        leaf.own(() -> order.add(2));
        leaf.own(() -> order.add(3));

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
        TestLeaf leaf = new TestLeaf();
        AtomicBoolean disposed = new AtomicBoolean(false);
        leaf.own(() -> disposed.set(true));

        leaf.dispose();
        assertTrue(leaf.isDisposed());
        assertTrue(disposed.get());

        // Repeated dispose must not throw
        assertDoesNotThrow(leaf::dispose);

        // Accessing element after disposal throws IllegalStateException
        assertThrows(IllegalStateException.class, leaf::element);
    }

    @Test
    void nameConfigurationUpdatesBothModelAndElement() {
        Element el = new Element();
        TestLeaf leaf = new TestLeaf(el);

        assertTrue(leaf.name().startsWith("solim-testleaf-element"));
        assertEquals(leaf.name(), el.name);

        leaf.name("my-custom-leaf");
        assertEquals("my-custom-leaf", leaf.name());
        assertEquals("my-custom-leaf", el.name);
    }
}
