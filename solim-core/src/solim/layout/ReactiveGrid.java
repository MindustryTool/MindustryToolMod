package solim.layout;
import solim.modifier.CellConfig;

import arc.Core;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.*;
import arc.func.Func2;
import arc.func.Func;
import arc.func.Prov;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.SolimToken;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;
import solim.runtime.StructuralReconciler;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.core.Units;
import solim.modifier.ElementConfig;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableConfig;

/**
 * Keyed reactive grid that reflows existing component cells when column count
 * changes and structurally reconciles items when the item collection changes.
 */
public final class ReactiveGrid<T> extends BaseComponent
        implements CellConfig<ReactiveGrid<T>>, ElementConfig<ReactiveGrid<T>>, TableConfig<ReactiveGrid<T>>,
        GapContainer {
    private final Signal<Float> tableWidth = Signal.of(0f);
    private final Signal<Float> gapSignal = Signal.of(0f);
    private final Computed<Float> itemWidth;
    private final GridItemContext context;

    private final Table table = new Table() {
        @Override
        public void layout() {
            super.layout();
            checkWidth(getWidth());
        }

        @Override
        protected void sizeChanged() {
            super.sizeChanged();
            checkWidth(getWidth());
        }
    };

    private final PendingCellConfig constraints = new PendingCellConfig();
    private Readable<Integer> columnCount = Readable.of(1);
    private final Readable<? extends Iterable<T>> items;
    private Func<T, ?> keyExtractor = v -> v;
    private @Nullable Func2<T, GridItemContext, Component> itemFactory;
    private final StructuralReconciler<Object, Component> reconciler = new StructuralReconciler<>();
    private final List<Disposable> itemBindings = new ArrayList<>();

    private Runnable emptyRunnable;
    private Prov<Component> emptyViewSupplier;
    private Component currentEmptyComponent;
    private float gap = 0f;

    public ReactiveGrid(Readable<? extends Iterable<T>> items) {
        this.table.name = "solim-reactive-grid-table";
        SolimToken.bind(this.table, this, constraints);
        this.table.top().left();
        this.table.defaults().top().left();
        this.table.update(() -> checkWidth(this.table.getWidth()));
        this.items = items;

        this.itemWidth = new Computed<>(() -> {
            float tw = tableWidth.get();
            int cols = Math.max(1, columnCount.get() != null ? columnCount.get() : 1);
            float g = gapSignal.get();
            float horizontalMargin = table.getMarginLeft() + table.getMarginRight();
            float availableWidth = Math.max(0f, tw - horizontalMargin);
            float totalGaps = (cols - 1) * g;
            if (tw <= 0f) {
                float sw = Units.screenWidth();
                float fallbackW = sw > 0f ? sw - 32f : 300f;
                return Math.max(0f, (fallbackW - totalGaps) / cols);
            }
            return Math.max(0f, (availableWidth - totalGaps) / cols);
        });

        this.context = new GridItemContext() {
            @Override
            public Readable<Float> itemWidth() {
                return itemWidth;
            }

            @Override
            public Readable<Integer> columnCount() {
                return columnCount;
            }
        };

        growX();
    }

    public static <T> ReactiveGrid<T> of(Readable<? extends Iterable<T>> items) {
        return new ReactiveGrid<>(items);
    }

    public ReactiveGrid<T> columns(int columns) {
        return columns(Readable.of(columns));
    }

    public ReactiveGrid<T> columns(@Nullable Readable<Integer> columns) {
        this.columnCount = columns != null ? columns : Readable.of(1);
        return this;
    }

    public ReactiveGrid<T> key(@Nullable Func<T, ?> keyExtractor) {
        this.keyExtractor = keyExtractor != null ? keyExtractor : v -> v;
        return this;
    }

    public void children(@Nullable Func<T, Component> itemFactory) {
        children(itemFactory != null ? (item, ctx) -> itemFactory.get(item) : null);
    }

    public void children(@Nullable Func2<T, GridItemContext, Component> itemFactory) {
        this.itemFactory = itemFactory;
        refreshIfBuilt();
    }

    private void refreshIfBuilt() {
        if (itemFactory == null || !isBuilt()) {
            return;
        }
        Integer cols = columnCount.peek();
        updateItemsAndReflow(items.peek(), Math.max(1, cols != null ? cols : 1));
    }

    private Object extractKey(T item) {
        return keyExtractor.get(item);
    }

    public GridItemContext context() {
        return context;
    }

    public Readable<Float> itemWidth() {
        return itemWidth;
    }

    private void checkWidth(float w) {
        if (w > 0f && Math.abs(w - tableWidth.get()) > 0.5f) {
            tableWidth.set(w);
        }
    }

    public ReactiveGrid<T> empty(Runnable emptyRunnable) {
        this.emptyRunnable = emptyRunnable;
        return this;
    }

    public ReactiveGrid<T> emptyView(Prov<Component> Prov) {
        this.emptyViewSupplier = Prov;
        return this;
    }

    public ReactiveGrid<T> gap(float gap) {
        this.gap = gap;
        this.gapSignal.set(gap);
        respace();
        return this;
    }

    @Override
    public Direction direction() {
        return Direction.HORIZONTAL;
    }

    @Override
    public float gap() {
        return gap;
    }

    @Override
    public void respace() {
        int cols = Math.max(1, columnCount.get() != null ? columnCount.get() : 1);
        GapContainer.applyGridSpacing(table, cols, gap);
    }

    public ReactiveGrid<T> gap(@Nullable Readable<Float> gapSignal) {
        if (gapSignal != null) {
            ComponentContext.register(Effect.of(() -> {
                Float g = gapSignal.get();
                if (g != null) {
                    gap(g);
                }
            }));
        }
        return this;
    }

    @Override
    public Table table() {
        return table;
    }

    @Override
    public ReactiveGrid<T> name(@Nullable String name) {
        super.name(name);
        return this;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    protected Element build() {
        table.top().left();

        Effect.of(() -> {
            Iterable<T> itemList = items.get();
            int cols = Math.max(1, columnCount.get() != null ? columnCount.get() : 1);
            if (table.getScene() != null && Core.app != null) {
                Core.app.post(() -> {
                    updateItemsAndReflow(itemList, cols);
                });
            } else {
                updateItemsAndReflow(itemList, cols);
            }
        });

        return table;
    }

    private void updateItemsAndReflow(Iterable<T> itemList, int cols) {
        Func2<T, GridItemContext, Component> factory = itemFactory;
        if (factory == null) {
            return;
        }
        reconciler.reconcile(itemList, this::extractKey, item -> factory.get(item, context));
        reflow(cols);
    }

    private void reflow(int cols) {
        for (Disposable d : itemBindings) {
            d.dispose();
        }
        itemBindings.clear();

        table.clear();
        table.top().left();

        if (reconciler.isEmpty()) {
            if (emptyRunnable != null) {
                Table emptyTable = new Table();
                ParentStack.push(emptyTable);
                try {
                    emptyRunnable.run();
                } finally {
                    ParentStack.pop();
                }
                table.add(emptyTable).center().colspan(cols).growX();
            } else if (emptyViewSupplier != null) {
                if (currentEmptyComponent == null) {
                    currentEmptyComponent = ParentStack.isolate(() -> {
                        Component c = emptyViewSupplier.get();
                        if (c != null) {
                            c.element();
                        }
                        return c;
                    });
                }
                table.add(currentEmptyComponent.element()).center().colspan(cols).growX();
            }
            return;
        }

        if (currentEmptyComponent != null) {
            currentEmptyComponent.dispose();
            currentEmptyComponent = null;
        }

        int col = 0;
        for (Component comp : reconciler.activeComponents().values()) {
            Element el = comp.element();
            Cell<?> cell = table.add(el).top().left();
            PendingCellConfig sc = PendingCellConfig.find(comp);
            if (sc == null) {
                sc = PendingCellConfig.find(el);
            }
            if (sc != null) {
                List<Disposable> effects = sc.applyToCell(cell);
                itemBindings.addAll(effects);
                if (sc.growX || !sc.hasExplicitWidth()) {
                    cell.growX().uniformX();
                }
            } else {
                cell.growX().uniformX();
            }
            if (++col % cols == 0) {
                table.row();
            }
        }
        while (col % cols != 0) {
            table.add().uniformX().growX();
            col++;
        }
        table.row();
        respace();
        table.invalidateHierarchy();
    }

    @Override
    protected void onDispose() {
        for (Disposable d : itemBindings) {
            d.dispose();
        }
        itemBindings.clear();

        reconciler.dispose();
        itemWidth.dispose();

        if (currentEmptyComponent != null) {
            currentEmptyComponent.dispose();
            currentEmptyComponent = null;
        }
        table.clear();
    }

    @Override
    public ReactiveGrid<T> self() {
        return this;
    }
}
