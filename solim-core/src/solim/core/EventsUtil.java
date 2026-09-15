package solim.core;

import arc.Events;
import arc.func.Cons;
import arc.func.Func;
import java.util.function.Function;
import java.util.function.Supplier;
import solim.signal.Signal;

/** Utility for lifecycle-safe Arc event listening and reactive signal creation. */
public final class EventsUtil {
	private EventsUtil() {}

	public static <T> Disposable listen(Class<T> type, Cons<T> listener) {
		Events.on(type, listener);
		return () -> Events.remove(type, listener);
	}

	/**
	 * Creates a reactive signal initialized from the supplier that recalculates whenever the
	 * specified Arc event fires.
	 */
	public static <E, T> Signal<T> createSignal(Class<E> eventType, Supplier<T> supplier) {
		Signal<T> signal = Signal.of(supplier.get());
		Events.on(eventType, e -> signal.set(supplier.get()));
		return signal;
	}

	/**
	 * Creates a reactive signal that updates with mapped event data whenever the specified Arc event
	 * fires.
	 */
	public static <E, T> Signal<T> createSignal(Class<E> eventType, Func<E, T> mapper, T initial) {
		Signal<T> signal = Signal.of(initial);
		Events.on(eventType, e -> signal.set(mapper.get(e)));
		return signal;
	}

	/**
	 * Creates a reactive signal initialized from the supplier that recalculates whenever the callback
	 * registrar invokes the given callback.
	 */
	public static <T> Signal<T> createSignal(Function<Runnable, Disposable> registrar, Supplier<T> supplier) {
		Signal<T> signal = Signal.of(supplier.get());
		if (registrar != null) {
			registrar.apply(() -> signal.set(supplier.get()));
		}
		return signal;
	}
}
