package solim.input;

import arc.graphics.g2d.Font;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.Label;
import arc.scene.ui.Label.LabelStyle;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Scaling;
import arc.func.Cons;
import solim.core.Component;
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
import solim.modifier.Values;
import arc.util.Time;

import solim.modifier.ButtonCapability;
import solim.modifier.CheckableCapability;
import solim.modifier.IconCapability;
import solim.modifier.TextCapability;
import solim.modifier.WrappableCapability;

public final class Button
        extends Component
        implements GapContainer, Configurable<Button>, CellCapability, ContainerCapability<Button>, AlignmentCapability,
        ButtonCapability, CheckableCapability, IconCapability, TextCapability, WrappableCapability {

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
    private @Nullable Props<Button> pending;

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
                SolimImage img = new SolimImage(Drawables.scalable(drawable), Scaling.fit);
                img.props(Values.size(Units.unit(6)), Values.prefWidth(Units.unit(6)),
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
                img.props(Values.src(drawable.map(Drawables::scalable)), Values.scaling(Scaling.fit),
                        Values.size(Units.unit(6)), Values.prefWidth(Units.unit(6)),
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

    public void setDisabled(boolean disabled) {
        button.setDisabled(disabled);
    }

    public void setStyle(@Nullable ButtonStyle style) {
        if (style != null) {
            button.setStyle(style);
        }
    }

    public void setStyle(@Nullable Cons<SolimButtonStyleBuilder> config) {
        if (config == null) {
            return;
        }
        SolimButtonStyleBuilder builder = new SolimButtonStyleBuilder();
        config.get(builder);
        if (builder.isStatic()) {
            applyResolvedStyle(builder.build());
        } else {
            applyResolvedStyle(builder.build());
            Effect.of(() -> applyResolvedStyle(builder.build()));
        }
    }

    public void setStyle(@Nullable SolimButtonStyle resolved) {
        if (resolved != null) {
            applyResolvedStyle(resolved);
        }
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

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public Table table() {
        return button;
    }

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
