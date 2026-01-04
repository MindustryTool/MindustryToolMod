package mindustrytool.features.gameplay.autodrill;

import arc.math.geom.Point2;

/**
 * Direction enum for drill output placement.
 */
public enum Direction {
    RIGHT(new Point2(1, 0), 0),
    UP(new Point2(0, 1), 1),
    LEFT(new Point2(-1, 0), 2),
    DOWN(new Point2(0, -1), 3);

    public final Point2 p;
    public final int r;

    Direction(Point2 p, int r) {
        this.p = p;
        this.r = r;
    }

    public int primaryAxis(Point2 point) {
        return point.x * this.p.x + point.y * this.p.y;
    }

    public int secondaryAxis(Point2 point) {
        return point.x * this.p.y + point.y * this.p.x;
    }

    public static Direction getOpposite(Direction direction) {
        switch (direction) {
            case RIGHT:
                return LEFT;
            case UP:
                return DOWN;
            case LEFT:
                return RIGHT;
            default:
                return UP;
        }
    }
}
