package solim.reactive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import arc.func.Cons;
import arc.func.Func;
import arc.func.Prov;
import solim.core.DebugReactiveNode;
import solim.core.Disposable;
import solim.core.ReactiveObserver;
import solim.core.ReactiveSource;
import solim.core.OwnershipContext;
import solim.runtime.DebugReactiveContext;
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
    private final List<Cons<T>> listeners = new ArrayList<>();

    public Computed(Prov<T> Prov) {
        this.Prov = Prov;
        OwnershipContext.register(this);
        if (ReactiveContext.instance() instanceof DebugReactiveContext) {
            ((DebugReactiveContext) ReactiveContext.instance()).observeComputed(this);
        }
    }

    @Override
    public T get() {
        if (disposed) {
            // still return cached if available, but no tracking
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
        ReactiveContext.computedStart(this);
        long startNs = System.nanoTime();
        T newValue = null;
        Throwable error = null;
        try {
            newValue = Prov.get();
        } catch (Throwable e) {
            error = e;
        } finally {
            long durationNs = System.nanoTime() - startNs;
            ReactiveContext.computedEnd(this, !hasValue || !Objects.equals(cachedValue, newValue));
            ReactiveContext.pop();
            collecting = null;
            computing = false;
            recordDebugRun(durationNs);
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
        boolean changed = !hasValue || !Objects.equals(cachedValue, newValue);
        cachedValue = newValue;
        hasValue = true;
        dirty = false;
        updateDependencies(newDeps);
        if (changed) {
            // notify listeners
            List<Cons<T>> copy = new ArrayList<>(listeners);
            for (Cons<T> l : copy) {
                l.get(cachedValue);
            }
            // downstream already marked dirty via earlier invalidate, no need to
            // re-propagate if value
            // changed
            // but if downstream was not previously dirty (e.g., no prior invalidate), we
            // should propagate
            // now
            // However our invalidate propagation already ran before this recompute, so no
            // action.
        }
    }

    private void recordDebugRun(long durationNs) {
        ReactiveContext context = ReactiveContext.instance();
        if (context instanceof DebugReactiveContext) {
            ((DebugReactiveContext) context).recordComputedRun(this, durationNs);
        }
    }

    /**
     * Diffs the staging buffer against the recorded dependency set and reconciles
     * subscriptions.
     *
     * <p>
     * Allocation-free when nothing changed, which is the common case for a stable
     * binding:
     * the membership tests below never reach
     * {@code removeObserver}/{@code addObserver}.
     */
    private void updateDependencies(Set<ReactiveSource> newDeps) {
        // remove old not in new
        for (ReactiveSource dep : dependencies) {
            if (!newDeps.contains(dep)) {
                dep.removeObserver(this);
            }
        }
        // add new not in old
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
        // propagate to downstream
        Set<ReactiveObserver> copy = new LinkedHashSet<>(observers);
        for (ReactiveObserver o : copy) {
            o.invalidate();
        }
        // eager recompute if has listeners
        if (!listeners.isEmpty() && !computing) {
            recompute();
        }
    }

    public Subscription subscribe(Cons<T> listener) {
        listeners.add(listener);
        return new Subscription() {
            private boolean disposedFlag = false;

            @Override
            public void dispose() {
                if (!disposedFlag) {
                    disposedFlag = true;
                    listeners.remove(listener);
                }
            }

            @Override
            public boolean isDisposed() {
                return disposedFlag;
            }
        };
    }

    public <R> Computed<R> map(Func<T, R> mapper) {
        return Signal.computed(() -> mapper.get(get()));
    }

    @Override
    public void dispose() {
        if (disposed)
            return;
        disposed = true;
        // remove from dependencies
        for (ReactiveSource dep : new ArrayList<>(dependencies)) {
            dep.removeObserver(this);
        }
        dependencies.clear();
        observers.clear();
        listeners.clear();
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

    // For tests
    int dependencyCount() {
        return dependencies.size();
    }

    int observerCount() {
        return observers.size();
    }

    int listenerCount() {
        return listeners.size();
    }

    boolean isDirty() {
        return dirty;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    // --- debug introspection (solim-runtime profiling)
    // --------------------------------------------

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
        return listeners.size();
    }

    @Override
    public boolean debugDirty() {
        return dirty;
    }
}
