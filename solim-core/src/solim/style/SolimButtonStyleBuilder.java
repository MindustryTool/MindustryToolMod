package solim.style;

import arc.graphics.Color;
import arc.graphics.g2d.Font;
import arc.scene.style.Drawable;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import arc.util.Nullable;
import java.util.function.Consumer;
import solim.graphics.RoundedDrawable;
import solim.reactive.Readable;

/**
 * Fluent chained builder for button styles supporting root properties,
 * state-specific sub-builders, reactive signals, layout values and
 * flyweight caching.
 */
public final class SolimButtonStyleBuilder extends BaseStyleBuilder<SolimButtonStyleBuilder> {

    private @Nullable Integer radius;
    private @Nullable Readable<Float> radiusSignal;
    private @Nullable Float stroke;
    private @Nullable Readable<Float> strokeSignal;
    private @Nullable Color borderColor;
    private @Nullable Readable<Color> borderColorSignal;
    private @Nullable Font font;

    private @Nullable ButtonStateBuilder up;
    private @Nullable ButtonStateBuilder over;
    private @Nullable ButtonStateBuilder down;
    private @Nullable ButtonStateBuilder disabled;
    private @Nullable ButtonStateBuilder checked;

    @Override
    protected SolimButtonStyleBuilder self() {
        return this;
    }

    public SolimButtonStyleBuilder rounded(int radius) {
        this.radius = radius;
        this.radiusSignal = null;
        propagateRadius(radius, null);
        return this;
    }

    public SolimButtonStyleBuilder rounded(float radius) {
        return rounded((int) radius);
    }

    public SolimButtonStyleBuilder rounded(@Nullable Readable<Float> radius) {
        if (radius != null) {
            this.radiusSignal = radius;
            propagateRadius(null, radius);
        }
        return this;
    }

    public SolimButtonStyleBuilder border(float stroke, @Nullable Color color) {
        this.stroke = stroke;
        this.strokeSignal = null;
        if (color != null) {
            this.borderColor = color.cpy();
            this.borderColorSignal = null;
        }
        propagateBorder(stroke, null, color != null ? color : null, null);
        return this;
    }

    public SolimButtonStyleBuilder border(float stroke, @Nullable Readable<Color> color) {
        this.stroke = stroke;
        this.strokeSignal = null;
        if (color != null) {
            this.borderColorSignal = color;
            propagateBorder(stroke, null, null, color);
        } else {
            propagateBorder(stroke, null, null, null);
        }
        return this;
    }

    public SolimButtonStyleBuilder border(@Nullable Color color) {
        if (color != null) {
            this.borderColor = color.cpy();
            this.borderColorSignal = null;
            propagateBorder(null, null, color, null);
        }
        return this;
    }

    public SolimButtonStyleBuilder border(@Nullable Readable<Color> color) {
        if (color != null) {
            this.borderColorSignal = color;
            propagateBorder(null, null, null, color);
        }
        return this;
    }

    public SolimButtonStyleBuilder font(@Nullable Font font) {
        this.font = font;
        return this;
    }

    public SolimButtonStyleBuilder up(@Nullable Consumer<ButtonStateBuilder> config) {
        if (config != null) {
            if (up == null) {
                up = new ButtonStateBuilder();
            }
            config.accept(up);
        }
        return this;
    }

    public SolimButtonStyleBuilder over(@Nullable Consumer<ButtonStateBuilder> config) {
        if (config != null) {
            if (over == null) {
                over = new ButtonStateBuilder();
            }
            config.accept(over);
        }
        return this;
    }

    public SolimButtonStyleBuilder down(@Nullable Consumer<ButtonStateBuilder> config) {
        if (config != null) {
            if (down == null) {
                down = new ButtonStateBuilder();
            }
            config.accept(down);
        }
        return this;
    }

    public SolimButtonStyleBuilder disabled(@Nullable Consumer<ButtonStateBuilder> config) {
        if (config != null) {
            if (disabled == null) {
                disabled = new ButtonStateBuilder();
            }
            config.accept(disabled);
        }
        return this;
    }

