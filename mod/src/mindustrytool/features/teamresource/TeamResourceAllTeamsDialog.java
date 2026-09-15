package mindustrytool.features.teamresource;

import static solim.UI.*;

import arc.Core;
import mindustry.game.Team;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import solim.overlay.SolimDialog;

/**
 * Modal dialog for selecting an active team when many teams exist, using
 * SolimDialog.
 */
public class TeamResourceAllTeamsDialog extends SolimDialog {

    public TeamResourceAllTeamsDialog(TeamResourceState state) {
        super(Core.bundle.get("team-resources.all-teams", "All Teams"));

        name("teamResourceAllTeamsDialog");
        addCloseButton();
        closeOnBack();

        children(() -> {
            scroll().size(360f, 280f).children(() -> {
                dynamic(state.validTeamsSignal, teams -> grid(3).gap(unit(1)).children(() -> {
                    if (teams != null) {
                        for (Team team : teams) {
                            button()
                                    .style(Styles.clearTogglei)
                                    .checked(state.selectedTeamSignal.map(sel -> sel == team))
                                    .onClick(() -> {
                                        state.setSelectedTeam(team);
                                        hide();
                                    })
                                    .growX()
                                    .children(() -> {
                                        row().left().gap(unit(1)).padding(unit(1)).children(() -> {
                                            image(Tex.whiteui).size(unit(4)).color(team.color);
                                            text(team.localized()).color(team.color);
                                        });
                                    });
                        }
                    }
                }));
            });
        });
    }
}
