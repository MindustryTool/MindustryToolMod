package mindustrytool.features.teamresource;

import static solim.UI.*;

import arc.Core;
import mindustry.gen.Tex;
import mindustrytool.components.WebStyles;
import solim.overlay.SolimDialog;

public class TeamResourceAllTeamsDialog extends SolimDialog {

    public TeamResourceAllTeamsDialog(TeamResourceState state) {
        super(Core.bundle.get("team-resources.all-teams", "All Teams"));

        name("teamResourceAllTeamsDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> {
            column().grow().center().padding(unit(2)).children(() -> {
                scroll().size(380f, 280f).children(() -> {
                    reactiveGrid(state.validTeamsSignal)
                            .columns(3)
                            .key(t -> String.valueOf(t.id))
                            .gap(unit(1.5f))
                            .children(team -> button()
                                    .style(WebStyles.filterChip())
                                    .checked(state.selectedTeamSignal.map(sel -> sel == team))
                                    .onClick(() -> {
                                        state.setSelectedTeam(team);
                                        hide();
                                    })
                                    .growX()
                                    .children(() -> {
                                        row().left().gap(unit(1.5f)).padding(unit(1)).children(() -> {
                                            image(Tex.whiteui).size(unit(4)).color(team.color);
                                            text(team.localized()).color(team.color);
                                        });
                                    }));
                });
            });
        });
    }
}
