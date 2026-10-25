package solim.overlay;

public enum DockSide {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    INSIDE;

    public boolean above() {
        return this == TOP || this == TOP_LEFT || this == TOP_RIGHT;
    }

    public boolean below() {
        return this == BOTTOM || this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
    }

    public boolean left() {
        return this == LEFT || this == TOP_LEFT || this == BOTTOM_LEFT;
    }

    public boolean right() {
        return this == RIGHT || this == TOP_RIGHT || this == BOTTOM_RIGHT;
    }
}
