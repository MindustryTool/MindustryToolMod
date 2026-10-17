package solim.input;

import arc.scene.Element;
import arc.scene.event.ChangeListener;
import arc.scene.ui.TextButton;
import arc.util.Nullable;
import java.util.List;
import solim.core.Component;
import solim.core.DisposableAction;
import solim.core.OwnershipContext;
import solim.reactive.Signal;
import solim.reactive.TwoWayBinding;
import solim.modifier.Configurable;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import arc.scene.ui.layout.Table;

public final class SolimSelect<T> extends Component
        implements Configurable<SolimSelect<T>> {
    private final TextButton selectBox;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<SolimSelect<T>> pending;
    private int selectedIndex = 0;

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public @Nullable Table table() {
        return null;
    }

    @Override
    public SolimSelect<T> children(@Nullable Runnable block) {
        applyPendingProps();
        OwnershipContext.push(this);
        try {
            if (block != null) {
                block.run();
            }
        } finally {
            OwnershipContext.pop();
        }
        return this;
    }

    @Override
    public @Nullable Props<SolimSelect<T>> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<SolimSelect<T>> props) {
        this.pending = props;
    }

    public SolimSelect(Signal<T> signal, List<T> options) {
        this(new TextButton(""), signal, options);
    }

    private SolimSelect(TextButton selectBox, Signal<T> signal, List<T> options) {
        super(selectBox);
        this.selectBox = selectBox;
        selectBox.name = "solim-select-selectBox";
        if (signal.peek() != null) {
            int idx = options.indexOf(signal.peek());
            if (idx >= 0)
                selectedIndex = idx;
        }
        selectBox.setText(String.valueOf(signal.peek()));
        OwnershipContext.push(this);
        try {
            new TwoWayBinding<>(
                    signal,
                    () -> options.isEmpty() ? null : options.get(selectedIndex),
                    cur -> {
                        int idx = options.indexOf(cur);
                        if (idx >= 0)
                            selectedIndex = idx;
                        selectBox.setText(String.valueOf(cur));
                    },
                    onChange -> {

                        ChangeListener listener = new ChangeListener() {
                            @Override
                            public void changed(ChangeEvent event, @Nullable Element actor) {
                                if (!options.isEmpty()) {
                                    selectedIndex = (selectedIndex + 1) % options.size();
                                    onChange.run();
                                }
                            }
                        };
                        selectBox.addListener(listener);
                        return DisposableAction.of(() -> selectBox.removeListener(listener));
                    });
        } finally {
            OwnershipContext.pop();
        }
    }

    public static <T> SolimSelect<T> of(Signal<T> signal, List<T> options) {
        return new SolimSelect<>(signal, options);
    }

    public TextButton selectBox() {
        return selectBox;
    }

    public SolimSelect<T> name(String name) {
        setName(name);
        return this;
    }

    @Override
    public SolimSelect<T> self() {
        return this;
    }
}
