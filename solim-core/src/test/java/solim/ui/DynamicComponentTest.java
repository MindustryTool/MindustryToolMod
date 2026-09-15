package solim.ui;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.scene.Element;
import java.util.HashMap;
import java.util.Map;
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
import solim.layout.Column;
import solim.layout.Row;
import solim.runtime.ParentStack;
import solim.signal.Readable;

class DynamicComponentTest {

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
    void dynamicCreatesWithInitialContentA() {
        Map<String, TestComponent> instances = new HashMap<>();
        Signal<Boolean> toggle = Signal.of(true);
        Dynamic<Boolean> dyn = new Dynamic<>(toggle, val -> {
            TestComponent tc = new TestComponent(val ? "A" : "B");
            instances.put(tc.id, tc);
            return tc;
        });
        dyn.element();
        assertEquals("A", instances.get("A").id);
        assertEquals(1, dyn.container().getChildren().size);
        assertSame(dyn.container(), dyn.element());
        dyn.dispose();
    }

    @Test
    void dynamicRendersContent() {
        Signal<Boolean> toggle = Signal.of(true);
        Dynamic<Boolean> dyn = new Dynamic<>(toggle, val -> new TestComponent(val ? "A" : "B"));
        dyn.element();
        assertEquals(1, dyn.container().getChildren().size);
        dyn.dispose();
    }

    @Test
    void dynamicSwitchesContent() {
        Signal<Boolean> toggle = Signal.of(true);
        Map<String, TestComponent> instances = new HashMap<>();
        Dynamic<Boolean> dyn = new Dynamic<>(toggle, val -> {
            TestComponent tc = new TestComponent(val ? "A" : "B");
            instances.put(tc.id, tc);
            return tc;
        });
        dyn.element();
        TestComponent compA = instances.get("A");
        assertEquals("A", compA.id);

        toggle.set(false);
        SignalDispatcher.flush();
        assertTrue(compA.wasDisposed, "Previous component must be disposed on change");
        TestComponent compB = instances.get("B");
        assertEquals("B", compB.id);
        assertFalse(compB.wasDisposed);
        assertEquals(1, dyn.container().getChildren().size);
        dyn.dispose();
    }

    @Test
    void dynamicDisposal() {
        Signal<Boolean> toggle = Signal.of(true);
        Map<String, TestComponent> instances = new HashMap<>();
        Dynamic<Boolean> dyn = new Dynamic<>(toggle, val -> {
            TestComponent tc = new TestComponent(val ? "A" : "B");
            instances.put(tc.id, tc);
            return tc;
        });
        dyn.element();
        dyn.dispose();
        assertTrue(instances.get("A").wasDisposed);
    }

    @Test
    void dynamicChildDoesNotGrowByDefault() {
        Signal<String> source = Signal.of("A");
        Dynamic<String> dyn = new Dynamic<>(source, val -> new TestComponent(val));
        dyn.element();
        Cell<?> cell = dyn.container().getCells().first();
        assertEquals(0, CellAccess.expandX(cell));
        assertEquals(0, CellAccess.expandY(cell));
        dyn.dispose();
    }

    @Test
    void dynamicFactoryDoesNotTrackSignalsEvaluatedDuringChildBuild() {
        Signal<Boolean> switcher = Signal.of(true);
        Signal<String> internalChildSignal = Signal.of("initial");
        int[] factoryBuildCount = new int[] { 0 };

        Dynamic<Boolean> dyn = new Dynamic<>(switcher, val -> {
            factoryBuildCount[0]++;
            String text = internalChildSignal.get();
            return new TestComponent(val + "-" + text);
        });
        dyn.element();
        assertEquals(1, factoryBuildCount[0]);

        internalChildSignal.set("updated");
        SignalDispatcher.flush();
        assertEquals(1, factoryBuildCount[0],
                "Updating signal read during child build must not re-run Dynamic factory");

        switcher.set(false);
        SignalDispatcher.flush();
        assertEquals(2, factoryBuildCount[0], "Updating switcher source must trigger Dynamic factory");
        dyn.dispose();
    }

