package mindustrytool.plugins.playerconnect;

import arc.Core;

/** Simple reactive state holder with listener support. */
public class State<T> {
    private volatile T value;
    private volatile Runnable listener;

    public State(T initial) { value = initial; }
    public T get() { return value; }
    public void set(T newValue) { 
        value = newValue; 
        Runnable l = listener;
        if (l != null) Core.app.post(l); 
    }
    public void bind(Runnable r) { listener = r; }
    
    public static <T> State<T> of(T initial) { return new State<>(initial); }
}
