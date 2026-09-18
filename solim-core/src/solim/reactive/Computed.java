package solim.reactive;

import arc.util.Log;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import solim.core.Disposable;
import solim.core.ReactiveObserver;
import solim.runtime.ComponentContext;
import solim.runtime.ReactiveContext;

/** Lazy computed value with dynamic dependency tracking. */
public final class Computed<T> implements Disposable, ReactiveObserver, Readable<T> {
	private final Supplier<T> supplier;
	private T cachedValue;
	private boolean hasValue = false;
	private boolean dirty = true;
	private boolean disposed = false;
	private boolean computing = false;

	private final Set<Object> dependencies = new LinkedHashSet<>();
	private Set<Object> collecting = null;
	private final Set<ReactiveObserver> observers = new LinkedHashSet<>();
	private final List<Consumer<T>> listeners = new ArrayList<>();

	public Computed(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	@Override
	public T get() {
		if (disposed) {
			// still return cached if available, but no tracking
			return cachedValue;
		}
		if (ReactiveContext.current() == null && ComponentContext.current() != null) {
			Log.debug("[Solim Reactivity Warning] Computed.get() was called during build()! This severs reactivity. Pass the Computed/Readable directly to the component or use .map(). If an untracked read is intentional, use .peek().");
		}
		if (dirty || !hasValue) {
			recompute();
		}
		ReactiveContext.track(this);
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
		Set<Object> newDeps = new LinkedHashSet<>();
		collecting = newDeps;
		ReactiveContext.push(this);
		T newValue = null;
		Throwable error = null;
		try {
			newValue = supplier.get();
		} catch (Throwable e) {
			error = e;
		} finally {
			ReactiveContext.pop();
			collecting = null;
			computing = false;
		}
		if (error != null) {
			updateDependencies(newDeps);
			dirty = false;
			if (error instanceof IllegalStateException) throw (IllegalStateException) error;
			System.err.println("[Computed] supplier error: " + error.getMessage());
			error.printStackTrace();
			return;
		}
		boolean changed = !hasValue || !Objects.equals(cachedValue, newValue);
		cachedValue = newValue;
		hasValue = true;
		dirty = false;
		updateDependencies(newDeps);
		if (changed) {
			// notify listeners
			List<Consumer<T>> copy = new ArrayList<>(listeners);
			for (Consumer<T> l : copy) {
				try {
					l.accept(cachedValue);
				} catch (Throwable e) {
					System.err.println("[Computed] listener error: " + e.getMessage());
					e.printStackTrace();
				}
			}
			// downstream already marked dirty via earlier invalidate, no need to re-propagate if value
			// changed
			// but if downstream was not previously dirty (e.g., no prior invalidate), we should propagate
			// now
			// However our invalidate propagation already ran before this recompute, so no action.
		}
	}

	private void updateDependencies(Set<Object> newDeps) {
		// remove old not in new
		Set<Object> toRemove = new LinkedHashSet<>(dependencies);
		toRemove.removeAll(newDeps);
		for (Object dep : toRemove) {
			if (dep instanceof Signal) {
				((Signal<?>) dep).removeObserver(this);
			} else if (dep instanceof Computed) {
				((Computed<?>) dep).removeObserver(this);
			}
		}
		// add new not in old
		Set<Object> toAdd = new LinkedHashSet<>(newDeps);
		toAdd.removeAll(dependencies);
		for (Object dep : toAdd) {
			if (dep instanceof Signal) {
				((Signal<?>) dep).addObserver(this);
			} else if (dep instanceof Computed) {
				((Computed<?>) dep).addObserver(this);
			}
		}
		dependencies.clear();
		dependencies.addAll(newDeps);
	}

	@Override
	public void addDependency(Object observable) {
		if (disposed) return;
		if (collecting != null) {
			collecting.add(observable);
		}
	}

	@Override
	public void invalidate() {
		if (disposed || dirty) return;
		dirty = true;
		// propagate to downstream
		Set<ReactiveObserver> copy = new LinkedHashSet<>(observers);
		for (ReactiveObserver o : copy) {
			try {
				o.invalidate();
			} catch (Throwable e) {
				System.err.println("[Computed] downstream invalidate error: " + e.getMessage());
				e.printStackTrace();
			}
		}
		// eager recompute if has listeners
		if (!listeners.isEmpty() && !computing) {
			recompute();
		}
	}

	public Subscription subscribe(Consumer<T> listener) {
		listeners.add(listener);
		// eagerly ensure value computed and send current?
		// Spec expects subscriber to be notified on future changes, not immediately. So don't call
		// immediately.
		AtomicBoolean disposedFlag = new AtomicBoolean(false);
		return new Subscription() {
			@Override
			public void dispose() {
				if (disposedFlag.compareAndSet(false, true)) {
					listeners.remove(listener);
				}
			}

			@Override
			public boolean isDisposed() {
				return disposedFlag.get();
			}
		};
	}

	public <R> Computed<R> map(Function<T, R> mapper) {
		return Signal.computed(() -> mapper.apply(get()));
	}

	@Override
	public void dispose() {
		if (disposed) return;
		disposed = true;
		// remove from dependencies
		for (Object dep : new ArrayList<>(dependencies)) {
			if (dep instanceof Signal) {
				((Signal<?>) dep).removeObserver(this);
			} else if (dep instanceof Computed) {
				((Computed<?>) dep).removeObserver(this);
			}
		}
		dependencies.clear();
		observers.clear();
		listeners.clear();
		dirty = false;
		hasValue = false;
		cachedValue = null;
	}

	void addObserver(ReactiveObserver observer) {
		observers.add(observer);
	}

	void removeObserver(ReactiveObserver observer) {
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
}
