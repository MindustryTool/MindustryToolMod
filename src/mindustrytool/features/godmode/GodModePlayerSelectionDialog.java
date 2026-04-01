package mindustrytool.features.godmode;

import arc.Core;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Tex;
import mindustry.ui.dialogs.BaseDialog;

import java.util.function.BiConsumer;

public class GodModePlayerSelectionDialog extends BaseDialog {
    public GodModePlayerSelectionDialog(BiConsumer<Player, mindustry.game.Team> onSelect) {
        super("Select Player");
        addCloseButton();

        cont.table(t -> {
            t.pane(p -> {
                int i = 0;
                int size = 300;
                int cols = Math.max(1, (int) (Math.min(Core.graphics.getWidth(), 800) / (size + 10)));

                for (Player player : Groups.player) {
                    p.button(b -> {
                        b.image(Tex.whiteui).color(player.team().color).size(40).padRight(10);
                        b.add(player.name).growX().left();
                    }, () -> {
                        remove();
                        new GodModeTeamSelectionDialog(team -> onSelect.accept(player, team)).show();
                    }).size(size, 60).pad(5);

                    if (++i % cols == 0) {
                        p.row();
                    }
                }
            }).grow().maxWidth(800);
        }).grow();
    }
}
