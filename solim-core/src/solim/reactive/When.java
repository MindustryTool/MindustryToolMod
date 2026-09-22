package solim.reactive;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.SolimToken;
import solim.layout.GapContainer;
import solim.modifier.CellConfig;
import solim.modifier.ElementConfig;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableConfig;
import solim.runtime.Fragment;
import solim.runtime.ReactiveContext;

/**
 * Structural reactive component for boolean conditional rendering with a
 * fluent chaining API.
 *
 * <p>Declare branches with {@link #thenDo(Runnable)} and {@link #elseDo(Runnable)}.
 * Both accept void runnables that create components declaratively without
 * returning a value. When the condition is {@code true} the then-branch mounts;
 * otherwise the else-branch mounts. A branch that creates nothing collapses,
 * exactly like {@link Dynamic} with an empty void factory.</p>
 *
 * <p>Branch actions should be chained before the first mount for efficiency,
 * but may also be updated after mounting: a post-mount update remounts the
 * current condition value immediately.</p>
 */
public final class When extends BaseComponent
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

    /**
     * Sets the branch mounted when the condition is {@code true}.
     * Returns this for chaining.
     */
    public When thenDo(@Nullable Runnable action) {
        this.thenAction = action;
        if (isBuilt()) {
            refresh();
        }
        return this;
    }

    /**
     * Sets the branch mounted when the condition is not {@code true}
     * (false or null). Returns this for chaining.
     */
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
        super.name(name);
        return this;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
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
        lastValue = condition.get();
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
        // Capture and eager-build inside untracked so signals read during the
        // branch runnables never re-trigger this When effect.
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
            for (Component root : f.roots()) {
                root.element();
            }
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
        Cell<?> parentCell = container.parent instanceof Table ? ((Table) container.parent).getCell(container) : null;
        if (currentComponent != null) {
            container.visible = true;
            if (parentCell != null) {
                parentCell.minWidth(Float.NEGATIVE_INFINITY).minHeight(Float.NEGATIVE_INFINITY);
                parentCell.maxWidth(Float.NEGATIVE_INFINITY).maxHeight(Float.NEGATIVE_INFINITY);
                constraints.applyToCell(parentCell);
            }
        } else {
            container.visible = false;
            if (parentCell != null) {
                parentCell.size(0f).pad(0f);
            }
        }
        if (container.parent instanceof Table) {
            GapContainer.respace((Table) container.parent);
        }
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
