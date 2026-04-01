package mindustrytool.features.godmode;

import arc.Core;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.type.UnitType;
import mindustry.ui.dialogs.BaseDialog;

import java.util.function.BiConsumer;

public class GodModeUnitSelectionDialog extends BaseDialog {
    public GodModeUnitSelectionDialog(GodModeDialogs.UnitSpawnConsumer onSpawn, BiConsumer<UnitType, mindustry.game.Team> onKill) {
        super("Select Unit");
        addCloseButton();

        cont.table(t -> {
            t.pane(p -> {
                int i = 0;
                int size = 200;
                int cols = Math.max(1, (int) (Math.min(Core.graphics.getWidth() * 0.9, 1200) / (size + 10)));

                for (UnitType unit : Vars.content.units()) {
                    if (unit.isHidden()) {
                        continue;
                    }

                    p.button(u -> {
                        u.background(Tex.underline);
                        u.image(unit.uiIcon).size(32).padRight(5);
                        u.add(unit.localizedName).width(120).left().ellipsis(true);
                    }, () -> {
                        remove();
                        new GodModeUnitConfigDialog(unit, onSpawn, onKill).show();
                    }).pad(5);

                    if (++i % cols == 0) {
                        p.row();
                    }
                }
            }).grow().maxWidth(1200);
        }).grow().center();
    }
}
