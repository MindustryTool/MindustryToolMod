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
import solim.instrumentation.Instrumentation;
import solim.instrumentation.ReactiveCreated;
import solim.instrumentation.SignalWrite;
import solim.runtime.ReactiveContext;
import solim.core.SolimAssert;

public final class Signal<T> implements Readable<T>, ReactiveSource, DebugReactiveNode {
    private T value;
    private Set<ReactiveObserver> observers = Collections.emptySet();

    private Signal(T initial) {
        this.value = initial;
        if (Instrumentation.enabled()) {
            Instrumentation.emit(new ReactiveCreated(this));
        }
    }

    public static <T> Signal<T> of(T initial) {
        return new Signal<>(initial);
    }

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
        ReactiveContext.trackWithWarning(this,
                "[Solim Reactivity Warning] Signal.get() was called during build()! This severs reactivity. Pass the Signal/Readable directly to the component or use .map(). If an untracked read is intentional, use .peek().");
        return value;
    }

    @Override
    public T peek() {
        return value;
    }

    public void set(T newValue) {
        SolimAssert.checkMainThread();
        if (Objects.equals(value, newValue)) {
            return;
        }
        T oldValue = this.value;
        long startNs = Instrumentation.enabled() ? System.nanoTime() : 0L;
        this.value = newValue;
        for (ReactiveObserver observer : observers) {
            observer.invalidate();
        }
        if (Instrumentation.enabled()) {
            Instrumentation.emit(new SignalWrite(this, oldValue, newValue, startNs));
        }
    }

    public void update(Func<T, T> updater) {
        set(updater.get(value));
    }

    public <R> Computed<R> map(Func<T, R> mapper) {
        return Signal.computed(() -> mapper.get(get()));
    }

    @Override
    public void addObserver(ReactiveObserver observer) {
        if (!observers.contains(observer)) {
            Set<ReactiveObserver> next = new LinkedHashSet<>(observers);
            next.add(observer);
            observers = Collections.unmodifiableSet(next);
        }
    }

    @Override
    public void removeObserver(ReactiveObserver observer) {
        if (observers.contains(observer)) {
            Set<ReactiveObserver> next = new LinkedHashSet<>(observers);
            next.remove(observer);
            observers = Collections.unmodifiableSet(next);
        }
    }

    public int observerCount() {
        return observers.size();
    }

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
        return 0;
    }

    @Override
    public boolean debugDirty() {
        return false;
    }
}
