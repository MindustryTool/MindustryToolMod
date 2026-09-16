package solim.layout;

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
import java.util.concurrent.atomic.AtomicInteger;

class VirtualListTest {

    @BeforeAll
    static void initArc() {
        if (Core.app == null) {
            Core.app = new MockApplication();
        }
        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
    }

    static class TestItemComponent extends BaseComponent {
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
        float[] yOffsets = new float[] {0f, 100f, 200f, 300f, 400f};
        float[] heights = new float[] {100f, 100f, 100f, 100f, 100f};

        // At scrollY = 0, first visible should be 0
        assertEquals(0, VirtualList.findFirstVisible(yOffsets, heights, 0f));
        // At scrollY = 150, first visible should be 1 (since 100 + 100 >= 150)
        assertEquals(1, VirtualList.findFirstVisible(yOffsets, heights, 150f));
        // At scrollY = 200, first visible should be 2
        assertEquals(2, VirtualList.findFirstVisible(yOffsets, heights, 200f));

        // At bottom = 250, last visible should be 2 (since offset 200 < 250, but 300 >= 250)
        assertEquals(2, VirtualList.findLastVisible(yOffsets, 250f));
        // At bottom = 150, last visible should be 1
        assertEquals(1, VirtualList.findLastVisible(yOffsets, 150f));
    }

    @Test
    void testEmptyList() {
        Signal<List<String>> items = Signal.of(Collections.<String>emptyList());
        VirtualList<String, String> vl = new VirtualList<>(
                items,
                id -> id,
                (id, w) -> 50f,
                TestItemComponent::new
        );
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

        VirtualList<String, String> vl = new VirtualList<>(
                items,
                id -> id,
                (id, w) -> 50f,
                id -> {
                    TestItemComponent c = new TestItemComponent(id);
                    created.put(id, c);
                    return c;
                }
        );
        vl.overscan(2);
        vl.element();
        SignalDispatcher.flush();

        // Total height for 100 items @ 50px = 5000px
        assertEquals(5000f, vl.getTotalHeight(), 0.001f);

        // When not scrolled (top 0..1000 viewport by default in headless),
        // 1000px viewport fits 20 items (indices 0..19), plus 2 overscan = indices 0..21 (22 items)
        assertTrue(vl.getMountedCount() <= 25, "Should only mount visible items + overscan, but was: " + vl.getMountedCount());
        assertTrue(vl.getMountedCount() >= 10);
        assertTrue(created.containsKey("item-0"));
        // Far away items must NOT have been instantiated
        assertFalse(created.containsKey("item-90"));

        vl.dispose();
    }

    @Test
    void testGapSpacing() {
        List<String> list = Arrays.asList("A", "B", "C");
        VirtualList<String, String> vl = new VirtualList<>(
                Signal.of(list),
                id -> id,
                (id, w) -> 50f,
                TestItemComponent::new
        );
        vl.gap(10f);
        vl.element();
        SignalDispatcher.flush();

        // 3 items * 50 + 2 gaps * 10 = 170
        assertEquals(170f, vl.getTotalHeight(), 0.001f);
        vl.dispose();
    }

    @Test
    void testContainerWidthResizeRecalculates() {
        List<String> list = Arrays.asList("A", "B");
        VirtualList<String, String> vl = new VirtualList<>(
                Signal.of(list),
                id -> id,
                (id, w) -> w < 200f ? 100f : 40f,
                TestItemComponent::new
        );
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
        VirtualList<String, String> vl = new VirtualList<>(
                Signal.of(list),
                id -> id,
                (id, w) -> 50f,
                id -> {
                    TestItemComponent c = new TestItemComponent(id);
                    created.put(id, c);
                    return c;
                }
        );
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

        class ReactiveChild extends BaseComponent {
            @Override
            protected Element build() {
                childBuildCount.incrementAndGet();
                // Reading a child signal during component build
                String val = childSignal.get();
                Element el = new Element();
                el.name = val;
                return el;
            }
        }

        Signal<List<String>> listSignal = Signal.of(Collections.singletonList("item1"));
        VirtualList<String, String> vl = new VirtualList<>(
                listSignal,
                id -> id,
                (id, w) -> 50f,
                id -> new ReactiveChild()
        );
        vl.element();
        SignalDispatcher.flush();

        assertEquals(1, childBuildCount.get());

        // Mutate child signal
        childSignal.set("updated");

        // The collection observer should NOT be queued in SignalDispatcher
        assertEquals(0, SignalDispatcher.size(), "Child signal mutation must not trigger VirtualList collection effect");
        SignalDispatcher.flush();

        // childBuildCount should still be 1 (not rebuilt by collection effect)
        assertEquals(1, childBuildCount.get());

        vl.dispose();
    }

    @Test
    void virtualListSupportsElementAndTableConfig() {
        Signal<List<String>> items = Signal.of(Collections.emptyList());
        VirtualList<String, String> list = new VirtualList<>(
                items,
                s -> s,
                (s, i) -> 50f,
                TestItemComponent::new
        )
                .width(350f)
                .height(500f)
                .visible(false)
                .padding(6f);

        assertEquals(350f, list.element().getWidth());
        assertEquals(500f, list.element().getHeight());
        assertFalse(list.element().visible);
        assertEquals(6f, list.table().getMarginTop());
        list.dispose();
    }
}
