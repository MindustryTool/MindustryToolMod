package mindustrytool.features.smartdrill.ui;

import arc.Core;
import arc.func.Boolp;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustrytool.features.smartdrill.SmartDrillFeature;
import mindustrytool.features.smartdrill.plan.Rect;

public final class DrillAreaOverlay {

    private static final int EDGE_LEFT = 0;
    private static final int EDGE_RIGHT = 1;
    private static final int EDGE_BOTTOM = 2;
    private static final int EDGE_TOP = 3;

    private static final int HANDLE_MARGIN = 1;
    private static final float HALF_TILE = 0.5f;

    private final SmartDrillFeature feature;
    private boolean dragging;
    private int edge = -1;
    private boolean lockAdded;
    private final Boolp dragLock = () -> dragging;

    public DrillAreaOverlay(SmartDrillFeature feature) {
        this.feature = feature;
    }

    public void draw() {
        Rect rect = feature.getRect();
        if (rect == null) {
            return;
        }

        float x = (rect.x - HALF_TILE) * Vars.tilesize;
        float y = (rect.y - HALF_TILE) * Vars.tilesize;
        float width = rect.width * Vars.tilesize;
        float height = rect.height * Vars.tilesize;

        Draw.z(Layer.overlayUI);
        Draw.color(Pal.accent, 0.10f);
        Fill.rect(x, y, width, height);
        Draw.color(Pal.accent);
        Lines.stroke(2f);
        Lines.rect(x, y, width, height);
        drawHandle(rect, EDGE_LEFT);
        drawHandle(rect, EDGE_RIGHT);
        drawHandle(rect, EDGE_BOTTOM);
        drawHandle(rect, EDGE_TOP);
        Draw.reset();
    }

    public void update() {
        if (!Core.input.isTouched()) {
            if (dragging) {
                dragging = false;
                edge = -1;
                feature.onRectDragFinished();
            }
            releaseLock();
            return;
        }

        Rect rect = feature.getRect();
        if (rect == null) {
            dragging = false;
            releaseLock();
            return;
        }

        int tileX = tileAt(Core.input.mouseWorldX());
        int tileY = tileAt(Core.input.mouseWorldY());

        if (Core.input.justTouched()) {
            int hit = hitTest(rect, tileX, tileY);
            if (hit >= 0) {
                dragging = true;
                edge = hit;
                addLock();
                return;
            }
        }

        if (!dragging) {
            return;
        }

        Rect resized = resize(rect, edge, tileX, tileY);
        if (!resized.equals(rect)) {
            feature.setRectFromDrag(resized);
        }
    }

    private static int tileAt(float world) {
        return (int) Math.floor(world / Vars.tilesize + 0.5f);
    }

    private int hitTest(Rect rect, int tileX, int tileY) {
        if (tileX < rect.x - HANDLE_MARGIN || tileX > rect.right() + HANDLE_MARGIN
                || tileY < rect.y - HANDLE_MARGIN || tileY > rect.top() + HANDLE_MARGIN) {
            return -1;
        }
        if (tileX <= rect.x + HANDLE_MARGIN - 1 || tileX <= rect.x) {
            return EDGE_LEFT;
        }
        if (tileX >= rect.right() - HANDLE_MARGIN + 1 || tileX >= rect.right()) {
            return EDGE_RIGHT;
        }
        if (tileY <= rect.y + HANDLE_MARGIN - 1 || tileY <= rect.y) {
            return EDGE_BOTTOM;
        }
        if (tileY >= rect.top() - HANDLE_MARGIN + 1 || tileY >= rect.top()) {
            return EDGE_TOP;
        }
        return -1;
    }

    private Rect resize(Rect rect, int edge, int tileX, int tileY) {
        int maxX = Vars.world != null ? Vars.world.width() - 1 : Integer.MAX_VALUE;
        int maxY = Vars.world != null ? Vars.world.height() - 1 : Integer.MAX_VALUE;

        switch (edge) {
            case EDGE_LEFT: {
                int newLeft = clamp(tileX, 0, rect.right());
                return new Rect(newLeft, rect.y, rect.right() - newLeft + 1, rect.height);
            }
            case EDGE_RIGHT: {
                int newRight = clamp(tileX, rect.x, maxX);
                return new Rect(rect.x, rect.y, newRight - rect.x + 1, rect.height);
            }
            case EDGE_BOTTOM: {
                int newBottom = clamp(tileY, 0, rect.top());
                return new Rect(rect.x, newBottom, rect.width, rect.top() - newBottom + 1);
            }
            case EDGE_TOP: {
                int newTop = clamp(tileY, rect.y, maxY);
                return new Rect(rect.x, rect.y, rect.width, newTop - rect.y + 1);
            }
            default:
                return rect;
        }
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : (value > max ? max : value);
    }

    private void drawHandle(Rect rect, int edge) {
        float size = Vars.tilesize * 0.9f;
        float x;
        float y;
        switch (edge) {
            case EDGE_LEFT:
                x = (rect.x - HALF_TILE) * Vars.tilesize;
                y = (rect.y + rect.height / 2f - HALF_TILE) * Vars.tilesize;
                break;
            case EDGE_RIGHT:
                x = (rect.right() + HALF_TILE) * Vars.tilesize;
                y = (rect.y + rect.height / 2f - HALF_TILE) * Vars.tilesize;
                break;
            case EDGE_BOTTOM:
                x = (rect.x + rect.width / 2f - HALF_TILE) * Vars.tilesize;
                y = (rect.y - HALF_TILE) * Vars.tilesize;
                break;
            default:
                x = (rect.x + rect.width / 2f - HALF_TILE) * Vars.tilesize;
                y = (rect.top() + HALF_TILE) * Vars.tilesize;
                break;
        }
        Fill.rect(x - size / 2f, y - size / 2f, size, size);
    }

    private void addLock() {
        if (lockAdded || Vars.control == null || Vars.control.input == null) {
            return;
        }
        Vars.control.input.inputLocks.add(dragLock);
        lockAdded = true;
    }

    private void releaseLock() {
        if (!lockAdded) {
            return;
        }
        lockAdded = false;
        if (Vars.control != null && Vars.control.input != null) {
            Vars.control.input.inputLocks.remove(dragLock);
        }
    }

    public void dispose() {
        dragging = false;
        edge = -1;
        releaseLock();
    }

    public boolean isDragging() {
        return dragging;
    }

    public static @Nullable Rect clampToWorld(Rect rect) {
        if (Vars.world == null) {
            return rect;
        }
        int width = Vars.world.width();
        int height = Vars.world.height();
        int x = Math.max(0, Math.min(rect.x, width - 1));
        int y = Math.max(0, Math.min(rect.y, height - 1));
        int w = Math.max(1, Math.min(rect.width, width - x));
        int h = Math.max(1, Math.min(rect.height, height - y));
        return new Rect(x, y, w, h);
    }
}