package solim.core;

import arc.Events;
import arc.func.Cons;
import arc.scene.Element;
import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import mindustry.Vars;
import solim.instrumentation.Dispose;
import solim.instrumentation.Instrumentation;

public abstract class Component implements DisposableOwner {

    private @Nullable Element cached;
    private @Nullable String componentName;

    private final List<Disposable> disposables = new ArrayList<>();
    private final Set<Disposable> disposableIndex = Collections
            .newSetFromMap(new IdentityHashMap<Disposable, Boolean>());

    private final List<Component> children = new ArrayList<>();

    private @Nullable Component parent;
    private volatile boolean disposed = false;

    protected Component() {
        joinAmbientScope();
    }

    protected Component(Element prebuilt) {
        this.cached = prebuilt;
        applyDefaultName(prebuilt);
        joinAmbientScope();
    }

    private void joinAmbientScope() {
        OwnershipContext.registerChild(this);
        if (attachesToParent()) {
            AttachmentBridge.registerPending(this);
        }
    }

    protected boolean attachesToParent() {
        return true;
    }

    /**
     * Pushes this component as the ambient owner. Use it where a component creates owned
     * resources outside its {@code build()} — in a constructor or in a modifier — so the ambient
     * owner is unambiguous instead of whatever happened to be on the stack. Always pair with
     * {@link #closeSelfWindow()} in a {@code finally} block.
     */
    public final void openSelfWindow() {
        OwnershipContext.push(this);
    }

    public final void closeSelfWindow() {
        OwnershipContext.pop(this);
    }

    /**
     * Runs {@code block} with this component as the ambient owner, so every resource it creates
     * is adopted by this component and disposed with it.
     */
    // TODO: Remove
    public final void withinOwnership(Runnable block) {
        openSelfWindow();
        try {
            block.run();
        } finally {
            closeSelfWindow();
        }
    }

    protected Element build() {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " was constructed lazily but does not implement build()");
    }

    protected void onRealized(Element element) {
    }

    public boolean isAncestorOf(@Nullable Component other) {
        for (Component node = other; node != null; node = node.parent) {
            if (node == this) {
                return true;
            }
        }
        return false;
    }

    public @Nullable Component owner() {
        return parent;
    }

    public List<Component> children() {
        return Collections.unmodifiableList(new ArrayList<>(children));
    }

    public List<Disposable> disposables() {
        return Collections.unmodifiableList(new ArrayList<>(disposables));
    }

    public @Nullable Element peekElement() {
        return cached;
    }

    private void applyDefaultName(Element element) {
        if (componentName != null) {
            element.name = componentName;
            return;
        }
        if (element.name != null) {
            return;
        }
        String compName = getClass().getSimpleName();
        String elementName = element.getClass().getSimpleName();
        element.name = "solim-" + (compName.isEmpty() ? "component" : compName.toLowerCase(Locale.ROOT))
                + "-" + (elementName.isEmpty() ? "element" : elementName.toLowerCase(Locale.ROOT));
    }

    public final Element element() {
        SolimAssert.checkMainThread();
        checkNotDisposed();
        Element built = cached;
        if (built != null) {
            applySpacing();
            return built;
        }

        OwnershipContext.push(this);
        try {
            built = build();
            if (built == null) {
                throw new IllegalStateException("build() returned null for " + getClass().getSimpleName());
            }
            cached = built;
            applyDefaultName(built);
            applySpacing();
            onRealized(built);
            return built;
        } catch (Throwable throwable) {
            if (Vars.ui != null) {
                Vars.ui.showException(throwable);
            } else {
                Log.err(throwable);
            }
            dispose();
            throw throwable;
        } finally {
            OwnershipContext.pop(this);
        }
    }

    private void applySpacing() {
        if (this instanceof SpacingAware) {
            ((SpacingAware) this).applySpacing();
        }
    }

    public @Nullable String name() {
        return componentName != null ? componentName : cached != null ? cached.name : null;
    }

    public Component name(@Nullable String name) {
        setName(name);
        return this;
    }

    protected final void setName(@Nullable String name) {
        this.componentName = name;
        if (cached != null && name != null) {
            cached.name = name;
        }
    }

    @Override
    public void adopt(@Nullable Disposable resource) {
        SolimAssert.checkMainThread();
        if (resource == null || disposed) {
            return;
        }
        if (resource instanceof Component) {
            Component child = (Component) resource;
            if (child.parent == this) {
                return;
            }
            if (child.parent != null) {
                child.parent.children.remove(child);
            } else {
                SessionRoot.get().remove(child);
            }
            child.parent = this;
            children.add(child);
            return;
        }
        OwnershipGuard.rejectDuplicate(resource, this);
        if (disposableIndex.add(resource)) {
            disposables.add(resource);
        }
    }

    public <T> Disposable listen(Class<T> eventType, Cons<T> listener) {
        Events.on(eventType, listener);
        return OwnershipContext.register(DisposableAction.of(() -> Events.remove(eventType, listener)));
    }

    public final boolean isBuilt() {
        return cached != null;
    }

    protected void onDispose() {
    }

    protected final void checkNotDisposed() {
        if (disposed) {
            throw new IllegalStateException(
                    "Cannot use disposed component: " + getClass().getSimpleName());
        }
    }

    private void detachFromParent() {
        Component owner = parent;
        if (owner != null) {
            owner.children.remove(this);
        } else {
            SessionRoot.get().remove(this);
        }
        parent = null;
    }

    @Override
    public void dispose() {
        SolimAssert.checkMainThread();
        if (disposed) {
            return;
        }
        disposed = true;

        if (Instrumentation.enabled()) {
            Instrumentation.emit(new Dispose(this, children.size(), disposables.size()));
        }

        Throwable firstFailure = null;
        try {
            onDispose();
        } catch (Throwable t) {
            firstFailure = t;
        }
        detachFromParent();
        for (int i = children.size() - 1; i >= 0; i--) {
            try {
                children.get(i).dispose();
            } catch (Throwable t) {
                firstFailure = record(firstFailure, t);
            }
        }
        children.clear();
        for (int i = disposables.size() - 1; i >= 0; i--) {
            try {
                disposables.get(i).dispose();
            } catch (Throwable t) {
                firstFailure = record(firstFailure, t);
            }
        }
        disposables.clear();
        disposableIndex.clear();
        if (firstFailure != null) {
            if (firstFailure instanceof RuntimeException) {
                throw (RuntimeException) firstFailure;
            }
            if (firstFailure instanceof Error) {
                throw (Error) firstFailure;
            }
            throw new RuntimeException("Error disposing " + getClass().getSimpleName(), firstFailure);
        }
    }

    private static Throwable record(@Nullable Throwable first, Throwable next) {
        if (first == null) {
            return next;
        }
        if (first != next) {
            first.addSuppressed(next);
        }
        return first;
    }

    @Override
    public final boolean isDisposed() {
        return disposed;
    }
}
