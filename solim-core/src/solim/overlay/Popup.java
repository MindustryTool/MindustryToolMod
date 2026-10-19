package solim.overlay;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.input.KeyCode;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Time;
import arc.func.Func;
import mindustry.game.EventType.ResizeEvent;
import solim.core.Component;
import solim.core.SolimToken;
import solim.layout.Spacer;
import solim.modifier.Configurable;
import solim.modifier.ContainerCapability;
import solim.modifier.TableCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.core.OwnershipContext;
import solim.runtime.AttachmentStack;
import solim.runtime.OwnershipScope;

public final class Popup<T> extends Component
        implements Configurable<Popup<T>>, TableCapability, ContainerCapability<Popup<T>> {

    public enum Edge {
        TOP,
        BOTTOM
    }

    public static final AttachmentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        cell.top().left();
        return cell;
    };

    private final Table table;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<Popup<T>> pending;
    private @Nullable Edge pinnedEdge;
    private @Nullable Func<T, Component> provider;
    private @Nullable Component currentContent;
    private @Nullable Element layerAnchor;
    private boolean touchAttached = false;
    private boolean keyAttached = false;
    private long lastHideTime = 0L;
    private int hideGeneration = 0;

    private final Cons<ResizeEvent> resizeListener = e -> {
        Element el = element();
        if (el != null && el.parent != null) {
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
            // Defer the removal: mutating the scene graph while a touch-down event is still
            // being dispatched makes Arc call getScene().addTouchFocus(...) on a detached
            // element and crash with a NullPointerException. Hiding on the next frame keeps
            // the tap falling through to underlying elements without the mid-dispatch detach.
            final int generation = hideGeneration;
            Runnable hideTask = () -> {
                if (generation == hideGeneration && isShowing()) {
                    hide();
                }
            };
            if (Core.app != null) {
                Core.app.post(hideTask);
            } else {
                hideTask.run();
            }
            return false;
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

    private static final float PIN_EPSILON = 1.0f;

    public Popup() {
        this(new PopupTable());
    }

    private Popup(PopupTable table) {
        super(table);
        this.table = table;
        table.owner = this;
        SolimToken.bind(table, this, constraints);
        this.table.name = "solim-popup-menu";
        Events.on(ResizeEvent.class, resizeListener);
    }

    public Table table() {
        return table;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public @Nullable Props<Popup<T>> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Popup<T>> props) {
        this.pending = props;
    }

    @Override
    protected void onRealized(Element element) {
        applyPendingProps();
    }

    public Popup<T> children(@Nullable Runnable block) {
        applyPendingProps();
        OwnershipContext.push(this);
        AttachmentStack.push(table, ATTACHER);
        try {
            if (block != null) {
                block.run();
            }
        } finally {
            AttachmentStack.pop();
            OwnershipContext.pop();
        }
        return this;
    }

    public void setPinnedEdge(@Nullable Edge edge) {
        this.pinnedEdge = edge;
    }

    public @Nullable Edge pinnedEdge() {
        return pinnedEdge;
    }

    public Popup<T> layerBehind(@Nullable Element anchor) {
        this.layerAnchor = anchor;
        return this;
    }

    public static Vec2 pinnedPosition(@Nullable Edge edge, float x, float y, float w, float h,
            float newW, float newH, float stageW, float stageH) {
        float nx = x;
        float ny = y;
        if (edge == Edge.TOP) {
            ny = (y + h) - newH;
        }
        if (stageW > 0f && stageH > 0f) {
            nx = Math.max(0f, Math.min(nx, Math.max(0f, stageW - newW)));
            ny = Math.max(0f, Math.min(ny, Math.max(0f, stageH - newH)));
        }
        return new Vec2(nx, ny);
    }

    public Popup<T> items(@Nullable Func<T, Component> provider) {
        applyPendingProps();
        this.provider = provider;
        return this;
    }

    public Popup<T> show(@Nullable T data, float stageX, float stageY) {
        hideGeneration++;
        if (data == null || Core.scene == null || Core.scene.root == null) {
            if (data == null) {
                hide();
            }
            return this;
        }
        render(data, stageX, stageY);
        return this;
    }

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
            Component content = OwnershipScope.build(() -> provider.get(data));
            if (content != null) {
                currentContent = content;
                Cell<?> cell = AttachmentStack.attach(table, content.element(), ATTACHER, content);
                if (cell == null) {
                    content.dispose();
                    currentContent = null;
                }
            }
        }

        table.pack();
        Vec2 pos = place(stageX, stageY, table.getWidth(), table.getHeight(),
                Core.scene.getWidth(), Core.scene.getHeight());
        table.setPosition(pos.x, pos.y);
        mount();
        attachListeners();
    }

    private void mount() {
        if (table.parent != null) {
            return;
        }
        if (layerAnchor != null && layerAnchor.parent != null) {
            layerAnchor.parent.addChildBefore(layerAnchor, table);
        } else {
            Core.scene.add(table);
        }
    }

    private void clearContent() {
        table.clearChildren();
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

    private static final class PopupTable extends Table {
        @Nullable
        Popup<?> owner;

        @Override
        public void validate() {
            if (owner != null) {
                owner.reconcilePinnedEdge();
            }
            super.validate();
        }
    }

    private void reconcilePinnedEdge() {
        Edge edge = pinnedEdge;
        if (edge == null || !table.needsLayout()) {
            return;
        }
        float w = table.getWidth();
        float h = table.getHeight();
        float pw = table.getPrefWidth();
        float ph = table.getPrefHeight();
        if (pw <= 0f || ph <= 0f || Math.abs(h - ph) <= PIN_EPSILON) {
            return;
        }
        float stageW = stageWidth();
        float stageH = stageHeight();
        Vec2 pos = pinnedPosition(edge, table.x, table.y, w, h, pw, ph, stageW, stageH);
        table.setSize(pw, ph);
        table.setPosition(pos.x, pos.y);
    }

    private static float stageWidth() {
        float scl = Scl.scl();
        return Core.scene != null ? Core.scene.getWidth()
                : (Core.graphics != null ? Core.graphics.getWidth() / (scl > 0f ? scl : 1f) : 0f);
    }

    private static float stageHeight() {
        float scl = Scl.scl();
        return Core.scene != null ? Core.scene.getHeight()
                : (Core.graphics != null ? Core.graphics.getHeight() / (scl > 0f ? scl : 1f) : 0f);
    }
}
