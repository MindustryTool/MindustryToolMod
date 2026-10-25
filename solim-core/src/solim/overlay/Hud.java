package solim.overlay;

import arc.Core;
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
import solim.modifier.ContainerCapability;

public class Hud extends Component
        implements Configurable<Hud>, TableCapability, ContainerCapability<Hud> {

    private static final float MIN_VIEWPORT_SIZE = 100f;

    private final Table root;
    private final Table container;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<Hud> pending;
    private @Nullable Signal<Float> boundXSignal;
    private @Nullable Signal<Float> boundYSignal;
    private @Nullable DockEngine dockEngine;
    private @Nullable DockAnchorStore dockStore;
    private @Nullable String dockTargetId;
    private @Nullable DockGuideRenderer dockGuide;

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
                        hud.keepInScreen();
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
        listen(ResizeEvent.class, resizeListener);
    }

    private void keepInScreenIfLive() {
        if (!isDisposed()) {
            keepInScreen();
        }
    }

    @Override
    protected void onDispose() {
        if (dockGuide != null) {
            dockGuide.hide();
            dockGuide = null;
        }
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

    public Hud dockable(@Nullable Element handle, DockEngine engine) {
        return dockable(handle, engine, null, null, null, null);
    }

    public Hud dockable(@Nullable Element handle, DockEngine engine, @Nullable DockAnchorStore store) {
        return dockable(handle, engine, store, null, null, null);
    }

    public Hud dockable(@Nullable Element handle, DockEngine engine, @Nullable DockAnchorStore store,
            @Nullable String targetId) {
        return dockable(handle, engine, store, null, null, targetId);
    }

    public Hud dockable(@Nullable Element handle, DockEngine engine, @Nullable DockAnchorStore store,
            @Nullable Signal<Float> xSignal, @Nullable Signal<Float> ySignal, @Nullable String targetId) {
        this.dockEngine = engine;
        this.dockStore = store;
        this.dockTargetId = targetId;
        if (xSignal != null)
            this.boundXSignal = xSignal;
        if (ySignal != null)
            this.boundYSignal = ySignal;
        installDrag(handle, this, xSignal, ySignal, engine, store, targetId);
        return this;
    }

    public static void makeDraggable(@Nullable Element handle, @Nullable Hud hud, @Nullable Signal<Float> xSignal,
            @Nullable Signal<Float> ySignal) {
        installDrag(handle, hud, xSignal, ySignal, null, null, null);
    }

    public static void makeDockable(@Nullable Element handle, @Nullable Hud hud, DockEngine engine,
            @Nullable DockAnchorStore store, @Nullable Signal<Float> xSignal, @Nullable Signal<Float> ySignal,
            @Nullable String targetId) {
        Hud target = hud != null ? hud : Hud.find(handle);
        if (target != null) {
            target.dockEngine = engine;
            target.dockStore = store;
            target.dockTargetId = targetId;
        }
        installDrag(handle, target, xSignal, ySignal, engine, store, targetId);
    }

    private DockGuideRenderer dockGuide() {
        DockGuideRenderer current = dockGuide;
        if (current == null) {
            current = new DockGuideRenderer();
            dockGuide = current;
        }
        return current;
    }

    private void hideDockGuide() {
        if (dockGuide != null) {
            dockGuide.hide();
        }
    }

    private static void installDrag(@Nullable Element handle, @Nullable Hud hud, @Nullable Signal<Float> xSignal,
            @Nullable Signal<Float> ySignal, @Nullable DockEngine dockEngine, @Nullable DockAnchorStore dockStore,
            @Nullable String dockTargetId) {
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
            private float dragStartX;
            private float dragStartY;
            private float dragOffsetX;
            private float dragOffsetY;
            private boolean useStage = false;
            private boolean didDrag = false;
            private @Nullable DockCandidate activeSnap;

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

            private @Nullable DockCandidate storedSnap(Hud targetHud) {
                if (dockEngine == null || dockStore == null) {
                    return null;
                }
                DockAnchor anchor = dockStore.load();
                if (anchor == null) {
                    return null;
                }
                DockTarget target = dockEngine.find(anchor.targetId());
                if (target == null || !target.active()) {
                    return null;
                }
                Element root = targetHud.element();
                return new DockCandidate(target, anchor.side(), root.x, root.y, anchor.offset(), 0f);
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
                activeSnap = storedSnap(targetHud);
                dragStartX = targetHud.element().x;
                dragStartY = targetHud.element().y;
                dragOffsetX = 0f;
                dragOffsetY = 0f;
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
                dragOffsetX += dx;
                dragOffsetY += dy;

                Element element = targetHud.element();
                if (element.getWidth() <= 0f || element.getHeight() <= 0f) {
                    targetHud.pack();
                }
                float width = element.getWidth();
                float height = element.getHeight();
                float rawX = dragStartX + dragOffsetX;
                float rawY = dragStartY + dragOffsetY;

                if (dockEngine == null) {
                    element.moveBy(dx, dy);
                    targetHud.clampToViewport();
                    targetHud.writePositionSignals(xSignal, ySignal);
                    return;
                }

                if (activeSnap != null && dockEngine.breaksAway(activeSnap, rawX, rawY)) {
                    activeSnap = null;
                    targetHud.hideDockGuide();
                }

                if (activeSnap != null) {
                    element.setPosition(activeSnap.x(), activeSnap.y());
                } else {
                    activeSnap = dockEngine.findSnap(rawX, rawY, width, height, dockTargetId);
                    if (activeSnap != null) {
                        element.setPosition(activeSnap.x(), activeSnap.y());
                        targetHud.dockGuide().show(activeSnap, width, height);
                    } else {
                        element.setPosition(rawX, rawY);
                        targetHud.hideDockGuide();
                    }
                }
                targetHud.clampToViewport();
                targetHud.writePositionSignals(xSignal, ySignal);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                Hud targetHud = resolveHud();
                if (targetHud == null)
                    return;
                if (didDrag) {
                    targetHud.clampToViewport();
                    if (dockStore != null) {
                        dockStore.save(activeSnap != null ? activeSnap.toAnchor() : null);
                    }
                    targetHud.writePositionSignals(xSignal, ySignal);
                }
                targetHud.hideDockGuide();
                activeSnap = null;
                didDrag = false;
            }
        });
    }

    private void writePositionSignals(@Nullable Signal<Float> xSignal, @Nullable Signal<Float> ySignal) {
        Signal<Float> targetX = xSignal != null ? xSignal : boundXSignal;
        Signal<Float> targetY = ySignal != null ? ySignal : boundYSignal;
        if (targetX != null) {
            targetX.set(root.x);
        }
        if (targetY != null) {
            targetY.set(root.y);
        }
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
        float sw = viewportWidth();
        float sh = viewportHeight();
        if (isDegenerateViewport(sw, sh)) {
            return;
        }
        applyDesiredPosition();
        clampDisplay(sw, sh);
    }

    private void clampToViewport() {
        float sw = viewportWidth();
        float sh = viewportHeight();
        if (isDegenerateViewport(sw, sh)) {
            return;
        }
        clampDisplay(sw, sh);
    }

    private float viewportWidth() {
        if (Core.scene != null) {
            return Core.scene.getWidth();
        }
        float scl = Scl.scl();
        return Core.graphics != null ? Core.graphics.getWidth() / (scl > 0f ? scl : 1f) : 0f;
    }

    private float viewportHeight() {
        if (Core.scene != null) {
            return Core.scene.getHeight();
        }
        float scl = Scl.scl();
        return Core.graphics != null ? Core.graphics.getHeight() / (scl > 0f ? scl : 1f) : 0f;
    }

    private static boolean isDegenerateViewport(float sw, float sh) {
        return sw <= MIN_VIEWPORT_SIZE || sh <= MIN_VIEWPORT_SIZE;
    }

    private void applyDesiredPosition() {
        if (applyDockPosition()) {
            return;
        }
        if (boundXSignal != null) {
            Float value = boundXSignal.peek();
            if (value != null) {
                root.x = value;
            }
        }
        if (boundYSignal != null) {
            Float value = boundYSignal.peek();
            if (value != null) {
                root.y = value;
            }
        }
    }

    private boolean applyDockPosition() {
        if (dockEngine == null || dockStore == null) {
            return false;
        }
        DockAnchor anchor = dockStore.load();
        if (anchor == null) {
            return false;
        }
        DockCandidate resolved = dockEngine.resolve(anchor, root.getWidth(), root.getHeight());
        if (resolved != null) {
            root.setPosition(resolved.x(), resolved.y());
        } else {
            root.setPosition(anchor.x(), anchor.y());
        }
        return true;
    }

    private void clampDisplay(float sw, float sh) {
        if (root.getWidth() <= 0f || root.getHeight() <= 0f) {
            root.pack();
        }
        if (root.getWidth() <= 0f || root.getHeight() <= 0f) {
            return;
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
