package mindustrytool.ui.common;

import arc.Core;

public class State<T> {
    private T value;
    private Runnable listener;

    public State(T initial) { value = initial; }
    public T get() { return value; }
    public void set(T newValue) { value = newValue; if (listener != null) Core.app.post(listener); }
    public void bind(Runnable r) { listener = r; }
    
    public static <T> State<T> of(T initial) { return new State<>(initial); }
}
