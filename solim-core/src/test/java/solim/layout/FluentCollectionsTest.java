package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import arc.scene.Element;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.reactive.ForEach;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.runtime.ParentStack;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class FluentCollectionsTest extends SolimEnv {

    static class ItemComp extends BaseComponent {
        final String id;
        boolean disposed = false;

        ItemComp(String id) {
            this.id = id;
        }

        @Override
        protected Element build() {
            Element el = new Element();
            el.name = id;
            return el;
        }

        @Override
        protected void onDispose() {
            disposed = true;
        }
    }

    @Test
    void reactiveGridMountsReconcilesAddRemoveUpdateAndReflowsOnColumnsChange() {
        Signal<Integer> cols = Signal.of(2);
        Signal<List<String>> items = Signal.of(new ArrayList<String>(Arrays.asList("A", "B", "C")));
        final Map<String, ItemComp> created = new HashMap<String, ItemComp>();

        ReactiveGrid<String> grid = new ReactiveGrid<String>(items);
        grid.columns(cols).key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        }).gap(8f);
        grid.children(new arc.func.Func<String, Component>() {
            @Override
            public Component get(String item) {
                ItemComp c = new ItemComp(item);
                created.put(item, c);
                return c;
            }
        });
        grid.element();
        SignalDispatcher.flush();
        assertEquals(3, grid.table().getChildren().size);

        ItemComp aBefore = created.get("A");
        ItemComp cBefore = created.get("C");

        items.set(new ArrayList<String>(Arrays.asList("A", "B", "C", "D")));
        SignalDispatcher.flush();
        assertEquals(4, grid.table().getChildren().size);
        assertNotNull(created.get("D"));

        items.set(new ArrayList<String>(Arrays.asList("A", "C")));
        SignalDispatcher.flush();
        assertEquals(2, grid.table().getChildren().size);
        assertSame(aBefore.element(), grid.table().getChildren().get(0));
        assertSame(cBefore.element(), grid.table().getChildren().get(1));

        cols.set(3);
        SignalDispatcher.flush();
        assertEquals(2, grid.table().getChildren().size);
        grid.dispose();
    }

    @Test
    void reactiveGridColumnsDefaultToOne() {
        Signal<List<String>> items = Signal.of(Arrays.asList("A", "B"));
        final AtomicReference<GridItemContext> captured = new AtomicReference<GridItemContext>();
        ReactiveGrid<String> grid = new ReactiveGrid<String>(items);
        grid.key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        });
        grid.children(new arc.func.Func2<String, GridItemContext, Component>() {
            @Override
            public Component get(String item, GridItemContext ctx) {
                captured.set(ctx);
                return new ItemComp(item);
            }
        });
        grid.element();
        SignalDispatcher.flush();
        assertNotNull(captured.get());
        assertEquals(1, captured.get().columnCount().get().intValue());
        assertNotNull(captured.get().itemWidth().get());
        grid.dispose();
    }

    @Test
    void reactiveGridFunctionFactoryReceivesUsableContext() {
        Signal<Integer> cols = Signal.of(3);
        Signal<List<String>> items = Signal.of(Arrays.asList("A"));
        final AtomicReference<GridItemContext> captured = new AtomicReference<GridItemContext>();
        ReactiveGrid<String> grid = new ReactiveGrid<String>(items);
        grid.columns(cols).key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        });
        grid.children(new arc.func.Func2<String, GridItemContext, Component>() {
            @Override
            public Component get(String item, GridItemContext ctx) {
                captured.set(ctx);
                return new ItemComp(item);
            }
        });
        grid.element();
        SignalDispatcher.flush();
        assertNotNull(captured.get());
        assertEquals(3, captured.get().columnCount().get().intValue());
        assertNotNull(captured.get().itemWidth().get());
        grid.dispose();
    }

    @Test
    void reactiveGridStaticIterableMounts() {
        ReactiveGrid<String> grid = ReactiveGrid.of(Readable.of(Arrays.asList("A", "B")));
        grid.key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        });
        grid.children(ItemComp::new);
        grid.element();
        SignalDispatcher.flush();
        assertEquals(2, grid.table().getChildren().size);
        grid.dispose();
    }

    @Test
    void reactiveGridRendersOneElementPerItemInOrderWithKeyedReuse() {
        Signal<List<String>> items = Signal.of(new ArrayList<String>(Arrays.asList("A", "B", "C")));
        final Map<String, ItemComp> created = new HashMap<String, ItemComp>();
        ReactiveGrid<String> grid = new ReactiveGrid<String>(items);
        grid.columns(2).key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        });
        grid.children(new arc.func.Func<String, Component>() {
            @Override
            public Component get(String item) {
                ItemComp c = new ItemComp(item);
                created.put(item, c);
                return c;
            }
        });
        grid.element();
        SignalDispatcher.flush();
        assertEquals(3, grid.table().getChildren().size);
        assertSame(created.get("A").element(), grid.table().getChildren().get(0));
        assertSame(created.get("B").element(), grid.table().getChildren().get(1));
        assertSame(created.get("C").element(), grid.table().getChildren().get(2));

        items.set(new ArrayList<String>(Arrays.asList("C", "B", "A")));
        SignalDispatcher.flush();
        assertEquals(3, grid.table().getChildren().size);
        assertSame(created.get("C").element(), grid.table().getChildren().get(0));
        assertSame(created.get("B").element(), grid.table().getChildren().get(1));
        assertSame(created.get("A").element(), grid.table().getChildren().get(2));
        grid.dispose();
    }

    @Test
    void reactiveGridEmptyRendersWhenEmptyAndReplacedWhenNonEmpty() {
        Signal<List<String>> items = Signal.of(Collections.<String>emptyList());
        ReactiveGrid<String> grid = new ReactiveGrid<String>(items);
        grid.columns(2).key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        });
        grid.empty(new Runnable() {
            @Override
            public void run() {
                Element el = new Element();
                el.name = "empty-marker";
                ParentStack.add(el);
            }
        });
        grid.children(ItemComp::new);
        grid.element();
        SignalDispatcher.flush();
        assertEquals(1, grid.table().getChildren().size);

        items.set(Arrays.asList("A", "B"));
        SignalDispatcher.flush();
        assertEquals(2, grid.table().getChildren().size);
        grid.dispose();
    }

    @Test
    void reactiveGridEmptyViewRendersWhenEmptyAndReplacedWhenNonEmpty() {
        Signal<List<String>> items = Signal.of(Collections.<String>emptyList());
        final AtomicInteger emptyDisposals = new AtomicInteger();
        ReactiveGrid<String> grid = new ReactiveGrid<String>(items);
        grid.columns(2).key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        });
        grid.emptyView(new arc.func.Prov<Component>() {
            @Override
            public Component get() {
                ItemComp c = new ItemComp("empty") {
                    @Override
                    protected void onDispose() {
                        super.onDispose();
                        emptyDisposals.incrementAndGet();
                    }
                };
                return c;
            }
        });
        grid.children(ItemComp::new);
        grid.element();
        SignalDispatcher.flush();
        assertEquals(1, grid.table().getChildren().size);

        items.set(Arrays.asList("A"));
        SignalDispatcher.flush();
        assertEquals(1, grid.table().getChildren().size);
        assertEquals(1, emptyDisposals.get());
        grid.dispose();
        assertEquals(1, emptyDisposals.get());
    }

    @Test
    void missingFactoryRendersEmptyAndLaterChildrenRefreshesBuiltContent() {
        Signal<List<String>> items = Signal.of(Arrays.asList("A", "B"));
        ReactiveGrid<String> grid = new ReactiveGrid<String>(items);
        grid.columns(2).key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        });
        grid.element();
        SignalDispatcher.flush();
        assertEquals(0, grid.table().getChildren().size);

        grid.children(ItemComp::new);
        SignalDispatcher.flush();
        assertEquals(2, grid.table().getChildren().size);
        grid.dispose();
    }

    @Test
    void forEachMissingFactoryRendersEmptyAndLaterChildrenRefreshes() {
        Signal<List<String>> items = Signal.of(Arrays.asList("A"));
        ForEach<String> fe = new ForEach<String>(items);
        fe.key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        });
        fe.element();
        SignalDispatcher.flush();
        assertEquals(0, fe.container().getChildren().size);

        fe.children(ItemComp::new);
        SignalDispatcher.flush();
        assertEquals(1, fe.container().getChildren().size);
        fe.dispose();
    }

    @Test
    void forEachIdentityDefaultReconcilesByEquals() {
        Signal<List<String>> items = Signal.of(new ArrayList<String>(Arrays.asList("A", "B")));
        final Map<String, ItemComp> created = new HashMap<String, ItemComp>();
        ForEach<String> fe = new ForEach<String>(items);
        fe.children(new arc.func.Func<String, Component>() {
            @Override
            public Component get(String item) {
                ItemComp c = new ItemComp(item);
                created.put(item, c);
                return c;
            }
        });
        fe.element();
        SignalDispatcher.flush();
        ItemComp aBefore = created.get("A");

        items.set(new ArrayList<String>(Arrays.asList(new String("A"), new String("B"), "C")));
        SignalDispatcher.flush();
        assertEquals(3, fe.container().getChildren().size);
        assertSame(aBefore, created.get("A"));
        fe.dispose();
    }

    @Test
    void forEachExplicitKeyRestoresReuseAcrossOrderChange() {
        Signal<List<String>> items = Signal.of(new ArrayList<String>(Arrays.asList("A", "B", "C")));
        final Map<String, ItemComp> created = new HashMap<String, ItemComp>();
        ForEach<String> fe = new ForEach<String>(items);
        fe.key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        });
        fe.children(new arc.func.Func<String, Component>() {
            @Override
            public Component get(String item) {
                ItemComp c = new ItemComp(item);
                created.put(item, c);
                return c;
            }
        });
        fe.element();
        SignalDispatcher.flush();

        items.set(new ArrayList<String>(Arrays.asList("C", "B", "A")));
        SignalDispatcher.flush();
        assertEquals(3, fe.container().getChildren().size);
        assertSame(created.get("C").element(), fe.container().getChildren().get(0));
        assertSame(created.get("B").element(), fe.container().getChildren().get(1));
        assertSame(created.get("A").element(), fe.container().getChildren().get(2));
        fe.dispose();
    }

    @Test
    void forEachMountsAndPreservesAcrossCollectionUpdates() {
        Signal<List<String>> items = Signal.of(new ArrayList<String>(Arrays.asList("A", "B")));
        final Map<String, ItemComp> created = new HashMap<String, ItemComp>();
        ForEach<String> fe = new ForEach<String>(items);
        fe.key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        });
        fe.children(new arc.func.Func<String, Component>() {
            @Override
            public Component get(String item) {
                ItemComp c = new ItemComp(item);
                created.put(item, c);
                return c;
            }
        });
        fe.element();
        SignalDispatcher.flush();
        assertEquals(2, fe.container().getChildren().size);
        ItemComp aBefore = created.get("A");

        items.set(new ArrayList<String>(Arrays.asList("A", "B", "C")));
        SignalDispatcher.flush();
        assertEquals(3, fe.container().getChildren().size);
        assertSame(aBefore, created.get("A"));
        fe.dispose();
        assertTrue(created.get("A").disposed);
    }

    @Test
    void virtualListMountsVisiblePlusOverscanAndUpdatesOnSignalChange() {
        List<String> raw = new ArrayList<String>();
        for (int i = 0; i < 100; i++) {
            raw.add("item-" + i);
        }
        Signal<List<String>> items = Signal.of(raw);
        VirtualList<String> vl = new VirtualList<String>(items, new ItemHeightProvider<String>() {
            @Override
            public float getHeight(String item, float width) {
                return 50f;
            }
        });
        vl.key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        });
        vl.overscan(2).gap(0f);
        vl.onReachTop(50f, new Runnable() {
            @Override
            public void run() {
            }
        });
        vl.children(ItemComp::new);
        vl.element();
        SignalDispatcher.flush();
        assertEquals(5000f, vl.getTotalHeight(), 0.001f);
        assertTrue(vl.getMountedCount() <= 25);
        assertTrue(vl.getMountedCount() >= 10);

        items.set(raw.subList(0, 10));
        SignalDispatcher.flush();
        assertEquals(500f, vl.getTotalHeight(), 0.001f);
        vl.dispose();
    }

    @Test
    void virtualListReferenceBeforeChildrenSupportsPostConstructionCalls() {
        Signal<List<String>> items = Signal.of(Arrays.asList("A", "B", "C"));
        VirtualList<String> vl = new VirtualList<String>(items, new ItemHeightProvider<String>() {
            @Override
            public float getHeight(String item, float width) {
                return 50f;
            }
        });
        vl.key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        });
        vl.overscan(2).gap(5f);
        vl.children(ItemComp::new);
        vl.element();
        SignalDispatcher.flush();
        assertTrue(vl.getMountedCount() >= 1);
        vl.gap(10f);
        SignalDispatcher.flush();
        assertTrue(vl.getMountedCount() >= 1);
        vl.dispose();
    }

    @Test
    void disposingGridDisposesItemsAndSurvivesSignalChanges() {
        Signal<Integer> cols = Signal.of(2);
        Signal<List<String>> items = Signal.of(new ArrayList<String>(Arrays.asList("A", "B")));
        final Map<String, ItemComp> created = new HashMap<String, ItemComp>();
        ReactiveGrid<String> grid = new ReactiveGrid<String>(items);
        grid.columns(cols).key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        });
        grid.children(new arc.func.Func<String, Component>() {
            @Override
            public Component get(String item) {
                ItemComp c = new ItemComp(item);
                created.put(item, c);
                return c;
            }
        });
        grid.element();
        SignalDispatcher.flush();
        grid.dispose();
        assertTrue(created.get("A").disposed);
        assertTrue(created.get("B").disposed);

        items.set(Arrays.asList("C"));
        cols.set(1);
        SignalDispatcher.flush();
    }

    @Test
    void disposingForEachDisposesItemsExactlyOnce() {
        Signal<List<String>> items = Signal.of(new ArrayList<String>(Arrays.asList("A")));
        final AtomicInteger disposals = new AtomicInteger();
        ForEach<String> fe = new ForEach<String>(items);
        fe.key(new arc.func.Func<String, Object>() {
            @Override
            public Object get(String s) {
                return s;
            }
        });
        fe.children(new arc.func.Func<String, Component>() {
            @Override
            public Component get(String item) {
                return new ItemComp(item) {
                    @Override
                    protected void onDispose() {
                        super.onDispose();
                        disposals.incrementAndGet();
                    }
                };
            }
        });
        fe.element();
        SignalDispatcher.flush();
        fe.dispose();
        fe.dispose();
        assertEquals(1, disposals.get());
    }
}
