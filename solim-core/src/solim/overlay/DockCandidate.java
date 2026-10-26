package solim.overlay;

public final class DockCandidate {

    private final DockTarget target;
    private final DockSide side;
    private final float x;
    private final float y;
    private final float offset;
    private final float distance;

    public DockCandidate(DockTarget target, DockSide side, float x, float y, float offset, float distance) {
        this.target = target;
        this.side = side;
        this.x = x;
        this.y = y;
        this.offset = offset;
        this.distance = distance;
    }

    public DockTarget target() {
        return target;
    }

    public DockSide side() {
        return side;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float offset() {
        return offset;
    }

    public float distance() {
        return distance;
    }

    public DockAnchor toAnchor() {
        return new DockAnchor(target.id(), side, offset, x, y);
    }
}