    public SolimButtonStyleBuilder checked(@Nullable Consumer<ButtonStateBuilder> config) {
        if (config != null) {
            if (checked == null) {
                checked = new ButtonStateBuilder();
            }
            config.accept(checked);
        }
        return this;
    }

    public SolimButtonStyleBuilder from(@Nullable ButtonStyle base) {
        if (base == null) {
            return this;
        }
        copyStateFromDrawable("up", base.up);
        copyStateFromDrawable("over", base.over);
        copyStateFromDrawable("down", base.down);
        copyStateFromDrawable("disabled", base.disabled);
        copyStateFromDrawable("checked", base.checked);
        RoundedDrawable rootSource = firstRounded(base.up, base.over, base.down, base.disabled, base.checked);
        if (rootSource != null) {
            this.radius = rootSource.getRadius();
            this.radiusSignal = null;
            this.stroke = rootSource.getStroke();
            this.strokeSignal = null;
            if (rootSource.getBorderColor() != null) {
                this.borderColor = rootSource.getBorderColor().cpy();
            }
            this.borderColorSignal = null;
        }
        if (base instanceof TextButtonStyle) {
            TextButtonStyle textBase = (TextButtonStyle) base;
            if (textBase.font != null) {
                this.font = textBase.font;
            }
        }
        return this;
    }

    public SolimButtonStyleBuilder from(@Nullable SolimButtonStyleBuilder other) {
        if (other == null) {
            return this;
        }
        this.radius = other.radius;
        this.radiusSignal = other.radiusSignal;
        this.stroke = other.stroke;
        this.strokeSignal = other.strokeSignal;
        this.borderColor = other.borderColor != null ? other.borderColor.cpy() : null;
        this.borderColorSignal = other.borderColorSignal;
        this.font = other.font;
        copyLayoutFrom(other);
        this.up = copyState(other.up);
        this.over = copyState(other.over);
        this.down = copyState(other.down);
        this.disabled = copyState(other.disabled);
        this.checked = copyState(other.checked);
        return this;
    }

    public SolimButtonStyleBuilder from(@Nullable SolimButtonStyle other) {
        if (other == null) {
            return this;
        }
        from(other.style());
        if (other.padding() != null) {
            padding(other.padding().floatValue());
        }
        if (other.margin() != null) {
            margin(other.margin().floatValue());
        }
        if (other.gap() != null) {
            gap(other.gap().floatValue());
        }
        if (other.font() != null) {
            font(other.font());
        }
        return this;
    }

    public boolean isStatic() {
        if (hasLayoutSignals()) {
            return false;
        }
        if (radiusSignal != null || strokeSignal != null || borderColorSignal != null) {
            return false;
        }
        return !hasStateSignals(up) && !hasStateSignals(over) && !hasStateSignals(down)
                && !hasStateSignals(disabled) && !hasStateSignals(checked);
    }

