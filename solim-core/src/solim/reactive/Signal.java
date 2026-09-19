package solim.reactive;

import arc.util.Log;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import solim.core.ReactiveObserver;
import solim.core.ReactiveSource;
import solim.runtime.ReactiveContext;
import solim.runtime.SolimAssert;

/** Mutable reactive value. */
public final class Signal<T> implements Readable<T>, ReactiveSource {
	private T value;
	private final List<Consumer<T>> listeners = new ArrayList<>();
	private final Set<ReactiveObserver> observers = new LinkedHashSet<>();

	private Signal(T initial) {
		this.value = initial;
	}

	public static <T> Signal<T> of(T initial) {
		return new Signal<>(initial);
	}

	/**
	 * Creates a Signal initialized from the given supplier that recalculates whenever the callback
	 * registrar invokes the registered callback (e.g. {@code element::resized}).
	 */
	public static <T> Signal<T> fromCallback(Consumer<Runnable> callbackRegistrar, Supplier<T> supplier) {
		Signal<T> signal = Signal.of(supplier.get());
		if (callbackRegistrar != null) {
			callbackRegistrar.accept(() -> signal.set(supplier.get()));
		}
		return signal;
	}

	public static <T> Signal<T> of(Consumer<Runnable> callbackRegistrar, Supplier<T> supplier) {
		return fromCallback(callbackRegistrar, supplier);
	}

	public static <T> Computed<T> computed(Supplier<T> supplier) {
		return new Computed<>(supplier);
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
		this.value = newValue;
		// notify listeners
		List<Consumer<T>> copy = new ArrayList<>(listeners);
		for (Consumer<T> c : copy) {
			try {
				c.accept(value);
			} catch (Throwable e) {
				Log.err("[Signal] listener error", e);
			}
		}
		// notify observers (Computeds/Effects)
		Set<ReactiveObserver> obsCopy = new LinkedHashSet<>(observers);
		for (ReactiveObserver o : obsCopy) {
			try {
				o.invalidate();
			} catch (Throwable e) {
				Log.err("[Signal] observer invalidate error", e);
			}
		}
	}

	public void update(Function<T, T> updater) {
		set(updater.apply(value));
	}

	public Subscription subscribe(Consumer<T> listener) {
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

	public <R> Computed<R> map(Function<T, R> mapper) {
		return Signal.computed(() -> mapper.apply(get()));
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
	int listenerCount() {
		return listeners.size();
	}

	int observerCount() {
		return observers.size();
	}
}
