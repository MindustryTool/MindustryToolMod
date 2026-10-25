package solim.graphics;

import arc.scene.ui.layout.Table;
import arc.util.Nullable;

public final class BlurHelper {

    private BlurHelper() {
    }

    public static BlurDrawable getOrCreateBlur(@Nullable Table table, float defaultRadius) {
        if (table == null) {
            return new BlurDrawable(defaultRadius);
        }
        if (table.getBackground() instanceof BlurDrawable) {
            BlurDrawable bd = (BlurDrawable) table.getBackground();
            bd.owner(table);
            return bd;
        }
        if (table.getBackground() instanceof RoundedDrawable) {
            RoundedDrawable rd = (RoundedDrawable) table.getBackground();
            if (rd.getBaseDrawable() instanceof BlurDrawable) {
                BlurDrawable bd = (BlurDrawable) rd.getBaseDrawable();
                bd.owner(table);
                return bd;
            }
            BlurDrawable bd = new BlurDrawable(table, defaultRadius);
            bd.baseDrawable(rd.getBaseDrawable());
            rd.baseDrawable(bd);
            return bd;
        }
        BlurDrawable bd = new BlurDrawable(table, defaultRadius);
        if (table.getBackground() != null) {
            bd.baseDrawable(table.getBackground());
        }
        table.setBackground(bd);
        return bd;
    }

    public static void applyBackdropBlur(@Nullable Table table, float radius) {
        if (table != null) {
            BlurDrawable bd = getOrCreateBlur(table, radius);
            bd.blurRadius(radius);
        }
    }
}
