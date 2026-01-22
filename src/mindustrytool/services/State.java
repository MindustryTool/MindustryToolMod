package mindustrytool.services;

import arc.Core;
import arc.struct.Seq;
import arc.util.Nullable;

import java.util.Objects;

public final class State<T> {

    public interface Listener<T> {
        void onChanged(@Nullable T newValue, @Nullable T oldValue);
    }

    private final Seq<Listener<T>> listeners = new Seq<>();
    private T value;

    public State() {
    }

    public State(@Nullable T initialValue) {
        this.value = initialValue;
    }

    public @Nullable T get() {
        return value;
    }

    public void set(@Nullable T newValue) {
        if (Objects.equals(value, newValue)) {
            return;
        }

        T old = value;
        value = newValue;

        Core.app.post(() -> notifyListeners(newValue, old));
    }

    public void subscribe(Listener<T> listener) {
        listeners.add(listener);
        listener.onChanged(value, value);
    }

    public void unsubscribe(Listener<T> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(T newValue, T oldValue) {
        for (Listener<T> l : listeners) {
            l.onChanged(newValue, oldValue);
        }
    }
}
