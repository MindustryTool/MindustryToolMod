package solim.performance;

import java.util.Collections;
import java.util.List;

/**
 * Immutable debug snapshot of one live {@code Component} in the {@code SessionRoot} hierarchy.
 *
 * <p>The snapshot is fully detached from the live tree: it stores labels and primitive layout
 * bounds rather than element or component references, so a written profile report cannot keep
 * disposed components alive.
 */
public final class ComponentNodeSnapshot {

    /** Monotonic per-session identifier assigned by the profiler while traversing the tree. */
    public final int id;
    /** Component class simple name, e.g. {@code Column}. */
    public final String className;
    /** Explicit debug name, element name, or a class-derived fallback. */
    public final String name;
    /** Arc element class simple name, or {@code null} when the component has not built yet. */
    public final String elementType;
    /** Layout bounds {@code [x, y, width, height]} in scene units. */
    public final float[] bounds;
    /** Whether the underlying element is currently visible. */
    public final boolean visible;
    /** Whether the component reports itself as disposed. */
    public final boolean disposed;
    /** Child component snapshots, in adoption order. */
    public final List<ComponentNodeSnapshot> children;
    /** Signals created while this component was the active owner, plus dependency-discovered signals. */
    public final List<SignalSnapshot> signals;
    /** Computeds owned by this component and reached through its owned reactive graphs. */
    public final List<ComputedSnapshot> computeds;
    /** Effects owned by this component. */
    public final List<EffectSnapshot> effects;
    /** Class simple names of other owned {@code Disposable} leaves that are not reactive nodes. */
    public final List<String> otherDisposables;

    public ComponentNodeSnapshot(int id, String className, String name, String elementType,
            float[] bounds, boolean visible, boolean disposed,
            List<ComponentNodeSnapshot> children, List<SignalSnapshot> signals,
            List<ComputedSnapshot> computeds, List<EffectSnapshot> effects,
            List<String> otherDisposables) {
        this.id = id;
        this.className = className != null ? className : "";
        this.name = name != null && !name.trim().isEmpty() ? name : (this.className.isEmpty() ? "Component" : this.className);
        this.elementType = elementType;
        this.bounds = bounds != null ? bounds : new float[]{0f, 0f, 0f, 0f};
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