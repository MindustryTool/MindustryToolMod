package solim.reactive;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Nullable;
import java.util.*;
import arc.func.Func;
import solim.core.Component;
import solim.core.OwnershipContext;
import solim.core.SolimToken;

import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.Configurable;
import solim.modifier.ContainerCapability;
import solim.modifier.TableCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.runtime.AttachmentStack;
import solim.runtime.StructuralReconciler;

public final class ForEach<T> extends Component
        implements Configurable<ForEach<T>>, TableCapability, CellCapability, ContainerCapability<ForEach<T>>, AlignmentCapability {
    private final Table container = new Table();
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final Readable<? extends Iterable<T>> collection;
    private Func<T, ?> keyExtractor = v -> v;
    private final Signal<Func<T, Component>> itemFactorySignal = Signal.of(null);
    private final StructuralReconciler<Object, Component> reconciler = new StructuralReconciler<>();
    private @Nullable Props<ForEach<T>> pending;

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

    public ForEach<T> items(@Nullable Func<T, Component> itemFactory) {
        this.itemFactorySignal.set(itemFactory);
        if (itemFactory != null && isBuilt()) {
            reconcile();
        }
        return this;
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

    
    public ForEach<T> children(@Nullable Runnable block) {
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
    public @Nullable Props<ForEach<T>> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<ForEach<T>> props) {
        this.pending = props;
    }

    @Override
    protected void onRealized(Element element) {
        applyPendingProps();
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

    public ForEach<T> align(int align) {
        table().align(align);
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

    public ForEach<T> center() {
        table().center();
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

    public ForEach<T> top() {
        table().top();
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

    public ForEach<T> bottom() {
        table().bottom();
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

    public ForEach<T> left() {
        table().left();
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

    public ForEach<T> right() {
        table().right();
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

        container.clearChildren();
        for (Component comp : active.values()) {
            Element el = comp.element();
            Cell<?> cell = AttachmentStack.attach(container, el, null, comp);
            if (cell == null) {
                continue;
            }
            cell.minWidth(0f);
            if (PendingCellConfig.find(comp) == null && PendingCellConfig.find(el) == null
                    && SolimToken.isExpandingChild(el)) {
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
        reconciler.dispose();
        container.clearChildren();
    }

    @Override
    public ForEach<T> self() {
        return this;
    }
}
