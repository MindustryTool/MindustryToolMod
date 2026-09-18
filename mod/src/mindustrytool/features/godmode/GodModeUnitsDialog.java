package mindustrytool.features.godmode;

import static solim.UI.*;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.UnitType;
import mindustrytool.components.WebStyles;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

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

        countString.subscribe(s -> {
            if (s != null) {
                try {
                    int val = Integer.parseInt(s.trim());
                    count.set(Math.max(1, val));
                } catch (NumberFormatException ignored) {
                }
            }
        });

        Seq<UnitType> allUnits = Vars.content.units();
        Readable<Seq<UnitType>> filteredUnits = searchQuery.map(q -> {
            if (q == null || q.trim().isEmpty()) {
                return allUnits;
            }
            String lower = q.trim().toLowerCase();
            return allUnits.select(unit -> unit.localizedName.toLowerCase().contains(lower));
        });

        Computed<String> posLabel = Signal.computed(() ->
                Core.bundle.format("feature.god-mode.units.position",
                        Math.round(posX.get() != null ? posX.get() : 0f),
                        Math.round(posY.get() != null ? posY.get() : 0f)));

        children(() -> {
            column().gap(unit(2.5f)).padding(unit(3)).children(() -> {
                row().growX().gap(unit(1.5f))
                        .padding(unit(1.5f))
                        .rounded(unit(2), WebStyles.Colors.SECONDARY_BG)
                        .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                        .center()
                        .children(() -> {
                            icon(Icon.zoom).size(unit(4.5f)).color(WebStyles.Colors.GHOST_FG);
                            textField(searchQuery)
                                    .growX()
                                    .height(unit(8))
                                    .style(WebStyles.clearInput())
                                    .placeholder(Core.bundle.get("feature.god-mode.common.search"));
                        });

                text(Core.bundle.get("feature.god-mode.units.select-unit")).color(WebStyles.Colors.GHOST_FG);

                scroll().size(440f, 130f).children(() -> {
                    dynamic(filteredUnits, units -> wrap().left().gap(unit(1.5f)).children(() -> {
                        if (units != null) {
                            for (UnitType u : units) {
                                button()
                                        .style(WebStyles.filterChip())
                                        .checked(selectedUnit.map(sel -> sel == u))
                                        .onClick(() -> selectedUnit.set(u))
                                        .size(unit(10), unit(10))
                                        .padding(unit(1))
                                        .tooltip(u.localizedName)
                                        .children(() -> image(new TextureRegionDrawable(u.uiIcon)).size(unit(6.5f)));
                            }
                        }
                    }));
                });

                text(Core.bundle.get("feature.god-mode.items.target-team")).color(WebStyles.Colors.GHOST_FG);

                scroll().size(440f, 65f).children(() -> {
                    wrap().left().gap(unit(1.5f)).children(() -> {
                        for (Team t : Team.baseTeams) {
                            button()
                                    .style(WebStyles.filterChip())
                                    .checked(selectedTeam.map(sel -> sel == t))
                                    .onClick(() -> selectedTeam.set(t))
                                    .padding(unit(1.5f))
                                    .children(() -> {
                                        row().gap(unit(1.5f)).center().children(() -> {
                                            image(Tex.whiteui).size(unit(3)).color(t.color);
                                            text(t.localized()).color(t.color);
                                        });
                                    });
                        }
                    });
                });

                wrap().gap(unit(2)).center().children(() -> {
                    row().gap(unit(1.5f)).center().children(() -> {
                        text(Core.bundle.get("feature.god-mode.units.count") + ": ").color(WebStyles.Colors.GHOST_FG);

                        row()
                                .padding(unit(1))
                                .rounded(unit(2), WebStyles.Colors.SECONDARY_BG)
                                .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                                .children(() -> {
                                    textField(countString)
                                            .width(unit(18))
                                            .height(unit(8))
                                            .style(WebStyles.clearInput());
                                });
                    });

                    wrap().gap(unit(1.5f)).center().children(() -> {
                        button(() -> addCount(1)).style(WebStyles.outline()).padding(unit(1)).size(unit(10), unit(7)).children(() -> text("+1"));
                        button(() -> addCount(5)).style(WebStyles.outline()).padding(unit(1)).size(unit(10), unit(7)).children(() -> text("+5"));
                        button(() -> addCount(10)).style(WebStyles.outline()).padding(unit(1)).size(unit(11), unit(7)).children(() -> text("+10"));
                        button(() -> addCount(50)).style(WebStyles.outline()).padding(unit(1)).size(unit(11), unit(7)).children(() -> text("+50"));
                    });
                });

                row().growX().gap(unit(2)).center().children(() -> {
                    text(posLabel).color(WebStyles.Colors.GHOST_FG);
                    button()
                            .style(WebStyles.secondary())
                            .padding(unit(1.5f))
                            .onClick(() -> MapPositionPicker.pick(this::hide, (x, y) -> {
                                posX.set(x);
                                posY.set(y);
                                show();
                            }))
                            .children(() -> text(Core.bundle.get("feature.god-mode.units.select-position")));
                });

                row().growX().gap(unit(2)).children(() -> {
                    button()
                            .style(WebStyles.primary())
                            .growX()
                            .padding(unit(2))
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
                            })
                            .children(() -> text(Core.bundle.get("feature.god-mode.units.spawn")));

                    button()
                            .style(WebStyles.danger())
                            .growX()
                            .padding(unit(2))
                            .onClick(() -> {
                                UnitType u = selectedUnit.get();
                                Team tm = selectedTeam.get();
                                if (u != null && tm != null) {
                                    provider.killUnits(u, tm);
                                }
                                hide();
                            })
                            .children(() -> text(Core.bundle.get("feature.god-mode.units.kill-all")));
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
