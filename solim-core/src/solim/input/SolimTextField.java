package solim.input;

import solim.reactive.TwoWayBinding;

import arc.Core;
import arc.graphics.Color;
import arc.input.KeyCode;
import solim.graphics.RoundedDrawable;
import arc.scene.ui.TextField;
import arc.util.Nullable;
import arc.func.Cons;
import arc.func.Boolf;
import solim.core.Component;
import solim.core.DisposableAction;
import solim.modifier.ElementConfig;
import solim.modifier.CellConfig;
import solim.modifier.PendingCellConfig;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.style.InputStyle;
import solim.reactive.Binding;

/**
 * TextField widget with two-way binding to a Signal&lt;String&gt;. Equality
 * guard prevents feedback loop. Automatically registers with the active
 * OwnershipContext if created during a component build.
 */
public final class SolimTextField extends Component implements ElementConfig<SolimTextField>, CellConfig<SolimTextField> {

    private final TextField field;
    private final PendingCellConfig constraints = new PendingCellConfig();
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
        this.field.name = "solim-textfield-textField";
        field.setText(signal.peek() != null ? signal.peek() : "");

        own(new TwoWayBinding<>(
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
                }));
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

    public SolimTextField disabled(boolean disabled) {
        field.setDisabled(disabled);
        return this;
    }

    public SolimTextField disabled(Readable<Boolean> disabled) {
        if (disabledEffect != null) {
            disabledEffect.dispose();
        }
        disabledEffect = own(Binding.bind(disabled, d -> field.setDisabled(Boolean.TRUE.equals(d))));
        return this;
    }

    public SolimTextField placeholder(String placeholder) {
        field.setMessageText(placeholder);
        return this;
    }

    public SolimTextField style(@Nullable InputStyle style) {
        if (style == null) {
            return this;
        }
        field.setStyle(style.appliedTo(field.getStyle()));
        return this;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    public TextField field() {
        return field;
    }


    public SolimTextField rounded(int radius) {
        return rounded(radius, (Color) null);
    }

    public SolimTextField rounded(int radius, @Nullable Color color) {
        TextField.TextFieldStyle s = field.getStyle();
        if (s != null) {
            if (s.background instanceof RoundedDrawable) {
                RoundedDrawable rd = (RoundedDrawable) s.background;
                rd.radius(radius);
                if (color != null)
                    rd.fillColor(color);
            } else {
                s.background = RoundedDrawable.of(radius, color != null ? color : Color.darkGray);
            }
        }
        return this;
    }

    public SolimTextField rounded(int radius, @Nullable Readable<Color> color) {
        TextField.TextFieldStyle s = field.getStyle();
        if (s != null) {
            if (s.background instanceof RoundedDrawable) {
                RoundedDrawable rd = (RoundedDrawable) s.background;
                rd.radius(radius);
                if (color != null)
                    rd.fillColor(color);
            } else {
                RoundedDrawable rd = new RoundedDrawable(radius);
                if (color != null)
                    rd.fillColor(color);
                s.background = rd;
            }
        }
        return this;
    }

    public SolimTextField border(float stroke, @Nullable Color color) {
        TextField.TextFieldStyle s = field.getStyle();
        if (s != null) {
            if (s.background instanceof RoundedDrawable) {
                ((RoundedDrawable) s.background).border(stroke, color != null ? color : Color.white);
            } else {
                s.background = RoundedDrawable.of(6, Color.clear, stroke, color != null ? color : Color.white);
            }
        }
        return this;
    }

    public SolimTextField border(float stroke, @Nullable Readable<Color> color) {
        TextField.TextFieldStyle s = field.getStyle();
        if (s != null) {
            if (s.background instanceof RoundedDrawable) {
                ((RoundedDrawable) s.background).border(stroke, color);
            } else {
                RoundedDrawable rd = new RoundedDrawable(6, Color.clear);
                rd.border(stroke, color);
                s.background = rd;
            }
        }
        return this;
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
