package solim.style;

import arc.util.Nullable;
import solim.reactive.Readable;

/**
 * Base abstractions for Solim style builders providing common layout properties.
 * Supports both static values and reactive {@link Readable} signals for
 * {@code padding}, {@code margin} and {@code gap}.
 */
public abstract class BaseStyleBuilder<S extends BaseStyleBuilder<S>> {

    protected @Nullable Float padding;
    protected @Nullable Readable<Float> paddingSignal;
    protected @Nullable Float margin;
    protected @Nullable Readable<Float> marginSignal;
    protected @Nullable Float gap;
    protected @Nullable Readable<Float> gapSignal;

    protected abstract S self();

    public S padding(float padding) {
        this.padding = padding;
        this.paddingSignal = null;
        return self();
    }

    public S padding(@Nullable Readable<Float> padding) {
        if (padding != null) {
            this.paddingSignal = padding;
        }
        return self();
    }

    public S margin(float margin) {
        this.margin = margin;
        this.marginSignal = null;
        return self();
    }

    public S margin(@Nullable Readable<Float> margin) {
        if (margin != null) {
            this.marginSignal = margin;
        }
        return self();
    }

    public S gap(float gap) {
        this.gap = gap;
        this.gapSignal = null;
        return self();
    }

    public S gap(@Nullable Readable<Float> gap) {
        if (gap != null) {
            this.gapSignal = gap;
        }
        return self();
    }

    public boolean hasLayoutSignals() {
        return paddingSignal != null || marginSignal != null || gapSignal != null;
    }

    public @Nullable Float paddingValue() {
        return padding;
    }

    public @Nullable Readable<Float> paddingSignal() {
        return paddingSignal;
    }

    public @Nullable Float marginValue() {
        return margin;
    }

    public @Nullable Readable<Float> marginSignal() {
        return marginSignal;
    }

    public @Nullable Float gapValue() {
        return gap;
    }

    public @Nullable Readable<Float> gapSignal() {
        return gapSignal;
    }

    protected void copyLayoutFrom(BaseStyleBuilder<?> other) {
        if (other == null) {
            return;
        }
        this.padding = other.padding;
        this.paddingSignal = other.paddingSignal;
        this.margin = other.margin;
        this.marginSignal = other.marginSignal;
        this.gap = other.gap;
        this.gapSignal = other.gapSignal;
    }

    protected @Nullable Float resolvePadding() {
        if (paddingSignal != null) {
            Float v = paddingSignal.get();
            if (v != null) {
                return v;
            }
        }
        return padding;
    }

    protected @Nullable Float resolveMargin() {
        if (marginSignal != null) {
            Float v = marginSignal.get();
            if (v != null) {
                return v;
            }
        }
        return margin;
    }

    protected @Nullable Float resolveGap() {
        if (gapSignal != null) {
            Float v = gapSignal.get();
            if (v != null) {
                return v;
            }
        }
        return gap;
    }

    protected void appendLayoutKey(StringBuilder sb) {
        sb.append("|p=").append(keyOf(resolveStaticFloat(padding, paddingSignal)));
        sb.append("|m=").append(keyOf(resolveStaticFloat(margin, marginSignal)));
        sb.append("|g=").append(keyOf(resolveStaticFloat(gap, gapSignal)));
    }

    private static String keyOf(@Nullable Float value) {
        if (value == null) {
            return "null";
        }
        return Integer.toString(Float.floatToIntBits(value));
    }

    private static @Nullable Float resolveStaticFloat(@Nullable Float value, @Nullable Readable<Float> signal) {
        if (signal != null) {
            return null;
        }
        return value;
    }
}
