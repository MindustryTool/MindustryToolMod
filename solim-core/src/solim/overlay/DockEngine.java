package solim.overlay;

import arc.struct.Seq;
import arc.util.Nullable;

public class DockEngine {

    public static final float DEFAULT_SNAP_THRESHOLD = 16f;
    public static final float DEFAULT_BREAKAWAY_DISTANCE = 24f;
    public static final float DEFAULT_GAP = 4f;

    private final Seq<DockTarget> targets = new Seq<>();
    private final Scratch scratch = new Scratch();

    private float snapThreshold = DEFAULT_SNAP_THRESHOLD;
    private float breakawayDistance = DEFAULT_BREAKAWAY_DISTANCE;
    private float gap = DEFAULT_GAP;

    public float snapThreshold() {
        return snapThreshold;
    }

    public void snapThreshold(float value) {
        this.snapThreshold = Math.max(0f, value);
    }

    public float breakawayDistance() {
        return breakawayDistance;
    }

    public void breakawayDistance(float value) {
        this.breakawayDistance = Math.max(0f, value);
    }

    public float gap() {
        return gap;
    }

    public void gap(float value) {
        this.gap = Math.max(0f, value);
    }

    public int size() {
        return targets.size;
    }

    public DockEngine register(DockTarget target) {
        if (target == null || target.id() == null) {
            return this;
        }
        unregister(target.id());
        targets.add(target);
        return this;
    }

    public boolean unregister(@Nullable String targetId) {
        if (targetId == null) {
            return false;
        }
        for (int i = targets.size - 1; i >= 0; i--) {
            DockTarget target = targets.get(i);
            if (target != null && targetId.equals(target.id())) {
                targets.remove(i);
                return true;
            }
        }
        return false;
    }

    public void clear() {
        targets.clear();
    }

    public @Nullable DockTarget find(@Nullable String targetId) {
        if (targetId == null) {
            return null;
        }
        for (int i = 0; i < targets.size; i++) {
            DockTarget target = targets.get(i);
            if (target != null && targetId.equals(target.id())) {
                return target;
            }
        }
        return null;
    }

    public @Nullable DockCandidate findSnap(float x, float y, float width, float height) {
        return findSnap(x, y, width, height, null);
    }

    public @Nullable DockCandidate findSnap(float x, float y, float width, float height,
            @Nullable String excludeTargetId) {
        if (width <= 0f || height <= 0f) {
            return null;
        }
        scratch.reset();
        for (int i = 0; i < targets.size; i++) {
            DockTarget target = targets.get(i);
            if (target == null || !target.active()) {
                continue;
            }
            if (excludeTargetId != null && excludeTargetId.equals(target.id())) {
                continue;
            }
            considerEdges(target, x, y, width, height);
        }
        return scratch.toCandidate();
    }

    public @Nullable DockCandidate resolve(DockAnchor anchor, float width, float height) {
        if (anchor == null || width <= 0f || height <= 0f) {
            return null;
        }
        DockTarget target = find(anchor.targetId());
        if (target == null || !target.active() || !target.supports(anchor.side())) {
            return null;
        }
        return placement(target, anchor.side(), anchor.offset(), width, height);
    }

    public boolean breaksAway(DockCandidate candidate, float x, float y) {
        if (candidate == null) {
            return false;
        }
        float dx = x - candidate.x();
        float dy = y - candidate.y();
        return dx * dx + dy * dy > breakawayDistance * breakawayDistance;
    }

