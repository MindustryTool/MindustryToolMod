package solim.reactive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import arc.func.Func;
import arc.func.Prov;
import solim.core.DebugReactiveNode;
import solim.core.Disposable;
import solim.core.ReactiveObserver;
import solim.core.ReactiveSource;
import solim.core.OwnershipContext;
import solim.core.SolimAssert;
import solim.instrumentation.ComputedRun;
import solim.instrumentation.Instrumentation;
import solim.instrumentation.ReactiveCreated;
import solim.runtime.ReactiveContext;

public final class Computed<T> implements Disposable, ReactiveObserver, Readable<T>, ReactiveSource,
        DebugReactiveNode {
    private final Prov<T> Prov;
    private T cachedValue;
    private boolean hasValue = false;
    private boolean dirty = true;
    private boolean disposed = false;
    private boolean computing = false;

    private final Set<ReactiveSource> dependencies = new LinkedHashSet<>();

    private final Set<ReactiveSource> staging = new LinkedHashSet<>();
    private Set<ReactiveSource> collecting = null;
    private final Set<ReactiveObserver> observers = new LinkedHashSet<>();

    public Computed(Prov<T> Prov) {
        this.Prov = Prov;
        OwnershipContext.register(this);
        if (Instrumentation.enabled()) {
            Instrumentation.emit(new ReactiveCreated(this));
        }
    }

    @Override
    public T get() {
        if (disposed) {
            return cachedValue;
        }
        if (dirty || !hasValue) {
            recompute();
        }
        ReactiveContext.trackWithWarning(this,
                "[Solim Reactivity Warning] Computed.get() was called during build()! This severs reactivity. Pass the Computed/Readable directly to the component or use .map(). If an untracked read is intentional, use .peek().");
        return cachedValue;
    }

    @Override
    public T peek() {
        if (disposed) {
            return cachedValue;
        }
        if (dirty || !hasValue) {
            recompute();
        }
        return cachedValue;
    }

    private void recompute() {
        if (computing) {
            throw new IllegalStateException("Cycle detected in Computed");
        }
        computing = true;
        Set<ReactiveSource> newDeps = staging;
        newDeps.clear();
        collecting = newDeps;
        ReactiveContext.push(this);
        long startNs = System.nanoTime();
        T newValue = null;
        Throwable error = null;
        try {
            newValue = Prov.get();
        } catch (Throwable e) {
            error = e;
        } finally {
            long durationNs = System.nanoTime() - startNs;
            boolean changed = !hasValue || !Objects.equals(cachedValue, newValue);
            ReactiveContext.pop(this);
            collecting = null;
            computing = false;
            if (Instrumentation.enabled()) {
                Instrumentation.emit(new ComputedRun(this, startNs, durationNs, changed));
            }
        }
        if (error != null) {
            updateDependencies(newDeps);
            if (error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            if (error instanceof Error) {
                throw (Error) error;
            }
            throw new RuntimeException(error);
        }
        cachedValue = newValue;
        hasValue = true;
        dirty = false;
        updateDependencies(newDeps);
    }

    private void updateDependencies(Set<ReactiveSource> newDeps) {

        for (ReactiveSource dep : dependencies) {
            if (!newDeps.contains(dep)) {
                dep.removeObserver(this);
            }
        }

        for (ReactiveSource dep : newDeps) {
            if (!dependencies.contains(dep)) {
                dep.addObserver(this);
            }
        }
        dependencies.clear();
        dependencies.addAll(newDeps);
    }

    @Override
    public void addDependency(ReactiveSource observable) {
        if (disposed)
            return;
        if (collecting != null) {
            collecting.add(observable);
        }
    }

    @Override
    public void invalidate() {
        if (disposed || dirty)
            return;
        dirty = true;

        Set<ReactiveObserver> copy = new LinkedHashSet<>(observers);
        for (ReactiveObserver o : copy) {
            o.invalidate();
        }
    }

    public <R> Computed<R> map(Func<T, R> mapper) {
        return Signal.computed(() -> mapper.get(get()));
    }

    @Override
    public void dispose() {
        SolimAssert.checkMainThread();
        if (disposed)
            return;
        disposed = true;

        for (ReactiveSource dep : new ArrayList<>(dependencies)) {
            dep.removeObserver(this);
        }
        dependencies.clear();
        observers.clear();
        dirty = false;
        hasValue = false;
        cachedValue = null;
    }

    @Override
    public void addObserver(ReactiveObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(ReactiveObserver observer) {
        observers.remove(observer);
    }

    int dependencyCount() {
        return dependencies.size();
    }

    int observerCount() {
        return observers.size();
    }

    boolean isDirty() {
        return dirty;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public String debugLabel() {
        return "Computed";
    }

    @Override
    public String debugValue() {
        if (!hasValue || cachedValue == null) {
            return hasValue ? "null" : "<unevaluated>";
        }
        if (cachedValue instanceof Collection) {
            return "Collection(size=" + ((Collection<?>) cachedValue).size() + ")";
        }
        return String.valueOf(cachedValue);
    }

    @Override
    public List<ReactiveSource> debugDependencies() {
        return Collections.unmodifiableList(new ArrayList<>(dependencies));
    }

    @Override
    public List<ReactiveObserver> debugObservers() {
        return Collections.unmodifiableList(new ArrayList<>(observers));
    }

    @Override
    public int debugListenerCount() {
        return 0;
    }

    @Override
    public boolean debugDirty() {
        return dirty;
    }
}
