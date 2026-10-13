package solim.reactive;

import arc.func.Func;
import arc.func.Prov;

@FunctionalInterface
public interface Readable<T> extends Prov<T> {

    @Override
    T get();

    /**
     * Returns the current value without dependency tracking.
     * Use for non-reactive reads (e.g. inside event handlers or callbacks).
     */
    default T peek() {
        return get();
    }

    default <R> Computed<R> map(Func<T, R> mapper) {
        return new Computed<>(() -> mapper.get(get()));
    }

    static <T> Readable<T> of(T value) {
        return () -> value;
    }

    static <T> Readable<T> from(Prov<T> Prov) {
        return Prov::get;
    }
}