    public String cacheKey() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("r=").append(radius != null ? radius.toString() : "null").append(";");
        sb.append("s=").append(stroke != null ? Integer.toString(Float.floatToIntBits(stroke)) : "null").append(";");
        sb.append("bc=").append(colorKey(borderColor)).append(";");
        sb.append("f=").append(font != null ? Integer.toString(System.identityHashCode(font)) : "null").append(";");
        sb.append("up{");
        appendStateKey(sb, up);
        sb.append("}over{");
        appendStateKey(sb, over);
        sb.append("}down{");
        appendStateKey(sb, down);
        sb.append("}dis{");
        appendStateKey(sb, disabled);
        sb.append("}chk{");
        appendStateKey(sb, checked);
        sb.append("}");
        appendLayoutKey(sb);
        return sb.toString();
    }

    public SolimButtonStyle build() {
        if (isStatic()) {
            String key = cacheKey();
            SolimButtonStyle cached = StyleCache.get(key);
            if (cached != null) {
                return cached;
            }
            SolimButtonStyle fresh = buildFresh();
            return StyleCache.getOrCreate(key, () -> fresh);
        }
        return buildFresh();
    }

    static String colorKey(@Nullable Color color) {
        if (color == null) {
            return "null";
        }
        return Integer.toString(Float.floatToIntBits(color.r)) + ","
                + Integer.toString(Float.floatToIntBits(color.g)) + ","
                + Integer.toString(Float.floatToIntBits(color.b)) + ","
                + Integer.toString(Float.floatToIntBits(color.a));
    }

    private SolimButtonStyle buildFresh() {
        int rootRadius = resolveRootRadius();
        float rootStroke = resolveRootStroke();
        Color rootBorder = resolveRootBorder();

        ResolvedState upResolved = resolveState(up, rootRadius, rootStroke, rootBorder, null);
        Color upBackground = upResolved.background;

        ResolvedState overResolved = resolveOverDown(over, rootRadius, rootStroke, rootBorder, upBackground, true);
        ResolvedState downResolved = resolveOverDown(down, rootRadius, rootStroke, rootBorder, upBackground, false);
        ResolvedState disabledResolved = resolveOptional(disabled, rootRadius, rootStroke, rootBorder, upBackground);
        ResolvedState checkedResolved = resolveOptional(checked, rootRadius, rootStroke, rootBorder, upBackground);

        ButtonStyle style = new ButtonStyle();
        style.up = toDrawable(upResolved);
        style.over = toDrawable(overResolved);
        style.down = toDrawable(downResolved);
        style.disabled = toDrawable(disabledResolved);
        style.checked = toDrawable(checkedResolved);

        return new SolimButtonStyle(style, resolvePadding(), resolveMargin(), resolveGap(), font);
    }

    private int resolveRootRadius() {
        if (radius != null) {
            return radius.intValue();
        }
        if (radiusSignal != null) {
            Float v = radiusSignal.get();
            if (v != null) {
                return (int) v.floatValue();
            }
        }
        return 0;
    }

    private float resolveRootStroke() {
        if (stroke != null) {
            return stroke.floatValue();
        }
        if (strokeSignal != null) {
            Float v = strokeSignal.get();
            if (v != null) {
                return v.floatValue();
            }
        }
        return 0f;
    }

    private @Nullable Color resolveRootBorder() {
        if (borderColor != null) {
            return borderColor;
        }
        if (borderColorSignal != null) {
            return borderColorSignal.get();
        }
        return null;
    }

    private ResolvedState resolveState(@Nullable ButtonStateBuilder state,
            int rootRadius, float rootStroke, @Nullable Color rootBorder,
            @Nullable Color fallbackBackground) {
        if (state == null || state.isEmpty()) {
            if (fallbackBackground == null && state == null) {
                return new ResolvedState(null, rootRadius, rootStroke, rootBorder, null);
            }
            if (state == null) {
                return new ResolvedState(fallbackBackground, rootRadius, rootStroke, rootBorder, null);
            }
        }
        if (state != null && state.explicitDrawable() != null) {
            return new ResolvedState(null, rootRadius, rootStroke, rootBorder, state.explicitDrawable());
        }
        Color bg = null;
        int r = rootRadius;
        float s = rootStroke;
        Color bc = rootBorder;
        if (state != null) {
            if (state.backgroundSignal() != null) {
                bg = state.backgroundSignal().get();
            } else if (state.backgroundValue() != null) {
                bg = state.backgroundValue();
            } else if (fallbackBackground != null) {
                bg = fallbackBackground;
            }
            if (state.radiusValue() != null) {
                r = state.radiusValue().intValue();
            } else if (state.radiusSignal() != null) {
                Float v = state.radiusSignal().get();
                if (v != null) {
                    r = (int) v.floatValue();
                }
            }
            if (state.strokeValue() != null) {
                s = state.strokeValue().floatValue();
            } else if (state.strokeSignal() != null) {
                Float v = state.strokeSignal().get();
                if (v != null) {
                    s = v.floatValue();
                }
            }
            if (state.borderColorSignal() != null) {
                Color v = state.borderColorSignal().get();
                if (v != null) {
                    bc = v;
                }
            } else if (state.borderColorValue() != null) {
                bc = state.borderColorValue();
            }
        } 
        return new ResolvedState(bg, r, s, bc, null);
    }

    private ResolvedState resolveOverDown(@Nullable ButtonStateBuilder state,
            int rootRadius, float rootStroke, @Nullable Color rootBorder,
            @Nullable Color upBackground, boolean lighten) {
        boolean hasExplicit = state != null && !state.isEmpty();
        Color bg = null;
        if (state != null && state.explicitDrawable() != null) {
            return new ResolvedState(null, rootRadius, rootStroke, rootBorder, state.explicitDrawable());
        }
        if (state != null) {
            if (state.backgroundSignal() != null) {
                bg = state.backgroundSignal().get();
            } else if (state.backgroundValue() != null) {
                bg = state.backgroundValue();
            }
        }
        if (bg == null && upBackground != null) {
            bg = lighten ? lighten(upBackground) : darken(upBackground);
        }
        if (bg == null && !hasExplicit) {
            return new ResolvedState(null, rootRadius, rootStroke, rootBorder, null);
        }
        int r = rootRadius;
        float s = rootStroke;
        Color bc = rootBorder;
        if (state != null) {
            if (state.radiusValue() != null) {
                r = state.radiusValue().intValue();
            } else if (state.radiusSignal() != null) {
                Float v = state.radiusSignal().get();
                if (v != null) {
                    r = (int) v.floatValue();
                }
            }
            if (state.strokeValue() != null) {
                s = state.strokeValue().floatValue();
            } else if (state.strokeSignal() != null) {
                Float v = state.strokeSignal().get();
                if (v != null) {
                    s = v.floatValue();
                }
            }
            if (state.borderColorSignal() != null) {
                Color v = state.borderColorSignal().get();
                if (v != null) {
                    bc = v;
                }
            } else if (state.borderColorValue() != null) {
                bc = state.borderColorValue();
            }
        }
        return new ResolvedState(bg, r, s, bc, null);
    }

    private ResolvedState resolveOptional(@Nullable ButtonStateBuilder state,
            int rootRadius, float rootStroke, @Nullable Color rootBorder,
            @Nullable Color upBackground) {
        if (state == null || state.isEmpty()) {
            return new ResolvedState(null, rootRadius, rootStroke, rootBorder, null);
        }
        if (state.explicitDrawable() != null) {
            return new ResolvedState(null, rootRadius, rootStroke, rootBorder, state.explicitDrawable());
        }
        return resolveState(state, rootRadius, rootStroke, rootBorder, upBackground);
    }

    private @Nullable Drawable toDrawable(ResolvedState resolved) {
        if (resolved.explicit != null) {
            return resolved.explicit;
        }
        if (resolved.background == null && resolved.stroke <= 0f) {
            return null;
        }
        Color bg = resolved.background != null ? resolved.background : Color.clear;
        Color bc = resolved.borderColor;
        if (resolved.stroke > 0f && bc == null) {
            bc = Color.white;
        }
        if (bc == null) {
            bc = Color.clear;
        }
        return new RoundedDrawable(Math.max(0, resolved.radius), bg, Math.max(0f, resolved.stroke), bc);
    }

    private static Color lighten(Color base) {
        return base.cpy().mul(1.15f);
    }

    private static Color darken(Color base) {
        return base.cpy().mul(0.85f);
    }

    private void propagateRadius(@Nullable Integer value, @Nullable Readable<Float> signal) {
        propagateToStates(value, signal);
    }

    private void propagateToStates(@Nullable Integer radiusValue, @Nullable Readable<Float> radiusSig) {
        if (up != null) {
            if (radiusSig != null) {
                up.setRadiusSignalDirect(radiusSig);
            } else if (radiusValue != null) {
                up.setRadiusDirect(radiusValue);
            }
        }
        if (over != null) {
            if (radiusSig != null) {
                over.setRadiusSignalDirect(radiusSig);
            } else if (radiusValue != null) {
                over.setRadiusDirect(radiusValue);
            }
        }
        if (down != null) {
            if (radiusSig != null) {
                down.setRadiusSignalDirect(radiusSig);
            } else if (radiusValue != null) {
                down.setRadiusDirect(radiusValue);
            }
        }
        if (disabled != null) {
            if (radiusSig != null) {
                disabled.setRadiusSignalDirect(radiusSig);
            } else if (radiusValue != null) {
                disabled.setRadiusDirect(radiusValue);
            }
        }
        if (checked != null) {
            if (radiusSig != null) {
                checked.setRadiusSignalDirect(radiusSig);
            } else if (radiusValue != null) {
                checked.setRadiusDirect(radiusValue);
            }
        }
    }

    private void propagateBorder(@Nullable Float strokeValue, @Nullable Readable<Float> strokeSig,
            @Nullable Color borderValue, @Nullable Readable<Color> borderSig) {
        ButtonStateBuilder[] states = new ButtonStateBuilder[]{up, over, down, disabled, checked};
        for (ButtonStateBuilder state : states) {
            if (state == null) {
                continue;
            }
            if (strokeSig != null) {
                state.setStrokeSignalDirect(strokeSig);
            } else if (strokeValue != null) {
                state.setStrokeDirect(strokeValue);
            }
            if (borderSig != null) {
                state.setBorderColorSignalDirect(borderSig);
            } else if (borderValue != null) {
                state.setBorderColorDirect(borderValue);
            }
        }
    }

    private void copyStateFromDrawable(String name, @Nullable Drawable drawable) {
        if (drawable == null) {
            setState(name, null);
            return;
        }
        if (drawable instanceof RoundedDrawable) {
            RoundedDrawable rd = (RoundedDrawable) drawable;
            ButtonStateBuilder builder = getOrCreateState(name);
            builder.background(rd.getFillColor() != null ? rd.getFillColor().cpy() : Color.clear);
            builder.rounded(rd.getRadius());
            builder.border(rd.getStroke(), rd.getBorderColor() != null ? rd.getBorderColor().cpy() : Color.clear);
        } else {
            ButtonStateBuilder builder = getOrCreateState(name);
            builder.drawable(drawable);
        }
    }

    private ButtonStateBuilder getOrCreateState(String name) {
        if ("up".equals(name)) {
            if (up == null) {
                up = new ButtonStateBuilder();
            }
            return up;
        }
        if ("over".equals(name)) {
            if (over == null) {
                over = new ButtonStateBuilder();
            }
            return over;
        }
        if ("down".equals(name)) {
            if (down == null) {
                down = new ButtonStateBuilder();
            }
            return down;
        }
        if ("disabled".equals(name)) {
            if (disabled == null) {
                disabled = new ButtonStateBuilder();
            }
            return disabled;
        }
        if (checked == null) {
            checked = new ButtonStateBuilder();
        }
        return checked;
    }

    private void setState(String name, @Nullable ButtonStateBuilder value) {
        if ("up".equals(name)) {
            up = value;
        } else if ("over".equals(name)) {
            over = value;
        } else if ("down".equals(name)) {
            down = value;
        } else if ("disabled".equals(name)) {
            disabled = value;
        } else {
            checked = value;
        }
    }

    private static @Nullable RoundedDrawable firstRounded(@Nullable Drawable... drawables) {
        if (drawables == null) {
            return null;
        }
        for (Drawable d : drawables) {
            if (d instanceof RoundedDrawable) {
                return (RoundedDrawable) d;
            }
        }
        return null;
    }

    private static boolean hasStateSignals(@Nullable ButtonStateBuilder state) {
        return state != null && state.hasSignals();
    }

    private static void appendStateKey(StringBuilder sb, @Nullable ButtonStateBuilder state) {
        if (state == null) {
            sb.append("null");
            return;
        }
        state.appendCacheKey(sb);
    }

    private static @Nullable ButtonStateBuilder copyState(@Nullable ButtonStateBuilder other) {
        if (other == null) {
            return null;
        }
        ButtonStateBuilder copy = new ButtonStateBuilder();
        copy.copyFrom(other);
        return copy;
    }

    private static final class ResolvedState {
        final @Nullable Color background;
        final int radius;
        final float stroke;
        final @Nullable Color borderColor;
        final @Nullable Drawable explicit;

        ResolvedState(@Nullable Color background, int radius, float stroke,
                @Nullable Color borderColor, @Nullable Drawable explicit) {
            this.background = background;
            this.radius = radius;
            this.stroke = stroke;
            this.borderColor = borderColor;
            this.explicit = explicit;
        }
    }
}
