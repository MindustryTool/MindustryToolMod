package solim.style;

import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.util.Nullable;
import solim.signal.Readable;

/**
 * State-specific visual configuration for a single button interaction state
 * ({@code up}, {@code over}, {@code down}, {@code disabled}, {@code checked}).
 * Supports static values and reactive signals. Unset properties inherit from
 * the root {@link SolimButtonStyleBuilder} at build time.
 */
public final class ButtonStateBuilder {

    private @Nullable Color background;
    private @Nullable Readable<Color> backgroundSignal;
    private @Nullable Integer radius;
    private @Nullable Readable<Float> radiusSignal;
    private @Nullable Float stroke;
    private @Nullable Readable<Float> strokeSignal;
    private @Nullable Color borderColor;
    private @Nullable Readable<Color> borderColorSignal;
    private @Nullable Drawable explicitDrawable;

    public ButtonStateBuilder background(@Nullable Color background) {
        if (background != null) {
            this.background = background.cpy();
        } else {
            this.background = null;
        }
        this.backgroundSignal = null;
        this.explicitDrawable = null;
        return this;
    }

    public ButtonStateBuilder background(@Nullable Readable<Color> background) {
        if (background != null) {
            this.backgroundSignal = background;
            this.explicitDrawable = null;
        }
        return this;
    }

    public ButtonStateBuilder rounded(int radius) {
        this.radius = radius;
        this.radiusSignal = null;
        this.explicitDrawable = null;
        return this;
    }

    public ButtonStateBuilder rounded(float radius) {
        return rounded((int) radius);
    }

    public ButtonStateBuilder rounded(@Nullable Readable<Float> radius) {
        if (radius != null) {
            this.radiusSignal = radius;
            this.explicitDrawable = null;
        }
        return this;
    }

    public ButtonStateBuilder border(float stroke, @Nullable Color color) {
        this.stroke = stroke;
        if (color != null) {
            this.borderColor = color.cpy();
        }
        this.strokeSignal = null;
        this.borderColorSignal = null;
        this.explicitDrawable = null;
        return this;
    }

    public ButtonStateBuilder border(float stroke, @Nullable Readable<Color> color) {
        this.stroke = stroke;
        this.strokeSignal = null;
        if (color != null) {
            this.borderColorSignal = color;
            this.explicitDrawable = null;
        }
        return this;
    }

    public ButtonStateBuilder border(@Nullable Color color) {
        if (color != null) {
            this.borderColor = color.cpy();
            this.borderColorSignal = null;
            this.explicitDrawable = null;
        }
        return this;
    }

    public ButtonStateBuilder border(@Nullable Readable<Color> color) {
        if (color != null) {
            this.borderColorSignal = color;
            this.explicitDrawable = null;
        }
        return this;
    }

    public ButtonStateBuilder drawable(@Nullable Drawable drawable) {
        this.explicitDrawable = drawable;
        if (drawable != null) {
            this.background = null;
            this.backgroundSignal = null;
            this.radius = null;
            this.radiusSignal = null;
            this.stroke = null;
            this.strokeSignal = null;
            this.borderColor = null;
            this.borderColorSignal = null;
        }
        return this;
    }

    public boolean hasSignals() {
        return backgroundSignal != null || radiusSignal != null || strokeSignal != null || borderColorSignal != null;
    }

    public boolean isEmpty() {
        return background == null && backgroundSignal == null
                && radius == null && radiusSignal == null
                && stroke == null && strokeSignal == null
                && borderColor == null && borderColorSignal == null
                && explicitDrawable == null;
    }

    public @Nullable Color backgroundValue() {
        return background;
    }

    public @Nullable Readable<Color> backgroundSignal() {
        return backgroundSignal;
    }

    public @Nullable Integer radiusValue() {
        return radius;
    }

    public @Nullable Readable<Float> radiusSignal() {
        return radiusSignal;
    }

    public @Nullable Float strokeValue() {
        return stroke;
    }

    public @Nullable Readable<Float> strokeSignal() {
        return strokeSignal;
    }

    public @Nullable Color borderColorValue() {
        return borderColor;
    }

    public @Nullable Readable<Color> borderColorSignal() {
        return borderColorSignal;
    }

    public @Nullable Drawable explicitDrawable() {
        return explicitDrawable;
    }

    public void copyFrom(@Nullable ButtonStateBuilder other) {
        if (other == null) {
            return;
        }
        this.background = other.background != null ? other.background.cpy() : null;
        this.backgroundSignal = other.backgroundSignal;
        this.radius = other.radius;
        this.radiusSignal = other.radiusSignal;
        this.stroke = other.stroke;
        this.strokeSignal = other.strokeSignal;
        this.borderColor = other.borderColor != null ? other.borderColor.cpy() : null;
        this.borderColorSignal = other.borderColorSignal;
        this.explicitDrawable = other.explicitDrawable;
    }

    void setRadiusDirect(@Nullable Integer radius) {
        this.radius = radius;
        this.radiusSignal = null;
    }

    void setRadiusSignalDirect(@Nullable Readable<Float> signal) {
        if (signal != null) {
            this.radiusSignal = signal;
        }
    }

    void setStrokeDirect(@Nullable Float stroke) {
        this.stroke = stroke;
        this.strokeSignal = null;
    }

    void setStrokeSignalDirect(@Nullable Readable<Float> signal) {
        if (signal != null) {
            this.strokeSignal = signal;
        }
    }

    void setBorderColorDirect(@Nullable Color color) {
        this.borderColor = color != null ? color.cpy() : null;
        this.borderColorSignal = null;
    }

    void setBorderColorSignalDirect(@Nullable Readable<Color> signal) {
        if (signal != null) {
            this.borderColorSignal = signal;
        }
    }

    void appendCacheKey(StringBuilder sb) {
        if (explicitDrawable != null) {
            sb.append("draw=").append(System.identityHashCode(explicitDrawable)).append(";");
            return;
        }
        sb.append("bg=").append(SolimButtonStyleBuilder.colorKey(background)).append(";");
        sb.append("r=").append(radius != null ? radius.toString() : "null").append(";");
        sb.append("s=").append(stroke != null ? Integer.toString(Float.floatToIntBits(stroke)) : "null").append(";");
        sb.append("bc=").append(SolimButtonStyleBuilder.colorKey(borderColor)).append(";");
    }
}
