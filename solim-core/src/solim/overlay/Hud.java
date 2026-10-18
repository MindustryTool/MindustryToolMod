package solim.overlay;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import mindustry.game.EventType.ResizeEvent;
import solim.core.Component;
import solim.core.DisposableAction;
import solim.core.OwnershipContext;
import solim.core.SolimToken;
import solim.layout.Row;
import solim.modifier.Configurable;
import solim.modifier.TableCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.runtime.AttachmentStack;
import solim.reactive.Signal;
import arc.scene.event.ClickListener;
import arc.scene.event.EventListener;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Scl;
import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.ContainerCapability;

public class Hud extends Component
        implements Configurable<Hud>, TableCapability, CellCapability, ContainerCapability<Hud>, AlignmentCapability {

    private final Table root;
    private final Table container;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<Hud> pending;
    private @Nullable Signal<Float> boundXSignal;
    private @Nullable Signal<Float> boundYSignal;

    public static class HudRootTable extends Table {

        private @Nullable Hud hud;

        @Override
        public void validate() {
            if (needsLayout()) {
                float pw = getPrefWidth();
                float ph = getPrefHeight();
                if (pw > 0f && ph > 0f && (Math.abs(getWidth() - pw) > 1.0f || Math.abs(getHeight() - ph) > 1.0f)) {
                    setSize(pw, ph);
                    if (hud != null) {
                        hud.keepInScreen(false);
                    }
                }
            }
            super.validate();
        }
    }

    public Hud() {
        this(new HudRootTable());
    }

    private Hud(HudRootTable root) {
        super(root);
        this.root = root;
        root.hud = this;
        this.root.name = "solim-hud-root";
        this.root.touchable = Touchable.childrenOnly;
        this.root.toFront();

        SolimToken.bind(this.root, this, constraints);

        this.container = new Table();
        this.container.name = "solim-hud-container";
        this.container.touchable = Touchable.enabled;
        SolimToken.bind(this.container, this, constraints);

        this.root.add(container).pad(0).margin(0);

        Cons<ResizeEvent> resizeListener = e -> {
            keepInScreen();
            Core.app.post(this::keepInScreenIfLive);
        };
        Events.on(ResizeEvent.class, resizeListener);
        withinOwnership(() -> OwnershipContext.register(
                DisposableAction.of(() -> Events.remove(ResizeEvent.class, resizeListener))));
    }

    private void keepInScreenIfLive() {
        if (!isDisposed()) {
            keepInScreen();
        }
    }

    @Override
    protected void onDispose() {
        if (Core.scene != null) {
            Core.scene.unfocus(root);
        }
    }

    public Table root() {
        return root;
    }

    public Table container() {
        return container;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    public Table table() {
        return container;
    }

    public Hud toFrontOnTouch() {
        root.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                root.toFront();
                return false;
            }
        });
        return this;
    }

    public Hud mount(@Nullable Group parent) {
        if (parent != null) {
            parent.addChild(root);
        }
        return this;
    }

    public Hud mountToScene() {
        if (Core.scene != null) {
            Core.scene.add(root);
        }
        return this;
    }

    public static @Nullable Hud find(@Nullable Element element) {
        Element cur = element;
        while (cur != null) {
            Component comp = SolimToken.getComponent(cur);
            if (comp instanceof Hud) {
                return (Hud) comp;
            }
            cur = cur.parent;
        }
        Table t = AttachmentStack.find(table -> table != null && SolimToken.getComponent(table) instanceof Hud);
        if (t != null) {
            Component comp = SolimToken.getComponent(t);
            if (comp instanceof Hud) {
                return (Hud) comp;
            }
        }
        return null;
    }

    public Hud children(@Nullable Runnable r) {
        applyPendingProps();
        OwnershipContext.push(this);
        AttachmentStack.push(container, Row.ATTACHER);
        try {
            if (r != null) {
                r.run();
            }
        } finally {
            AttachmentStack.pop();
            OwnershipContext.pop();
        }
        root.pack();
        return this;
    }

    public void bindXSignal(@Nullable Signal<Float> xSignal) {
        this.boundXSignal = xSignal;
    }

    public void bindYSignal(@Nullable Signal<Float> ySignal) {
        this.boundYSignal = ySignal;
    }

    public Hud draggable(Element handle) {
        makeDraggable(handle, this, null, null);
        return this;
    }

    public Hud draggable(Element handle, @Nullable Signal<Float> xSignal, @Nullable Signal<Float> ySignal) {
        if (xSignal != null)
            this.boundXSignal = xSignal;
        if (ySignal != null)
            this.boundYSignal = ySignal;
        makeDraggable(handle, this, xSignal, ySignal);
        return this;
    }

    public static void makeDraggable(@Nullable Element handle, @Nullable Hud hud, @Nullable Signal<Float> xSignal,
            @Nullable Signal<Float> ySignal) {
        if (handle == null)
            return;
        handle.touchable = Touchable.enabled;
        if (hud != null) {
            if (xSignal != null)
                hud.bindXSignal(xSignal);
            if (ySignal != null)
                hud.bindYSignal(ySignal);
        }
        handle.addListener(new InputListener() {
            private float lastStageX;
            private float lastStageY;
            private float lastX;
            private float lastY;
            private boolean useStage = false;
            private boolean didDrag = false;

            private @Nullable Hud resolveHud() {
                Hud target = hud != null ? hud : Hud.find(handle);
                if (target != null) {
                    if (xSignal != null)
                        target.bindXSignal(xSignal);
                    if (ySignal != null)
                        target.bindYSignal(ySignal);
                }
                return target;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                if (event != null && event.listenerActor != null && handle.getScene() == null)
                    return false;
                if (event != null && isInteractiveDescendant(event.targetActor, handle))
                    return false;
                Hud targetHud = resolveHud();
                if (targetHud == null)
                    return false;
                didDrag = false;
                lastX = x;
                lastY = y;
                if (event != null && (event.stageX != 0f || event.stageY != 0f)) {
                    lastStageX = event.stageX;
                    lastStageY = event.stageY;
                    useStage = true;
                } else {
                    useStage = false;
                }
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                Hud targetHud = resolveHud();
                if (targetHud == null)
                    return;
                float dx, dy;
                if (useStage && event != null) {
                    dx = event.stageX - lastStageX;
                    dy = event.stageY - lastStageY;
                    lastStageX = event.stageX;
                    lastStageY = event.stageY;
                } else {
                    dx = x - lastX;
                    dy = y - lastY;
                    lastX = x;
                    lastY = y;
                }
                if (Math.abs(dx) > 0.5f || Math.abs(dy) > 0.5f) {
                    didDrag = true;
                    for (EventListener l : handle.getListeners()) {
                        if (l instanceof ClickListener) {
                            ((ClickListener) l).cancel();
                        }
                    }
                }
                targetHud.element().moveBy(dx, dy);
                targetHud.keepInScreen();
                if (xSignal != null) {
                    xSignal.set(targetHud.element().x);
                }
                if (ySignal != null) {
                    ySignal.set(targetHud.element().y);
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                Hud targetHud = resolveHud();
                if (targetHud == null)
                    return;
                if (didDrag) {
                    targetHud.keepInScreen();
                    if (xSignal != null) {
                        xSignal.set(targetHud.element().x);
                    }
                    if (ySignal != null) {
                        ySignal.set(targetHud.element().y);
                    }
                }
                didDrag = false;
            }
        });
    }

    private static boolean isInteractiveDescendant(@Nullable Element target, Element handle) {
        Element curr = target;
        while (curr != null && curr != handle) {
            if (curr instanceof Button) {
                return true;
            }
            for (EventListener l : curr.getListeners()) {
                if (l instanceof ClickListener) {
                    return true;
                }
            }
            curr = curr.parent;
        }
        return false;
    }

    public void keepInScreen() {
        keepInScreen(true);
    }

    public void keepInScreen(boolean updateSignals) {
        float scl = Scl.scl();
        float sw = Core.scene != null ? Core.scene.getWidth()
                : (Core.graphics != null ? Core.graphics.getWidth() / (scl > 0f ? scl : 1f) : 0f);
        float sh = Core.scene != null ? Core.scene.getHeight()
                : (Core.graphics != null ? Core.graphics.getHeight() / (scl > 0f ? scl : 1f) : 0f);
        if (sw <= 0f || sh <= 0f)
            return;

        if (root.getWidth() <= 0f || root.getHeight() <= 0f) {
            root.pack();
        }

        float w = root.getWidth();
        float h = root.getHeight();

        float curX = root.x;
        float curY = root.y;

        if (curX + w > sw)
            curX = Math.max(0, sw - w);
        if (curX < 0)
            curX = 0;
        if (curY + h > sh)
            curY = Math.max(0, sh - h);
        if (curY < 0)
            curY = 0;

        root.setPosition(curX, curY);

        if (updateSignals) {
            if (boundXSignal != null && (boundXSignal.get() == null || Math.abs(boundXSignal.get() - curX) > 0.5f)) {
                boundXSignal.set(curX);
            }
            if (boundYSignal != null && (boundYSignal.get() == null || Math.abs(boundYSignal.get() - curY) > 0.5f)) {
                boundYSignal.set(curY);
            }
        }
    }

    public void pack() {
        root.pack();
    }

    @Override
    public @Nullable Props<Hud> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Hud> props) {
        this.pending = props;
    }

    @Override
    public Hud name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public Hud self() {
        return this;
    }
}