package solim.overlay;

import java.util.Objects;

public final class DockAnchor {

    private final String targetId;
    private final DockSide side;
    private final float offset;
    private final float x;
    private final float y;

    public DockAnchor(String targetId, DockSide side, float offset, float x, float y) {
        if (targetId == null || targetId.trim().isEmpty()) {
            throw new IllegalArgumentException("targetId must not be empty");
        }
        if (side == null) {
            throw new IllegalArgumentException("side must not be null");
        }
        this.targetId = targetId;
        this.side = side;
        this.offset = offset;
        this.x = x;
        this.y = y;
    }

    public String targetId() {
        return targetId;
    }

    public DockSide side() {
        return side;
    }

    public float offset() {
        return offset;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DockAnchor)) {
            return false;
        }
        DockAnchor anchor = (DockAnchor) other;
        return targetId.equals(anchor.targetId)
                && side == anchor.side
                && Float.compare(offset, anchor.offset) == 0
                && Float.compare(x, anchor.x) == 0
                && Float.compare(y, anchor.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetId, side, offset, x, y);
    }

    @Override
    public String toString() {
        return "DockAnchor{" + targetId + ", " + side + ", offset=" + offset + ", x=" + x + ", y=" + y + '}';
    }
}
