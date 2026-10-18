package mindustrytool.features.teamresource;

import static solim.UI.*;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;
import static solim.modifier.Values.checked;
import static solim.modifier.Values.color;

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
            column().props(grow(), padding(unit(2)), center()).children(() -> {
                scroll().props(size(380f, 280f), prefWidth(380f), prefHeight(280f)).children(() -> {
                    reactiveGrid(state.validTeamsSignal)
                            .columns(3)
                            .key(t -> String.valueOf(t.id))
                            .props(gap(unit(1.5f)))
                            .items(team -> button()
                                    .props(style(WebStyles.filterChip()), checked(state.selectedTeamSignal.map(sel -> sel == team)), growX())
                                    .onClick(() -> {
                                        state.setSelectedTeam(team);
                                        hide();
                                    })
                                    .children(() -> {
                                        row().props(left(), gap(unit(1.5f)), padding(unit(1))).children(() -> {
                                            image(Tex.whiteui).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)), color(team.color));
                                            text(team.localized()).props(color(team.color));
                                        });
                                    }));
                });
            });
        });
    }
}