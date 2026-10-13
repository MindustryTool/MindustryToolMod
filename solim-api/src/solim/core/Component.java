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

/**
 * The single base class for every Solim component. A component owns an Arc
 * {@link Element} and a
 * tree of resources that die with it.
 *
 * <p>
 * Two construction strategies, one class:
 * <ul>
 * <li>{@link #Component()} — <b>lazy</b>. The element is created by
 * {@link #build()} the first
 * time {@link #element()} is called, inside an ownership scope so that
 * everything the build
 * creates belongs to this component.</li>
 * <li>{@link #Component(Element)} — <b>eager</b>. The element already exists
 * and {@link #element()}
 * returns it immediately.</li>
 * </ul>
 *
 * <p>
 * Lifecycle rules:
 * <ul>
 * <li>This component is a node in the ownership tree. Child {@code Component}s
 * adopted through
 * {@link #adopt} are kept apart from the flat list of non-component resources,
 * so the two are
 * disposed in different phases.</li>
 * <li>Disposal runs {@link #onDispose()} first, then child components, then the
 * component's own
 * non-component resources — children first so a reactive resource cannot
 * recompute against an
 * element a child already disposed.</li>
 * <li>Within each phase, resources are disposed in reverse registration order
 * (LIFO), and each
 * disposal is attempted independently so one failure does not stop the
 * rest.</li>
 * <li>Calling {@link #element()} on a disposed component throws
 * {@link IllegalStateException}.</li>
 * <li>If {@link #build()} throws or returns {@code null}, every resource
 * registered so far is
 * disposed before the failure propagates.</li>
 * </ul>
 */
public abstract class Component implements DisposableOwner {

    private Element cached;
    private @Nullable String componentName;

    /**
     * Non-component resources in registration order; disposed LIFO after children.
     */
    private final List<Disposable> disposables = new ArrayList<>();

    private final List<Component> children = new ArrayList<>();

    private @Nullable Component parent;
    private boolean disposed = false;

    /**
     * Lazy flavour: {@link #build()} assigns the element on the first
     * {@link #element()} call.
     */
    protected Component() {
        joinAmbientScope();
    }

    protected Component(Element prebuilt) {
        this.cached = prebuilt;
        applyDefaultName(prebuilt);
        joinAmbientScope();
    }

    /**
     * Announces this component to whatever is currently building. The order
     * matters: the element
     * must already exist so an eager component can be attached the moment its
     * parent pops.
     */
    private void joinAmbientScope() {
        OwnershipContext.registerChild(this);
        if (attachesToParent()) {
            AttachmentBridge.registerPending(this);
        }
    }

    /**
     * Whether this component's element should be attached to the parent that is
     * open when the
     * constructor runs. Ownership always applies; only attachment is skippable, and
     * only by an
     * ownership unit that never mounts an element of its own — a multi-root
     * fragment, whose
     * captured roots are what belong to the parent.
     */
    protected boolean attachesToParent() {
        return true;
    }

    /**
     * Creates the element for a lazily-built component. Only called on the
     * {@link #Component()}
     * path; the eager constructor supplies the element instead.
     */
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

    /**
     * Returns the component that owns this one, or {@code null} when nothing has
     * adopted it yet.
     */
    public @Nullable Component owner() {
        return parent;
    }

    public List<Component> children() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Returns this component's non-component resources — reactive nodes
     * ({@code Computed},
     * {@code Effect}, {@code Binding}), listeners, and other {@link Disposable}
     * leaves — in
     * registration order. Child components are excluded because they are exposed by
     * {@link #children()}.
     *
     * <p>
     * Debug introspection uses this to discover which reactive nodes a component
     * owns so the
     * profiler can walk their dependency graphs.
     */
    public List<Disposable> disposables() {
        return Collections.unmodifiableList(disposables);
    }

    /**
     * Returns the underlying Arc element only if {@link #element()} has already
     * built it, or
     * {@code null} for a lazy component that has not been built yet.
     *
     * <p>
     * Unlike {@link #element()}, this never triggers a build. Debug introspection
     * uses it to
     * report layout bounds without mutating the tree it is observing.
     */
    public @Nullable Element peekElement() {
        return cached;
    }

    /**
     * Applies the inherited naming rules to an element this component just gained.
     */
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

    /**
     * The root element. Eager components return their element; lazy components
     * build it exactly
     * once, with this component installed as the ambient owner for the duration.
     */
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

    /**
     * Applies the spacing rules of a component that declares them, for example
     * {@link Text}.
     */
    private void applySpacing() {
        if (this instanceof SpacingAware) {
            ((SpacingAware) this).applySpacing();
        }
    }

    public @Nullable String name() {
        return componentName != null ? componentName : cached != null ? cached.name : null;
    }

    /**
     * Sets the explicit debug name, applied to the element as soon as it exists.
     * Subclasses
     * narrow the return type so the setter stays chainable.
     */
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

    /**
     * Takes responsibility for a resource. A {@link Component} becomes a child node
     * so it is
     * disposed in the children phase; anything else is kept as a leaf.
     *
     * <p>
     * A resource is owned by exactly one component: adopting one that already has
     * an owner
     * moves it, and re-adopting what this component already holds does nothing.
     * That is what
     * makes it safe for both the ambient scope and an explicit
     * {@link #own(Disposable)} call to
     * hand over the same object.
     */
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

    /**
     * Hands a resource to this component and returns it, so a modifier reads as one
     * expression.
     * Ownership is inherited, so there is no per-modifier step that can be
     * forgotten.
     */
    public final <T extends Disposable> T own(@Nullable T resource) {
        adopt(resource);
        return resource;
    }

    /**
     * Registers an Arc event listener that automatically unregisters when this
     * component is
     * disposed. Inherited, so no component has to remember to clean a listener up
     * by hand.
     */
    public <T> Disposable listen(Class<T> eventType, Cons<T> listener) {
        Events.on(eventType, listener);
        return own(DisposableAction.of(() -> Events.remove(eventType, listener)));
    }

    /**
     * Returns whether {@link #element()} has already produced the underlying Arc
     * element.
     */
    protected final boolean isBuilt() {
        return cached != null;
    }

    /**
     * Subclass hook executed during disposal, before owned resources are disposed.
     */
    protected void onDispose() {
    }

    protected final void checkNotDisposed() {
        if (disposed) {
            throw new IllegalStateException(
                    "Cannot use disposed component: " + getClass().getSimpleName());
        }
    }

    /**
     * Detaches this component from its owner so a long-lived parent does not retain
     * it.
     */
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
        // The hook runs first so it still sees a live tree, as subclasses expect.
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
