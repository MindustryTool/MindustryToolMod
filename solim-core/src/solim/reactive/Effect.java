package solim.reactive;

import arc.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import arc.func.Cons;
import arc.func.Prov;
import solim.core.DebugReactiveNode;
import solim.core.Disposable;
import solim.core.DisposableOwner;
import solim.core.ReactiveObserver;
import solim.core.ReactiveSource;
import solim.core.SchedulableEffect;
import solim.core.OwnershipContext;
import solim.runtime.DebugReactiveContext;
import solim.runtime.OwnershipScope;
import solim.runtime.ReactiveContext;
import solim.runtime.SignalDispatcher;

public final class Effect implements ReactiveObserver, DisposableOwner, SchedulableEffect,
        DebugReactiveNode {

    public interface Cleanup {
        void add(Runnable runnable);
    }

    private final Runnable runnable;
    private final Prov<Runnable> supplierWithCleanup;
    private final Cons<Cleanup> cleanupConsumer;

    private final Set<ReactiveSource> dependencies = new LinkedHashSet<>();

    private final Set<ReactiveSource> staging = new LinkedHashSet<>();
    private Set<ReactiveSource> collecting = null;
    private final List<Runnable> cleanups = new ArrayList<>();

    private final List<Disposable> owned = new ArrayList<>();

    private boolean disposed = false;
    private boolean running = false;
    private boolean pending = false;

    private Effect(Runnable runnable, Prov<Runnable> Prov, Cons<Cleanup> cleanupConsumer) {
        this.runnable = runnable;
        this.supplierWithCleanup = Prov;
        this.cleanupConsumer = cleanupConsumer;
    }

    public static Effect of(Runnable runnable) {
        return create(runnable, null, null);
    }

    public static <T> Effect of(Readable<T> source, Cons<T> target) {
        return of(() -> target.get(source.get()));
    }

    public static Effect of(Prov<Runnable> Prov) {
        return create(null, Prov, null);
    }

    public static Effect ofSupplier(Prov<Runnable> Prov) {
        return of(Prov);
    }

    public static Effect of(Cons<Cleanup> Cons) {
        return create(null, null, Cons);
    }

    public static Effect effect(Runnable r) {
        return of(r);
    }

    private static Effect create(Runnable runnable, Prov<Runnable> Prov, Cons<Cleanup> cleanupConsumer) {
        Effect effect = new Effect(runnable, Prov, cleanupConsumer);
        OwnershipContext.register(effect);
        ReactiveContext context = ReactiveContext.instance();
        if (context instanceof DebugReactiveContext) {
            ((DebugReactiveContext) context).observeEffect(effect);
        }
        effect.runEffect();
        return effect;
    }

    private void runEffect() {
        if (disposed || running) {
            if (running)
                pending = true;
            return;
        }
        running = true;

        runCleanups();

        Set<ReactiveSource> newDeps = staging;
        newDeps.clear();
        collecting = newDeps;
        ReactiveContext.push(this);
        ReactiveContext.effectStart(this);
        long startNs = System.nanoTime();

        OwnershipScope scope = OwnershipScope.open();
        Runnable returned = null;
        try {
            if (runnable != null) {
                runnable.run();
            } else if (supplierWithCleanup != null) {
                returned = supplierWithCleanup.get();
            } else if (cleanupConsumer != null) {
                CleanupImpl ci = new CleanupImpl();
                cleanupConsumer.get(ci);
            }
            scope.adoptInto(this);
        } finally {
            long durationNs = System.nanoTime() - startNs;
            ReactiveContext.effectEnd(this);
            ReactiveContext.pop();
            collecting = null;
            running = false;
            scope.close();
            recordDebugRun(durationNs);
        }
        if (returned != null) {
            cleanups.add(returned);
        }
        updateDependencies(newDeps);
    }

    @Override
    public void adopt(Disposable resource) {
        if (resource != null && !disposed) {
            owned.add(resource);
        }
    }

    private void disposeOwned() {
        for (int i = owned.size() - 1; i >= 0; i--) {
            try {
                owned.get(i).dispose();
            } catch (Throwable t) {
                Log.err("[Effect] error disposing body resource", t);
            }
        }
        owned.clear();
    }

    private void runCleanups() {
        List<Runnable> copy = new ArrayList<>(cleanups);
        cleanups.clear();
        for (Runnable r : copy) {
            try {
                r.run();
            } catch (Throwable e) {
                Log.err("[Effect] cleanup error", e);
            }
        }
    }

    private void recordDebugRun(long durationNs) {
        ReactiveContext context = ReactiveContext.instance();
        if (context instanceof DebugReactiveContext) {
            ((DebugReactiveContext) context).recordEffectRun(this, durationNs);
        }
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
        if (disposed || pending)
            return;
        pending = true;
        SignalDispatcher.enqueue(this);
    }

    @Override
    public void runPending() {
        if (disposed) {
            pending = false;
            return;
        }
        pending = false;
        runEffect();
    }

    @Override
    public void clearPending() {
        pending = false;
    }

    boolean isPending() {
        return pending;
    }

    public void dispose() {
        if (disposed)
            return;
        disposed = true;
        pending = false;

        for (ReactiveSource dep : new ArrayList<>(dependencies)) {
            dep.removeObserver(this);
        }
        dependencies.clear();
        runCleanups();
        disposeOwned();
    }

    public boolean isDisposed() {
        return disposed;
    }

    int dependencyCount() {
        return dependencies.size();
    }

    @Override
    public String debugLabel() {
        return "Effect";
    }

    @Override
    public String debugValue() {
        return "deps=" + dependencies.size();
    }

    @Override
    public List<ReactiveSource> debugDependencies() {
        return Collections.unmodifiableList(new ArrayList<>(dependencies));
    }

    @Override
    public List<ReactiveObserver> debugObservers() {
        return Collections.emptyList();
    }

    @Override
    public int debugListenerCount() {
        return 0;
    }

    @Override
    public boolean debugDirty() {
        return pending;
    }

    private class CleanupImpl implements Cleanup {
        @Override
        public void add(Runnable runnable) {
            if (runnable != null)
                cleanups.add(runnable);
        }
    }
}
