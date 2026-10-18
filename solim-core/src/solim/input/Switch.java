package solim.input;

import solim.reactive.TwoWayBinding;

import arc.scene.ui.TextButton;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.DisposableAction;
import solim.core.OwnershipContext;
import solim.reactive.Signal;
import solim.modifier.CheckableCapability;
import solim.modifier.Configurable;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;

public final class Switch extends Component
        implements Configurable<Switch>, CheckableCapability {
    private final TextButton button;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<Switch> pending;
    private boolean state;

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public @Nullable Props<Switch> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Switch> props) {
        this.pending = props;
    }

    public Switch(Signal<Boolean> signal) {
        this(new TextButton(""), signal);
    }

    private Switch(TextButton button, Signal<Boolean> signal) {
        super(button);
        this.button = button;
        button.name = "solim-switch-switchBox";
        state = Boolean.TRUE.equals(signal.peek());
        button.setText(state ? "ON" : "OFF");
        OwnershipContext.push(this);
        try {
            new TwoWayBinding<>(
                    signal,
                    () -> state,
                    val -> {
                        state = Boolean.TRUE.equals(val);
                        button.setText(state ? "ON" : "OFF");
                    },
                    onChange -> {
                        button.changed(() -> {
                            state = !state;
                            onChange.run();
                        });
                        return DisposableAction.empty();
                    });
        } finally {
            OwnershipContext.pop();
        }
    }

    public static Switch of(Signal<Boolean> signal) {
        return new Switch(signal);
    }

    public TextButton button() {
        return button;
    }

    public Switch name(String name) {
        setName(name);
        return this;
    }

    public void setChecked(boolean checked) {
        state = checked;
        button.setText(checked ? "ON" : "OFF");
        button.setChecked(checked);
    }

    public boolean isChecked() {
        return state;
    }

    @Override
    public Switch self() {
        return this;
    }
}
