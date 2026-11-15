package mindustrytool.features.smartdrill.plan;

public final class Rect {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public Rect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    public int right() {
        return x + width - 1;
    }

    public int top() {
        return y + height - 1;
    }

    public int centerX() {
        return x + (width - 1) / 2;
    }

    public int centerY() {
        return y + (height - 1) / 2;
    }

    public boolean containsTile(int tx, int ty) {
        return tx >= x && tx <= right() && ty >= y && ty <= top();
    }

    public boolean containsFootprint(int tx, int ty, int size, int offset) {
        int minX = tx + offset;
        int minY = ty + offset;
        int maxX = minX + size - 1;
        int maxY = minY + size - 1;
        return minX >= x && minY >= y && maxX <= right() && maxY <= top();
    }

    public Rect clampTo(int maxWidth, int maxHeight) {
        int w = Math.min(width, maxWidth);
        int h = Math.min(height, maxHeight);
        return width == w && height == h ? this : new Rect(x, y, w, h);
    }

    public Rect expanded(int dx, int dy) {
        int left = dx < 0 ? dx : 0;
        int top = dy > 0 ? dy : 0;
        int right = dx > 0 ? dx : 0;
        int bottom = dy < 0 ? dy : 0;
        return new Rect(x + left, y + bottom, width + right - left, height + top - bottom);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Rect)) {
            return false;
        }
        Rect rect = (Rect) other;
        return x == rect.x && y == rect.y && width == rect.width && height == rect.height;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * x + y) + width) + height;
    }

    @Override
    public String toString() {
        return "Rect[" + x + "," + y + " " + width + "x" + height + "]";
    }
}