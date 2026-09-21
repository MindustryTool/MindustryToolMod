package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import solim.core.BaseComponent;
import solim.core.SolimToken;
import solim.modifier.CellConfig;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class ForEachComponentTest extends SolimEnv {


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
        ForEach<String> fe = new ForEach<>(items);
        fe.key(id -> id);
        fe.children(id -> {
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
        ForEach<String> fe = new ForEach<>(items);
        fe.key(id -> id);
        fe.children(id -> {
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
        ForEach<String> fe = new ForEach<>(items);
        fe.key(id -> id);
        fe.children(id -> {
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
        ForEach<String> fe = new ForEach<>(items);
        fe.key(id -> id);
        fe.children(id -> new TestComponent(id));
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
        ForEach<String> fe = new ForEach<>(items);
        fe.key(id -> id);
        fe.children(id -> new TestComponent(id));
        assertTrue(fe instanceof CellConfig);
        assertNotNull(fe.cellConfig());
        assertFalse(fe.cellConfig().growX);
        fe.growX();
        assertTrue(fe.cellConfig().growX);
        assertSame(fe, SolimToken.getComponent(fe.container()));
        fe.dispose();
    }

    @Test
    void forEachInsideDynamicEnforcesMinWidthZeroOnCells() {
        Signal<Boolean> hasItems = Signal.of(true);
        Signal<List<String>> items = Signal.of(Arrays.asList("A"));

        Dynamic<Boolean> dyn = Dynamic.of(hasItems, available -> {
            if (Boolean.TRUE.equals(available)) {
                ForEach<String> list = new ForEach<>(items);
                list.key(id -> id);
                list.children(TestComponent::new);
                return list.growX();
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

    @Test
    void forEachSupportsElementAndTableConfig() {
        Signal<List<String>> items = Signal.of(Arrays.asList("1", "2"));
        ForEach<String> fe = new ForEach<>(items);
        fe.key(id -> id);
        fe.children(TestComponent::new);
        fe.width(300f)
                .height(200f)
                .visible(true)
                .padding(8f);

        Element el = fe.element();
        assertEquals(300f, el.getWidth());
        assertEquals(200f, el.getHeight());
        assertTrue(el.visible);
        assertEquals(8f, fe.table().getMarginTop());
        fe.dispose();
    }
}
