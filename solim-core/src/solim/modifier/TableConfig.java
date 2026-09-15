package solim.modifier;

import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.graphics.RoundedDrawable;
import solim.layout.GapContainer;
import solim.runtime.ComponentContext;
import solim.signal.Effect;
import solim.signal.Readable;

/**
 * Mixin interface for operations that configure an Arc {@link Table}'s content
 * defaults and child spacing.
 *
 * <p>
 * Implementing components provide {@link #table()} to supply their underlying
 * Arc Table. All methods are default methods that operate on the table returned
 * by {@code table()}.
 *
 * <p>
 * Targets:
 * <ul>
 * <li>{@code align/top/bottom/left/right/center} — table content alignment</li>
 * <li>{@code padding*} — table inner padding</li>
 * <li>{@code gap/respace} — inter-child spacing</li>
 * </ul>
 *
 * @param <SELF> the concrete component type, enabling fluent chaining
 */
public interface TableConfig<SELF extends TableConfig<SELF>> {

    /** Returns the underlying Arc Table for this component. */
    Table table();

    // ---------- alignment ----------

    /** Sets the table's content alignment. */
    default SELF align(int align) {
        Table t = table();
        if (t != null)
            t.align(align);
        return self();
    }

    /** Aligns table content to the top. */
    default SELF top() {
        Table t = table();
        if (t != null)
            t.top();
        return self();
    }

    /** Aligns table content to the bottom. */
    default SELF bottom() {
        Table t = table();
        if (t != null)
            t.bottom();
        return self();
    }

    /** Aligns table content to the left. */
    default SELF left() {
        Table t = table();
        if (t != null)
            t.left();
        return self();
    }

    /** Aligns table content to the right. */
    default SELF right() {
        Table t = table();
        if (t != null)
            t.right();
        return self();
    }

    /** Centers table content. */
    default SELF center() {
        Table t = table();
        if (t != null)
            t.center();
        return self();
    }

    // ---------- padding (inner insets on Table) ----------

    /** Sets inner padding equally on all four sides. */
    default SELF padding(float padding) {
        Table t = table();
        if (t != null)
            t.margin(padding);
        return self();
    }

    /** Sets inner padding on all four sides individually. */
    default SELF padding(float top, float left, float bottom, float right) {
        Table t = table();
        if (t != null)
            t.margin(top, left, bottom, right);
        return self();
    }

    /** Sets top inner padding. */
    default SELF paddingTop(float top) {
        Table t = table();
        if (t != null)
            t.marginTop(top);
        return self();
    }

    /** Sets bottom inner padding. */
    default SELF paddingBottom(float bottom) {
        Table t = table();
        if (t != null)
            t.marginBottom(bottom);
        return self();
    }

    /** Sets left inner padding. */
    default SELF paddingLeft(float left) {
        Table t = table();
        if (t != null)
            t.marginLeft(left);
        return self();
    }

    /** Sets right inner padding. */
    default SELF paddingRight(float right) {
        Table t = table();
        if (t != null)
            t.marginRight(right);
        return self();
    }

    /** Sets horizontal inner padding. */
    default SELF paddingX(float x) {
        Table t = table();
        if (t != null) {
            t.marginLeft(x);
            t.marginRight(x);
        }
        return self();
    }

    /** Sets vertical inner padding. */
    default SELF paddingY(float y) {
        Table t = table();
        if (t != null) {
            t.marginTop(y);
            t.marginBottom(y);
        }
        return self();
    }

    // ---------- padding (reactive) ----------

