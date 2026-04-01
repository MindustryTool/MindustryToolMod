package mindustrytool.features.godmode;

import arc.func.Cons2;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class PositionSelectionDialog extends BaseDialog {
    public PositionSelectionDialog(Cons2<Float, Float> onSelect) {
        super("Select Position");
        addCloseButton();

        float[] pos = { Vars.player.x, Vars.player.y };

        Table table = cont;
        table.table(posTable -> {
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

        table.button("Current Position", Icon.map, () -> {
            pos[0] = Vars.player.x;
            pos[1] = Vars.player.y;

            onSelect.get(pos[0], pos[1]);
            hide();
        }).growX().pad(10).row();

        table.button("Confirm", Styles.togglet, () -> {
            onSelect.get(pos[0], pos[1]);
            hide();
        }).growX().pad(10);
    }
}
