package mindustrytool.features.godmode;

import arc.func.Cons2;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class PositionSelector {

    public static void select(Cons2<Float, Float> onSelect) {
        if (Vars.ui.hudGroup == null) {
            Log.err("PositionSelector: HUD group is null");
            return;
        }

        BaseDialog dialog = new BaseDialog("Select Position");
        dialog.addCloseButton();

        float[] pos = { Vars.player.x, Vars.player.y };

        Table t = dialog.cont;
        t.table(posTable -> {
            posTable.add("X: ");
            posTable.field(String.valueOf(pos[0]), s -> {
                try {
                    pos[0] = Float.parseFloat(s);
                } catch (NumberFormatException ignored) {
                }
            }).width(100).padRight(10);

            posTable.add("Y: ");
            posTable.field(String.valueOf(pos[1]), s -> {
                try {
                    pos[1] = Float.parseFloat(s);
                } catch (NumberFormatException ignored) {
                }
            }).width(100);
        }).row();

        t.button("Current Position", Icon.map, () -> {
            pos[0] = Vars.player.x;
            pos[1] = Vars.player.y;

            onSelect.get(pos[0], pos[1]);
            dialog.hide();
        }).growX().pad(10).row();

        t.button("Confirm", Styles.togglet, () -> {
            onSelect.get(pos[0], pos[1]);
            dialog.hide();
        }).growX().pad(10);

        dialog.show();
    }
}
