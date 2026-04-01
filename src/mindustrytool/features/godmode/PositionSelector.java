package mindustrytool.features.godmode;

import arc.func.Cons2;
import arc.util.Log;
import mindustry.Vars;

public class PositionSelector {

    public static void select(Cons2<Float, Float> onSelect) {
        if (Vars.ui.hudGroup == null) {
            Log.err("PositionSelector: HUD group is null");
            return;
        }

        new PositionSelectionDialog(onSelect).show();
    }
}
