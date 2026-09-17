package solim.overlay;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import mindustry.game.EventType.ResizeEvent;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.core.Disposable;
import solim.graphics.RoundedDrawable;
import solim.layout.Spacer;
import solim.modifier.PendingCellConfig;
import solim.modifier.RoundedHelper;
import solim.modifier.TableConfig;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;
import solim.runtime.ReactiveContext;
import solim.reactive.Effect;
import solim.reactive.Readable;

/**
 * Floating context menu: a scene-hosted, non-modal menu with reactive provider
 * content, anchor positioning, and built-in dismissal.
 *
 * <p>
 * Usage:
 * 
 * <pre>
 * Popup&lt;ChatMessage&gt; menu = popup()
 *         .children(request -> column()...rows...)
 *         .rounded(2);
 * menu.show(message, stageX, stageY);
 * menu.hide();
 * </pre>
 *
 * <p>
 * All mutating methods return this instance for chaining. Show and hide are
 * explicit and safe to call headless (no-ops without a scene).
 */
public final class Popup<T> extends BaseComponent implements TableConfig<Popup<T>> {

    private static final Color DEFAULT_FILL = new Color(0.09f, 0.09f, 0.12f, 0.96f);
    private static final int DEFAULT_RADIUS = 8;

    private final Table table = new Table();
    private @Nullable Function<T, Component> provider;
    private @Nullable Component currentContent;
    private final List<Disposable> currentBindings = new ArrayList<>();
    private boolean touchAttached = false;
    private boolean keyAttached = false;
    private long lastHideTime = 0L;

    private final Cons<ResizeEvent> resizeListener = e -> {
        if (table.parent != null) {
            hide();
        }
    };