    @Test
    void nullComponentCollapsesContainerAndParentCell() {
        Signal<String> source = Signal.of("show");
        Dynamic<String> dyn = new Dynamic<>(source, val -> "show".equals(val) ? new TestComponent("active") : null);

        Table parent = new Table();
        parent.defaults().padTop(8f).padBottom(8f);
        Cell<?> parentCell = parent.add(dyn.element());
        parent.pack();

        assertTrue(dyn.container().visible);
        assertEquals(1, dyn.container().getChildren().size);

        source.set("hide");
        SignalDispatcher.flush();
        parent.layout();

        assertFalse(dyn.container().visible);
        assertEquals(0, dyn.container().getChildren().size);
        assertEquals(0f, CellAccess.padTop(parentCell), 0.01f);
        assertEquals(0f, CellAccess.padBottom(parentCell), 0.01f);

        dyn.dispose();
    }

    @Test
    void expandAfterCollapseResetsCellConstraints() {
        Signal<String> source = Signal.of("show");
        Dynamic<String> dyn = new Dynamic<>(source, val -> "show".equals(val) ? new TestComponent("active") : null);

        Table parent = new Table();
        parent.add(dyn.element());
        parent.pack();

        // Collapse
        source.set(null);
        SignalDispatcher.flush();
        parent.layout();

        assertFalse(dyn.container().visible);

        // Expand
        source.set("show");
        SignalDispatcher.flush();
        parent.layout();

        assertTrue(dyn.container().visible);
        assertEquals(1, dyn.container().getChildren().size);
        assertTrue(dyn.container().getChildren().first().getMinHeight() >= 0,
                "Expanded container child must have non-negative minHeight");
    }

    @Test
    void nullThenNonNullShowsVisibleContainerWithChild() {
        Signal<String> source = Signal.of(null);
        Dynamic<String> dyn = new Dynamic<>(source, val -> val != null ? new TestComponent("item") : null);

        Table parent = new Table();
        parent.add(dyn.element());
        parent.pack();

        assertFalse(dyn.container().visible);
        assertEquals(0, dyn.container().getChildren().size);

        source.set("show");
        SignalDispatcher.flush();
        parent.layout();

        assertTrue(dyn.container().visible);
        assertEquals(1, dyn.container().getChildren().size);
        dyn.dispose();
    }

    @Test
    void collapseExpandCyclePreservesContainerChild() {
        Signal<String> source = Signal.of("show");
        Dynamic<String> dyn = new Dynamic<>(source, val -> "show".equals(val) ? new TestComponent("dynamic") : null);

        Table parent = new Table();
        parent.add(dyn.element());
        parent.setSize(400f, 300f);
        parent.validate();
        parent.layout();

        assertTrue(dyn.container().visible);
        assertEquals(1, dyn.container().getChildren().size);

        // Collapse
        source.set(null);
        SignalDispatcher.flush();
        parent.layout();
        assertFalse(dyn.container().visible);
        assertEquals(0, dyn.container().getChildren().size);

        // Expand
        source.set("show");
        SignalDispatcher.flush();
        parent.layout();
        assertTrue(dyn.container().visible);
        assertEquals(1, dyn.container().getChildren().size);

        dyn.dispose();
    }

    @Test
    void dynamicPreservesTopRightAlignmentWithoutGrowX() {
        Signal<Boolean> state = Signal.of(true);
        Dynamic<Boolean> dyn = Dynamic.of(state, s -> {
            Row row = Ui.row();
            row.cellConfig().prefWidth = Readable.of(100f);
            row.cellConfig().prefHeight = Readable.of(40f);
            return row;
        }).top().right();

        assertFalse(dyn.cellConfig().growX, "Dynamic must not growX by default");

        Column col = Ui.column().fillParent().top().right().children(() -> {
            ParentStack.add(dyn);
        });

        Table table = col.table();
        table.setSize(800f, 600f);
        table.validate();
        table.layout();

        Cell<?> cell = table.getCells().first();
        assertEquals(0, CellAccess.expandX(cell), "Cell in top-right column must not expandX");
        assertTrue(dyn.element().x > 600f,
                "Element must be positioned on the right side (was " + dyn.element().x + ")");

        dyn.dispose();
        col.dispose();
    }

