package solim.reactive;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import arc.func.Cons;
import arc.func.Prov;
import solim.core.Disposable;
import solim.core.DisposableOwner;
import solim.core.OwnershipContext;
import arc.func.Boolf2;

public final class TwoWayBinding<T> implements DisposableOwner {

    private boolean updating = false;
    private boolean disposed = false;
    private final Effect effect;
    private final Disposable listenerDisposable;
    private final List<Disposable> owned = new ArrayList<>();

    public TwoWayBinding(
            Signal<T> signal,
            Prov<T> widgetGetter,
            Cons<T> widgetSetter,
            ListenerInstaller listenerInstaller,
            Boolf2<T, T> equalityChecker) {
        Objects.requireNonNull(signal, "signal cannot be null");
        Objects.requireNonNull(widgetGetter, "widgetGetter cannot be null");
        Objects.requireNonNull(widgetSetter, "widgetSetter cannot be null");
        Objects.requireNonNull(listenerInstaller, "listenerInstaller cannot be null");
        Objects.requireNonNull(equalityChecker, "equalityChecker cannot be null");

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

        OwnershipContext.push(this);
        try {
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
        } finally {
            OwnershipContext.pop();
        }

        OwnershipContext.register(this);
    }

    public TwoWayBinding(
            Signal<T> signal,
            Prov<T> widgetGetter,
            Cons<T> widgetSetter,
            ListenerInstaller listenerInstaller) {
        this(signal, widgetGetter, widgetSetter, listenerInstaller, Objects::equals);
    }

    @Override
    public void adopt(Disposable resource) {
        if (resource != null && !disposed) {
            owned.add(resource);
        }
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        effect.dispose();
        listenerDisposable.dispose();
        for (int i = owned.size() - 1; i >= 0; i--) {
            owned.get(i).dispose();
        }
        owned.clear();
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