package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import arc.struct.Seq;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import solim.core.BaseComponent;
import solim.layout.Column;
import solim.layout.Row;
import solim.runtime.ParentStack;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class DynamicComponentTest extends SolimEnv {

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
        Dynamic<Boolean> dyn = Dynamic.of(toggle, val -> {
            TestComponent tc = new TestComponent(val ? "A" : "B");
            instances.put(tc.id, tc);
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
        Dynamic<Boolean> dyn = Dynamic.of(toggle, val -> new TestComponent(val ? "A" : "B"));
        dyn.element();
        assertEquals(1, dyn.container().getChildren().size);
        dyn.dispose();
    }

    @Test
    void dynamicSwitchesContent() {
        Signal<Boolean> toggle = Signal.of(true);
        Map<String, TestComponent> instances = new HashMap<>();
        Dynamic<Boolean> dyn = Dynamic.of(toggle, val -> {
            TestComponent tc = new TestComponent(val ? "A" : "B");
            instances.put(tc.id, tc);
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
        Dynamic<Boolean> dyn = Dynamic.of(toggle, val -> {
            TestComponent tc = new TestComponent(val ? "A" : "B");
            instances.put(tc.id, tc);
        });
        dyn.element();
        dyn.dispose();
        assertTrue(instances.get("A").wasDisposed);
    }

    @Test
    void dynamicChildDoesNotGrowByDefault() {
        Signal<String> source = Signal.of("A");
        Dynamic<String> dyn = Dynamic.of(source, val -> new TestComponent(val));
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

        Dynamic<Boolean> dyn = Dynamic.of(switcher, val -> {
            factoryBuildCount[0]++;
            String text = internalChildSignal.get();
            new TestComponent(val + "-" + text);
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
        Dynamic<String> dyn = Dynamic.of(source, val -> {
            if ("show".equals(val)) {
                new TestComponent("active");
            }
        });

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
        Dynamic<String> dyn = Dynamic.of(source, val -> {
            if ("show".equals(val)) {
                new TestComponent("active");
            }
        });

        Table parent = new Table();
        parent.add(dyn.element());
        parent.pack();

        source.set(null);
        SignalDispatcher.flush();
        parent.layout();

        assertFalse(dyn.container().visible);

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
        Dynamic<String> dyn = Dynamic.of(source, val -> {
            if (val != null) {
                new TestComponent("item");
            }
        });

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
        Dynamic<String> dyn = Dynamic.of(source, val -> {
            if ("show".equals(val)) {
                new TestComponent("dynamic");
            }
        });

        Table parent = new Table();
        parent.add(dyn.element());
        parent.setSize(400f, 300f);
        parent.validate();
        parent.layout();

        assertTrue(dyn.container().visible);
        assertEquals(1, dyn.container().getChildren().size);

        source.set(null);
        SignalDispatcher.flush();
        parent.layout();
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
    void dynamicPreservesTopRightAlignmentWithoutGrowX() {
        Signal<Boolean> state = Signal.of(true);
        Dynamic<Boolean> dyn = Dynamic.of(state, s -> {
            Row row = new Row();
            row.cellConfig().prefWidth = Readable.of(100f);
            row.cellConfig().prefHeight = Readable.of(40f);
        }).top().right();

        assertFalse(dyn.cellConfig().growX, "Dynamic must not growX by default");

        Column col = new Column().fillParent().top().right().children(() -> {
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
            if (!Boolean.TRUE.equals(show)) {
                return;
            }
            Dynamic.of(inner, val -> {
                innerBuildCount[0]++;
                TestComponent tc = new TestComponent(val);
                instances.put(val, tc);
            });
        });

        root.element();
        assertEquals(1, outerBuildCount[0]);
        assertEquals(1, innerBuildCount[0]);
        assertNotNull(instances.get("A"));
        assertFalse(instances.get("A").wasDisposed);

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
            if (!Boolean.TRUE.equals(show)) {
                return;
            }
            Dynamic.of(inner, val -> {
                TestComponent tc = new TestComponent(val);
                instances.put(val, tc);
            });
        });

        root.element();
        TestComponent compA = instances.get("A");
        assertNotNull(compA);
        assertFalse(compA.wasDisposed);
        assertTrue(root.container().visible);
        assertEquals(1, root.container().getChildren().size);

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
            if (!Boolean.TRUE.equals(show)) {
                return;
            }
            Dynamic.of(inner, val -> {
                innerBuildCount[0]++;
                TestComponent tc = new TestComponent(val);
                instances.put(val, tc);
            });
        });

        root.element();
        assertEquals(1, innerBuildCount[0]);

        outer.set(false);
        SignalDispatcher.flush();
        assertTrue(instances.get("A").wasDisposed);

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
            if (!Boolean.TRUE.equals(show)) {
                return;
            }
            Dynamic.of(inner, val -> {
                innerBuildCount[0]++;
                TestComponent tc = new TestComponent(val + "-" + innerBuildCount[0]);
                instances.put(tc.id, tc);
            });
        });

        root.element();
        assertEquals(1, innerBuildCount[0]);
        TestComponent first = instances.get("A-1");
        assertNotNull(first);
        assertFalse(first.wasDisposed);

        outer.set(false);
        SignalDispatcher.flush();
        assertTrue(first.wasDisposed);
        assertFalse(root.container().visible);

        outer.set(true);
        SignalDispatcher.flush();
        assertTrue(root.container().visible);
        assertEquals(2, innerBuildCount[0], "Inner factory must run when re-expanded");
        TestComponent second = instances.get("A-2");
        assertNotNull(second);
        assertFalse(second.wasDisposed);
        assertNotSame(first, second);

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
            if (!Boolean.TRUE.equals(show)) {
                return;
            }
            Dynamic.of(inner, val -> {
                innerBuildCount[0]++;
                TestComponent tc = new TestComponent(val);
                instances.put(val, tc);
            });
        });

        root.element();
        assertEquals(1, innerBuildCount[0]);
        assertFalse(instances.get("A").wasDisposed);

        outer.set(false);
        inner.set("B");
        SignalDispatcher.flush();

        assertFalse(root.container().visible);
        assertEquals(0, root.container().getChildren().size);
        assertTrue(instances.get("A").wasDisposed);
        assertEquals(1, innerBuildCount[0],
                "Inner factory must NOT be called for B when outer collapsed in same flush");
        assertNull(instances.get("B"));

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
    void dynamicMultipleRoots() {
        Signal<String> source = Signal.of("items");
        List<TestComponent> created = new ArrayList<>();

        Dynamic<String> dyn = Dynamic.of(source, val -> {
            created.add(new TestComponent("a"));
            created.add(new TestComponent("b"));
        });

        dyn.element();
        assertEquals(2, dyn.container().getChildren().size,
                "Void factory with multiple roots should mount all roots");

        dyn.dispose();
        assertTrue(created.get(0).wasDisposed);
        assertTrue(created.get(1).wasDisposed);
    }

    @Test
    void dynamicSupportsElementAndTableConfig() {
        Signal<String> text = Signal.of("initial");
        Dynamic<String> dyn = Dynamic.of(text, val -> new TestComponent(val))
                .width(250f)
                .height(150f)
                .visible(false)
                .padding(10f);

        Element el = dyn.element();
        assertEquals(250f, el.getWidth());
        assertEquals(150f, el.getHeight());
        assertFalse(el.visible);
        assertEquals(10f, dyn.table().getMarginTop());
        dyn.dispose();
    }

    @Test
    void dynamicFactoryInvokedWithNullWhenSourceEmitsNull() {
        Signal<String> source = Signal.of("initial");
        boolean[] calledWithNull = new boolean[] { false };
        Dynamic<String> dyn = Dynamic.of(source, val -> {
            if (val == null) {
                calledWithNull[0] = true;
                return;
            }
            new TestComponent(val);
        });
        dyn.element();
        assertFalse(calledWithNull[0], "Factory must not be called with null initially");

        source.set(null);
        SignalDispatcher.flush();
        assertTrue(calledWithNull[0], "Factory must be called with null when source emits null");
        dyn.dispose();
    }

    @Test
    void dynamicRendersFallbackComponentWhenSourceEmitsNull() {
        Signal<String> source = Signal.of(null);
        Map<String, TestComponent> instances = new HashMap<>();
        Dynamic<String> dyn = Dynamic.of(source, val -> {
            TestComponent comp = new TestComponent(val != null ? "active:" + val : "fallback");
            instances.put(comp.id, comp);
        });

        Table parent = new Table();
        parent.add(dyn.element());
        parent.pack();

        assertTrue(dyn.container().visible, "Container must be visible when factory returns a fallback component for null source");
        assertEquals(1, dyn.container().getChildren().size, "Fallback component must be added to container");
        TestComponent fallbackComp = instances.get("fallback");
        assertNotNull(fallbackComp, "Fallback component instance must exist");
        assertFalse(fallbackComp.wasDisposed);

        source.set("hello");
        SignalDispatcher.flush();
        parent.layout();

        assertTrue(fallbackComp.wasDisposed, "Fallback component must be disposed when transitioning to active value");
        TestComponent activeComp = instances.get("active:hello");
        assertNotNull(activeComp);
        assertFalse(activeComp.wasDisposed);
        assertTrue(dyn.container().visible);
        assertEquals(1, dyn.container().getChildren().size);

        source.set(null);
        SignalDispatcher.flush();
        parent.layout();

        assertTrue(activeComp.wasDisposed, "Active component must be disposed when transitioning back to null");
        assertEquals(1, dyn.container().getChildren().size);
        assertTrue(dyn.container().visible);

        dyn.dispose();
    }

    @Test
    void dynamicCollapsesWhenFactoryReturnsNullForNullSource() {
        Signal<String> source = Signal.of("active");
        Dynamic<String> dyn = Dynamic.of(source, val -> {
            if (val != null) {
                new TestComponent(val);
            }
        });

        Table parent = new Table();
        parent.defaults().padTop(6f).padBottom(6f);
        Cell<?> cell = parent.add(dyn.element());
        parent.pack();

        assertTrue(dyn.container().visible);
        assertEquals(1, dyn.container().getChildren().size);

        source.set(null);
        SignalDispatcher.flush();
        parent.layout();

        assertFalse(dyn.container().visible, "Container must be hidden when factory returns null for null source");
        assertEquals(0, dyn.container().getChildren().size);
        assertEquals(0f, CellAccess.padTop(cell), 0.01f);
        assertEquals(0f, CellAccess.padBottom(cell), 0.01f);

        dyn.dispose();
    }

    @Test
    void dynamicThrowsWhenFactoryIsNull() {
        Signal<String> source = Signal.of("value");
        assertThrows(NullPointerException.class, () -> Dynamic.of(source, null));
    }

    @Test
    void rootStaysMountedWhileUnrelatedSignalsChange() {
        Signal<String> label = Signal.of("first");
        Signal<String> source = Signal.of("mode");
        List<TestComponent> created = new ArrayList<>();

        Dynamic<String> dyn = Dynamic.of(source, val -> {
            created.add(new TestComponent(val));
        });

        dyn.element();
        TestComponent root = created.get(0);
        assertNotNull(root);

        label.set("changed");
        SignalDispatcher.flush();
        assertFalse(root.wasDisposed, "Root must stay mounted while unrelated signals change");

        dyn.dispose();
    }

    @Test
    void perRootCellConfigAppliesToOwnCell() {
        Signal<String> source = Signal.of("go");

        Dynamic<String> dyn = Dynamic.of(source, val -> {
            new Row().growX();
            new TestComponent("fluid");
        });

        dyn.element();
        @SuppressWarnings("rawtypes")
        Seq<Cell> cells = dyn.container().getCells();
        assertEquals(2, cells.size);
        assertEquals(1, CellAccess.expandX(cells.get(0)),
                "First root's growX config must apply to its own cell");
        assertEquals(0, CellAccess.expandX(cells.get(1)),
                "Second root without config must not grow");

        dyn.dispose();
    }

    @Test
    void voidFactoryExceptionDisposesPartialRootsAndCollapses() {
        Signal<String> source = Signal.of("start");
        List<TestComponent> created = new ArrayList<>();
        int[] boomPhase = new int[] { 0 };

        Dynamic<String> dyn = Dynamic.of(source, val -> {
            created.add(new TestComponent("first:" + val));
            if (boomPhase[0] == 1) {
                throw new IllegalStateException("boom");
            }
        });

        dyn.element();
        assertFalse(created.get(0).wasDisposed);
        assertEquals(1, dyn.container().getChildren().size);

        boomPhase[0] = 1;
        source.set("next");
        SignalDispatcher.flush();

        assertTrue(created.get(1).wasDisposed,
                "Root created before the exception must be disposed");
        assertEquals(0, dyn.container().getChildren().size,
                "Container must be empty after failed capture");

        dyn.dispose();
    }

    @Test
    void voidFactoryCollapseWithNoRoots() {
        Signal<String> source = Signal.of("show");
        Dynamic<String> dyn = Dynamic.of(source, val -> {
            if ("show".equals(val)) {
                new TestComponent("active");
            }
        });

        Table parent = new Table();
        parent.add(dyn.element());
        parent.pack();

        assertTrue(dyn.container().visible);
        assertEquals(1, dyn.container().getChildren().size);

        source.set("hide");
        SignalDispatcher.flush();
        parent.layout();

        assertFalse(dyn.container().visible);
        assertEquals(0, dyn.container().getChildren().size);

        dyn.dispose();
    }
}
