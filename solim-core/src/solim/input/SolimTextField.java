package solim.input;
import solim.reactive.TwoWayBinding;

import arc.Core;
import arc.graphics.Color;
import arc.input.KeyCode;
import solim.graphics.RoundedDrawable;
import arc.scene.Element;
import arc.scene.ui.TextField;
import arc.util.Nullable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import solim.core.Component;
import solim.modifier.ElementConfig;
import solim.modifier.CellConfig;
import solim.modifier.PendingCellConfig;
import solim.runtime.ComponentContext;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.style.InputStyle;
import solim.reactive.Binding;

/**
 * TextField widget with two-way binding to a Signal&lt;String&gt;. Equality
 * guard prevents feedback loop. Automatically registers with the active
 * ComponentContext if created during a component build.
 */
public final class SolimTextField implements Component, ElementConfig<SolimTextField>, CellConfig<SolimTextField> {

    private final TextField field;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable TwoWayBinding<String> binding;
    private Effect disabledEffect;
    private boolean disposed = false;
    private Predicate<String> validator;
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
        this.field = style != null ? new TextField("", style) : new TextField("");
        this.field.name = "solim-textfield-textField";
        field.setText(signal.peek() != null ? signal.peek() : "");
        this.binding = new TwoWayBinding<>(
                signal,
                field::getText,
                val -> {
                    field.setText(val != null ? val : "");
                    if (validator != null) {
                        valid.set(validator.test(val));
                    }
                },
                onChange -> {
                    field.changed(() -> {
                        onChange.run();
                        if (validator != null) {
                            valid.set(validator.test(field.getText()));
                        }
                    });
                    return () -> {
                    };
                });

        ComponentContext.register(this);
    }

    public static SolimTextField of(Signal<String> signal) {
        return new SolimTextField(signal);
    }

    public SolimTextField validator(Predicate<String> validator) {
        this.validator = validator;
        this.valid.set(validator == null || validator.test(field.getText()));
        return this;
    }

    public Readable<Boolean> valid() {
        return valid;
    }

    public boolean isValid() {
        return Boolean.TRUE.equals(valid.get());
    }

    public SolimTextField onEnter(Consumer<String> onSubmit) {
        field.keyDown(key -> {
            if (key == KeyCode.enter && !field.isDisabled()) {
                onSubmit.accept(field.getText());
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
        disabledEffect = Binding.bind(disabled, d -> field.setDisabled(Boolean.TRUE.equals(d)));
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

    @Override
    public Element element() {
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
            if (!field.isDisabled()) {
                Core.scene.setKeyboardFocus(field);
                field.requestKeyboard();
            }
        });

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
        if (disabledEffect != null) {
            disabledEffect.dispose();
            disabledEffect = null;
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public SolimTextField self() {
        return this;
    }
}
