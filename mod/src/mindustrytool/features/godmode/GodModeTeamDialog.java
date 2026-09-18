package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Tex;
import mindustrytool.components.WebStyles;
import solim.overlay.SolimDialog;
import solim.reactive.Signal;

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
            column().gap(unit(2.5f)).padding(unit(3)).children(() -> {
                text(Core.bundle.get("feature.god-mode.team.select-player")).color(WebStyles.Colors.GHOST_FG);

                scroll().size(440f, 110f).children(() -> {
                    wrap().left().gap(unit(1.5f)).children(() -> {
                        for (Player p : players) {
                            button()
                                    .style(WebStyles.cardAction())
                                    .checked(selectedPlayer.map(sel -> sel == p))
                                    .onClick(() -> selectedPlayer.set(p))
                                    .padding(unit(1.5f))
                                    .children(() -> {
                                        row().left().gap(unit(1.5f)).center().children(() -> {
                                            image(Tex.whiteui).size(unit(2.5f)).color(p.team().color);
                                            text(p.name).color(p.team().color);
                                        });
                                    });
                        }
                    });
                });

                text(Core.bundle.get("feature.god-mode.team.select-team")).color(WebStyles.Colors.GHOST_FG);

                scroll().size(440f, 130f).children(() -> {
                    wrap().left().gap(unit(1.5f)).children(() -> {
                        for (Team t : teams) {
                            button()
                                    .style(WebStyles.filterChip())
                                    .checked(selectedTeam.map(sel -> sel == t))
                                    .onClick(() -> selectedTeam.set(t))
                                    .padding(unit(1.5f))
                                    .children(() -> {
                                        row().left().gap(unit(1.5f)).center().children(() -> {
                                            image(Tex.whiteui).size(unit(3)).color(t.color);
                                            text(t.localized()).color(t.color);
                                        });
                                    });
                        }
                    });
                });

                button()
                        .style(WebStyles.primary())
                        .growX()
                        .padding(unit(2))
                        .onClick(() -> {
                            provider.changeTeam(selectedPlayer.get(), selectedTeam.get());
                            hide();
                        })
                        .children(() -> text(Core.bundle.get("feature.god-mode.team.apply")));
            });
        });
    }
}
