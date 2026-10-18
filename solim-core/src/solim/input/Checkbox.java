package solim.input;

import solim.reactive.TwoWayBinding;

import arc.Core;
import arc.scene.ui.CheckBox;
import arc.scene.ui.CheckBox.CheckBoxStyle;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.func.Cons;
import solim.core.Component;
import solim.core.DisposableAction;
import solim.core.OwnershipContext;
import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.CheckableCapability;
import solim.modifier.Configurable;
import solim.modifier.ElementCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.modifier.WrappableCapability;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;

public final class Checkbox extends Component
        implements Configurable<Checkbox>, ElementCapability, CellCapability, AlignmentCapability, CheckableCapability, WrappableCapability {

    private final CheckBox checkBox;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<Checkbox> pending;

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public Table table() {
        return null;
    }

    @Override
    public @Nullable Props<Checkbox> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Checkbox> props) {
        this.pending = props;
    }

    public Checkbox(String label, Signal<Boolean> signal) {
        this(label, signal, Core.scene == null ? new CheckBoxStyle() : null);
    }

    public Checkbox(String label, Signal<Boolean> signal, @Nullable CheckBoxStyle style) {
        this(create(label, style), signal);
    }

    private Checkbox(CheckBox checkBox, Signal<Boolean> signal) {
        super(checkBox);
        this.checkBox = checkBox;
        checkBox.top().left();
        checkBox.name = "solim-checkbox-checkBox";
        checkBox.setChecked(signal.peek());
        OwnershipContext.push(this);
        try {
            new TwoWayBinding<>(
                    signal,
                    checkBox::isChecked,
                    checkBox::setChecked,
                    onChange -> {
                        checkBox.changed(onChange::run);

                        return DisposableAction.empty();
                    });
        } finally {
            OwnershipContext.pop();
        }
    }

    private static CheckBox create(String label, @Nullable CheckBoxStyle style) {
        return style != null
                ? new CheckBox(label != null ? label : "", style)
                : new CheckBox(label != null ? label : "");
    }

    public Checkbox(String label, boolean initial, Cons<Boolean> onChanged) {
        this(label, initial, Core.scene == null ? new CheckBoxStyle() : null, onChanged);
    }

    public Checkbox(String label, boolean initial, @Nullable CheckBoxStyle style, Cons<Boolean> onChanged) {
        this(create(label, style), initial, onChanged);
    }

    private Checkbox(CheckBox checkBox, boolean initial, @Nullable Cons<Boolean> onChanged) {
        super(checkBox);
        this.checkBox = checkBox;
        checkBox.top().left();
        checkBox.name = "solim-checkbox-checkBox";
        checkBox.setChecked(initial);
        if (onChanged != null) {
            checkBox.changed(() -> onChanged.get(checkBox.isChecked()));
        }
    }

    public static Checkbox of(String label, Signal<Boolean> signal) {
        return new Checkbox(label, signal);
    }

    public static Checkbox of(String label, boolean initial, Cons<Boolean> onChanged) {
        return new Checkbox(label, initial, onChanged);
    }

    public CheckBox checkBox() {
        return checkBox;
    }

    public Checkbox name(String name) {
        setName(name);
        return this;
    }

    public void setWrap(boolean wrap) {
        if (checkBox.getLabel() != null) {
            checkBox.getLabel().setWrap(wrap);
        }
        if (checkBox.getLabelCell() != null && wrap) {
            checkBox.getLabelCell().growX().minWidth(0f);
        }
    }

    public void setChecked(@Nullable Readable<Boolean> source) {
        if (source != null) {
            OwnershipContext.push(this);
            try {
                Effect.of(() -> checkBox.setChecked(Boolean.TRUE.equals(source.get())));
            } finally {
                OwnershipContext.pop();
            }
        }
    }

    public boolean isChecked() {
        return checkBox.isChecked();
    }

    @Override
    public Checkbox self() {
        return this;
    }
}
