package mindustrytool.features.display.teamresource;

import arc.Core;
import mindustry.game.Team;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class TeamResourceAllTeamsDialog extends BaseDialog {
    public TeamResourceAllTeamsDialog(TeamResourceFeature feature) {
        super("@team-resources.all-teams");
        name = "teamResourceAllTeamsDialog";
        addCloseButton();

        cont.pane(p -> {
            p.defaults().width(150f).height(50f).pad(5f);
            int i = 0;
            for (Team team : Team.all) {
                p.button(b -> {
                    b.image().color(team.color).margin(4f).size(24f).padRight(10f);
                    b.add(team.localized()).color(team.color);
                }, Styles.flatTogglet, () -> {
                    feature.selectedTeam = team;
                    feature.rebuild();
                    hide();
                }).checked(team == feature.selectedTeam);

                if (++i % 3 == 0) {
                    p.row();
                }
            }
        }).grow();

        Core.app.post(this::show);
    }
}
