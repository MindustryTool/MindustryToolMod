package solim.reactive;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.Objects;
import arc.func.Cons;
import solim.core.Component;
import solim.core.OwnershipContext;
import solim.core.SolimToken;
import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.Configurable;
import solim.modifier.ContainerCapability;
import solim.modifier.TableCapability;
import solim.layout.CollapseHelper;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.runtime.AttachmentStack;
import solim.runtime.Fragment;
import solim.runtime.ReactiveContext;

public final class Dynamic<T> extends Component
        implements Configurable<Dynamic<T>>, TableCapability, CellCapability, ContainerCapability<Dynamic<T>>, AlignmentCapability {
    private final Table container = new Table();
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final Readable<T> source;
    private final Cons<T> factory;
    private Component currentComponent;
    private @Nullable Props<Dynamic<T>> pending;
    private @Nullable T lastValue;
    private boolean hasValue;

    private Dynamic(Readable<T> source, Cons<T> factory) {
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.factory = Objects.requireNonNull(factory, "factory must not be null");
        SolimToken.bind(this.container, this, constraints);
        this.container.top().left();
        this.container.defaults().top().left();
    }

    public static <T> Dynamic<T> of(Readable<T> source, Cons<T> factory) {
        return new Dynamic<>(source, factory);
    }

    public Table container() {
        return container;
    }

    @Override
    public Table table() {
        return container;
    }

    
    public Dynamic<T> children(@Nullable Runnable block) {
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
    public @Nullable Props<Dynamic<T>> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Dynamic<T>> props) {
        this.pending = props;
    }

    @Override
    protected void onRealized(Element element) {
        applyPendingProps();
    }

    @Override
    public Dynamic<T> name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    public Dynamic<T> align(int align) {
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

    public Dynamic<T> center() {
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

    public Dynamic<T> top() {
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

    public Dynamic<T> bottom() {
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

    public Dynamic<T> left() {
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

    public Dynamic<T> right() {
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
        Effect.of(() -> {
            T value = source.get();
            if (hasValue && Objects.equals(value, lastValue)) {
                return;
            }
            hasValue = true;
            lastValue = value;
            if (currentComponent != null) {
                currentComponent.dispose();
                currentComponent = null;
            }
            container.clearChildren();
            mount(value);
            applyContainerAlign();
            updateParentCell();
            container.invalidateHierarchy();
        });
        return container;
    }

    private void mount(T value) {

        Fragment fragment = ReactiveContext.untracked(() -> {
            Fragment f = Fragment.capture(() -> factory.get(value));
            AttachmentStack.isolate(() -> {
                for (Component root : f.roots()) {
                    root.element();
                }
            });
            return f;
        });
        if (fragment.isEmpty()) {
            fragment.dispose();
            return;
        }
        currentComponent = fragment;
        int mounted = 0;
        for (Component root : fragment.roots()) {
            Element el = root.element();
            if (el.parent != null) {
                continue;
            }
            Cell<?> cell = AttachmentStack.attach(container, el, null, root);
            if (cell == null) {
                continue;
            }
            cell.minWidth(0f);
            if (PendingCellConfig.find(root) == null && PendingCellConfig.find(el) == null
                    && SolimToken.isExpandingChild(el)) {
                cell.growX();
            }
            mounted++;
        }
        if (mounted == 0) {
            currentComponent.dispose();
            currentComponent = null;
        }
    }

    private void applyContainerAlign() {
        if (constraints.align != null) {
            container.align(constraints.align);
        } else {
            container.top();
        }
    }

    private void updateParentCell() {
        CollapseHelper.updateParentCell(container, currentComponent != null, constraints, this);
    }

    @Override
    protected void onDispose() {
        if (currentComponent != null) {
            currentComponent.dispose();
            currentComponent = null;
        }
        container.clearChildren();
    }

    @Override
    public Dynamic<T> self() {
        return this;
    }
}
