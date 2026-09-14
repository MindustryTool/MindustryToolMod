package solim.modifier;

import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.graphics.RoundedDrawable;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.scene.ui.Button;
import arc.scene.ui.Button.ButtonStyle;

/**
 * Helper for creating and managing {@link RoundedDrawable} instances
 * on Table-backed elements.
 */
public final class RoundedHelper {

    private RoundedHelper() {
    }

    static class ColorDrawable implements Drawable {
        private final Color color;

        ColorDrawable(Color color) {
            this.color = color != null ? color : Color.clear;
        }

        @Override
        public void draw(float x, float y, float width, float height) {
            Draw.color(color);
            Fill.rect(x, y, width, height);
        }

        @Override
        public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX,
                float scaleY, float rotation) {
            Draw.color(color);
            Fill.rect(x, y, width, height);
        }

        @Override
        public float getLeftWidth() { return 0; }
        @Override
        public void setLeftWidth(float leftWidth) {}
        @Override
        public float getRightWidth() { return 0; }
        @Override
        public void setRightWidth(float rightWidth) {}
        @Override
        public float getTopHeight() { return 0; }
        @Override
        public void setTopHeight(float topHeight) {}
        @Override
        public float getBottomHeight() { return 0; }
        @Override
        public void setBottomHeight(float bottomHeight) {}
        @Override
        public float getMinWidth() { return 0; }
        @Override
        public void setMinWidth(float minWidth) {}
        @Override
        public float getMinHeight() { return 0; }
        @Override
        public void setMinHeight(float minHeight) {}
    }

    public static RoundedDrawable getOrCreateRounded(@Nullable Table table, int defaultRadius) {
        if (table == null) return new RoundedDrawable(defaultRadius);
        if (table instanceof Button) {
            Button btn = (Button) table;
            ButtonStyle s = btn.getStyle();
            if (s != null && s.up instanceof RoundedDrawable) {
                return (RoundedDrawable) s.up;
            }
            if (btn.getBackground() instanceof RoundedDrawable) {
                return (RoundedDrawable) btn.getBackground();
            }
            RoundedDrawable rd = new RoundedDrawable(defaultRadius);
            if (s != null && s.up != null) {
                rd.baseDrawable(s.up);
            } else if (btn.getBackground() != null) {
                rd.baseDrawable(btn.getBackground());
            }
            btn.setBackground(rd);
            if (s == null) {
                s = new ButtonStyle();
                btn.setStyle(s);
            }
            s.up = rd;
            return rd;
        }
        if (table.getBackground() instanceof RoundedDrawable) {
            return (RoundedDrawable) table.getBackground();
        }
        RoundedDrawable rd = new RoundedDrawable(defaultRadius);
        if (table.getBackground() != null) {
            rd.baseDrawable(table.getBackground());
        }
        table.setBackground(rd);
        return rd;
    }
}
