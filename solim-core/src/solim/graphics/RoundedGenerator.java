package solim.graphics;

import arc.graphics.Color;
import arc.graphics.Pixmap;

/**
 * Procedural rasterizer for continuous-curvature rounded shapes (Apple-style L4 superellipse)
 * with sub-pixel anti-aliasing.
 */
public final class RoundedGenerator {

    private RoundedGenerator() {
    }

    /**
     * Calculates the L4 superellipse distance from corner origin (dx, dy).
     * Formula: d = (dx^4 + dy^4)^(1/4)
     */
    public static float calculateL4Distance(float dx, float dy) {
        if (dx <= 0f && dy <= 0f) return 0f;
        if (dx <= 0f) return dy;
        if (dy <= 0f) return dx;
        float dx2 = dx * dx;
        float dy2 = dy * dy;
        return (float) Math.sqrt(Math.sqrt(dx2 * dx2 + dy2 * dy2));
    }

    /**
     * Computes the sub-pixel anti-aliased alpha value for a solid rounded corner.
     */
    public static float computeAlpha(float dx, float dy, float radius) {
        float d = calculateL4Distance(dx, dy);
        return clamp(radius + 0.5f - d);
    }

    /**
     * Computes the sub-pixel anti-aliased alpha value for a hollow rounded border stroke.
     */
    public static float computeBorderAlpha(float dx, float dy, float radius, float stroke) {
        float d = calculateL4Distance(dx, dy);
        float alphaOut = clamp(radius + 0.5f - d);
        float alphaIn = clamp(d - (radius - stroke) + 0.5f);
        return Math.min(alphaOut, alphaIn);
    }

    /**
     * Generates a white Pixmap for a solid rounded rectangle with dimensions (2R+1) x (2R+1).
     */
    public static Pixmap generateSolidPixmap(int radius) {
        int r = Math.max(1, radius);
        int size = 2 * r + 1;
        Pixmap pixmap = new Pixmap(size, size);

        for (int y = 0; y < size; y++) {
            float dy = offsetFromCenter(y, r);
            for (int x = 0; x < size; x++) {
                float dx = offsetFromCenter(x, r);
                float a = computeAlpha(dx, dy, r);
                if (a > 0f) {
                    pixmap.set(x, y, Color.rgba8888(1f, 1f, 1f, a));
                }
            }
        }
        return pixmap;
    }

    /**
     * Generates a white Pixmap for a hollow rounded border stroke with dimensions (2R+1) x (2R+1).
     */
    public static Pixmap generateBorderPixmap(int radius, float stroke) {
        int r = Math.max(1, radius);
        float s = Math.max(0.5f, Math.min(stroke, (float) r));
        int size = 2 * r + 1;
        Pixmap pixmap = new Pixmap(size, size);

        for (int y = 0; y < size; y++) {
            float dy = offsetFromCenter(y, r);
            for (int x = 0; x < size; x++) {
                float dx = offsetFromCenter(x, r);
                float a = computeBorderAlpha(dx, dy, r, s);
                if (a > 0f) {
                    pixmap.set(x, y, Color.rgba8888(1f, 1f, 1f, a));
                }
            }
        }
        return pixmap;
    }

    private static float offsetFromCenter(int coord, int radius) {
        if (coord < radius) {
            return radius - 0.5f - coord;
        } else if (coord > radius) {
            return coord - (radius + 0.5f);
        }
        return 0f;
    }

    private static float clamp(float value) {
        if (value <= 0f) return 0f;
        if (value >= 1f) return 1f;
        return value;
    }
}