    @Test
    void nestedDynamicInnerSwitchesIndependentlyWithoutOuterRebuild() {
        Signal<Boolean> outer = Signal.of(true);
        Signal<String> inner = Signal.of("A");
        int[] outerBuildCount = new int[] { 0 };
        int[] innerBuildCount = new int[] { 0 };
        Map<String, TestComponent> instances = new HashMap<>();

        Dynamic<Boolean> root = Dynamic.of(outer, show -> {
            outerBuildCount[0]++;
            if (!Boolean.TRUE.equals(show))
                return null;
            return Dynamic.of(inner, val -> {
                innerBuildCount[0]++;
                TestComponent tc = new TestComponent(val);
                instances.put(val, tc);
                return tc;
            });
        });

        root.element();
        assertEquals(1, outerBuildCount[0]);
        assertEquals(1, innerBuildCount[0]);
        assertNotNull(instances.get("A"));
        assertFalse(instances.get("A").wasDisposed);

        // Inner switches from A to B
        inner.set("B");
        SignalDispatcher.flush();

        assertEquals(1, outerBuildCount[0], "Outer factory must NOT re-run when inner signal changes");
        assertEquals(2, innerBuildCount[0], "Inner factory must run for new inner signal value");
        assertTrue(instances.get("A").wasDisposed, "Previous inner component must be disposed");
        assertNotNull(instances.get("B"));
        assertFalse(instances.get("B").wasDisposed, "New inner component must be active");

        root.dispose();
    }

    @Test
    void nestedDynamicOuterCollapseCascadesDisposal() {
        Signal<Boolean> outer = Signal.of(true);
        Signal<String> inner = Signal.of("A");
        Map<String, TestComponent> instances = new HashMap<>();

        Dynamic<Boolean> root = Dynamic.of(outer, show -> {
            if (!Boolean.TRUE.equals(show))
                return null;
            return Dynamic.of(inner, val -> {
                TestComponent tc = new TestComponent(val);
                instances.put(val, tc);
                return tc;
            });
        });

        root.element();
        TestComponent compA = instances.get("A");
        assertNotNull(compA);
        assertFalse(compA.wasDisposed);
        assertTrue(root.container().visible);
        assertEquals(1, root.container().getChildren().size);

        // Collapse outer
        outer.set(false);
        SignalDispatcher.flush();

        assertFalse(root.container().visible, "Outer container must be hidden when collapsed");
        assertEquals(0, root.container().getChildren().size, "Outer container must have no children");
        assertTrue(compA.wasDisposed, "Active inner component must be recursively disposed on outer collapse");

        root.dispose();
    }

    @Test
    void nestedDynamicZombieEffectSuppressionAfterOuterCollapse() {
        Signal<Boolean> outer = Signal.of(true);
        Signal<String> inner = Signal.of("A");
        int[] innerBuildCount = new int[] { 0 };
        Map<String, TestComponent> instances = new HashMap<>();

        Dynamic<Boolean> root = Dynamic.of(outer, show -> {
            if (!Boolean.TRUE.equals(show))
                return null;
            return Dynamic.of(inner, val -> {
                innerBuildCount[0]++;
                TestComponent tc = new TestComponent(val);
                instances.put(val, tc);
                return tc;
            });
        });

        root.element();
        assertEquals(1, innerBuildCount[0]);

        // Collapse outer
        outer.set(false);
        SignalDispatcher.flush();
        assertTrue(instances.get("A").wasDisposed);

        // Mutate inner signal while outer is collapsed
        inner.set("B");
        SignalDispatcher.flush();

        assertEquals(1, innerBuildCount[0], "Inner factory must NOT be invoked after outer collapse");
        assertNull(instances.get("B"), "No component should be instantiated for mutated inner signal");

        root.dispose();
    }

    @Test
    void nestedDynamicReexpansionReinstantiatesSubtree() {
        Signal<Boolean> outer = Signal.of(true);
        Signal<String> inner = Signal.of("A");
        int[] innerBuildCount = new int[] { 0 };
        Map<String, TestComponent> instances = new HashMap<>();

        Dynamic<Boolean> root = Dynamic.of(outer, show -> {
            if (!Boolean.TRUE.equals(show))
                return null;
            return Dynamic.of(inner, val -> {
                innerBuildCount[0]++;
                TestComponent tc = new TestComponent(val + "-" + innerBuildCount[0]);
                instances.put(tc.id, tc);
                return tc;
            });
        });

        root.element();
        assertEquals(1, innerBuildCount[0]);
        TestComponent first = instances.get("A-1");
        assertNotNull(first);
        assertFalse(first.wasDisposed);

        // Collapse outer
        outer.set(false);
        SignalDispatcher.flush();
        assertTrue(first.wasDisposed);
        assertFalse(root.container().visible);

        // Re-expand outer
        outer.set(true);
        SignalDispatcher.flush();
        assertTrue(root.container().visible);
        assertEquals(2, innerBuildCount[0], "Inner factory must run when re-expanded");
        TestComponent second = instances.get("A-2");
        assertNotNull(second);
        assertFalse(second.wasDisposed);
        assertNotSame(first, second);

        // Subsequent inner updates on re-expanded subtree work properly
        inner.set("B");
        SignalDispatcher.flush();
        assertEquals(3, innerBuildCount[0]);
        assertTrue(second.wasDisposed);
        TestComponent third = instances.get("B-3");
        assertNotNull(third);
        assertFalse(third.wasDisposed);

        root.dispose();
    }

