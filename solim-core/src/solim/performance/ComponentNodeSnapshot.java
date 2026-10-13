package solim.performance;

import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ComponentNodeSnapshot {

    public final int id;

    public final String className;

    public final String name;

    public final @Nullable String elementType;

    public final float[] bounds;

    public final boolean visible;

    public final boolean disposed;

    public final List<ComponentNodeSnapshot> children;

    public final List<SignalSnapshot> signals;

    public final List<ComputedSnapshot> computeds;

    public final List<EffectSnapshot> effects;

    public final List<String> otherDisposables;

    public ComponentNodeSnapshot(int id, @Nullable String className, @Nullable String name,
            @Nullable String elementType, @Nullable float[] bounds, boolean visible, boolean disposed,
            @Nullable List<ComponentNodeSnapshot> children, @Nullable List<SignalSnapshot> signals,
            @Nullable List<ComputedSnapshot> computeds, @Nullable List<EffectSnapshot> effects,
            @Nullable List<String> otherDisposables) {
        this.id = id;
        this.className = className != null ? className : "";
        this.name = name != null && !name.trim().isEmpty() ? name
                : (this.className.isEmpty() ? "Component" : this.className);
        this.elementType = elementType;
        this.bounds = bounds != null ? bounds.clone() : new float[] { 0f, 0f, 0f, 0f };
        this.visible = visible;
        this.disposed = disposed;
        this.children = children != null
                ? Collections.unmodifiableList(new ArrayList<>(children))
                : Collections.<ComponentNodeSnapshot>emptyList();
        this.signals = signals != null
                ? Collections.unmodifiableList(new ArrayList<>(signals))
                : Collections.<SignalSnapshot>emptyList();
        this.computeds = computeds != null
                ? Collections.unmodifiableList(new ArrayList<>(computeds))
                : Collections.<ComputedSnapshot>emptyList();
        this.effects = effects != null
                ? Collections.unmodifiableList(new ArrayList<>(effects))
                : Collections.<EffectSnapshot>emptyList();
        this.otherDisposables = otherDisposables != null
                ? Collections.unmodifiableList(new ArrayList<>(otherDisposables))
                : Collections.<String>emptyList();
    }

    @Override
    public String toString() {
        return "ComponentNodeSnapshot#" + id + "[" + className + " " + name
                + " children=" + children.size() + "]";
    }
}
