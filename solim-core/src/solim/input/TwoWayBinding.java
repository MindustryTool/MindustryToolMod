package solim.input;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import solim.core.Disposable;
import solim.signal.Effect;
import solim.signal.Signal;
import java.util.function.BiPredicate;

/**
 * Shared two-way binding utility for Solim input components.
 *
 * <p>Synchronizes a {@link Signal} with a widget in both directions:
 * <ul>
 *   <li>Signal → widget: an {@link Effect} watches the signal and calls the widget setter.</li>
 *   <li>Widget → signal: the widget listener calls the signal setter.</li>
 * </ul>
 *
 * <p>A {@code updating} flag prevents feedback loops: a programmatic widget update (driven by
 * a signal change) will not trigger the widget listener to write back to the signal.
 *
 * <p>Implements {@link Disposable}. When disposed, the signal-watching Effect is disposed and
 * the widget listener is removed (if the widget supports listener removal). The binding is
 * automatically owned by the active {@link solim.runtime.ComponentContext} when created inside
 * a component {@code build()}.
 *
 * @param <T> the value type shared by the signal and the widget
 */
class TwoWayBinding<T> implements Disposable {

    private boolean updating = false;
    private boolean disposed = false;
    private final Effect effect;
    private final Disposable listenerDisposable;

    /**
     * Creates a two-way binding.
     *
     * @param signal           the signal to bind
     * @param widgetGetter     reads the current value from the widget
     * @param widgetSetter     pushes a value to the widget (suppressed during signal→widget flow)
     * @param listenerInstaller installs the widget→signal listener; must call {@code onChange.run()}
     *                         when the widget value changes. Returns a {@link Disposable} that
     *                         removes the listener (or a no-op if the widget API does not support
     *                         removal).
     * @param equalityChecker  returns {@code true} if two values are considered equal (used to
     *                         suppress unnecessary updates in both directions)
     */
    TwoWayBinding(
            Signal<T> signal,
            Supplier<T> widgetGetter,
            Consumer<T> widgetSetter,
            ListenerInstaller listenerInstaller,
            BiPredicate<T, T> equalityChecker) {

        // Widget → Signal: install the change listener
        this.listenerDisposable = listenerInstaller.install(() -> {
            if (disposed || updating) {
                return;
            }
            T widgetValue = widgetGetter.get();
            T signalValue = signal.peek();
            if (!equalityChecker.test(widgetValue, signalValue)) {
                signal.set(widgetValue);
            }
        });

        // Signal → Widget: reactive effect
        this.effect = Effect.of(() -> {
            T signalValue = signal.get();
            T widgetValue = widgetGetter.get();
            if (!equalityChecker.test(signalValue, widgetValue)) {
                updating = true;
                try {
                    widgetSetter.accept(signalValue);
                } finally {
                    updating = false;
                }
            }
        });
    }

    /**
     * Convenience constructor using {@link Objects#equals} for equality checks.
     */
    TwoWayBinding(
            Signal<T> signal,
            Supplier<T> widgetGetter,
            Consumer<T> widgetSetter,
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

    /**
     * Functional interface for installing a widget change listener that returns a cleanup handle.
     */
    @FunctionalInterface
    interface ListenerInstaller {
        /**
         * Installs the given {@code onChange} callback on the widget. Returns a {@link Disposable}
         * that removes the listener, or a no-op {@code Disposable} if the widget API does not
         * support listener removal.
         */
        Disposable install(Runnable onChange);
    }
}
