package solim.reactive;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.Objects;
import arc.func.Cons;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.core.SolimToken;
import solim.modifier.CellConfig;
import solim.layout.GapContainer;
import solim.modifier.ElementConfig;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableConfig;
import solim.runtime.Fragment;
import solim.runtime.ReactiveContext;
import java.util.ArrayList;
import java.util.List;
import solim.core.Disposable;

/**
 * Structural reactive component for switching dynamic subtrees based on a
 * reactive value.
 *
 * <p>The factory {@code Cons<T>} is invoked with the current source value (including {@code null}).
 * The factory creates components declaratively without returning a value. If the factory creates
 * no components, the container collapses.</p>
 *
 * <p>Use {@link #of} to create a Dynamic with a void factory.</p>
 */
public final class Dynamic<T> extends BaseComponent
         implements CellConfig<Dynamic<T>>, TableConfig<Dynamic<T>>, ElementConfig<Dynamic<T>> {
     private static final Object SENTINEL = new Object();
     private final Table container = new Table();
     private final PendingCellConfig constraints = new PendingCellConfig();
     private final Readable<T> source;
     private final Cons<T> factory;
     private Component currentComponent;
     @SuppressWarnings("unchecked")
     private T lastValue = (T) SENTINEL;
     private final List<Disposable> currentBindings = new ArrayList<>();

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

    @Override
    public Dynamic<T> name(@Nullable String name) {
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
            T value = source.get();
            if (Objects.equals(value, lastValue)) {
                return;
            }
            lastValue = value;
            if (currentComponent != null) {
                currentComponent.dispose();
                currentComponent = null;
            }
            for (Disposable d : currentBindings) {
                d.dispose();
            }
            currentBindings.clear();
            container.clearChildren();
            mount(value);
            applyContainerAlign();
            updateParentCell();
            container.invalidateHierarchy();
        });
        return container;
    }

    private void mount(T value) {
        // Capture and eager-build inside untracked so signals read during the void factory
        // (including inside captured root builds) never re-trigger this Dynamic effect.
        Fragment fragment = ReactiveContext.untracked(() -> {
            Fragment f = Fragment.capture(() -> factory.get(value));
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
    public Dynamic<T> self() {
        return this;
    }
}
