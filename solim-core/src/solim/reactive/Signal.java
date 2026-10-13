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
import solim.core.ReactiveObserver;
import solim.core.ReactiveSource;
import solim.runtime.ReactiveContext;
import solim.core.SolimAssert;

/** Mutable reactive value. */
public final class Signal<T> implements Readable<T>, ReactiveSource, DebugReactiveNode {
	private T value;
	private final List<Cons<T>> listeners = new ArrayList<>();
	private final Set<ReactiveObserver> observers = new LinkedHashSet<>();

	private Signal(T initial) {
		this.value = initial;
		ReactiveContext.signalCreated(this);
	}

	public static <T> Signal<T> of(T initial) {
		return new Signal<>(initial);
	}

	/**
	 * Creates a Signal initialized from the given Prov that recalculates whenever the callback
	 * registrar invokes the registered callback (e.g. {@code element::resized}).
	 */
	public static <T> Signal<T> fromCallback(Cons<Runnable> callbackRegistrar, Prov<T> Prov) {
		Signal<T> signal = Signal.of(Prov.get());
		if (callbackRegistrar != null) {
			callbackRegistrar.get(() -> signal.set(Prov.get()));
		}
		return signal;
	}

	public static <T> Signal<T> of(Cons<Runnable> callbackRegistrar, Prov<T> Prov) {
		return fromCallback(callbackRegistrar, Prov);
	}

	public static <T> Computed<T> computed(Prov<T> Prov) {
		return new Computed<>(Prov);
	}

	@Override
	public T get() {
		ReactiveContext.trackWithWarning(this, "[Solim Reactivity Warning] Signal.get() was called during build()! This severs reactivity. Pass the Signal/Readable directly to the component or use .map(). If an untracked read is intentional, use .peek().");
		return value;
	}

	@Override
	public T peek() {
		return value;
	}

	public void set(T newValue) {
		SolimAssert.checkMainThread();
		if (Objects.equals(value, newValue)) return;
		T oldValue = this.value;
		ReactiveContext.signalWriteStart(this, oldValue, newValue);
		this.value = newValue;
		try {
			// notify listeners
			List<Cons<T>> copy = new ArrayList<>(listeners);
			for (Cons<T> c : copy) {
				c.get(value);
			}
			// notify observers (Computeds/Effects)
			Set<ReactiveObserver> obsCopy = new LinkedHashSet<>(observers);
			for (ReactiveObserver o : obsCopy) {
				o.invalidate();
			}
		} finally {
			ReactiveContext.signalWriteEnd(this);
		}
	}

	public void update(Func<T, T> updater) {
		set(updater.get(value));
	}

	public Subscription subscribe(Cons<T> listener) {
		listeners.add(listener);
		return new Subscription() {
			private boolean disposed = false;

			@Override
			public void dispose() {
				if (!disposed) {
					disposed = true;
					listeners.remove(listener);
				}
			}

			@Override
			public boolean isDisposed() {
				return disposed;
			}
		};
	}

	public <R> Computed<R> map(Func<T, R> mapper) {
		return Signal.computed(() -> mapper.get(get()));
	}

	@Override
	public void addObserver(ReactiveObserver observer) {
		observers.add(observer);
	}

	@Override
	public void removeObserver(ReactiveObserver observer) {
		observers.remove(observer);
	}

	// For testing: listener/observer counts
	public int listenerCount() {
		return listeners.size();
	}

	public int observerCount() {
		return observers.size();
	}

	// --- debug introspection (solim-runtime profiling) --------------------------------------------

	@Override
	public String debugLabel() {
		return "Signal(" + (value != null ? value.getClass().getSimpleName() : "null") + ")";
	}

	@Override
	public String debugValue() {
		if (value == null) {
			return "null";
		}
		if (value instanceof Object[]) {
			return value.getClass().getSimpleName() + "[" + ((Object[]) value).length + "]";
		}
		if (value instanceof Collection) {
			return "Collection(size=" + ((Collection<?>) value).size() + ")";
		}
		return String.valueOf(value);
	}

	/** A signal is a leaf source, so it reads nothing. */
	@Override
	public List<ReactiveSource> debugDependencies() {
		return Collections.emptyList();
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
		return false;
	}
}
