package mindustrytool.features.godmode;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;

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
            column().props(padding(unit(3)), gap(unit(2.5f))).children(() -> {
                text(Core.bundle.get("feature.god-mode.team.select-player")).color(WebStyles.Colors.GHOST_FG);

                scroll().props(size(440f, 110f), prefWidth(440f), prefHeight(110f)).children(() -> {
                    wrap().props(left(), gap(unit(1.5f))).children(() -> {
                        for (Player p : players) {
                            button()
                                    .style(WebStyles.cardAction())
                                    .checked(selectedPlayer.map(sel -> sel == p))
                                    .onClick(() -> selectedPlayer.set(p))
                                    .props(padding(unit(1.5f)))
                                    .children(() -> {
                                        row().props(left(), gap(unit(1.5f)), center()).children(() -> {
                                            image(Tex.whiteui).props(size(unit(2.5f)), prefWidth(unit(2.5f)), prefHeight(unit(2.5f))).color(p.team().color);
                                            text(p.name).color(p.team().color);
                                        });
                                    });
                        }
                    });
                });

                text(Core.bundle.get("feature.god-mode.team.select-team")).color(WebStyles.Colors.GHOST_FG);

                scroll().props(size(440f, 130f), prefWidth(440f), prefHeight(130f)).children(() -> {
                    wrap().props(left(), gap(unit(1.5f))).children(() -> {
                        for (Team t : teams) {
                            button()
                                    .style(WebStyles.filterChip())
                                    .checked(selectedTeam.map(sel -> sel == t))
                                    .onClick(() -> selectedTeam.set(t))
                                    .props(padding(unit(1.5f)))
                                    .children(() -> {
                                        row().props(left(), gap(unit(1.5f)), center()).children(() -> {
                                            image(Tex.whiteui).props(size(unit(3)), prefWidth(unit(3)), prefHeight(unit(3))).color(t.color);
                                            text(t.localized()).color(t.color);
                                        });
                                    });
                        }
                    });
                });

                button()
                        .style(WebStyles.primary())
                        .props(growX(), padding(unit(2)))
                        .onClick(() -> {
                            provider.changeTeam(selectedPlayer.get(), selectedTeam.get());
                            hide();
                        })
                        .children(() -> text(Core.bundle.get("feature.god-mode.team.apply")));
            });
        });
    }
}
