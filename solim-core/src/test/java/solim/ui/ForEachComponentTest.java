package solim.ui;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.scene.Element;
import java.util.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;
import solim.signal.Signal;
import solim.runtime.SignalDispatcher;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import solim.layout.CellConfig;

class ForEachComponentTest {

    @BeforeAll
    static void initArc() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
    }

    static class TestComponent extends BaseComponent {
        final String id;
        boolean wasDisposed = false;

        TestComponent(String id) {
            this.id = id;
        }

        @Override
        protected Element build() {
            return new Element();
        }

        @Override
        protected void onDispose() {
            wasDisposed = true;
        }
    }

    @Test
    void forEachRendersItemsInOrder() {
        Signal<List<String>> items = Signal.of(Arrays.asList("A", "B", "C"));
        Map<String, TestComponent> created = new HashMap<>();
        ForEach<String, String> fe = new ForEach<>(items, id -> id, id -> {
            TestComponent tc = new TestComponent(id);
            created.put(id, tc);
            return tc;
        });
        fe.element();
        assertEquals(3, fe.container().getChildren().size);
        assertSame(created.get("A").element(), fe.container().getChildren().get(0));
        assertSame(created.get("B").element(), fe.container().getChildren().get(1));
        assertSame(created.get("C").element(), fe.container().getChildren().get(2));
        assertSame(fe.container(), fe.element());
        fe.dispose();
    }

    @Test
    void forEachKeyedReuse() {
        Signal<List<String>> items = Signal.of(Arrays.asList("A", "B", "C"));
        Map<String, TestComponent> created = new HashMap<>();
        ForEach<String, String> fe = new ForEach<>(items, id -> id, id -> {
            TestComponent tc = new TestComponent(id);
            created.put(id, tc);
            return tc;
        });
        fe.element();

        TestComponent a = created.get("A");
        assertEquals("A", a.id);

        // Update items to [B, C, D]
        items.set(Arrays.asList("B", "C", "D"));
        SignalDispatcher.flush();
        assertTrue(a.wasDisposed, "Removed item A must be disposed");
        assertFalse(created.get("B").wasDisposed, "Retained item B must not be disposed");

        fe.dispose();
    }

    @Test
    void forEachDisposal() {
        Signal<List<String>> items = Signal.of(Arrays.asList("A", "B"));
        Map<String, TestComponent> created = new HashMap<>();
        ForEach<String, String> fe = new ForEach<>(items, id -> id, id -> {
            TestComponent tc = new TestComponent(id);
            created.put(id, tc);
            return tc;
        });
        fe.element();
        fe.dispose();
        assertTrue(created.get("A").wasDisposed);
        assertTrue(created.get("B").wasDisposed);
    }

    @Test
    void forEachChildrenDoNotGrowByDefault() {
        Signal<List<String>> items = Signal.of(Arrays.asList("A"));
        ForEach<String, String> fe = new ForEach<>(items, id -> id, id -> new TestComponent(id));
        fe.element();
        Cell<?> cell = fe.container().getCells().first();
        assertEquals(0, CellAccess.expandX(cell));
        assertEquals(0, CellAccess.expandY(cell));
        assertEquals(0f, CellAccess.minWidth(cell), 0.001f);
        fe.dispose();
    }

    @Test
    void forEachImplementsLayoutModifiersAndSupportsGrowX() {
        Signal<List<String>> items = Signal.of(Arrays.asList("A"));
        ForEach<String, String> fe = new ForEach<>(items, id -> id, id -> new TestComponent(id));
        assertTrue(fe instanceof CellConfig);
        assertNotNull(fe.cellConfig());
        assertFalse(fe.cellConfig().growX);
        fe.growX();
        assertTrue(fe.cellConfig().growX);
        assertSame(fe, fe.container().userObject);
        fe.dispose();
    }

    @Test
    void forEachInsideDynamicEnforcesMinWidthZeroOnCells() {
        Signal<Boolean> hasItems = Signal.of(true);
        Signal<List<String>> items = Signal.of(Arrays.asList("A"));

        Dynamic<Boolean> dyn = Dynamic.of(hasItems, available -> {
            if (Boolean.TRUE.equals(available)) {
                return ForEach.of(items, id -> id, id -> new TestComponent(id)).growX();
            }
            return null;
        });

        Table root = new Table();
        root.add(dyn.element()).width(300f);
        root.validate();

        Cell<?> dynamicCell = dyn.container().getCells().first();
        assertNotNull(dynamicCell);
        assertEquals(0f, CellAccess.minWidth(dynamicCell), 0.001f);

        dyn.dispose();
    }
}
