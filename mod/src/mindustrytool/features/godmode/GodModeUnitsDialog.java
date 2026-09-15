package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import solim.overlay.SolimDialog;
import solim.signal.Computed;
import solim.signal.Readable;
import solim.signal.Signal;

public class GodModeUnitsDialog extends SolimDialog {

    private final Signal<String> searchQuery = Signal.of("");
    private final Signal<UnitType> selectedUnit = Signal.of(Vars.content.units().size > 0 ? Vars.content.units().first() : null);
    private final Signal<Team> selectedTeam = Signal.of(Vars.player != null ? Vars.player.team() : Team.sharded);
    private final Signal<Integer> count = Signal.of(1);
    private final Signal<String> countString = Signal.of("1");
    private final Signal<Float> posX = Signal.of(Vars.player != null ? Vars.player.x : 0f);
    private final Signal<Float> posY = Signal.of(Vars.player != null ? Vars.player.y : 0f);

    public GodModeUnitsDialog(GodModeProvider provider) {
        super(Core.bundle.get("feature.god-mode.units.title"));

        name("godModeUnitsDialog");
        addCloseButton();
        closeOnBack();

        Seq<UnitType> allUnits = Vars.content.units();
        Readable<Seq<UnitType>> filteredUnits = searchQuery.map(q -> {
            if (q == null || q.trim().isEmpty()) {
                return allUnits;
            }
            String lower = q.trim().toLowerCase();
            return allUnits.select(unit -> unit.localizedName.toLowerCase().contains(lower));
        });

        Readable<String> posLabel = posX.combine(posY, (x, y) ->
                Core.bundle.format("feature.god-mode.units.position",
                        Math.round(x != null ? x : 0f),
                        Math.round(y != null ? y : 0f)));

        children(() -> {
            column().gap(unit(2)).padding(unit(2)).children(() -> {
                row().growX().gap(unit(1)).center().children(() -> {
                    icon(Icon.zoom).size(unit(4));
                    textField(searchQuery)
                            .growX()
                            .placeholder(Core.bundle.get("feature.god-mode.common.search"));
                });

                text(Core.bundle.get("feature.god-mode.units.select-unit")).color(Color.lightGray);

                scroll().size(420f, 130f).children(() -> {
                    dynamic(filteredUnits, units -> grid(6).gap(unit(1)).children(() -> {
                        if (units != null) {
                            for (UnitType u : units) {
                                button()
                                        .style(Styles.clearTogglei)
                                        .checked(selectedUnit.map(sel -> sel == u))
                                        .onClick(() -> selectedUnit.set(u))
                                        .size(unit(9), unit(9))
                                        .tooltip(u.localizedName)
                                        .children(() -> image(u.uiIcon).size(unit(6)));
                            }
                        }
                    }));
                });

                text(Core.bundle.get("feature.god-mode.items.target-team")).color(Color.lightGray);

                scroll().size(420f, 50f).children(() -> {
                    row().gap(unit(1)).children(() -> {
                        for (Team t : Team.baseTeams) {
                            button()
                                    .style(Styles.clearTogglei)
                                    .checked(selectedTeam.map(sel -> sel == t))
                                    .onClick(() -> selectedTeam.set(t))
                                    .padding(unit(1))
                                    .children(() -> {
                                        row().gap(unit(1)).children(() -> {
                                            image(Tex.whiteui).size(unit(3)).color(t.color);
                                            text(t.localized()).color(t.color);
                                        });
                                    });
                        }
                    });
                });

                row().growX().gap(unit(1)).center().children(() -> {
                    text(Core.bundle.get("feature.god-mode.units.count") + ": ").color(Color.lightGray);
                    textField(countString)
                            .width(unit(16))
                            .onTextChange(s -> {
                                try {
                                    int val = Integer.parseInt(s.trim());
                                    count.set(Math.max(1, val));
                                } catch (NumberFormatException ignored) {
                                }
                            });

                    button("+1", () -> addCount(1)).style(Styles.defaultt).size(unit(9), unit(6));
                    button("+5", () -> addCount(5)).style(Styles.defaultt).size(unit(9), unit(6));
                    button("+10", () -> addCount(10)).style(Styles.defaultt).size(unit(10), unit(6));
                    button("+50", () -> addCount(50)).style(Styles.defaultt).size(unit(10), unit(6));
                });

                row().growX().gap(unit(2)).center().children(() -> {
                    text(posLabel).color(Color.lightGray);
                    button(Core.bundle.get("feature.god-mode.units.select-position"))
                            .style(Styles.defaultt)
                            .onClick(() -> MapPositionPicker.pick(this::hide, (x, y) -> {
                                posX.set(x);
                                posY.set(y);
                                show();
                            }));
                });

                row().growX().gap(unit(2)).children(() -> {
                    button(Core.bundle.get("feature.god-mode.units.spawn"))
                            .style(Styles.defaultt)
                            .growX()
                            .onClick(() -> {
                                UnitType u = selectedUnit.get();
                                Team tm = selectedTeam.get();
                                int c = count.get() != null ? count.get() : 1;
                                float x = posX.get() != null ? posX.get() : 0f;
                                float y = posY.get() != null ? posY.get() : 0f;
                                if (u != null && tm != null) {
                                    provider.spawnUnits(u, c, tm, x, y);
                                }
                                hide();
                            });

                    button(Core.bundle.get("feature.god-mode.units.kill-all"))
                            .style(Styles.defaultt)
                            .growX()
                            .onClick(() -> {
                                UnitType u = selectedUnit.get();
                                Team tm = selectedTeam.get();
                                if (u != null && tm != null) {
                                    provider.killUnits(u, tm);
                                }
                                hide();
                            });
                });
            });
        });
    }

    private void addCount(int delta) {
        int current = count.get() != null ? count.get() : 1;
        int next = Math.max(1, current + delta);
        count.set(next);
        countString.set(String.valueOf(next));
    }
}