    private final InputListener touchCatcher = new InputListener() {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
            Element hit = null;
            try {
                if (Core.scene != null && Core.scene.root != null) {
                    hit = Core.scene.root.hit(event.stageX, event.stageY, true);
                }
            } catch (Throwable ignored) {
            }
            for (Element e = hit; e != null; e = e.parent) {
                if (e == table) {
                    return false;
                }
            }
            hide();
            return true;
        }
    };

    private final InputListener keyCatcher = new InputListener() {
        @Override
        public boolean keyDown(InputEvent event, KeyCode keycode) {
            if (keycode == KeyCode.back || keycode == KeyCode.escape) {
                hide();
                return true;
            }
            return false;
        }
    };

    public Popup() {
        this.table.name = "solim-popup-menu";
        Events.on(ResizeEvent.class, resizeListener);
    }

    public Table table() {
        return table;
    }

    public Popup<T> rounded(int radius) {
        RoundedHelper.getOrCreateRounded(table, radius);
        return this;
    }

    public Popup<T> rounded(int radius, @Nullable Color color) {
        RoundedDrawable rd = RoundedHelper.getOrCreateRounded(table, radius);
        if (color != null) {
            rd.fillColor(color);
        }
        return this;
    }

    public Popup<T> rounded(int radius, @Nullable Readable<Color> color) {
        RoundedDrawable rd = RoundedHelper.getOrCreateRounded(table, radius);
        if (color != null) {
            Effect e = Effect.of(() -> {
                Color c = color.get();
                if (c != null)
                    rd.fillColor(c);
            });
            ComponentContext.register(e);
        }
        return this;
    }

    public Popup<T> border(float stroke, @Nullable Color color) {
        RoundedDrawable rd = RoundedHelper.getOrCreateRounded(table, 8);
        rd.border(stroke, color != null ? color : Color.white);
        return this;
    }

    public Popup<T> border(float stroke, @Nullable Readable<Color> color) {
        RoundedDrawable rd = RoundedHelper.getOrCreateRounded(table, 8);
        if (color != null) {
            Effect e = Effect.of(() -> {
                Color c = color.get();
                rd.border(stroke, c != null ? c : Color.white);
            });
            ComponentContext.register(e);
        } else {
            rd.border(stroke, Color.white);
        }
        return this;
    }

    /**
     * Sets the content provider applied to the data passed to {@link #show}.
     * Content is rebuilt on every show call.
     */
    public Popup<T> children(@Nullable Function<T, Component> provider) {
        this.provider = provider;
        return this;
    }

    /**
     * Shows the menu for the given data anchored near the given stage coordinates.
     * Re-showing while open rebuilds content and re-anchors. Null data hides
     * instead. No-op without a scene.
     */
    public Popup<T> show(@Nullable T data, float stageX, float stageY) {
        if (data == null || Core.scene == null || Core.scene.root == null) {
            if (data == null) {
                hide();
            }
            return this;
        }
        render(data, stageX, stageY);
        return this;
    }

    /**
     * Removes the menu from the scene and detaches its listeners. Idempotent and
     * safe to call headless.
     */
    public Popup<T> hide() {
        lastHideTime = Time.millis();
        detachListeners();
        table.remove();
        clearContent();
        return this;
    }

    public boolean isShowing() {
        return table.parent != null;
    }

    public long getLastHideTime() {
        return lastHideTime;
    }

    /**
     * Computes menu placement: bottom edge at the anchor when it fits above,
     * flipped below the anchor on overflow, clamped inside the stage on both axes.
     * Pure function of its inputs, safe to unit test headless.
     */
    public static Vec2 place(float anchorX, float anchorY, float menuWidth, float menuHeight,
            float stageWidth, float stageHeight) {
        float x = Math.max(0f, Math.min(anchorX, Math.max(0f, stageWidth - menuWidth)));
        float y = anchorY;
        if (y + menuHeight > stageHeight) {
            y = Math.max(0f, anchorY - menuHeight);
        }
        y = Math.max(0f, Math.min(y, Math.max(0f, stageHeight - menuHeight)));
        return new Vec2(x, y);
    }

    @Override
    protected Element build() {
        return new Spacer().element();
    }

    @Override
    protected void onDispose() {
        detachListeners();
        table.remove();
        clearContent();
        try {
            Events.remove(ResizeEvent.class, resizeListener);
        } catch (Throwable ignored) {
        }
    }

    private void render(T data, float stageX, float stageY) {
        clearContent();
        if (provider != null) {
            Component content = ReactiveContext.untracked(() -> ParentStack.isolate(() -> {
                Component built = provider.apply(data);
                if (built != null) {
                    built.element();
                }
                return built;
            }));
            if (content != null) {
                currentContent = content;
                Cell<?> cell = table.add(content.element());
                PendingCellConfig config = PendingCellConfig.find(content);
                if (config == null) {
                    config = PendingCellConfig.find(content.element());
                }
                if (config != null) {
                    List<Disposable> effects = config.applyToCell(cell);
                    currentBindings.addAll(effects);
                    for (Disposable effect : effects) {
                        ComponentContext.register(effect);
                    }
                }
            }
        }
        if (table.getBackground() == null) {
            table.background(new RoundedDrawable(DEFAULT_RADIUS, new Color(DEFAULT_FILL)));
        }
        table.pack();
        Vec2 pos = place(stageX, stageY, table.getWidth(), table.getHeight(),
                Core.scene.getWidth(), Core.scene.getHeight());
        table.setPosition(pos.x, pos.y);
        if (table.parent == null) {
            Core.scene.add(table);
        }
        attachListeners();
    }

    private void clearContent() {
        table.clearChildren();
        for (Disposable d : currentBindings) {
            try {
                d.dispose();
            } catch (Throwable ignored) {
            }
        }
        currentBindings.clear();
        if (currentContent != null) {
            try {
                currentContent.dispose();
            } catch (Throwable ignored) {
            }
            currentContent = null;
        }
    }

    private void attachListeners() {
        if (Core.scene == null || Core.scene.root == null) {
            return;
        }
        if (!touchAttached) {
            touchAttached = true;
            Core.scene.root.addCaptureListener(touchCatcher);
        }
        if (!keyAttached) {
            keyAttached = true;
            Core.scene.root.addCaptureListener(keyCatcher);
        }
    }

    private void detachListeners() {
        if (touchAttached) {
            touchAttached = false;
            try {
                if (Core.scene != null && Core.scene.root != null) {
                    Core.scene.root.removeCaptureListener(touchCatcher);
                }
            } catch (Throwable ignored) {
            }
        }
        if (keyAttached) {
            keyAttached = false;
            try {
                if (Core.scene != null && Core.scene.root != null) {
                    Core.scene.root.removeCaptureListener(keyCatcher);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public Popup<T> self() {
        return this;
    }
}
