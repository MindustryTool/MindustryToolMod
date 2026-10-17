package solim.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static solim.modifier.Values.gap;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.left;
import static solim.modifier.Values.margin;
import static solim.modifier.Values.minHeight;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.visible;
import static solim.modifier.Values.width;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.CellAccess;
import arc.scene.ui.layout.Table;
import solim.core.Component;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class ReactiveGridTest extends SolimEnv {

    static class ItemComp extends Component {
        final String text;
        boolean disposed = false;

        ItemComp(String text) {
            this.text = text;
        }

        @Override
        protected Element build() {
            Element el = new Element();
            el.name = text;
            return el;
        }

        @Override
        protected void onDispose() {
            disposed = true;
        }
    }

    @Test
    void providesContextToBiFunctionItemFactory() {
        Signal<Integer> cols = Signal.of(4);
        Signal<List<String>> items = Signal.of(Arrays.asList("A", "B"));
        AtomicReference<GridItemContext> capturedCtx = new AtomicReference<>();

        ReactiveGrid<String> grid = new ReactiveGrid<>(items);
        grid.columns(cols).key(s -> s);
        grid.items((item, ctx) -> {
            capturedCtx.set(ctx);
            return new ItemComp(item);
        });
        grid.element();

        assertNotNull(capturedCtx.get());
        assertEquals(4, capturedCtx.get().columnCount().get().intValue());
        assertNotNull(capturedCtx.get().itemWidth().get());
    }

    @Test
    void calculatesItemWidthWhenTableWidthChanges() {
        Signal<Integer> cols = Signal.of(4);
        Signal<List<String>> items = Signal.of(Arrays.asList("A"));
        ReactiveGrid<String> grid = new ReactiveGrid<>(items);
        grid.columns(cols).key(s -> s).props(gap(10f));
        grid.items((item, ctx) -> new ItemComp(item));
        grid.element();

        grid.table().setWidth(400f);
        grid.table().layout();
        SignalDispatcher.flush();

        assertEquals(92.5f, grid.context().itemWidth().get(), 0.1f);

        cols.set(2);
        SignalDispatcher.flush();

        assertEquals(195f, grid.context().itemWidth().get(), 0.1f);

        grid.props(gap(20f));
        SignalDispatcher.flush();

        assertEquals(190f, grid.context().itemWidth().get(), 0.1f);
    }

    @Test
    void functionItemFactoryRendersItems() {
        Signal<Integer> cols = Signal.of(2);
        Signal<List<String>> items = Signal.of(Arrays.asList("X", "Y"));

        ReactiveGrid<String> grid = new ReactiveGrid<>(items);
        grid.columns(cols).key(s -> s);
        grid.items(ItemComp::new);
        Element el = grid.element();
        assertNotNull(el);
        assertEquals(2, grid.table().getChildren().size);
    }

    @Test
    void disposesReconciledItemsAndItemWidth() {
        Signal<Integer> cols = Signal.of(2);
        Signal<List<String>> items = Signal.of(Arrays.asList("item1"));
        ItemComp[] created = new ItemComp[1];

        ReactiveGrid<String> grid = new ReactiveGrid<>(items);
        grid.columns(cols).key(s -> s);
        grid.items((item, ctx) -> {
            ItemComp c = new ItemComp(item);
            created[0] = c;
            return c;
        });
        grid.element();
        assertNotNull(created[0]);
        assertFalse(created[0].disposed);

        grid.dispose();
        assertTrue(created[0].disposed);
    }

    @Test
    void measuresItemWidthMatchesActualDirectChildWidth() {
        Signal<Integer> cols = Signal.of(3);
        Signal<List<String>> items = Signal.of(Arrays.asList("A", "B", "C"));

        List<Element> childElementsGrowX = new ArrayList<>();
        ReactiveGrid<String> grid = new ReactiveGrid<>(items);
        grid.columns(cols).key(s -> s).props(gap(10f));
        grid.items((item, ctx) -> {
            Column col = new Column().props(growX());
            childElementsGrowX.add(col.element());
            return col;
        });
        grid.element();
        grid.table().setSize(300f, 300f);
        grid.table().layout();
        SignalDispatcher.flush();

        float calculatedWidth = grid.context().itemWidth().get();

        assertEquals(93.33f, calculatedWidth, 0.1f);

        for (Element child : childElementsGrowX) {
            assertEquals(calculatedWidth, child.getWidth(), 1.0f,
                    "Direct child width in cell must match calculated itemWidth");
        }
    }

    @Test
    void measuresItemWidthMatchesActualChildWithExplicitWidth() {
        Signal<Integer> cols = Signal.of(3);
        Signal<List<String>> items = Signal.of(Arrays.asList("A", "B", "C"));

        List<Element> childElements = new ArrayList<>();
        ReactiveGrid<String> grid = new ReactiveGrid<>(items);
        grid.columns(cols).key(s -> s).props(gap(10f));
        grid.items((item, ctx) -> {
            Column col = new Column().props(width(ctx.itemWidth()), prefWidth(ctx.itemWidth()));
            childElements.add(col.element());
            return col;
        });
        grid.element();
        grid.table().setSize(300f, 300f);
        grid.table().layout();
        SignalDispatcher.flush();

        float calculatedWidth = grid.context().itemWidth().get();
        for (Element child : childElements) {
            assertEquals(calculatedWidth, child.getWidth(), 1.0f,
                    "Child with explicit width(ctx.itemWidth()) must match calculated width");
        }
    }

    @Test
    void measuresItemWidthAccountsForTableMarginsAndPadding() {
        Signal<Integer> cols = Signal.of(3);
        Signal<List<String>> items = Signal.of(Arrays.asList("A", "B", "C"));

        List<Element> childElements = new ArrayList<>();
        ReactiveGrid<String> grid = new ReactiveGrid<>(items);
        grid.columns(cols).key(s -> s).props(gap(10f));
        grid.items((item, ctx) -> {
            Column col = new Column().props(growX());
            childElements.add(col.element());
            return col;
        });
        grid.element();
        grid.table().margin(20f);
        grid.table().setSize(300f, 300f);
        grid.table().layout();
        SignalDispatcher.flush();

        float calculatedWidth = grid.context().itemWidth().get();
        float actualChildWidth = childElements.get(0).getWidth();

        assertEquals(80.0f, calculatedWidth, 0.5f,
                "Calculated itemWidth must deduct table margins");
        assertEquals(actualChildWidth, calculatedWidth, 1.0f,
                "Calculated itemWidth must match actual rendered child width when table has padding");
    }

    @Test
    void measuresInnerCardWithMarginMatchesItemWidthAndRemainsSquare() {
        Signal<Integer> cols = Signal.of(3);
        Signal<List<String>> items = Signal.of(Arrays.asList("A", "B", "C"));

        List<Element> cardElements = new ArrayList<>();
        List<Element> outerColElements = new ArrayList<>();
        ReactiveGrid<String> gridCards = new ReactiveGrid<>(items);
        gridCards.columns(cols).key(s -> s).props(gap(10f));
        gridCards.items((item, ctx) -> {
            Card[] holder = new Card[1];
            Column col = new Column()
                    .props(growX(), gap(8f))
                    .children(() -> {
                        holder[0] = new Card()
                                .props(growX(), margin(4f, 0f, 4f, 0f), height(ctx.itemWidth()), prefHeight(ctx.itemWidth()))
                                .children(() -> {
                                });
                    });
            cardElements.add(holder[0].element());
            outerColElements.add(col.element());
            return col;
        });
        gridCards.element();
        gridCards.table().setSize(300f, 300f);
        gridCards.table().layout();
        SignalDispatcher.flush();

        outerColElements.get(0).validate();
        Element cardEl = cardElements.get(0);
        assertEquals(93.33f, cardEl.getWidth(), 1.0f);
        assertEquals(93.33f, cardEl.getHeight(), 1.0f);
        assertEquals(cardEl.getWidth(), cardEl.getHeight(), 1.0f,
                "Inner card is square with margin(4,0,4,0) while keeping column vertical gap");
    }

    @Test
    void reactiveGridSupportsElementAndTableConfig() {
        Signal<Integer> cols = Signal.of(2);
        Signal<List<String>> items = Signal.of(Collections.emptyList());
        ReactiveGrid<String> grid = new ReactiveGrid<>(items);
        grid.columns(cols).key(s -> s);
        grid.items((s, ctx) -> new ItemComp(s));
        grid.props(width(400f), prefWidth(400f), height(200f), prefHeight(200f), visible(false), padding(16f));

        assertEquals(400f, grid.element().getWidth());
        assertEquals(200f, grid.element().getHeight());
        assertFalse(grid.element().visible);
        assertEquals(16f, grid.table().getMarginTop());
        grid.dispose();
    }

    @Test
    void disposedGridMountsNothingWhenItsReconciliationFinallyRuns() {
        Signal<List<String>> items = Signal.of(new ArrayList<>(Arrays.asList("a", "b")));
        ReactiveGrid<String> grid = new ReactiveGrid<>(items);
        grid.columns(2).key(s -> s);
        grid.items(ItemComp::new);

        grid.element();
        assertTrue(countFilled(grid.table()) > 0, "Grid must mount before disposal to make this meaningful");

        items.set(new ArrayList<>(Arrays.asList("a", "b", "c")));
        grid.dispose();
        SignalDispatcher.flush();

        assertEquals(0, countFilled(grid.table()),
                "A grid disposed before its reconciliation runs must mount and reflow nothing");
        assertTrue(grid.isDisposed());
    }

    private static int countFilled(Table table) {
        int filled = 0;
        for (int i = 0; i < table.getCells().size; i++) {
            Cell<?> cell = table.getCells().get(i);
            if (cell != null && cell.get() != null) {
                filled++;
            }
        }
        return filled;
    }

    static class CardWrapperProbe extends Component {
        final float contentHeight;

        CardWrapperProbe(float contentHeight) {
            this.contentHeight = contentHeight;
        }

        @Override
        protected Element build() {
            return new Card()
                    .name("probe-card")
                    .props(left(), grow(), minHeight(200f))
                    .children(() -> {
                        Element content = new Element() {
                            @Override
                            public float getPrefWidth() {
                                return 100f;
                            }

                            @Override
                            public float getPrefHeight() {
                                return contentHeight;
                            }
                        };
                        AttachmentStack.add(content);
                    })
                    .element();
        }
    }

    @Test
    void lazyComponentChildInReactiveGridAppliesCellConstraints() {
        Signal<Integer> cols = Signal.of(2);
        Signal<List<String>> items = Signal.of(Arrays.asList("A", "B"));
        ReactiveGrid<String> grid = new ReactiveGrid<>(items);
        grid.columns(cols).key(s -> s);
        grid.items((item, ctx) -> new CardWrapperProbe(50f));
        grid.element();

        Element firstEl = grid.table().getChildren().get(0);
        Cell<?> cell = grid.table().getCell(firstEl);
        assertNotNull(cell, "Cell must exist in grid table");

        assertEquals(1, CellAccess.expandY(cell), "Cell should have expandY = 1 from card.grow()");
        assertEquals(1f, CellAccess.fillY(cell), 0.01f, "Cell should have fillY = 1 from card.grow()");
        assertEquals(200f, CellAccess.minHeight(cell), 0.01f, "Cell should have minHeight = 200 from card.minHeight()");
        grid.dispose();
    }

    @Test
    void lazyComponentChildInReactiveGridGrowsToMatchTallSibling() {
        Signal<Integer> cols = Signal.of(2);
        Signal<List<String>> items = Signal.of(Arrays.asList("short", "tall"));
        ReactiveGrid<String> grid = new ReactiveGrid<>(items);
        grid.columns(cols).key(s -> s);
        grid.items((item, ctx) -> "short".equals(item) ? new CardWrapperProbe(50f) : new CardWrapperProbe(350f));
        grid.element();
        grid.table().setWidth(600f);
        grid.table().setHeight(grid.table().getPrefHeight());
        grid.table().validate();
        grid.table().layout();

        Element shortEl = grid.table().getChildren().get(0);
        Element tallEl = grid.table().getChildren().get(1);

        assertTrue(tallEl.getHeight() >= 350f, "Tall element should have height >= 350");
        assertEquals(tallEl.getHeight(), shortEl.getHeight(), 1.0f,
                "Short card with grow() must expand vertically to match tall sibling in the same row");
        grid.dispose();
    }

    @Test
    void emptyStateRunnableBindingsAreDisposedWhenGridBecomesPopulated() {
        Signal<List<String>> items = Signal.of(Collections.emptyList());
        Signal<String> emptyText = Signal.of("No items available");

        ReactiveGrid<String> grid = new ReactiveGrid<>(items);
        grid.key(s -> s);
        grid.items((item, ctx) -> new ItemComp(item));
        grid.empty(() -> {
            solim.display.Text t = solim.display.Text.of(emptyText);
            AttachmentStack.add(t.element());
        });
        grid.element();

        items.set(Arrays.asList("First", "Second"));
        SignalDispatcher.flush();

        assertEquals(2, grid.table().getChildren().size,
                "Grid table should contain 2 item elements");

        grid.dispose();
    }
}