    @Test
    void nestedDynamicBatchedSignalUpdatesResolveDeterministically() {
        Signal<Boolean> outer = Signal.of(true);
        Signal<String> inner = Signal.of("A");
        int[] innerBuildCount = new int[] { 0 };
        Map<String, TestComponent> instances = new HashMap<>();

        Dynamic<Boolean> root = Dynamic.of(outer, show -> {
            if (!Boolean.TRUE.equals(show))
                return null;
            return Dynamic.of(inner, val -> {
                innerBuildCount[0]++;
                TestComponent tc = new TestComponent(val);
                instances.put(val, tc);
                return tc;
            });
        });

        root.element();
        assertEquals(1, innerBuildCount[0]);
        assertFalse(instances.get("A").wasDisposed);

        // Simultaneous update: collapse outer AND change inner in same flush
        outer.set(false);
        inner.set("B");
        SignalDispatcher.flush();

        assertFalse(root.container().visible);
        assertEquals(0, root.container().getChildren().size);
        assertTrue(instances.get("A").wasDisposed);
        assertEquals(1, innerBuildCount[0],
                "Inner factory must NOT be called for B when outer collapsed in same flush");
        assertNull(instances.get("B"));

        // Simultaneous update: re-expand outer AND change inner to C in same flush
        outer.set(true);
        inner.set("C");
        SignalDispatcher.flush();

        assertTrue(root.container().visible);
        assertEquals(2, innerBuildCount[0]);
        TestComponent compC = instances.get("C");
        assertNotNull(compC, "Subtree must immediately mount with latest inner value C");
        assertFalse(compC.wasDisposed);

        root.dispose();
    }

    @Test
    void nestedDynamicInsideLayoutContainerResizesAndCollapses() {
        Signal<Boolean> outer = Signal.of(true);
        Signal<Boolean> inner = Signal.of(true);

        Dynamic<Boolean> root = Dynamic.of(outer, showOuter -> {
            if (!Boolean.TRUE.equals(showOuter))
                return null;
            return Ui.column(() -> {
                Ui.dynamic(inner, showInner -> {
                    if (!Boolean.TRUE.equals(showInner))
                        return null;
                    return new TestComponent("leaf");
                });
            });
        });

        Table parent = new Table();
        Cell<?> cell = parent.add(root.element());
        parent.pack();

        assertTrue(root.container().visible);
        assertEquals(1, root.container().getChildren().size);

        // Collapse inner dynamic inside column
        inner.set(false);
        SignalDispatcher.flush();
        parent.layout();

        // Outer is still visible, but inner dynamic inside column is collapsed
        assertTrue(root.container().visible);

        // Collapse outer dynamic
        outer.set(false);
        SignalDispatcher.flush();
        parent.layout();

        assertFalse(root.container().visible);
        assertEquals(0f, CellAccess.minWidth(cell), 0.01f);
        assertEquals(0f, CellAccess.minHeight(cell), 0.01f);

        root.dispose();
    }

    @Test
    void dynamicSupportsElementAndTableConfig() {
        Signal<String> text = Signal.of("initial");
        Dynamic<String> dyn = new Dynamic<>(text, TestComponent::new)
                .width(250f)
                .height(150f)
                .visible(false)
                .margin(10f);

        Element el = dyn.element();
        assertEquals(250f, el.getWidth());
        assertEquals(150f, el.getHeight());
        assertFalse(el.visible);
        assertEquals(10f, dyn.table().getMarginTop());
        dyn.dispose();
    }
}
