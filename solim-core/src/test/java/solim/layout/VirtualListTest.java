package solim.layout;

import static org.junit.jupiter.api.Assertions.*;
import static solim.modifier.Values.gap;
import static solim.modifier.Values.height;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.visible;
import static solim.modifier.Values.width;

import arc.scene.Element;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import solim.core.Component;
import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class VirtualListTest extends SolimEnv {

    static class TestItemComponent extends Component {
        final String id;
        boolean wasDisposed = false;

        TestItemComponent(String id) {
            this.id = id;
        }

        @Override
        protected Element build() {
            Element el = new Element();
            el.name = "item-" + id;
            return el;
        }

        @Override
        protected void onDispose() {
            wasDisposed = true;
        }
    }

    @Test
    void testBinarySearchVisibleBounds() {
        float[] yOffsets = new float[] { 0f, 100f, 200f, 300f, 400f };
        float[] heights = new float[] { 100f, 100f, 100f, 100f, 100f };

        assertEquals(0, VirtualList.findFirstVisible(yOffsets, heights, 0f));

        assertEquals(1, VirtualList.findFirstVisible(yOffsets, heights, 150f));

        assertEquals(2, VirtualList.findFirstVisible(yOffsets, heights, 200f));

        assertEquals(2, VirtualList.findLastVisible(yOffsets, 250f));

        assertEquals(1, VirtualList.findLastVisible(yOffsets, 150f));
    }

    @Test
    void testEmptyList() {
        Signal<List<String>> items = Signal.of(Collections.<String>emptyList());
        VirtualList<String> vl = new VirtualList<>(items, (id, w) -> 50f);
        vl.key(id -> id);
        vl.items(TestItemComponent::new);
        vl.element();
        SignalDispatcher.flush();

        assertEquals(0f, vl.getTotalHeight(), 0.001f);
        assertEquals(0, vl.getMountedCount());
        vl.dispose();
    }

    @Test
    void testMountOnlyVisibleItemsWithOverscan() {
        List<String> rawItems = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            rawItems.add("item-" + i);
        }
        Signal<List<String>> items = Signal.of(rawItems);
        Map<String, TestItemComponent> created = new HashMap<>();

        VirtualList<String> vl = new VirtualList<>(items, (id, w) -> 50f);
        vl.key(id -> id);
        vl.items(id -> {
            TestItemComponent c = new TestItemComponent(id);
            created.put(id, c);
            return c;
        });
        vl.overscan(2);
        vl.element();
        if (vl.pane() != null) {
            vl.pane().setSize(300f, 1000f);
            vl.reconcileVisible();
        }
        SignalDispatcher.flush();

        assertEquals(5000f, vl.getTotalHeight(), 0.001f);

        assertTrue(vl.getMountedCount() <= 25,
                "Should only mount visible items + overscan, but was: " + vl.getMountedCount());
        assertTrue(vl.getMountedCount() >= 10);
        assertTrue(created.containsKey("item-0"));

        assertFalse(created.containsKey("item-90"));

        vl.dispose();
    }

    @Test
    void testGapSpacing() {
        List<String> list = Arrays.asList("A", "B", "C");
        VirtualList<String> vl = new VirtualList<>(Signal.of(list), (id, w) -> 50f);
        vl.key(id -> id);
        vl.items(TestItemComponent::new);
        vl.props(gap(10f));
        vl.element();
        SignalDispatcher.flush();

        assertEquals(170f, vl.getTotalHeight(), 0.001f);
        vl.dispose();
    }

    @Test
    void testContainerWidthResizeRecalculates() {
        List<String> list = Arrays.asList("A", "B");
        VirtualList<String> vl = new VirtualList<>(Signal.of(list), (id, w) -> w < 200f ? 100f : 40f);
        vl.key(id -> id);
        vl.items(TestItemComponent::new);
        vl.element();
        SignalDispatcher.flush();

        vl.recalculateHeights(300f);
        assertEquals(80f, vl.getTotalHeight(), 0.001f);

        vl.recalculateHeights(150f);
        assertEquals(200f, vl.getTotalHeight(), 0.001f);

        vl.dispose();
    }

    @Test
    void testDisposeCleansUpComponents() {
        List<String> list = Arrays.asList("A", "B");
        Map<String, TestItemComponent> created = new HashMap<>();
        VirtualList<String> vl = new VirtualList<>(Signal.of(list), (id, w) -> 50f);
        vl.key(id -> id);
        vl.items(id -> {
            TestItemComponent c = new TestItemComponent(id);
            created.put(id, c);
            return c;
        });
        vl.element();
        SignalDispatcher.flush();

        TestItemComponent a = created.get("A");
        assertNotNull(a);
        assertFalse(a.wasDisposed);

        vl.dispose();
        assertTrue(a.wasDisposed);
        assertEquals(0, vl.getMountedCount());
    }

    @Test
    void childSignalReadsDoNotAttachToCollectionObserver() {
        Signal<String> childSignal = Signal.of("initial");
        AtomicInteger childBuildCount = new AtomicInteger(0);

        class ReactiveChild extends Component {
            @Override
            protected Element build() {
                childBuildCount.incrementAndGet();

                String val = childSignal.get();
                Element el = new Element();
                el.name = val;
                return el;
            }
        }

        Signal<List<String>> listSignal = Signal.of(Collections.singletonList("item1"));
        VirtualList<String> vl = new VirtualList<>(listSignal, (id, w) -> 50f);
        vl.key(id -> id);
        vl.items(id -> new ReactiveChild());
        vl.element();
        SignalDispatcher.flush();

        assertEquals(1, childBuildCount.get());

        childSignal.set("updated");

        assertEquals(0, SignalDispatcher.size(),
                "Child signal mutation must not trigger VirtualList collection effect");
        SignalDispatcher.flush();

        assertEquals(1, childBuildCount.get());

        vl.dispose();
    }

    @Test
    void virtualListSupportsElementAndTableConfig() {
        Signal<List<String>> items = Signal.of(Collections.emptyList());
        VirtualList<String> list = new VirtualList<>(items, (s, i) -> 50f);
        list.key(s -> s);
        list.items(TestItemComponent::new);
        list.props(width(350f), prefWidth(350f), height(500f), prefHeight(500f), visible(false), padding(6f));

        assertEquals(350f, list.element().getWidth());
        assertEquals(500f, list.element().getHeight());
        assertFalse(list.element().visible);
        assertEquals(6f, list.table().getMarginTop());
        list.dispose();
    }
}
