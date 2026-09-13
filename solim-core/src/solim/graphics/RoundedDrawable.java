package solim.graphics;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.scene.style.Drawable;
import arc.scene.style.NinePatchDrawable;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Disposable;
import solim.runtime.ComponentContext;
import solim.signal.Effect;
import solim.signal.Readable;

/**
 * Composite continuous-curvature rounded drawable supporting independent background fill
 * and border strokes with zero extra draw calls.
 */
public class RoundedDrawable implements Drawable, Disposable {

    private int radius;
    private float stroke;
    private Color fillColor = Color.clear.cpy();
    private Color borderColor = Color.clear.cpy();

    private @Nullable NinePatchDrawable fillPatch;
    private @Nullable NinePatchDrawable borderPatch;

    private float leftWidth;
    private float rightWidth;
    private float topHeight;
    private float bottomHeight;
    private float minWidth;
    private float minHeight;

    private final List<Disposable> bindings = new ArrayList<>();

    public RoundedDrawable(int radius) {
        this(radius, Color.clear);
    }

    public RoundedDrawable(int radius, Color fillColor) {
        this(radius, fillColor, 0f, Color.clear);
    }

    public RoundedDrawable(int radius, Color fillColor, float stroke, Color borderColor) {
        this.radius = Math.max(1, radius);
        if (fillColor != null) {
            this.fillColor.set(fillColor);
        }
        this.stroke = Math.max(0f, stroke);
        if (borderColor != null) {
            this.borderColor.set(borderColor);
        }
        updatePatches();
    }

    public static RoundedDrawable of(int radius, Color fillColor) {
        return new RoundedDrawable(radius, fillColor);
    }

    public static RoundedDrawable of(int radius, Color fillColor, float stroke, Color borderColor) {
        return new RoundedDrawable(radius, fillColor, stroke, borderColor);
    }

    public int getRadius() {
        return radius;
    }

    public RoundedDrawable radius(int radius) {
        this.radius = Math.max(1, radius);
        updatePatches();
        return this;
    }

    public RoundedDrawable radius(Readable<Integer> radiusSignal) {
        if (radiusSignal != null) {
            Effect e = Effect.of(() -> {
                Integer r = radiusSignal.get();
                if (r != null) {
                    radius(r);
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public RoundedDrawable fillColor(Color color) {
        if (color != null) {
            this.fillColor.set(color);
        }
        return this;
    }

    public RoundedDrawable fillColor(Readable<Color> colorSignal) {
        if (colorSignal != null) {
            Effect e = Effect.of(() -> {
                Color c = colorSignal.get();
                if (c != null) {
                    fillColor(c);
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public float getStroke() {
        return stroke;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public RoundedDrawable border(float stroke, Color color) {
        this.stroke = Math.max(0f, stroke);
        if (color != null) {
            this.borderColor.set(color);
        }
        updatePatches();
        return this;
    }

    public RoundedDrawable border(float stroke, Readable<Color> colorSignal) {
        this.stroke = Math.max(0f, stroke);
        updatePatches();
        if (colorSignal != null) {
            Effect e = Effect.of(() -> {
                Color c = colorSignal.get();
                if (c != null) {
                    this.borderColor.set(c);
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public RoundedDrawable border(Readable<Float> strokeSignal, Readable<Color> colorSignal) {
        if (strokeSignal != null) {
            Effect e = Effect.of(() -> {
                Float s = strokeSignal.get();
                if (s != null) {
                    this.stroke = Math.max(0f, s);
                    updatePatches();
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
        }
        if (colorSignal != null) {
            Effect e = Effect.of(() -> {
                Color c = colorSignal.get();
                if (c != null) {
                    this.borderColor.set(c);
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    private void updatePatches() {
        this.fillPatch = RoundedCache.getSolid(radius);
        if (stroke > 0f) {
            this.borderPatch = RoundedCache.getBorder(radius, stroke);
        } else {
            this.borderPatch = null;
        }

        this.minWidth = 2f * radius + 1f;
        this.minHeight = 2f * radius + 1f;
    }

    @Override
    public void draw(float x, float y, float width, float height) {
        Color current = Draw.getColor();
        float r = current.r;
        float g = current.g;
        float b = current.b;
        float a = current.a;

        if (fillPatch != null && fillColor.a > 0.001f) {
            Draw.color(r * fillColor.r, g * fillColor.g, b * fillColor.b, a * fillColor.a);
            fillPatch.draw(x, y, width, height);
        }

        if (borderPatch != null && stroke > 0f && borderColor.a > 0.001f) {
            Draw.color(r * borderColor.r, g * borderColor.g, b * borderColor.b, a * borderColor.a);
            borderPatch.draw(x, y, width, height);
        }

        Draw.color(r, g, b, a);
    }

    @Override
    public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        Color current = Draw.getColor();
        float r = current.r;
        float g = current.g;
        float b = current.b;
        float a = current.a;

        if (fillPatch != null && fillColor.a > 0.001f) {
            Draw.color(r * fillColor.r, g * fillColor.g, b * fillColor.b, a * fillColor.a);
            fillPatch.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
        }

        if (borderPatch != null && stroke > 0f && borderColor.a > 0.001f) {
            Draw.color(r * borderColor.r, g * borderColor.g, b * borderColor.b, a * borderColor.a);
            borderPatch.draw(x, y, originX, originY, width, height, scaleX, scaleY, rotation);
        }

        Draw.color(r, g, b, a);
    }

    @Override
    public float getLeftWidth() {
        return leftWidth;
    }

    @Override
    public void setLeftWidth(float leftWidth) {
        this.leftWidth = leftWidth;
    }

    @Override
    public float getRightWidth() {
        return rightWidth;
    }

    @Override
    public void setRightWidth(float rightWidth) {
        this.rightWidth = rightWidth;
    }

    @Override
    public float getTopHeight() {
        return topHeight;
    }

    @Override
    public void setTopHeight(float topHeight) {
        this.topHeight = topHeight;
    }

    @Override
    public float getBottomHeight() {
        return bottomHeight;
    }

    @Override
    public void setBottomHeight(float bottomHeight) {
        this.bottomHeight = bottomHeight;
    }

    @Override
    public float getMinWidth() {
        return minWidth;
    }

    @Override
    public void setMinWidth(float minWidth) {
        this.minWidth = minWidth;
    }

    @Override
    public float getMinHeight() {
        return minHeight;
    }

    @Override
    public void setMinHeight(float minHeight) {
        this.minHeight = minHeight;
    }

    @Override
    public void dispose() {
        for (Disposable d : bindings) {
            d.dispose();
        }
        bindings.clear();
    }
}
