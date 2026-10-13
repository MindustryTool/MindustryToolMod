package solim.modifier;

import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.graphics.RoundedDrawable;
import solim.layout.GapContainer;
import solim.reactive.Effect;
import solim.reactive.Readable;

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

    Table table();

    default SELF align(int align) {
        Table t = table();
        if (t != null)
            t.align(align);
        return self();
    }

    default SELF top() {
        Table t = table();
        if (t != null)
            t.top();
        return self();
    }

    default SELF bottom() {
        Table t = table();
        if (t != null)
            t.bottom();
        return self();
    }

    default SELF left() {
        Table t = table();
        if (t != null)
            t.left();
        return self();
    }

    default SELF right() {
        Table t = table();
        if (t != null)
            t.right();
        return self();
    }

    default SELF center() {
        Table t = table();
        if (t != null)
            t.center();
        return self();
    }

    default SELF padding(float padding) {
        Table t = table();
        if (t != null)
            t.margin(padding);
        return self();
    }

    default SELF padding(float top, float left, float bottom, float right) {
        Table t = table();
        if (t != null)
            t.margin(top, left, bottom, right);
        return self();
    }

    default SELF paddingTop(float top) {
        Table t = table();
        if (t != null)
            t.marginTop(top);
        return self();
    }

    default SELF paddingBottom(float bottom) {
        Table t = table();
        if (t != null)
            t.marginBottom(bottom);
        return self();
    }

    default SELF paddingLeft(float left) {
        Table t = table();
        if (t != null)
            t.marginLeft(left);
        return self();
    }

    default SELF paddingRight(float right) {
        Table t = table();
        if (t != null)
            t.marginRight(right);
        return self();
    }

    default SELF paddingX(float x) {
        Table t = table();
        if (t != null) {
            t.marginLeft(x);
            t.marginRight(x);
        }
        return self();
    }

    default SELF paddingY(float y) {
        Table t = table();
        if (t != null) {
            t.marginTop(y);
            t.marginBottom(y);
        }
        return self();
    }

    default SELF padding(@Nullable Readable<Float> padding) {
        if (padding == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = padding.get();
            if (v != null)
                padding(v);
        });
        ModifierOwnership.own(this, e);
        return self();
    }

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
        ModifierOwnership.own(this, e);
        return self();
    }

    default SELF paddingTop(@Nullable Readable<Float> top) {
        if (top == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = top.get();
            if (v != null)
                paddingTop(v);
        });
        ModifierOwnership.own(this, e);
        return self();
    }

    default SELF paddingBottom(@Nullable Readable<Float> bottom) {
        if (bottom == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = bottom.get();
            if (v != null)
                paddingBottom(v);
        });
        ModifierOwnership.own(this, e);
        return self();
    }

    default SELF paddingLeft(@Nullable Readable<Float> left) {
        if (left == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = left.get();
            if (v != null)
                paddingLeft(v);
        });
        ModifierOwnership.own(this, e);
        return self();
    }

    default SELF paddingRight(@Nullable Readable<Float> right) {
        if (right == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = right.get();
            if (v != null)
                paddingRight(v);
        });
        ModifierOwnership.own(this, e);
        return self();
    }

    default SELF paddingX(@Nullable Readable<Float> x) {
        if (x == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = x.get();
            if (v != null)
                paddingX(v);
        });
        ModifierOwnership.own(this, e);
        return self();
    }

    default SELF paddingY(@Nullable Readable<Float> y) {
        if (y == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = y.get();
            if (v != null)
                paddingY(v);
        });
        ModifierOwnership.own(this, e);
        return self();
    }

    default SELF gap(float gap) {
        GenericGapContainer.setGap(table(), gap);
        return self();
    }

    default SELF gap(@Nullable Readable<Float> gap) {
        if (gap == null)
            return self();
        Effect e = Effect.of(() -> {
            Float v = gap.get();
            if (v != null)
                gap(v);
        });
        ModifierOwnership.own(this, e);
        return self();
    }

    default void respace() {
        GapContainer.respace(table());
    }

    default RoundedDrawable getOrCreateRounded(int defaultRadius) {
        return RoundedHelper.getOrCreateRounded(table(), defaultRadius);
    }

    default SELF rounded(int radius) {
        return rounded(radius, (Color) null);
    }

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
            ModifierOwnership.own(this, e);
        }
        return self();
    }

    default SELF border(float stroke, @Nullable Color color) {
        Table t = table();
        if (t == null)
            return self();
        RoundedDrawable rd = getOrCreateRounded(8);
        rd.border(stroke, color != null ? color : Color.white);
        return self();
    }

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
            ModifierOwnership.own(this, e);
        } else {
            rd.border(stroke, Color.white);
        }
        return self();
    }

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

    default SELF background(@Nullable Readable<Drawable> bg) {
        if (bg == null)
            return self();
        Table t = table();
        if (t != null) {
            Effect e = Effect.of(() -> {
                Drawable d = bg.get();
                background(d);
            });
            ModifierOwnership.own(this, e);
        }
        return self();
    }

    default SELF background(@Nullable Color color) {
        Table t = table();
        if (t == null)
            return self();
        RoundedDrawable rd = getOrCreateRounded(8);
        rd.fillColor(color != null ? color : Color.clear);
        return self();
    }

    default SELF backgroundColor(@Nullable Color color) {
        return background(color);
    }

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

    SELF self();
}
