package solim.reactive;

import java.util.Objects;
import arc.func.Cons;
import arc.func.Prov;
import solim.core.Disposable;
import arc.func.Boolf2;

public final class TwoWayBinding<T> implements Disposable {

    private boolean updating = false;
    private boolean disposed = false;
    private final Effect effect;
    private final Disposable listenerDisposable;

    public TwoWayBinding(
            Signal<T> signal,
            Prov<T> widgetGetter,
            Cons<T> widgetSetter,
            ListenerInstaller listenerInstaller,
            Boolf2<T, T> equalityChecker) {

        this.listenerDisposable = listenerInstaller.install(() -> {
            if (disposed || updating) {
                return;
            }
            T widgetValue = widgetGetter.get();
            T signalValue = signal.peek();
            if (!equalityChecker.get(widgetValue, signalValue)) {
                signal.set(widgetValue);
            }
        });

        this.effect = Effect.of(() -> {
            T signalValue = signal.get();
            T widgetValue = widgetGetter.get();
            if (!equalityChecker.get(signalValue, widgetValue)) {
                updating = true;
                try {
                    widgetSetter.get(signalValue);
                } finally {
                    updating = false;
                }
            }
        });
    }

    public TwoWayBinding(
            Signal<T> signal,
            Prov<T> widgetGetter,
            Cons<T> widgetSetter,
            ListenerInstaller listenerInstaller) {
        this(signal, widgetGetter, widgetSetter, listenerInstaller, Objects::equals);
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        effect.dispose();
        listenerDisposable.dispose();
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @FunctionalInterface
    public interface ListenerInstaller {

        Disposable install(Runnable onChange);
    }
}
