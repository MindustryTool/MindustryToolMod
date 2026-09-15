package solim.graphics;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.style.Drawable;

/**
 * Drawable wrapper that tints another drawable with a fixed color.
 * Draws the wrapped drawable with {@link Draw#color(Color)} applied and resets afterwards.
 */
public class ColoredDrawable implements Drawable {
    private final Color color;
    private final Drawable drawable;

    public ColoredDrawable(Color color, Drawable drawable) {
        this.color = color;
        this.drawable = drawable;
    }

    public static ColoredDrawable of(Color color, Drawable drawable) {
        return new ColoredDrawable(color, drawable);
    }

    @Override
    public void draw(float x, float y, float width, float height) {
        Draw.color(color);
        try {
            drawable.draw(x, y, width, height);
        } finally {
            Draw.reset();
        }
    }

    @Override
    public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        Draw.color(color);
        try {
            drawable.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
        } finally {
            Draw.reset();
        }
    }

    @Override
    public float getLeftWidth() { return drawable.getLeftWidth(); }

    @Override
    public void setLeftWidth(float leftWidth) { drawable.setLeftWidth(leftWidth); }

    @Override
    public float getRightWidth() { return drawable.getRightWidth(); }

    @Override
    public void setRightWidth(float rightWidth) { drawable.setRightWidth(rightWidth); }

    @Override
    public float getTopHeight() { return drawable.getTopHeight(); }

    @Override
    public void setTopHeight(float topHeight) { drawable.setTopHeight(topHeight); }

    @Override
    public float getBottomHeight() { return drawable.getBottomHeight(); }

    @Override
    public void setBottomHeight(float bottomHeight) { drawable.setBottomHeight(bottomHeight); }

    @Override
    public float getMinWidth() { return drawable.getMinWidth(); }

    @Override
    public void setMinWidth(float minWidth) { drawable.setMinWidth(minWidth); }

    @Override
    public float getMinHeight() { return drawable.getMinHeight(); }

    @Override
    public void setMinHeight(float minHeight) { drawable.setMinHeight(minHeight); }

    public Color getColor() { return color; }

    public Drawable getDrawable() { return drawable; }
}
