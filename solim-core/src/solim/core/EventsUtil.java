package solim.core;

import arc.Events;
import arc.func.Cons;
import arc.func.Func;
import arc.func.Prov;
import solim.reactive.Signal;

public final class EventsUtil {
    private EventsUtil() {
    }

    public static <T> Disposable listen(Class<T> type, Cons<T> listener) {
        Events.on(type, listener);
        return DisposableAction.of(() -> Events.remove(type, listener));
    }

    /**
     * Creates a reactive signal initialized from the Prov that recalculates
     * whenever the
     * specified Arc event fires. The listener is owned by {@code owner} and dies
     * with it.
     */
    public static <E, T> Signal<T> createSignal(Component owner, Class<E> eventType, Prov<T> Prov) {
        Signal<T> signal = Signal.of(Prov.get());
        owner.listen(eventType, e -> signal.set(Prov.get()));
        return signal;
    }

    /**
     * Creates a reactive signal that updates with mapped event data whenever the
     * specified Arc event
     * fires. The listener is owned by {@code owner} and dies with it.
     */
    public static <E, T> Signal<T> createSignal(Component owner, Class<E> eventType, Func<E, T> mapper, T initial) {
        Signal<T> signal = Signal.of(initial);
        owner.listen(eventType, e -> signal.set(mapper.get(e)));
        return signal;
    }

    /**
     * Creates a reactive signal initialized from the Prov that recalculates
     * whenever the callback
     * registrar invokes the given callback.
     */
    public static <T> Signal<T> createSignal(Func<Runnable, Disposable> registrar, Prov<T> Prov) {
        Signal<T> signal = Signal.of(Prov.get());
        if (registrar != null) {
            registrar.get(() -> signal.set(Prov.get()));
        }
        return signal;
    }
}
