package mindustrytool.features.godmode;

import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Tex;
import mindustry.ui.dialogs.BaseDialog;

import java.util.function.Consumer;

public class GodModeTeamSelectionDialog extends BaseDialog {
    public GodModeTeamSelectionDialog(Consumer<Team> onSelect) {
        super("Select Team");
        addCloseButton();

        cont.table(t -> {
            int i = 0;
            int cols = Vars.mobile ? 2 : 3;
            for (Team team : Team.baseTeams) {
                t.button(b -> {
                    b.image(Tex.whiteui).color(team.color).size(40).padRight(10);
                    b.add(team.name).growX();
                }, () -> {
                    onSelect.accept(team);
                    remove();
                }).growX().pad(5);

                if (++i % cols == 0) {
                    t.row();
                }
            }
        }).maxWidth(800);
    }
}
