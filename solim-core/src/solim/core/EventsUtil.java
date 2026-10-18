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

    public static <E, T> Signal<T> createSignal(Component owner, Class<E> eventType, Prov<T> Prov) {
        Signal<T> signal = Signal.of(Prov.get());
        owner.listen(eventType, e -> signal.set(Prov.get()));
        return signal;
    }

    public static <E, T> Signal<T> createSignal(Component owner, Class<E> eventType, Func<E, T> mapper, T initial) {
        Signal<T> signal = Signal.of(initial);
        owner.listen(eventType, e -> signal.set(mapper.get(e)));
        return signal;
    }

    public static <T> Signal<T> createSignal(Func<Runnable, Disposable> registrar, Prov<T> Prov) {
        Signal<T> signal = Signal.of(Prov.get());
        if (registrar != null) {
            registrar.get(() -> signal.set(Prov.get()));
        }
        return signal;
    }
}
