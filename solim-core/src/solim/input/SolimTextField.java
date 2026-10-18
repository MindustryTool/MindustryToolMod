package solim.input;

import solim.reactive.TwoWayBinding;

import arc.Core;
import arc.input.KeyCode;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.func.Cons;
import arc.func.Boolf;
import solim.core.Component;
import solim.core.DisposableAction;
import solim.core.OwnershipContext;
import solim.core.SolimToken;
import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.Configurable;
import solim.modifier.FieldCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.modifier.TextCapability;
import solim.modifier.WrappableCapability;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.style.InputStyle;

public final class SolimTextField extends Component
        implements Configurable<SolimTextField>, CellCapability, AlignmentCapability, FieldCapability, TextCapability,
        WrappableCapability {

    private final TextField field;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<SolimTextField> pending;
    private Effect disabledEffect;
    private Boolf<String> validator;
    private final Signal<Boolean> valid = Signal.of(true);

    public SolimTextField() {
        this("");
    }

    public SolimTextField(String text) {
        this(Signal.of(text != null ? text : ""));
    }

    public SolimTextField(Signal<String> signal) {
        this(signal, (TextField.TextFieldStyle) null);
    }

    public SolimTextField(Signal<String> signal, TextField.TextFieldStyle style) {
        this(style != null ? new TextField("", style) : new TextField(""), signal);
    }

    private SolimTextField(TextField field, Signal<String> signal) {
        super(field);
        this.field = field;
        SolimToken.bind(this.field, this, constraints);
        this.field.name = "solim-textfield-textField";
        field.setText(signal.peek() != null ? signal.peek() : "");

        OwnershipContext.push(this);
        try {
            new TwoWayBinding<>(
                    signal,
                    field::getText,
                    val -> {
                        field.setText(val != null ? val : "");

                        if (validator != null) {
                            valid.set(validator.get(val));
                        }
                    },
                    onChange -> {
                        field.changed(() -> {
                            onChange.run();
                            if (validator != null) {
                                valid.set(validator.get(field.getText()));
                            }
                        });

                        return DisposableAction.empty();
                    });
        } finally {
            OwnershipContext.pop();
        }
    }

    public static SolimTextField of(Signal<String> signal) {
        return new SolimTextField(signal);
    }

    public SolimTextField validator(Boolf<String> validator) {
        this.validator = validator;
        this.valid.set(validator == null || validator.get(field.getText()));
        return this;
    }

    public Readable<Boolean> valid() {
        return valid;
    }

    public boolean isValid() {
        return Boolean.TRUE.equals(valid.get());
    }

    public SolimTextField onEnter(Cons<String> onSubmit) {
        field.keyDown(key -> {
            if (key == KeyCode.enter && !field.isDisabled()) {
                onSubmit.get(field.getText());
            }
        });
        return this;
    }

    public SolimTextField onEnter(Runnable onSubmit) {
        return onEnter(text -> onSubmit.run());
    }

    public void setDisabled(boolean disabled) {
        field.setDisabled(disabled);
    }

    public void setDisabled(Readable<Boolean> disabled) {
        if (disabledEffect != null) {
            disabledEffect.dispose();
        }
        withinOwnership(() -> disabledEffect = Effect.of(disabled, d -> field.setDisabled(Boolean.TRUE.equals(d))));
    }

    public void setPlaceholder(String placeholder) {
        field.setMessageText(placeholder);
    }

    public void setStyle(@Nullable InputStyle style) {
        if (style == null) {
            return;
        }
        field.setStyle(style.appliedTo(field.getStyle()));
    }

    public void setStyle(@Nullable TextField.TextFieldStyle style) {
        if (style != null) {
            field.setStyle(style);
        }
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public @Nullable Table table() {
        return null;
    }

    @Override
    public @Nullable Props<SolimTextField> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<SolimTextField> props) {
        this.pending = props;
    }

    public TextField field() {
        return field;
    }

    public SolimTextField focus() {
        Core.app.post(() -> {
            if (!isDisposed() && !field.isDisabled()) {
                field.requestKeyboard();
            }
        });

        return this;
    }

    @Override
    public SolimTextField name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public SolimTextField self() {
        return this;
    }
}
