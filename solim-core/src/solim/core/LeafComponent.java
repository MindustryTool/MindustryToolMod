package solim.core;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.modifier.CellConfig;
import solim.modifier.ElementConfig;
import solim.modifier.PendingCellConfig;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;

/**
 * Base class for leaf/primitive UI components wrapping an Arc {@link Element}.
 *
 * <p>Automatically binds the element to {@link SolimToken}, registers with {@link ComponentContext},
 * registers pending attachment to {@link ParentStack}, and provides default implementations
 * for {@link CellConfig} and {@link ElementConfig}.</p>
 *
 * @param <E> The underlying Arc Element type
 * @param <SELF> Fluent builder return type
 */
public abstract class LeafComponent<E extends Element, SELF extends LeafComponent<E, SELF>>
        implements Component, CellConfig<SELF>, ElementConfig<SELF> {

    protected final E element;
    protected final PendingCellConfig constraints = new PendingCellConfig();
    private final List<Disposable> disposables = new ArrayList<>();
    private boolean disposed = false;
    private @Nullable String componentName;

    @SuppressWarnings("unchecked")
    @Override
    public SELF self() {
        return (SELF) this;
    }

    protected LeafComponent(E element) {
        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null for LeafComponent");
        }
        this.element = element;
        SolimToken.bind(element, this, constraints);
        applyDefaultName(element);
        ComponentContext.registerChild(this);
        Table parent = ParentStack.current();
        if (parent != null) {
            ParentStack.registerPendingComponent(this, parent);
        }
    }

    private void applyDefaultName(Element el) {
        if (el.name == null) {
            String compName = getClass().getSimpleName();
            if (compName.isEmpty()) {
                compName = "leaf";
            }
            String elemName = el.getClass().getSimpleName();
            if (elemName.isEmpty()) {
                elemName = "element";
            }
            el.name = "solim-" + compName.toLowerCase() + "-" + elemName.toLowerCase();
        }
    }

    @Override
    public E element() {
        checkNotDisposed();
        if (this instanceof SpacingAware) {
            ((SpacingAware) this).applySpacing();
        }
        return element;
    }

    @Override
    public final PendingCellConfig cellConfig() {
        return constraints;
    }

    public SELF name(@Nullable String name) {
        this.componentName = name;
        if (element != null && name != null) {
            element.name = name;
        }
        return self();
    }

    public @Nullable String name() {
        return componentName != null ? componentName : element.name;
    }

    public SELF own(@Nullable Disposable disposable) {
        if (disposable != null && !disposed) {
            disposables.add(disposable);
        }
        return self();
    }

    public SELF own(@Nullable Iterable<? extends Disposable> items) {
        if (items != null) {
            for (Disposable d : items) {
                own(d);
            }
        }
        return self();
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (int i = disposables.size() - 1; i >= 0; i--) {
            Disposable disposable = disposables.get(i);
            if (disposable != null) {
                disposable.dispose();
            }
        }
        disposables.clear();
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    protected void checkNotDisposed() {
        if (disposed) {
            throw new IllegalStateException(
                    "Cannot use disposed component: " + getClass().getSimpleName());
        }
    }
}
