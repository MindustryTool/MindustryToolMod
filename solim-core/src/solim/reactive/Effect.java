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

/** Reactive effect with auto-tracking and cleanup.
 *
 * <p>Fail-fast by design: an exception thrown by the body is <b>not</b> caught or logged here.
 * It propagates to whoever triggered the run (the caller of {@code Effect.of}, or the
 * dispatcher flush) so a broken binding fails loudly instead of silently going dead.
 */
public final class Effect implements ReactiveObserver, DisposableOwner, SchedulableEffect,
		DebugReactiveNode {

	public interface Cleanup {
		void add(Runnable runnable);
	}

	private final Runnable runnable;
	private final Prov<Runnable> supplierWithCleanup;
	private final Cons<Cleanup> cleanupConsumer;

	private final Set<ReactiveSource> dependencies = new LinkedHashSet<>();
	/** Staging buffer for dependency collection, cleared and refilled on every run. */
	private final Set<ReactiveSource> staging = new LinkedHashSet<>();
	private Set<ReactiveSource> collecting = null;
	private final List<Runnable> cleanups = new ArrayList<>();
	/** Resources created by the most recent body run; released before the next one. */
	private final List<Disposable> owned = new ArrayList<>();

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
	 * Internal factory: registers the effect with the active OwnershipContext (if any) BEFORE
	 * running it so that ownership is established even if the initial run throws.
	 */
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
			if (running) pending = true;
			return;
		}
		running = true;
		// run previous cleanups before re-running
		runCleanups();

		Set<ReactiveSource> newDeps = staging;
		newDeps.clear();
		collecting = newDeps;
		ReactiveContext.push(this);
		ReactiveContext.effectStart(this);
		long startNs = System.nanoTime();
		// The effect body runs at flush time when no component owns it, so resources it
		// creates would otherwise leak. The scope adopts them into this effect; they are
		// released with the effect, not before the next run — a re-running effect may own
		// live UI that must survive until the effect itself dies.
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

	/** Takes responsibility for a resource created by the effect body. */
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

	/** Feeds the measured body duration to the installed debug context, if any. */
	private void recordDebugRun(long durationNs) {
		ReactiveContext context = ReactiveContext.instance();
		if (context instanceof DebugReactiveContext) {
			((DebugReactiveContext) context).recordEffectRun(this, durationNs);
		}
	}

	/**
	 * Diffs the staging buffer against the recorded dependency set and reconciles subscriptions.
	 *
	 * <p>Allocation-free when nothing changed, which is the common case for a stable binding:
	 * the membership tests below never reach {@code removeObserver}/{@code addObserver}.
	 */
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
		disposeOwned();
	}

	public boolean isDisposed() {
		return disposed;
	}

	// For tests
	int dependencyCount() {
		return dependencies.size();
	}

	// --- debug introspection (solim-runtime profiling) --------------------------------------------

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

	/** An effect is a leaf sink, so nothing observes it. */
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
			if (runnable != null) cleanups.add(runnable);
		}
	}
}
