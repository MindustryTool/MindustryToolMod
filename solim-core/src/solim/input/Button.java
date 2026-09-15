package solim.input;

import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.input.KeyCode;
import solim.graphics.RoundedDrawable;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.Label;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import solim.core.Component;
import solim.core.Disposable;
import solim.layout.Direction;
import solim.layout.GapContainer;
import solim.layout.Row;
import solim.modifier.ElementConfig;
import solim.modifier.TableConfig;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.style.SolimButtonStyle;
import solim.style.SolimButtonStyleBuilder;
import arc.scene.style.Drawable;
import solim.layout.CellConfig;
import solim.modifier.PendingCellConfig;
import arc.util.Time;

/**
 * Pure Button container widget supporting explicit children composition, custom
 * width/height sizing, and reactive state.
 */
public final class Button
        implements Component, GapContainer, ElementConfig<Button>, TableConfig<Button>, CellConfig<Button> {

    private final arc.scene.ui.Button button;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final List<Disposable> bindings = new ArrayList<>();
    private float gap = 0f;
    private boolean stopClickPropagation = true;
    private @Nullable Runnable onClick;
    private @Nullable Runnable onLongClick;
    private long longClickDuration = 300L;
    private boolean longPressed = false;
    private long pressTime = -1L;
    private boolean hasClickListener = false;
    private boolean hasLongClickListener = false;

    public Button() {
        this(new arc.scene.ui.Button(new ButtonStyle()));
    }

    public Button(@Nullable ButtonStyle style) {
        this(new arc.scene.ui.Button(style != null ? style : new ButtonStyle()));
    }

    public Button(@Nullable Runnable onClick) {
        this();
        onClick(onClick);
    }

    public Button(@Nullable ButtonStyle style, @Nullable Runnable onClick) {
        this(style);
        onClick(onClick);
    }

    public Button(arc.scene.ui.Button button) {
        this.button = button;
        this.button.userObject = this;
        this.button.name = "solim-button-sizedButton";
        this.button.center();
    }

    public Button children(@Nullable Runnable r) {
        ParentStack.push(button, Row.ATTACHER);
        try {
            if (r != null) {
                r.run();
            }
        } finally {
            ParentStack.pop();
        }
        respace();
        return this;
    }

    public Button onClick(@Nullable Runnable action) {
        this.onClick = action;
        if (action != null) {
            ensureClickListener();
        }
        return this;
    }

    public Button onLongClick(@Nullable Runnable action) {
        return onLongClick(300L, action);
    }

    public Button onLongClick(long durationMs, @Nullable Runnable action) {
        this.onLongClick = action;
        this.longClickDuration = durationMs;
        if (action != null) {
            ensureClickListener();
            ensureLongClickListener();
        }
        return this;
    }

    private void ensureClickListener() {
        if (hasClickListener)
            return;
        hasClickListener = true;
        button.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode keyCode) {
                if (Button.this.button.getScene() == null)
                    return false;
                return super.touchDown(event, x, y, pointer, keyCode);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (stopClickPropagation && event != null) {
                    event.stop();
                }
                if (longPressed) {
                    longPressed = false;
                    return;
                }
                if (Button.this.onClick != null) {
                    try {
                        Button.this.onClick.run();
                    } catch (Exception e) {
                        Log.err("Error executing button onClick", e);
                    }
                }
            }
        });
    }

    private void ensureLongClickListener() {
        if (hasLongClickListener)
            return;
        hasLongClickListener = true;
        button.update(() -> {
            if (button.isPressed()) {
                if (pressTime == -1L) {
                    pressTime = Time.millis();
                    longPressed = false;
                } else if (!longPressed && Time.timeSinceMillis(pressTime) >= longClickDuration) {
                    longPressed = true;
                    if (Button.this.onLongClick != null) {
                        try {
                            Button.this.onLongClick.run();
                        } catch (Exception e) {
                            Log.err("Error executing button onLongClick", e);
                        }
                    }
                }
            } else {
                pressTime = -1L;
            }
        });
    }

    public Button stopClickPropagation(boolean stop) {
        this.stopClickPropagation = stop;
        return this;
    }

    public Button tooltip(@Nullable String tip) {
        if (tip != null && !tip.isEmpty()) {
            try {
                button.addListener(new Tooltip(t -> t.add(tip)));
            } catch (Throwable ignored) {
            }
        }
        return this;
    }

    public Button tooltip(@Nullable Readable<String> tip) {
        if (tip != null) {
            try {
                button.addListener(new Tooltip(t -> {
                    Label label = new Label("");
                    Effect e = Effect.of(() -> label.setText(tip.get() != null ? tip.get() : ""));
                    bindings.add(e);
                    ComponentContext.register(e);
                    t.add(label);
                }));
            } catch (Throwable ignored) {
            }
        }
        return this;
    }

    public Button enabled(@Nullable Readable<Boolean> signal) {
        if (signal != null) {
            Effect e = Effect.of(() -> button.setDisabled(!Boolean.TRUE.equals(signal.get())));
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public Button checked(@Nullable Readable<Boolean> signal) {
        if (signal != null) {
            Effect e = Effect.of(() -> button.setChecked(Boolean.TRUE.equals(signal.get())));
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public Button visible(@Nullable Readable<Boolean> signal) {
        if (signal != null) {
            Effect e = Effect.of(() -> button.visible = Boolean.TRUE.equals(signal.get()));
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public Button style(@Nullable ButtonStyle style) {
        if (style != null) {
            button.setStyle(style);
        }
        return this;
    }

    public Button style(@Nullable Readable<? extends ButtonStyle> style) {
        if (style != null) {
            Effect e = Effect.of(() -> {
                ButtonStyle s = style.get();
                if (s != null) {
                    button.setStyle(s);
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public Button style(@Nullable Consumer<SolimButtonStyleBuilder> config) {
        if (config == null) {
            return this;
        }
        SolimButtonStyleBuilder builder = new SolimButtonStyleBuilder();
        config.accept(builder);
        if (builder.isStatic()) {
            applyResolvedStyle(builder.build());
        } else {
            applyResolvedStyle(builder.build());
            Effect e = Effect.of(() -> applyResolvedStyle(builder.build()));
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public Button style(@Nullable SolimButtonStyle resolved) {
        if (resolved != null) {
            applyResolvedStyle(resolved);
        }
        return this;
    }

    private void applyResolvedStyle(@Nullable SolimButtonStyle resolved) {
        if (resolved == null) {
            return;
        }
        if (resolved.style() != null) {
            button.setStyle(resolved.style());
        }
        if (resolved.padding() != null) {
            button.margin(resolved.padding().floatValue());
        }
        if (resolved.margin() != null) {
            applyStyleMargin(resolved.margin().floatValue());
        }
        if (resolved.gap() != null) {
            gap(resolved.gap().floatValue());
        }
        if (resolved.font() != null) {
            applyStyleFont(resolved.font());
        }
    }

    private void applyStyleMargin(float margin) {
        if (button.parent instanceof Table) {
            Cell<?> cell = ((Table) button.parent).getCell(button);
            if (cell != null) {
                cell.pad(margin);
                ((Table) button.parent).invalidateHierarchy();
                return;
            }
        }
        button.margin(margin);
    }

    private void applyStyleFont(Font font) {
        try {
            for (Element child : button.getChildren()) {
                if (child instanceof Label) {
                    Label label = (Label) child;
                    try {
                        LabelStyle old = label.getStyle();
                        LabelStyle next = new LabelStyle(old);
                        next.font = font;
                        label.setStyle(next);
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public Button gap(float g) {
        this.gap = g;
        respace();
        return this;
    }

    public Button gap(@Nullable Readable<Float> gapSignal) {
        if (gapSignal != null) {
            Effect e = Effect.of(() -> {
                Float g = gapSignal.get();
                if (g != null) {
                    gap(g);
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    @Override
    public Direction direction() {
        return Direction.HORIZONTAL;
    }

    @Override
    public float gap() {
        return gap;
    }

    @Override
    public void respace() {
        GapContainer.applySpacing(button, Direction.HORIZONTAL, gap);
    }

    public arc.scene.ui.Button button() {
        return button;
    }

    public arc.scene.ui.Button sizedButton() {
        return button;
    }

    public Button color(Color color) {
        button.setColor(color);
        return this;
    }

    public Button color(Readable<Color> color) {
        if (color != null) {
            Effect e = Effect.of(() -> {
                Color c = color.get();
                if (c != null) {
                    button.setColor(c);
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public Button rounded(int radius) {
        return rounded(radius, (Color) null);
    }

    public Button rounded(float radius) {
        return rounded((int) radius, (Color) null);
    }

    public Button rounded(int radius, @Nullable Color color) {
        TableConfig.super.rounded(radius, color);
        RoundedDrawable rd = getOrCreateRounded(radius);
        if (rd != null) {
            ButtonStyle s = button.getStyle();
            if (s == null) {
                s = new ButtonStyle();
                button.setStyle(s);
            }
            s.up = rd;
            if (color != null) {
                Color overColor = color.cpy().mul(1.15f);
                Color downColor = color.cpy().mul(0.85f);
                s.over = RoundedDrawable.of(radius, overColor, rd.getStroke(), rd.getBorderColor());
                s.down = RoundedDrawable.of(radius, downColor, rd.getStroke(), rd.getBorderColor());
            }
        }
        return this;
    }

    public Button rounded(int radius, @Nullable Readable<Color> color) {
        TableConfig.super.rounded(radius, color);
        RoundedDrawable rd = getOrCreateRounded(radius);
        if (rd != null) {
            ButtonStyle s = button.getStyle();
            if (s == null) {
                s = new ButtonStyle();
                button.setStyle(s);
            }
            s.up = rd;
        }
        return this;
    }

    public Button background(@Nullable Drawable drawable) {
        button.setBackground(drawable);
        ButtonStyle s = button.getStyle();
        if (s == null) {
            s = new ButtonStyle();
            button.setStyle(s);
        }
        s.up = drawable;
        return this;
    }

    public Button background(@Nullable Color color) {
        if (color == null || color.a == 0f) {
            return background((Drawable) null);
        }
        return rounded(0, color);
    }

    public Button border(float stroke, @Nullable Color color) {
        TableConfig.super.border(stroke, color);
        RoundedDrawable rd = getOrCreateRounded(8);
        if (rd != null) {
            ButtonStyle s = button.getStyle();
            if (s == null) {
                s = new ButtonStyle();
                button.setStyle(s);
            }
            s.up = rd;
            if (s.over instanceof RoundedDrawable) {
                ((RoundedDrawable) s.over).border(stroke, color != null ? color : Color.white);
            }
            if (s.down instanceof RoundedDrawable) {
                ((RoundedDrawable) s.down).border(stroke, color != null ? color : Color.white);
            }
            if (s.checked instanceof RoundedDrawable) {
                ((RoundedDrawable) s.checked).border(stroke, color != null ? color : Color.white);
            }
        }
        return this;
    }

    public Button border(float stroke, @Nullable Readable<Color> color) {
        TableConfig.super.border(stroke, color);
        RoundedDrawable rd = getOrCreateRounded(8);
        if (rd != null) {
            ButtonStyle s = button.getStyle();
            if (s == null) {
                s = new ButtonStyle();
                button.setStyle(s);
            }
            s.up = rd;
            if (s.over instanceof RoundedDrawable) {
                ((RoundedDrawable) s.over).border(stroke, color);
            }
            if (s.down instanceof RoundedDrawable) {
                ((RoundedDrawable) s.down).border(stroke, color);
            }
            if (s.checked instanceof RoundedDrawable) {
                ((RoundedDrawable) s.checked).border(stroke, color);
            }
        }
        return this;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public arc.scene.ui.Button element() {
        return button;
    }

    @Override
    public Table table() {
        return button;
    }

    @Override
    public void dispose() {
        for (Disposable d : bindings) {
            d.dispose();
        }
        bindings.clear();
    }

    @Override
    public Button self() {
        return this;
    }
}
