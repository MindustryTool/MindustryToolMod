package solim.input;

import solim.reactive.TwoWayBinding;

import arc.scene.ui.TextButton;
import solim.core.Component;
import solim.core.DisposableAction;
import solim.reactive.Signal;
import solim.modifier.ElementConfig;
import solim.modifier.CellConfig;
import solim.modifier.PendingCellConfig;

public final class Switch extends Component implements ElementConfig<Switch>, CellConfig<Switch> {
    private final TextButton button;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private boolean state;

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
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
        own(new TwoWayBinding<>(
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
                }));
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

    @Override
    public Switch self() {
        return this;
    }
}