    private void considerEdges(DockTarget target, float x, float y, float width, float height) {
        float tx = target.x();
        float ty = target.y();
        float tw = target.width();
        float th = target.height();

        if (target.supports(DockSide.TOP)) {
            float distance = Math.abs(y - (ty + th));
            if (distance <= snapThreshold && overlaps(x, width, tx, tw)) {
                consider(target, DockSide.TOP, x, ty + th + gap, x - tx, distance);
            }
        }
        if (target.supports(DockSide.BOTTOM)) {
            float distance = Math.abs((y + height) - ty);
            if (distance <= snapThreshold && overlaps(x, width, tx, tw)) {
                consider(target, DockSide.BOTTOM, x, ty - gap - height, x - tx, distance);
            }
        }
        if (target.supports(DockSide.LEFT)) {
            float distance = Math.abs((x + width) - tx);
            if (distance <= snapThreshold && overlaps(y, height, ty, th)) {
                consider(target, DockSide.LEFT, tx - gap - width, y, y - ty, distance);
            }
        }
        if (target.supports(DockSide.RIGHT)) {
            float distance = Math.abs(x - (tx + tw));
            if (distance <= snapThreshold && overlaps(y, height, ty, th)) {
                consider(target, DockSide.RIGHT, tx + tw + gap, y, y - ty, distance);
            }
        }

        considerCorner(target, DockSide.TOP_LEFT, x, y, width, height, tx, ty, tw, th);
        considerCorner(target, DockSide.TOP_RIGHT, x, y, width, height, tx, ty, tw, th);
        considerCorner(target, DockSide.BOTTOM_LEFT, x, y, width, height, tx, ty, tw, th);
        considerCorner(target, DockSide.BOTTOM_RIGHT, x, y, width, height, tx, ty, tw, th);

        if (target.supports(DockSide.INSIDE) && width <= tw && height <= th) {
            float cx = tx + (tw - width) / 2f;
            float cy = ty + (th - height) / 2f;
            float distance = distance(x + width / 2f, y + height / 2f, tx + tw / 2f, ty + th / 2f);
            if (distance <= snapThreshold) {
                consider(target, DockSide.INSIDE, cx, cy, 0f, distance);
            }
        }
    }

    private void considerCorner(DockTarget target, DockSide side, float x, float y, float width, float height,
            float tx, float ty, float tw, float th) {
        if (!target.supports(side)) {
            return;
        }
        float horizontalDistance = side.left() ? Math.abs((x + width) - tx) : Math.abs(x - (tx + tw));
        float verticalDistance = side.above() ? Math.abs(y - (ty + th)) : Math.abs((y + height) - ty);
        if (horizontalDistance > snapThreshold || verticalDistance > snapThreshold) {
            return;
        }
        float cx = side.left() ? tx - gap - width : tx + tw + gap;
        float cy = side.above() ? ty + th + gap : ty - gap - height;
        consider(target, side, cx, cy, 0f, Math.max(horizontalDistance, verticalDistance));
    }

    private @Nullable DockCandidate placement(DockTarget target, DockSide side, float offset, float width,
            float height) {
        float tx = target.x();
        float ty = target.y();
        float tw = target.width();
        float th = target.height();

        float x;
        float y;
        switch (side) {
            case TOP:
                x = tx + offset;
                y = ty + th + gap;
                break;
            case BOTTOM:
                x = tx + offset;
                y = ty - gap - height;
                break;
            case LEFT:
                x = tx - gap - width;
                y = ty + offset;
                break;
            case RIGHT:
                x = tx + tw + gap;
                y = ty + offset;
                break;
            case TOP_LEFT:
                x = tx - gap - width;
                y = ty + th + gap;
                break;
            case TOP_RIGHT:
                x = tx + tw + gap;
                y = ty + th + gap;
                break;
            case BOTTOM_LEFT:
                x = tx - gap - width;
                y = ty - gap - height;
                break;
            case BOTTOM_RIGHT:
                x = tx + tw + gap;
                y = ty - gap - height;
                break;
            case INSIDE:
                x = tx + (tw - width) / 2f;
                y = ty + (th - height) / 2f;
                break;
            default:
                return null;
        }
        return new DockCandidate(target, side, x, y, offset, 0f);
    }

    private void consider(DockTarget target, DockSide side, float x, float y, float offset, float distance) {
        if (distance >= scratch.distance) {
            return;
        }
        scratch.target = target;
        scratch.side = side;
        scratch.x = x;
        scratch.y = y;
        scratch.offset = offset;
        scratch.distance = distance;
    }

    private static boolean overlaps(float start, float length, float otherStart, float otherLength) {
        return Math.min(start + length, otherStart + otherLength) - Math.max(start, otherStart) >= 0f;
    }

    private static float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static final class Scratch {
        private @Nullable DockTarget target;
        private @Nullable DockSide side;
        private float x;
        private float y;
        private float offset;
        private float distance = Float.MAX_VALUE;

        private void reset() {
            target = null;
            side = null;
            x = 0f;
            y = 0f;
            offset = 0f;
            distance = Float.MAX_VALUE;
        }

        private @Nullable DockCandidate toCandidate() {
            DockTarget foundTarget = target;
            DockSide foundSide = side;
            if (foundTarget == null || foundSide == null) {
                return null;
            }
            return new DockCandidate(foundTarget, foundSide, x, y, offset, distance);
        }
    }
}
