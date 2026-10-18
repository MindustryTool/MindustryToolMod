package solim.layout;


import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.func.Func2;
import arc.func.Func;
import arc.func.Prov;
import solim.core.Component;
import solim.core.SolimToken;
import solim.core.OwnershipContext;
import solim.runtime.OwnershipScope;
import solim.runtime.AttachmentStack;
import solim.runtime.StructuralReconciler;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.core.Units;
import solim.reactive.Signal;
import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.Configurable;
import solim.modifier.ContainerCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.modifier.TableCapability;

public final class ReactiveGrid<T> extends Component
        implements GapContainer, Configurable<ReactiveGrid<T>>, TableCapability, CellCapability, ContainerCapability<ReactiveGrid<T>>, AlignmentCapability {
    private final Signal<Float> tableWidth = Signal.of(0f);
    private final Signal<Float> gapSignal = Signal.of(0f);
    private @Nullable Computed<Float> itemWidth;
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
    private @Nullable Props<ReactiveGrid<T>> pending;
    private final Signal<Readable<Integer>> columnSource = Signal.of(Readable.of(1));
    private @Nullable Computed<Integer> resolvedColumnCount;
    private final Readable<? extends Iterable<T>> itemsSource;
    private Func<T, ?> keyExtractor = v -> v;
    private final Signal<Func2<T, GridItemContext, Component>> itemFactorySignal = Signal.of(null);
    private final StructuralReconciler<Object, Component> reconciler = new StructuralReconciler<>();

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
        this.itemsSource = items;

        this.context = new GridItemContext() {
            @Override
            public Readable<Float> itemWidth() {
                return itemWidth;
            }

            @Override
            public Readable<Integer> columnCount() {
                return resolvedColumnCount;
            }
        };

        constraints.growX = true;
    }

    public static <T> ReactiveGrid<T> of(Readable<? extends Iterable<T>> items) {
        return new ReactiveGrid<>(items);
    }

    public ReactiveGrid<T> columns(int columns) {
        return columns(Readable.of(columns));
    }

    public ReactiveGrid<T> columns(@Nullable Readable<Integer> columns) {
        this.columnSource.set(columns != null ? columns : Readable.of(1));
        return this;
    }

    public ReactiveGrid<T> key(@Nullable Func<T, ?> keyExtractor) {
        this.keyExtractor = keyExtractor != null ? keyExtractor : v -> v;
        return this;
    }

    public ReactiveGrid<T> items(@Nullable Func<T, Component> itemFactory) {
        items(itemFactory != null ? (item, ctx) -> itemFactory.get(item) : null);
        return this;
    }

    public ReactiveGrid<T> items(@Nullable Func2<T, GridItemContext, Component> itemFactory) {
        this.itemFactorySignal.set(itemFactory);
        if (itemFactory != null && isBuilt()) {
            Computed<Integer> columns = resolvedColumnCount;
            Integer cols = columns != null ? columns.peek() : null;
            updateItemsAndReflow(itemsSource.peek(), Math.max(1, cols != null ? cols : 1), itemFactory);
        }
        return this;
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

    @Override
    public void setGap(float gap) {
        this.gap = gap;
        this.gapSignal.set(gap);
        respace();
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
        Computed<Integer> columns = resolvedColumnCount;
        Integer cols = columns != null ? columns.peek() : null;
        respace(cols != null ? cols : 1);
    }

    private void respace(int cols) {
        GapContainer.applyGridSpacing(table, cols, gap);
    }

    @Override
    public Table table() {
        return table;
    }

    
    public ReactiveGrid<T> children(@Nullable Runnable block) {
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
    public @Nullable Props<ReactiveGrid<T>> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<ReactiveGrid<T>> props) {
        this.pending = props;
    }

    @Override
    protected void onRealized(Element element) {
        applyPendingProps();
    }

    @Override
    public ReactiveGrid<T> name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    protected Element build() {
        table.top().left();

        resolvedColumnCount = new Computed<>(() -> {
            Readable<Integer> src = columnSource.get();
            Integer val = src != null ? src.get() : null;
            return Math.max(1, val != null ? val : 1);
        });

        itemWidth = new Computed<>(() -> {
            float tw = tableWidth.get();
            Computed<Integer> columns = resolvedColumnCount;
            int cols = columns != null && columns.get() != null ? columns.get() : 1;
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

        Effect.of(() -> {
            Iterable<T> itemList = itemsSource.get();
            Computed<Integer> columns = resolvedColumnCount;
            int cols = columns != null && columns.get() != null ? columns.get() : 1;
            Func2<T, GridItemContext, Component> factory = itemFactorySignal.get();
            if (factory == null) {
                return;
            }
            if (isDisposed()) {
                return;
            }
            updateItemsAndReflow(itemList, cols, factory);
        });

        return table;
    }

    private void updateItemsAndReflow(Iterable<T> itemList, int cols, Func2<T, GridItemContext, Component> factory) {
        if (isDisposed()) {

            return;
        }
        reconciler.reconcile(itemList, this::extractKey, item -> factory.get(item, context));
        reflow(cols);
    }

    private void reflow(int cols) {
        table.clear();
        table.top().left();

        if (reconciler.isEmpty()) {
            if (emptyRunnable != null) {
                if (currentEmptyComponent == null) {
                    currentEmptyComponent = new Component(new Table()) {
                        @Override
                        protected Element build() {
                            return element();
                        }
                    };
                    Table emptyTable = (Table) currentEmptyComponent.element();
                    OwnershipScope scope = OwnershipScope.open();
                    AttachmentStack.push(emptyTable);
                    try {
                        emptyRunnable.run();
                    } finally {
                        AttachmentStack.pop();
                        scope.adoptInto(currentEmptyComponent);
                        scope.close();
                    }
                }
                table.add(currentEmptyComponent.element()).center().colspan(cols).growX();
            } else if (emptyViewSupplier != null) {
                if (currentEmptyComponent == null) {
                    currentEmptyComponent = AttachmentStack.isolate(() -> {
                        Component c = emptyViewSupplier.get();
                        if (c != null) {
                            c.element();
                        }
                        return c;
                    });
                }
                if (currentEmptyComponent != null) {
                    table.add(currentEmptyComponent.element()).center().colspan(cols).growX();
                }
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
            Cell<?> cell = AttachmentStack.attach(table, el, null, comp);
            if (cell == null) {
                continue;
            }
            cell.top().left();
            PendingCellConfig sc = PendingCellConfig.find(comp);
            if (sc == null) {
                sc = PendingCellConfig.find(el);
            }
            if (sc == null || sc.growX || !sc.hasExplicitWidth()) {
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
        respace(cols);
        table.invalidateHierarchy();
    }

    @Override
    protected void onDispose() {
        reconciler.dispose();
        if (itemWidth != null) {
            itemWidth.dispose();
            itemWidth = null;
        }
        if (resolvedColumnCount != null) {
            resolvedColumnCount.dispose();
            resolvedColumnCount = null;
        }

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
