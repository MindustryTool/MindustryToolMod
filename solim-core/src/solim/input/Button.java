package solim.input;

import arc.func.Boolp;
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
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Scaling;
import arc.func.Cons;
import solim.core.Component;
import solim.core.ReactiveSource;
import solim.core.SolimToken;
import solim.core.Units;
import solim.display.SolimImage;
import solim.display.Text;
import solim.graphics.Drawables;
import solim.layout.Direction;
import solim.layout.GapContainer;
import solim.layout.Row;
import solim.core.OwnershipContext;
import solim.runtime.AttachmentStack;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.style.SolimButtonStyle;
import solim.style.SolimButtonStyleBuilder;
import arc.scene.style.Drawable;
import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.Configurable;
import solim.modifier.ContainerCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.modifier.RoundedHelper;
import solim.modifier.TableCapability;
import solim.modifier.Values;
import arc.util.Time;

import solim.modifier.ButtonCapability;
import solim.modifier.CheckableCapability;
import solim.modifier.IconCapability;
import solim.modifier.TextCapability;
import solim.modifier.WrappableCapability;

public final class Button
        extends Component implements GapContainer, Configurable<Button>, TableCapability, CellCapability, ContainerCapability<Button>, AlignmentCapability, ButtonCapability, CheckableCapability, IconCapability, TextCapability, WrappableCapability {

    private final arc.scene.ui.Button button;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private float gap = 0f;
    private boolean stopClickPropagation = true;
    private @Nullable Runnable onClick;
    private @Nullable Runnable onLongClick;
    private long longClickDuration = 300L;
    private boolean longPressed = false;
    private long pressTime = -1L;
    private boolean hasClickListener = false;
    private boolean hasLongClickListener = false;
    private boolean hasCheckedReassertListener = false;
    private @Nullable Readable<Boolean> checkedBinding;
    private @Nullable Props<Button> pending;
    private static boolean checkedWarningLogged = false;

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
        super(button);
        this.button = button;
        SolimToken.bind(this.button, this, constraints);
        this.button.name = "solim-button-sizedButton";
        this.button.center();
    }

    public Button children(@Nullable Runnable r) {
        applyPendingProps();
        OwnershipContext.push(this);
        AttachmentStack.push(button, Row.ATTACHER);
        try {
            if (r != null) {
                r.run();
            }
        } finally {
            AttachmentStack.pop();
            OwnershipContext.pop();
        }
        respace();
        return this;
    }

    public Button text(@Nullable String s) {
        children(() -> {
            if (s != null) {
                Text text = Text.of(s);
                AttachmentStack.attachToParent(text.label());
            }
        });
        return this;
    }

    public Button text(@Nullable Readable<String> s) {
        children(() -> {
            if (s != null) {
                Text text = Text.of(s);
                AttachmentStack.attachToParent(text.label());
            }
        });
        return this;
    }

    public Button icon(@Nullable Drawable drawable) {
        children(() -> {
            if (drawable != null) {
                SolimImage img = new SolimImage(Drawables.scalable(drawable));
                img.scaling(Scaling.fit)
                        .props(Values.size(Units.unit(6)), Values.prefWidth(Units.unit(6)),
                                Values.prefHeight(Units.unit(6)));
                AttachmentStack.attachToParent(img.element());
            }
        });
        return this;
    }

    public Button icon(@Nullable Readable<Drawable> drawable) {
        children(() -> {
            if (drawable != null) {
                SolimImage img = new SolimImage();
                img.drawable(drawable.map(Drawables::scalable));
                img.scaling(Scaling.fit)
                        .props(Values.size(Units.unit(6)), Values.prefWidth(Units.unit(6)),
                                Values.prefHeight(Units.unit(6)));
                AttachmentStack.attachToParent(img.element());
            }
        });
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
                    Button.this.onClick.run();
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
                        Button.this.onLongClick.run();
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

    // TODO: use prop
    public Button enabled(@Nullable Readable<Boolean> signal) {
        if (signal != null) {
            withinOwnership(() -> Effect.of(() -> button.setDisabled(!Boolean.TRUE.equals(signal.get()))));
        }
        return this;
    }

    public Button checked(@Nullable Readable<Boolean> signal) {
        if (signal != null) {
            withinOwnership(() -> Effect.of(() -> button.setChecked(Boolean.TRUE.equals(signal.get()))));

            if (signal instanceof ReactiveSource) {
                checkedBinding = signal;
                ensureCheckedReassertListener();
            }

            ButtonStyle current = button.getStyle();
            if (current != null && current.checked == null) {
                warnCheckedWithoutVisual();
            }
        }
        return this;
    }

    private void ensureCheckedReassertListener() {
        if (hasCheckedReassertListener) {
            return;
        }
        hasCheckedReassertListener = true;
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Readable<Boolean> binding = checkedBinding;
                if (binding != null) {
                    Button.this.button.setChecked(Boolean.TRUE.equals(binding.peek()));
                }
            }
        });
    }

    private static void warnCheckedWithoutVisual() {
        if (checkedWarningLogged) {
            return;
        }
        checkedWarningLogged = true;
        Log.warn(
                "[Solim] .checked(...) bound to a style with no checked visual — the selected state will not render. Use a style that declares a checked state; action-chip styles deliberately have none.");
    }

    public Button visible(@Nullable Readable<Boolean> signal) {
        if (signal != null) {
            withinOwnership(() -> Effect.of(() -> button.visible = Boolean.TRUE.equals(signal.get())));
        }
        return this;
    }

    // TODO put it into .props()
    public Button style(@Nullable ButtonStyle style) {
        if (style != null) {
            button.setStyle(style);
        }
        return this;
    }

    public Button style(@Nullable Readable<? extends ButtonStyle> style) {
        if (style != null) {
            withinOwnership(() -> Effect.of(() -> {
                ButtonStyle s = style.get();
                if (s != null) {
                    button.setStyle(s);
                }
            }));
        }
        return this;
    };

    public Button style(@Nullable Cons<SolimButtonStyleBuilder> config) {
        if (config == null) {
            return this;
        }
        SolimButtonStyleBuilder builder = new SolimButtonStyleBuilder();
        config.get(builder);
        if (builder.isStatic()) {
            applyResolvedStyle(builder.build());
        } else {
            applyResolvedStyle(builder.build());
            withinOwnership(() -> Effect.of(() -> applyResolvedStyle(builder.build())));
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
            setGap(resolved.gap().floatValue());
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

    @Override
    public void setGap(float g) {
        this.gap = g;
        respace();
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

    public Button color(Color color) {
        button.setColor(color);
        return this;
    }

    public Button color(Readable<Color> color) {
        if (color != null) {
            withinOwnership(() -> Effect.of(() -> {
                Color c = color.get();
                if (c != null) {
                    button.setColor(c);
                }
            }));
        }
        return this;
    };

    public Button rounded(int radius) {
        return rounded(radius, (Color) null);
    }

    public Button rounded(float radius) {
        return rounded((int) radius, (Color) null);
    }

    public Button rounded(int radius, @Nullable Color color) {
        RoundedDrawable rd = RoundedHelper.getOrCreateRounded(table(), radius);
        if (rd != null) {
            rd.radius(radius);
            if (color != null) {
                rd.fillColor(color);
            }
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
        RoundedDrawable rd = RoundedHelper.getOrCreateRounded(table(), radius);
        if (rd != null) {
            rd.radius(radius);
            if (color != null) {
                OwnershipContext.push(this);
                try {
                    Effect.of(() -> {
                        Color c = color.get();
                        if (c != null) {
                            rd.fillColor(c);
                        }
                    });
                } finally {
                    OwnershipContext.pop();
                }
            }
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

    public Button disabled(Boolp prov) {
        button.setDisabled(prov);
        return this;
    }

    public Button border(float stroke, @Nullable Color color) {
        RoundedDrawable rd = RoundedHelper.getOrCreateRounded(table(), 8);
        if (rd != null) {
            rd.border(stroke, color != null ? color : Color.white);
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
        RoundedDrawable rd = RoundedHelper.getOrCreateRounded(table(), 8);
        if (rd != null) {
            if (color != null) {
                OwnershipContext.push(this);
                try {
                    Effect.of(() -> {
                        Color c = color.get();
                        rd.border(stroke, c != null ? c : Color.white);
                    });
                } finally {
                    OwnershipContext.pop();
                }
            } else {
                rd.border(stroke, Color.white);
            }
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
    public Table table() {
        return button;
    }

    public Button align(int align) {
        table().align(align);
        constraints.align = align;
        constraints.applyAlignToParentCell(element());
        button.defaults().align(align);
        for (Cell<?> c : button.getCells()) {
            if (c != null) {
                c.align(align);
            }
        }
        return this;
    }

    public Button center() {
        table().center();
        constraints.alignCenter();
        constraints.applyAlignToParentCell(element());
        button.defaults().center();
        for (Cell<?> c : button.getCells()) {
            if (c != null) {
                c.center();
            }
        }
        return this;
    }

    public Button top() {
        table().top();
        constraints.alignTop();
        constraints.applyAlignToParentCell(element());
        button.defaults().top();
        for (Cell<?> c : button.getCells()) {
            if (c != null) {
                c.top();
            }
        }
        return this;
    }

    public Button bottom() {
        table().bottom();
        constraints.alignBottom();
        constraints.applyAlignToParentCell(element());
        button.defaults().bottom();
        for (Cell<?> c : button.getCells()) {
            if (c != null) {
                c.bottom();
            }
        }
        return this;
    }

    public Button left() {
        table().left();
        constraints.alignLeft();
        constraints.applyAlignToParentCell(element());
        button.defaults().left();
        for (Cell<?> c : button.getCells()) {
            if (c != null) {
                c.left();
            }
        }
        return this;
    }

    public Button right() {
        table().right();
        constraints.alignRight();
        constraints.applyAlignToParentCell(element());
        button.defaults().right();
        for (Cell<?> c : button.getCells()) {
            if (c != null) {
                c.right();
            }
        }
        return this;
    }

    @Override
    public Button name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public @Nullable Props<Button> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Button> props) {
        this.pending = props;
    }

    public Button self() {
        return this;
    }
}
