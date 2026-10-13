package solim.reactive;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.*;
import arc.func.Func;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.SolimToken;

import solim.modifier.CellConfig;
import solim.modifier.ElementConfig;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableConfig;
import solim.runtime.StructuralReconciler;

public final class ForEach<T> extends Component
        implements CellConfig<ForEach<T>>, ElementConfig<ForEach<T>>, TableConfig<ForEach<T>> {
    private final Table container = new Table();
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final Readable<? extends Iterable<T>> collection;
    private Func<T, ?> keyExtractor = v -> v;
    private final Signal<Func<T, Component>> itemFactorySignal = Signal.of(null);
    private final StructuralReconciler<Object, Component> reconciler = new StructuralReconciler<>();
    private final List<Disposable> itemBindings = new ArrayList<>();

    public ForEach(Readable<? extends Iterable<T>> collection) {
        this.collection = collection;
        SolimToken.bind(this.container, this, constraints);
        this.container.top().left();
        this.container.defaults().top().left();
    }

    public static <T> ForEach<T> of(Readable<? extends Iterable<T>> collection) {
        return new ForEach<>(collection);
    }

    public ForEach<T> key(@Nullable Func<T, ?> keyExtractor) {
        this.keyExtractor = keyExtractor != null ? keyExtractor : v -> v;
        return this;
    }

    public void children(@Nullable Func<T, Component> itemFactory) {
        this.itemFactorySignal.set(itemFactory);
    }

    private Object extractKey(T item) {
        return keyExtractor.get(item);
    }

    public Table container() {
        return container;
    }

    @Override
    public Table table() {
        return container;
    }

    @Override
    public ForEach<T> name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public ForEach<T> align(int align) {
        TableConfig.super.align(align);
        constraints.align = align;
        constraints.applyAlignToParentCell(element());
        container.defaults().align(align);
        for (Cell<?> c : container.getCells()) {
            if (c != null) {
                c.align(align);
            }
        }
        return this;
    }

    @Override
    public ForEach<T> center() {
        TableConfig.super.center();
        constraints.alignCenter();
        constraints.applyAlignToParentCell(element());
        container.defaults().center();
        for (Cell<?> c : container.getCells()) {
            if (c != null) {
                c.center();
            }
        }
        return this;
    }

    @Override
    public ForEach<T> top() {
        TableConfig.super.top();
        constraints.alignTop();
        constraints.applyAlignToParentCell(element());
        container.defaults().top();
        for (Cell<?> c : container.getCells()) {
            if (c != null) {
                c.top();
            }
        }
        return this;
    }

    @Override
    public ForEach<T> bottom() {
        TableConfig.super.bottom();
        constraints.alignBottom();
        constraints.applyAlignToParentCell(element());
        container.defaults().bottom();
        for (Cell<?> c : container.getCells()) {
            if (c != null) {
                c.bottom();
            }
        }
        return this;
    }

    @Override
    public ForEach<T> left() {
        TableConfig.super.left();
        constraints.alignLeft();
        constraints.applyAlignToParentCell(element());
        container.defaults().left();
        for (Cell<?> c : container.getCells()) {
            if (c != null) {
                c.left();
            }
        }
        return this;
    }

    @Override
    public ForEach<T> right() {
        TableConfig.super.right();
        constraints.alignRight();
        constraints.applyAlignToParentCell(element());
        container.defaults().right();
        for (Cell<?> c : container.getCells()) {
            if (c != null) {
                c.right();
            }
        }
        return this;
    }

    @Override
    protected Element build() {
        applyContainerAlign();
        Effect.of(this::reconcile);
        return container;
    }

    private void reconcile() {
        Func<T, Component> factory = itemFactorySignal.get();
        Iterable<T> items = collection.get();
        if (factory == null) {
            return;
        }
        Map<Object, Component> active = reconciler.reconcile(items, this::extractKey, factory);

        for (Disposable d : itemBindings) {
            d.dispose();
        }
        itemBindings.clear();

        container.clearChildren();
        for (Component comp : active.values()) {
            Element el = comp.element();
            Cell<?> cell = container.add(el);
            cell.minWidth(0f);
            PendingCellConfig sc = PendingCellConfig.find(comp);
            if (sc == null) {
                sc = PendingCellConfig.find(el);
            }
            if (sc != null) {
                List<Disposable> cellEffects = sc.applyToCell(cell);
                itemBindings.addAll(cellEffects);
                for (Disposable d : cellEffects) {
                    comp.adopt(d);
                }
            } else if (SolimToken.isExpandingChild(el)) {
                cell.growX();
            }
            cell.row();
        }
        applyContainerAlign();
    }

    private void applyContainerAlign() {
        if (constraints.align != null) {
            container.align(constraints.align);
        } else {
            container.top();
        }
    }

    @Override
    protected void onDispose() {
        for (Disposable d : itemBindings) {
            d.dispose();
        }
        itemBindings.clear();
        reconciler.dispose();
        container.clearChildren();
    }

    @Override
    public ForEach<T> self() {
        return this;
    }
}