    /** Sets inner padding equally on all four sides reactively. */
    default SELF padding(@Nullable Readable<Float> padding) {
        if (padding == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = padding.get();
            if (v != null)
                padding(v);
        });
        ComponentContext.register(e);
        return self();
    }

    /** Sets inner padding on all four sides individually reactively. */
    default SELF padding(
            @Nullable Readable<Float> top,
            @Nullable Readable<Float> left,
            @Nullable Readable<Float> bottom,
            @Nullable Readable<Float> right) {
        Effect e = Effect.of(() -> {
            Table t = table();
            if (t != null) {
                float tv = top != null && top.get() != null ? top.get() : 0f;
                float lv = left != null && left.get() != null ? left.get() : 0f;
                float bv = bottom != null && bottom.get() != null ? bottom.get() : 0f;
                float rv = right != null && right.get() != null ? right.get() : 0f;
                t.margin(tv, lv, bv, rv);
            }
        });
        ComponentContext.register(e);
        return self();
    }

    /** Sets top inner padding reactively. */
    default SELF paddingTop(@Nullable Readable<Float> top) {
        if (top == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = top.get();
            if (v != null)
                paddingTop(v);
        });
        ComponentContext.register(e);
        return self();
    }

    /** Sets bottom inner padding reactively. */
    default SELF paddingBottom(@Nullable Readable<Float> bottom) {
        if (bottom == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = bottom.get();
            if (v != null)
                paddingBottom(v);
        });
        ComponentContext.register(e);
        return self();
    }

    /** Sets left inner padding reactively. */
    default SELF paddingLeft(@Nullable Readable<Float> left) {
        if (left == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = left.get();
            if (v != null)
                paddingLeft(v);
        });
        ComponentContext.register(e);
        return self();
    }

    /** Sets right inner padding reactively. */
    default SELF paddingRight(@Nullable Readable<Float> right) {
        if (right == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = right.get();
            if (v != null)
                paddingRight(v);
        });
        ComponentContext.register(e);
        return self();
    }

    /** Sets horizontal inner padding reactively. */
    default SELF paddingX(@Nullable Readable<Float> x) {
        if (x == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = x.get();
            if (v != null)
                paddingX(v);
        });
        ComponentContext.register(e);
        return self();
    }

    /** Sets vertical inner padding reactively. */
    default SELF paddingY(@Nullable Readable<Float> y) {
        if (y == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = y.get();
            if (v != null)
                paddingY(v);
        });
        ComponentContext.register(e);
        return self();
    }

    // ---------- gap ----------

    /** Sets gap spacing between children. */
    default SELF gap(float gap) {
        GenericGapContainer.setGap(table(), gap);
        return self();
    }

    /** Sets gap reactively. */
    default SELF gap(@Nullable Readable<Float> gap) {
        if (gap == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = gap.get();
            if (v != null)
                gap(v);
        });
        ComponentContext.register(e);
        return self();
    }

    /** Re-evaluates spacing across all children. */
    default void respace() {
        GapContainer.respace(table());
    }

    // ---------- rounded ----------

    /** Gets or creates a {@link RoundedDrawable} on this table. */
    default RoundedDrawable getOrCreateRounded(int defaultRadius) {
        return RoundedHelper.getOrCreateRounded(table(), defaultRadius);
    }

    /** Sets rounded corners with a fixed radius. */
    default SELF rounded(int radius) {
        return rounded(radius, (Color) null);
    }

    /** Sets rounded corners with a fixed radius and color. */
    default SELF rounded(int radius, @Nullable Color color) {
        Table t = table();
        if (t == null)
            return self();
        RoundedDrawable rd = getOrCreateRounded(radius);
        rd.radius(radius);
        if (color != null) {
            rd.fillColor(color);
        }
        return self();
    }

    /** Sets rounded corners with a reactive color. */
    default SELF rounded(int radius, @Nullable Readable<Color> color) {
        Table t = table();
        if (t == null)
            return self();
        RoundedDrawable rd = getOrCreateRounded(radius);
        rd.radius(radius);
        if (color != null) {
            Effect e = Effect.of(() -> {
                Color c = color.get();
                if (c != null)
                    rd.fillColor(c);
            });
            ComponentContext.register(e);
        }
        return self();
    }

    // ---------- border ----------

    /** Sets a border with fixed stroke and color. */
    default SELF border(float stroke, @Nullable Color color) {
        Table t = table();
        if (t == null)
            return self();
        RoundedDrawable rd = getOrCreateRounded(8);
        rd.border(stroke, color != null ? color : Color.white);
        return self();
    }

    /** Sets a border with reactive color. */
    default SELF border(float stroke, @Nullable Readable<Color> color) {
        Table t = table();
        if (t == null)
            return self();
        RoundedDrawable rd = getOrCreateRounded(8);
        if (color != null) {
            Effect e = Effect.of(() -> {
                Color c = color.get();
                rd.border(stroke, c != null ? c : Color.white);
            });
            ComponentContext.register(e);
        } else {
            rd.border(stroke, Color.white);
        }
        return self();
    }

    // ---------- background ----------

    /** Sets the table's background drawable. */
    default SELF background(@Nullable Drawable bg) {
        Table t = table();
        if (t == null)
            return self();
        if (t.getBackground() instanceof RoundedDrawable) {
            ((RoundedDrawable) t.getBackground()).baseDrawable(bg);
        } else {
            t.setBackground(bg);
        }
        return self();
    }

    /** Sets the table's background drawable reactively. */
    default SELF background(@Nullable Readable<Drawable> bg) {
        if (bg == null)
            return self();
        Table t = table();
        if (t != null) {
            Effect e = Effect.of(() -> {
                Drawable d = bg.get();
                background(d);
            });
            ComponentContext.register(e);
        }
        return self();
    }

    /** Sets the table's background color. */
    default SELF background(@Nullable Color color) {
        Table t = table();
        if (t == null)
            return self();
        RoundedDrawable rd = getOrCreateRounded(8);
        rd.fillColor(color != null ? color : Color.clear);
        return self();
    }

    /** Sets the table's background color. */
    default SELF backgroundColor(@Nullable Color color) {
        return background(color);
    }

    /** Sets the table's background color reactively. */
    default SELF backgroundColor(@Nullable Readable<Color> color) {
        if (color == null)
            return self();
        Table t = table();
        if (t != null) {
            RoundedDrawable rd = getOrCreateRounded(8);
            rd.fillColor(color);
        }
        return self();
    }

    // ---------- internal ----------

     SELF self();
}
