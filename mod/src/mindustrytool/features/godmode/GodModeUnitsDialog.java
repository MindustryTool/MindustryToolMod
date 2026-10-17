package mindustrytool.features.godmode;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.tooltip;
import static solim.modifier.Values.width;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.BlockUnitc;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.UnitType;
import mindustrytool.components.WebStyles;
import solim.modifier.Values;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class GodModeUnitsDialog extends SolimDialog {

    private final Signal<String> searchQuery = Signal.of("");
    private final Signal<UnitType> selectedUnit;
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

        Seq<UnitType> allUnits = Vars.content.units().select(GodModeUnitsDialog::isSpawnable);
        this.selectedUnit = Signal.of(allUnits.size > 0 ? allUnits.first() : null);

        Effect.of(countString, s -> {
            if (s != null) {
                try {
                    int val = Integer.parseInt(s.trim());
                    count.set(Math.max(1, val));
                } catch (NumberFormatException ignored) {
                }
            }
        });

        Readable<Seq<UnitType>> filteredUnits = searchQuery.map(q -> {
            if (q == null || q.trim().isEmpty()) {
                return allUnits;
            }
            String lower = q.trim().toLowerCase();
            return allUnits.select(unit -> unit.localizedName.toLowerCase().contains(lower));
        });

        Computed<String> posLabel = Signal.computed(() -> Core.bundle.format("feature.god-mode.units.position",
                Math.round(posX.get() != null ? posX.get() : 0f),
                Math.round(posY.get() != null ? posY.get() : 0f)));

        children(() -> {
            column().props(padding(unit(3)), gap(unit(2.5f))).children(() -> {
                row().props(growX(), padding(unit(1.5f)), Values.rounded(unit(2), WebStyles.Colors.SECONDARY_BG), Values.border(1.5f, WebStyles.Colors.BORDER_INPUT), gap(unit(1.5f)), center())
                        .children(() -> {
                            icon(Icon.zoom).props(size(unit(4.5f)), prefWidth(unit(4.5f)), prefHeight(unit(4.5f))).color(WebStyles.Colors.GHOST_FG);
                            textField(searchQuery)
                                    .props(growX(), height(unit(8)), prefHeight(unit(8)))
                                    .style(WebStyles.clearInput())
                                    .placeholder(Core.bundle.get("feature.god-mode.common.search"));
                        });

                text(Core.bundle.get("feature.god-mode.units.select-unit")).color(WebStyles.Colors.GHOST_FG);

                scroll().props(size(440f, 130f), prefWidth(440f), prefHeight(130f)).children(() -> {
                    dynamic(filteredUnits, units -> wrap().props(left(), gap(unit(1.5f))).children(() -> {
                        if (units != null) {
                            for (UnitType u : units) {
                                button()
                                        .style(WebStyles.filterChip())
                                        .checked(selectedUnit.map(sel -> sel == u))
                                        .onClick(() -> selectedUnit.set(u))
                                        .props(size(unit(10), unit(10)), prefWidth(unit(10)), prefHeight(unit(10)), padding(unit(1)), tooltip(u.localizedName))
                                        .children(() -> image(new TextureRegionDrawable(u.uiIcon)).props(size(unit(6.5f)), prefWidth(unit(6.5f)), prefHeight(unit(6.5f))));
                            }
                        }
                    }));
                });

                text(Core.bundle.get("feature.god-mode.items.target-team")).color(WebStyles.Colors.GHOST_FG);

                scroll().props(size(440f, 65f), prefWidth(440f), prefHeight(65f)).children(() -> {
                    wrap().props(left(), gap(unit(1.5f))).children(() -> {
                        for (Team t : Team.baseTeams) {
                            button()
                                    .style(WebStyles.filterChip())
                                    .checked(selectedTeam.map(sel -> sel == t))
                                    .onClick(() -> selectedTeam.set(t))
                                    .props(padding(unit(1.5f)))
                                    .children(() -> {
                                        row().props(gap(unit(1.5f)), center()).children(() -> {
                                            image(Tex.whiteui).props(size(unit(3)), prefWidth(unit(3)), prefHeight(unit(3))).color(t.color);
                                            text(t.localized()).color(t.color);
                                        });
                                    });
                        }
                    });
                });

                wrap().props(gap(unit(2)), center()).children(() -> {
                    row().props(gap(unit(1.5f)), center()).children(() -> {
                        text(Core.bundle.get("feature.god-mode.units.count") + ": ").color(WebStyles.Colors.GHOST_FG);

                        row()
                                .props(padding(unit(1)), Values.rounded(unit(2), WebStyles.Colors.SECONDARY_BG), Values.border(1.5f, WebStyles.Colors.BORDER_INPUT))
                                .children(() -> {
                                    textField(countString)
                                            .props(width(unit(18)), prefWidth(unit(18)), height(unit(8)), prefHeight(unit(8)))
                                            .style(WebStyles.clearInput());
                                });
                    });

                    wrap().props(gap(unit(1.5f)), center()).children(() -> {
                        button(() -> addCount(1)).style(WebStyles.outline()).props(padding(unit(1)), size(unit(10), unit(7)), prefWidth(unit(10)), prefHeight(unit(7)))
                                .children(() -> text("+1"));
                        button(() -> addCount(5)).style(WebStyles.outline()).props(padding(unit(1)), size(unit(10), unit(7)), prefWidth(unit(10)), prefHeight(unit(7)))
                                .children(() -> text("+5"));
                        button(() -> addCount(10)).style(WebStyles.outline()).props(padding(unit(1)), size(unit(11), unit(7)), prefWidth(unit(11)), prefHeight(unit(7)))
                                .children(() -> text("+10"));
                        button(() -> addCount(50)).style(WebStyles.outline()).props(padding(unit(1)), size(unit(11), unit(7)), prefWidth(unit(11)), prefHeight(unit(7)))
                                .children(() -> text("+50"));
                    });
                });

                row().props(growX(), gap(unit(2)), center()).children(() -> {
                    text(posLabel).color(WebStyles.Colors.GHOST_FG);
                    button()
                            .style(WebStyles.secondary())
                            .props(padding(unit(1.5f)))
                            .onClick(() -> MapPositionPicker.pick(this::hide, (x, y) -> {
                                posX.set(x);
                                posY.set(y);
                                show();
                            }))
                            .children(() -> text(Core.bundle.get("feature.god-mode.units.select-position")));
                });

                row().props(growX(), gap(unit(2))).children(() -> {
                    button()
                            .style(WebStyles.primary())
                            .props(growX(), padding(unit(2)))
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
                            .props(growX(), padding(unit(2)))
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

    private static boolean isSpawnable(UnitType unit) {
        if (unit == null || unit.internal || unit.isHidden()) {
            return false;
        }

        return unit.constructor != null && !(unit.constructor.get() instanceof BlockUnitc);
    }
}