package solim.reactive;


import solim.modifier.ElementCapability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static solim.modifier.Values.height;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.visible;
import static solim.modifier.Values.width;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import solim.core.Component;
import solim.core.SolimToken;
import arc.util.Nullable;
import solim.core.OwnershipContext;
import solim.modifier.Configurable;
import solim.modifier.ContainerCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.modifier.TableCapability;
import solim.runtime.SignalDispatcher;
import solim.test.SolimCoreEnv;

class ForEachComponentTest extends SolimCoreEnv {

    static class TestComponent extends Component
            implements Configurable<TestComponent>, ElementCapability, ContainerCapability<TestComponent> {
        final String id;
        boolean wasDisposed = false;
        private final PendingCellConfig constraints = new PendingCellConfig();
        private @Nullable Props<TestComponent> pending;

        TestComponent(String id) {
            this.id = id;
        }

        @Override
        public PendingCellConfig cellConfig() {
            return constraints;
        }

        public TestComponent children(@Nullable Runnable block) {
            applyPendingProps();
            OwnershipContext.push(this);
            try {
                if (block != null) {
                    block.run();
                }
            } finally {
                OwnershipContext.pop();
            }
            return this;
        }

        @Override
        public @Nullable solim.modifier.Props<TestComponent> pendingProps() {
            return pending;
        }

        @Override
        public void setPendingProps(@Nullable solim.modifier.Props<TestComponent> props) {
            this.pending = props;
        }

        @Override
        public TestComponent self() {
            return this;
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
        fe.items(id -> {
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
        fe.items(id -> {
            TestComponent tc = new TestComponent(id);
            created.put(id, tc);
            return tc;
        });
        fe.element();

        TestComponent a = created.get("A");
        assertEquals("A", a.id);

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
        fe.items(id -> {
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
        fe.items(id -> new TestComponent(id));
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
        fe.items(id -> new TestComponent(id));
        assertTrue(fe instanceof solim.modifier.Configurable);
        assertNotNull(fe.cellConfig());
        assertFalse(fe.cellConfig().growX);
        fe.props(solim.modifier.Values.growX());
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
                list.items(TestComponent::new);
                list.props(growX());
            }
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
        fe.items(TestComponent::new);
        fe.props(width(300f), prefWidth(300f), height(200f), prefHeight(200f), visible(true), padding(8f));

        Element el = fe.element();
        assertEquals(300f, el.getWidth());
        assertEquals(200f, el.getHeight());
        assertTrue(el.visible);
        assertEquals(8f, fe.table().getMarginTop());
        fe.dispose();
    }

    @Test
    void forEachCenterPropagatesAlignmentToParentCellAndChildren() {
        Signal<List<String>> items = Signal.of(Arrays.asList("1", "2"));
        AtomicReference<ForEach<String>> feHolder = new AtomicReference<>();

        solim.layout.Column col = new solim.layout.Column().children(() -> {
            ForEach<String> fe = new ForEach<>(items);
            fe.key(id -> id);
            fe.items(TestComponent::new);
            fe.center();
            solim.runtime.AttachmentStack.attachToParent(fe.element());
            feHolder.set(fe);
        });

        ForEach<String> fe = feHolder.get();
        Table colTable = col.table();
        Cell<?> feCell = colTable.getCell(fe.element());
        colTable.setSize(200f, 200f);
        colTable.validate();
        colTable.layout();

        assertEquals(Align.center, CellAccess.align(feCell));
        assertEquals(Align.center, fe.container().getAlign());
        assertEquals(Align.center, CellAccess.align(fe.container().getCells().first()));

        items.set(Arrays.asList("1", "2", "3"));
        SignalDispatcher.flush();
        colTable.layout();

        assertEquals(Align.center, CellAccess.align(feCell));
        assertEquals(Align.center, fe.container().getAlign());
        assertEquals(Align.center, CellAccess.align(fe.container().getCells().first()));

        fe.dispose();
        col.dispose();
    }

    @Test
    void forEachItemCellConstraintsAreDisposedWithItem() {
        Signal<List<String>> items = Signal.of(Arrays.asList("A", "B"));
        Signal<Float> padTopSignal = Signal.of(10f);
        Map<String, TestComponent> created = new HashMap<>();

        ForEach<String> fe = new ForEach<>(items);
        fe.key(id -> id);
        fe.items(id -> {
            TestComponent tc = new TestComponent(id);
            created.put(id, tc);
            tc.props(solim.modifier.Values.marginTop(padTopSignal));
            return tc;
        });
        fe.element();

        assertEquals(2, padTopSignal.observerCount(), "Both items should observe padTopSignal");

        items.set(Collections.singletonList("A"));
        SignalDispatcher.flush();

        assertTrue(created.get("B").wasDisposed, "the removed item must be disposed");
        assertEquals(1, padTopSignal.observerCount(),
                "a reconciled item's cell binding must have exactly one owner, so disposal releases it");

        fe.dispose();
        assertEquals(0, padTopSignal.observerCount(), "All cell constraints must be disposed when ForEach is disposed");
    }

    @Test
    void reusedItemDoesNotAccumulateCellBindingsAcrossReconciles() {
        Signal<List<String>> items = Signal.of(Collections.singletonList("A"));
        Signal<Float> padTopSignal = Signal.of(4f);
        Map<String, TestComponent> created = new HashMap<>();

        ForEach<String> fe = new ForEach<>(items);
        fe.key(id -> id);
        fe.items(id -> {
            TestComponent tc = new TestComponent(id);
            created.put(id, tc);
            tc.props(solim.modifier.Values.marginTop(padTopSignal));
            return tc;
        });
        fe.element();

        assertEquals(1, padTopSignal.observerCount(), "one binding after the first reconcile");

        items.set(Collections.singletonList("A"));
        items.set(Collections.singletonList("A"));
        SignalDispatcher.flush();

        assertEquals(1, padTopSignal.observerCount(),
                "re-attaching a reused item must release its previous cell binding, not stack a new one");
        assertSame(created.get("A").element(), fe.container().getChildren().first(),
                "the keyed item must be reused rather than rebuilt");

        fe.dispose();
        assertEquals(0, padTopSignal.observerCount());
    }

    @Test
    void childrenOnAlreadyBuiltForEachReconcilesImmediately() {
        Signal<List<String>> items = Signal.of(Arrays.asList("X", "Y"));
        ForEach<String> fe = new ForEach<>(items);
        fe.element();
        assertTrue(fe.isBuilt());
        assertEquals(0, fe.container().getChildren().size);

        fe.items(TestComponent::new);
        assertEquals(2, fe.container().getChildren().size, "Should reconcile immediately when isBuilt");

        SignalDispatcher.flush();
        fe.dispose();
    }
}
