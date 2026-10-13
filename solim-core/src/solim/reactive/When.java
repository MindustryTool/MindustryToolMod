package solim.reactive;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.SolimToken;
import solim.layout.CollapseHelper;
import solim.modifier.CellConfig;
import solim.modifier.ElementConfig;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableConfig;
import solim.runtime.AttachmentStack;
import solim.runtime.Fragment;
import solim.runtime.ReactiveContext;

public final class When extends Component
        implements CellConfig<When>, TableConfig<When>, ElementConfig<When> {
    private static final Object SENTINEL = new Object();
    private final Table container = new Table();
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final Readable<Boolean> condition;
    private @Nullable Runnable thenAction;
    private @Nullable Runnable elseAction;
    private Component currentComponent;
    private Object lastValue = SENTINEL;
    private final List<Disposable> currentBindings = new ArrayList<>();

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

    @Override
    public When name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public When align(int align) {
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
    public When center() {
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
    public When top() {
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
    public When bottom() {
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
    public When left() {
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
    public When right() {
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
        for (Disposable d : currentBindings) {
            d.dispose();
        }
        currentBindings.clear();
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
            Cell<?> cell = container.add(el);
            cell.minWidth(0f);
            applyRootCellConfig(root, el, cell);
            mounted++;
        }
        if (mounted == 0) {
            currentComponent.dispose();
            currentComponent = null;
        }
    }

    private void applyRootCellConfig(Component root, Element el, Cell<?> cell) {
        PendingCellConfig sc = PendingCellConfig.find(root);
        if (sc == null) {
            sc = PendingCellConfig.find(el);
        }
        if (sc != null) {
            currentBindings.addAll(sc.applyToCell(cell));
        } else if (SolimToken.isExpandingChild(el)) {
            cell.growX();
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
        CollapseHelper.updateParentCell(container, currentComponent != null, constraints);
    }

    @Override
    protected void onDispose() {
        if (currentComponent != null) {
            currentComponent.dispose();
            currentComponent = null;
        }
        for (Disposable d : currentBindings) {
            d.dispose();
        }
        currentBindings.clear();
        container.clearChildren();
    }

    @Override
    public When self() {
        return this;
    }
}
