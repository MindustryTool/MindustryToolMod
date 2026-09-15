package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import solim.overlay.SolimDialog;
import solim.signal.Signal;

public class GodModeTeamDialog extends SolimDialog {

    private final Signal<Player> selectedPlayer = Signal.of(Vars.player);
    private final Signal<Team> selectedTeam = Signal.of(Vars.player != null ? Vars.player.team() : Team.sharded);

    public GodModeTeamDialog(GodModeProvider provider) {
        super(Core.bundle.get("feature.god-mode.team.title"));

        name("godModeTeamDialog");
        addCloseButton();
        closeOnBack();

        Seq<Player> players = new Seq<>();
        Groups.player.each(players::add);
        if (Vars.player != null && !players.contains(Vars.player)) {
            players.add(Vars.player);
        }

        Team[] teams = Team.baseTeams;

        children(() -> {
            column().gap(unit(2)).padding(unit(2)).children(() -> {
                text(Core.bundle.get("feature.god-mode.team.select-player")).color(Color.lightGray);

                scroll().size(400f, 100f).children(() -> {
                    column().gap(unit(1)).growX().children(() -> {
                        for (Player p : players) {
                            button()
                                    .style(Styles.clearTogglei)
                                    .checked(selectedPlayer.map(sel -> sel == p))
                                    .onClick(() -> selectedPlayer.set(p))
                                    .growX()
                                    .children(() -> {
                                        row().left().gap(unit(1)).padding(unit(1)).children(() -> {
                                            text(p.name).color(p.team().color);
                                        });
                                    });
                        }
                    });
                });

                text(Core.bundle.get("feature.god-mode.team.select-team")).color(Color.lightGray);

                scroll().size(400f, 140f).children(() -> {
                    grid(3).gap(unit(1)).children(() -> {
                        for (Team t : teams) {
                            button()
                                    .style(Styles.clearTogglei)
                                    .checked(selectedTeam.map(sel -> sel == t))
                                    .onClick(() -> selectedTeam.set(t))
                                    .growX()
                                    .children(() -> {
                                        row().left().gap(unit(1)).padding(unit(1)).children(() -> {
                                            image(Tex.whiteui).size(unit(3)).color(t.color);
                                            text(t.localized()).color(t.color);
                                        });
                                    });
                        }
                    });
                });

                button(Core.bundle.get("feature.god-mode.team.apply"))
                        .style(Styles.defaultt)
                        .growX()
                        .onClick(() -> {
                            provider.changeTeam(selectedPlayer.get(), selectedTeam.get());
                            hide();
                        });
            });
        });
    }
}
