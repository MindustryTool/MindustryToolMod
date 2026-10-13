package solim.core;

import arc.Events;
import arc.func.Cons;
import arc.scene.Element;
import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mindustry.Vars;

public abstract class Component implements DisposableOwner {

    private Element cached;
    private @Nullable String componentName;

    private final List<Disposable> disposables = new ArrayList<>();

    private final List<Component> children = new ArrayList<>();

    private @Nullable Component parent;
    private boolean disposed = false;

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

    protected Element build() {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " was constructed lazily but does not implement build()");
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
        return Collections.unmodifiableList(children);
    }

    public List<Disposable> disposables() {
        return Collections.unmodifiableList(disposables);
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
        element.name = "solim-" + (compName.isEmpty() ? "component" : compName.toLowerCase())
                + "-" + (elementName.isEmpty() ? "element" : elementName.toLowerCase());
    }

    public final Element element() {
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
            OwnershipContext.pop();
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
        if (!disposables.contains(resource)) {
            disposables.add(resource);
        }
    }

    public final <T extends Disposable> T own(@Nullable T resource) {
        adopt(resource);
        return resource;
    }

    public <T> Disposable listen(Class<T> eventType, Cons<T> listener) {
        Events.on(eventType, listener);
        return own(DisposableAction.of(() -> Events.remove(eventType, listener)));
    }

    protected final boolean isBuilt() {
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
        if (disposed) {
            return;
        }
        disposed = true;

        onDispose();
        detachFromParent();
        Throwable firstFailure = null;
        for (int i = children.size() - 1; i >= 0; i--) {
            try {
                children.get(i).dispose();
            } catch (Throwable t) {
                if (firstFailure == null) {
                    firstFailure = t;
                }
            }
        }
        children.clear();
        for (int i = disposables.size() - 1; i >= 0; i--) {
            try {
                disposables.get(i).dispose();
            } catch (Throwable t) {
                if (firstFailure == null) {
                    firstFailure = t;
                }
            }
        }
        disposables.clear();
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

    @Override
    public final boolean isDisposed() {
        return disposed;
    }
}
