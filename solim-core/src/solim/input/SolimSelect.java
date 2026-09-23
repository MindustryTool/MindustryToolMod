package solim.input;

import arc.scene.ui.TextButton;
import java.util.List;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.DisposableAction;
import solim.reactive.Signal;
import solim.reactive.TwoWayBinding;
import solim.modifier.ElementConfig;
import solim.modifier.CellConfig;
import solim.modifier.PendingCellConfig;
import solim.runtime.OwnershipContext;

/**
 * Select widget bound to Signal&lt;T&gt;. Uses TextButton as placeholder (Arc
 * SelectBox unavailable in this version). State held in signal; visual updates
 * on change.
 */
public final class SolimSelect<T> implements Component, ElementConfig<SolimSelect<T>>, CellConfig<SolimSelect<T>> {
    private final TextButton selectBox = new TextButton("");
    private final PendingCellConfig constraints = new PendingCellConfig();
    private int selectedIndex = 0;
    private Disposable binding;
    private boolean disposed = false;

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    {
        selectBox.name = "solim-select-selectBox";
    }

    public SolimSelect(Signal<T> signal, List<T> options) {
        if (signal.peek() != null) {
            int idx = options.indexOf(signal.peek());
            if (idx >= 0)
                selectedIndex = idx;
        }
        selectBox.setText(String.valueOf(signal.peek()));
        this.binding = new TwoWayBinding<>(
                signal,
                () -> options.isEmpty() ? null : options.get(selectedIndex),
                cur -> {
                    int idx = options.indexOf(cur);
                    if (idx >= 0)
                        selectedIndex = idx;
                    selectBox.setText(String.valueOf(cur));
                },
                onChange -> {
                    selectBox.changed(() -> {
                        if (!options.isEmpty()) {
                            selectedIndex = (selectedIndex + 1) % options.size();
                            onChange.run();
                        }
                    });
                    return DisposableAction.empty();
                });
        OwnershipContext.register(this);
    }

    public static <T> SolimSelect<T> of(Signal<T> signal, List<T> options) {
        return new SolimSelect<>(signal, options);
    }

    public TextButton selectBox() {
        return selectBox;
    }

    @Override
    public TextButton element() {
        return selectBox;
    }

    public SolimSelect<T> name(String name) {
        selectBox.name = name;
        return this;
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        if (binding != null) {
            binding.dispose();
            binding = null;
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public SolimSelect<T> self() {
        return this;
    }
}
