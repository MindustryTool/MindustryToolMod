package solim.signal;

import java.util.function.Function;
import java.util.function.Supplier;

/** Common read-only interface for reactive sources (Signal, Computed) or static values. */
@FunctionalInterface
public interface Readable<T> extends Supplier<T> {

	@Override
	T get();

	/**
	 * Returns the current value without dependency tracking.
	 * Use for non-reactive reads (e.g. inside event handlers or callbacks).
	 */
	default T peek() {
		return get();
	}

	default <R> Computed<R> map(Function<T, R> mapper) {
		return new Computed<>(() -> mapper.apply(get()));
	}

	static <T> Readable<T> of(T value) {
		return () -> value;
	}

	static <T> Readable<T> from(Supplier<T> supplier) {
		return supplier::get;
	}
}
