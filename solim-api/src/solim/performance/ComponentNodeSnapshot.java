package solim.performance;

import java.util.Collections;
import java.util.List;

/**
 * Immutable debug snapshot of one live {@code Component} in the
 * {@code SessionRoot} hierarchy.
 *
 * <p>
 * The snapshot is fully detached from the live tree: it stores labels and
 * primitive layout
 * bounds rather than element or component references, so a written profile
 * report cannot keep
 * disposed components alive.
 */
public final class ComponentNodeSnapshot {

    public final int id;

    public final String className;

    public final String name;

    public final String elementType;

    public final float[] bounds;

    public final boolean visible;

    public final boolean disposed;

    public final List<ComponentNodeSnapshot> children;

    public final List<SignalSnapshot> signals;

    public final List<ComputedSnapshot> computeds;

    public final List<EffectSnapshot> effects;

    public final List<String> otherDisposables;

    public ComponentNodeSnapshot(int id, String className, String name, String elementType,
            float[] bounds, boolean visible, boolean disposed,
            List<ComponentNodeSnapshot> children, List<SignalSnapshot> signals,
            List<ComputedSnapshot> computeds, List<EffectSnapshot> effects,
            List<String> otherDisposables) {
        this.id = id;
        this.className = className != null ? className : "";
        this.name = name != null && !name.trim().isEmpty() ? name
                : (this.className.isEmpty() ? "Component" : this.className);
        this.elementType = elementType;
        this.bounds = bounds != null ? bounds : new float[] { 0f, 0f, 0f, 0f };
        this.visible = visible;
        this.disposed = disposed;
        this.children = children != null ? children : Collections.<ComponentNodeSnapshot>emptyList();
        this.signals = signals != null ? signals : Collections.<SignalSnapshot>emptyList();
        this.computeds = computeds != null ? computeds : Collections.<ComputedSnapshot>emptyList();
        this.effects = effects != null ? effects : Collections.<EffectSnapshot>emptyList();
        this.otherDisposables = otherDisposables != null ? otherDisposables : Collections.<String>emptyList();
    }

    @Override
    public String toString() {
        return "ComponentNodeSnapshot#" + id + "[" + className + " " + name
                + " children=" + children.size() + "]";
    }
}
