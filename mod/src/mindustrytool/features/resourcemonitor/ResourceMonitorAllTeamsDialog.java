package mindustrytool.features.resourcemonitor;

import static solim.UI.*;

import arc.Core;
import arc.struct.Seq;
import mindustry.game.Team;
import mindustry.gen.Tex;
import mindustrytool.components.WebStyles;
import solim.overlay.SolimDialog;

public class ResourceMonitorAllTeamsDialog extends SolimDialog {

    public ResourceMonitorAllTeamsDialog(ResourceMonitorState state) {
        super(Core.bundle.get("team-resources.all-teams"));
        name("resourceMonitorAllTeamsDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);
        blurBackdrop(12f);

        children(() -> scroll().props(grow(), scrollX(false)).children(() -> {
            wrap().props(growX(), padding(unit(2.5f)), gap(unit(2)), top(), center()).children(() -> {
                Seq<Team> valid = state.validTeamsSignal.get();
                Team[] teams = (valid != null && !valid.isEmpty())
                        ? valid.toArray(Team.class)
                        : Team.baseTeams;
                for (Team team : teams) {
                    if (team == null) {
                        continue;
                    }
                    boolean selected = team == state.getSelectedTeam();
                    button(() -> {
                        state.setSelectedTeam(team);
                        hide();
                    }).props(
                            style(WebStyles.filterChip()),
                            checked(selected),
                            width(150f),
                            height(unit(11)),
                            prefWidth(150f),
                            prefHeight(unit(11)),
                            padding(unit(2)),
                            gap(unit(2)))
                            .children(() -> {
                                if (Tex.whiteui != null) {
                                    icon(Tex.whiteui).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5)),
                                            color(team.color));
                                }
                                text(team.localized()).props(color(team.color), ellipsis(true), growX(), left());
                            });
                }
            });
        }));
    }
}
