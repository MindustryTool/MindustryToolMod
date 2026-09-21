package solim.reactive;

import arc.util.Log;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import arc.func.Cons;
import arc.func.Prov;
import solim.core.Disposable;
import solim.core.ReactiveObserver;
import solim.core.ReactiveSource;
import solim.core.SchedulableEffect;
import solim.runtime.ComponentContext;
import solim.runtime.ReactiveContext;
import solim.runtime.SignalDispatcher;

/** Reactive effect with auto-tracking and cleanup. */
public final class Effect implements ReactiveObserver, Disposable, SchedulableEffect {

	public interface Cleanup {
		void add(Runnable runnable);
	}

	private final Runnable runnable;
	private final Prov<Runnable> supplierWithCleanup;
	private final Cons<Cleanup> cleanupConsumer;

	private final Set<ReactiveSource> dependencies = new LinkedHashSet<>();
	private Set<ReactiveSource> collecting = null;
	private final List<Runnable> cleanups = new ArrayList<>();

	private boolean disposed = false;
	private boolean running = false;
	private boolean pending = false;

	private Effect(Runnable runnable, Prov<Runnable> Prov, Cons<Cleanup> cleanupConsumer) {
		this.runnable = runnable;
		this.supplierWithCleanup = Prov;
		this.cleanupConsumer = cleanupConsumer;
	}

	/**
	 * Creates an effect from a runnable. If called inside an active component build scope, the effect
	 * is automatically owned by that component (registered before its initial execution).
	 */
	public static Effect of(Runnable runnable) {
		return create(runnable, null, null);
	}

	/**
	 * Creates an effect from a Prov that returns a cleanup runnable. If called inside an active
	 * component build scope, the effect is automatically owned by that component.
	 */
	public static Effect of(Prov<Runnable> Prov) {
		return create(null, Prov, null);
	}

	/** Alias for {@link #of(Prov)}. */
	public static Effect ofSupplier(Prov<Runnable> Prov) {
		return of(Prov);
	}

	/**
	 * Creates an effect with a cleanup Cons. If called inside an active component build scope,
	 * the effect is automatically owned by that component.
	 *
	 * <p>For API {@code Effect.of(() -> { Subscription s=...; return s::dispose; })} use ofSupplier.
	 * This overload handles cleanup Cons: {@code Effect.of(cleanup -> { ... cleanup.add(...); })}.
	 */
	public static Effect of(Cons<Cleanup> Cons) {
		return create(null, null, Cons);
	}

	/** Convenience alias matching requirement example: effect(() -> {...}) */
	public static Effect effect(Runnable r) {
		return of(r);
	}

	/**
	 * Internal factory: registers the effect with the active ComponentContext (if any) BEFORE
	 * running it so that ownership is established even if the initial run throws.
	 */
	private static Effect create(Runnable runnable, Prov<Runnable> Prov, Cons<Cleanup> cleanupConsumer) {
		Effect effect = new Effect(runnable, Prov, cleanupConsumer);
		ComponentContext.register(effect);
		effect.runEffect();
		return effect;
	}

	private void runEffect() {
		if (disposed || running) {
			if (running) pending = true;
			return;
		}
		running = true;
		// run previous cleanups before re-running
		runCleanups();

		Set<ReactiveSource> newDeps = new LinkedHashSet<>();
		collecting = newDeps;
		ReactiveContext.push(this);
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
		} catch (Throwable e) {
			Log.err("[Effect] error", e);
		} finally {
			ReactiveContext.pop();
			collecting = null;
			running = false;
		}
		if (returned != null) {
			cleanups.add(returned);
		}
		updateDependencies(newDeps);
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

	private void updateDependencies(Set<ReactiveSource> newDeps) {
		Set<ReactiveSource> toRemove = new LinkedHashSet<>(dependencies);
		toRemove.removeAll(newDeps);
		for (ReactiveSource dep : toRemove) {
			dep.removeObserver(this);
		}
		Set<ReactiveSource> toAdd = new LinkedHashSet<>(newDeps);
		toAdd.removeAll(dependencies);
		for (ReactiveSource dep : toAdd) {
			dep.addObserver(this);
		}
		dependencies.clear();
		dependencies.addAll(newDeps);
	}

	@Override
	public void addDependency(ReactiveSource observable) {
		if (disposed) return;
		if (collecting != null) {
			collecting.add(observable);
		}
	}

	@Override
	public void invalidate() {
		if (disposed || pending) return;
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
		if (disposed) return;
		disposed = true;
		pending = false;
		// unsubscribe from dependencies
		for (ReactiveSource dep : new ArrayList<>(dependencies)) {
			dep.removeObserver(this);
		}
		dependencies.clear();
		runCleanups();
	}

	public boolean isDisposed() {
		return disposed;
	}

	// For tests
	int dependencyCount() {
		return dependencies.size();
	}

	private class CleanupImpl implements Cleanup {
		@Override
		public void add(Runnable runnable) {
			if (runnable != null) cleanups.add(runnable);
		}
	}
}
