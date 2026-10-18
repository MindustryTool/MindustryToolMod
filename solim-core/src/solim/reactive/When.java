package solim.reactive;


import solim.modifier.ElementCapability;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.Objects;
import solim.core.Component;
import solim.core.OwnershipContext;
import solim.core.SolimToken;
import solim.layout.CollapseHelper;
import solim.modifier.Configurable;
import solim.modifier.ContainerCapability;
import solim.modifier.TableCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.runtime.AttachmentStack;
import solim.runtime.Fragment;
import solim.runtime.ReactiveContext;

public final class When extends Component
        implements Configurable<When>, TableCapability, ElementCapability, ContainerCapability<When> {
    private static final Object SENTINEL = new Object();
    private final Table container = new Table();
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final Readable<Boolean> condition;
    private @Nullable Runnable thenAction;
    private @Nullable Runnable elseAction;
    private Component currentComponent;
    private @Nullable Props<When> pending;
    private Object lastValue = SENTINEL;

    private When(Readable<Boolean> condition) {
        this.condition = Objects.requireNonNull(condition, "condition must not be null");
        SolimToken.bind(this.container, this, constraints);
        this.container.top().left();
        this.container.defaults().top().left();
    }

    public static When of(Readable<Boolean> condition) {
        return new When(condition);
    }

    public When thenDo(@Nullable Runnable action) {
        this.thenAction = action;
        if (isBuilt()) {
            refresh();
        }
        return this;
    }

    public When elseDo(@Nullable Runnable action) {
        this.elseAction = action;
        if (isBuilt()) {
            refresh();
        }
        return this;
    }

    public Table container() {
        return container;
    }

    @Override
    public Table table() {
        return container;
    }

    
    public When children(@Nullable Runnable block) {
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
    public @Nullable Props<When> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<When> props) {
        this.pending = props;
    }

    @Override
    protected void onRealized(Element element) {
        applyPendingProps();
    }

    @Override
    public When name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    public When align(int align) {
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

    public When center() {
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

    public When top() {
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

    public When bottom() {
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

    public When left() {
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

    public When right() {
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
            Boolean value = condition.get();
            if (Objects.equals(value, lastValue)) {
                return;
            }
            lastValue = value;
            rebuild();
        });
        return container;
    }

    private void refresh() {
        lastValue = condition.peek();
        rebuild();
    }

    private void rebuild() {
        if (currentComponent != null) {
            currentComponent.dispose();
            currentComponent = null;
        }
        container.clearChildren();
        mount(condition.get());
        applyContainerAlign();
        updateParentCell();
        container.invalidateHierarchy();
    }

    private void mount(Boolean value) {

        Fragment fragment = ReactiveContext.untracked(() -> {
            Fragment f = Fragment.capture(() -> {
                if (Boolean.TRUE.equals(value)) {
                    if (thenAction != null) {
                        thenAction.run();
                    }
                } else {
                    if (elseAction != null) {
                        elseAction.run();
                    }
                }
            });
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
    public When self() {
        return this;
    }
}
