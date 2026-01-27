package mindustrytool.features.godmode;

import arc.Core;
import arc.scene.ui.Slider;
import arc.scene.ui.TextField;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Player;
import mindustry.gen.Tex;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GodModeDialogs {

    public interface UnitSpawnConsumer {
        void accept(UnitType unit, int amount, Team team, float x, float y);
    }

    public interface CorePlaceConsumer {
        void accept(Block core, Team team, float x, float y);
    }

    public interface ItemAddConsumer {
        void accept(Item item, int amount, Team team);
    }

    public static void showTeamDialog(BiConsumer<Player, Team> onSelect) {
        BaseDialog dialog = new BaseDialog("Select Player");
        dialog.addCloseButton();

        dialog.cont.table(t -> {
            t.pane(p -> {
                int i = 0;
                int size = 300;
                int cols = Math.max(1, (int) (Math.min(Core.graphics.getWidth(), 800) / (size + 10)));

                for (Player player : Groups.player) {
                    p.button(b -> {
                        b.image(Tex.whiteui).color(player.team().color).size(40).padRight(10);
                        b.add(player.name).growX().left();
                    }, () -> {
                        dialog.remove();
                        showTeamSelectionDialog(team -> onSelect.accept(player, team));
                    }).size(size, 60).pad(5);

                    if (++i % cols == 0)
                        p.row();
                }
            }).grow().maxWidth(800);
        }).grow();

        dialog.show();
    }

    private static void showTeamSelectionDialog(Consumer<Team> onSelect) {
        BaseDialog dialog = new BaseDialog("Select Team");
        dialog.addCloseButton();

        dialog.cont.table(t -> {
            int i = 0;
            int cols = Vars.mobile ? 2 : 3;
            for (Team team : Team.baseTeams) {
                t.button(b -> {
                    b.image(Tex.whiteui).color(team.color).size(40).padRight(10);
                    b.add(team.name).growX();
                }, () -> {
                    onSelect.accept(team);
                    dialog.remove();
                }).growX().pad(5);

                if (++i % cols == 0)
                    t.row();
            }
        }).maxWidth(800);

        dialog.show();
    }

    public static void showItemDialog(ItemAddConsumer onAdd) {
        BaseDialog dialog = new BaseDialog("Select Item");
        dialog.addCloseButton();

        dialog.cont.table(t -> {
            t.pane(p -> {
                int i = 0;
                int size = 200;
                int cols = Math.max(1, (int) (Math.min(Core.graphics.getWidth(), 800) / (size + 10)));

                for (Item item : Vars.content.items()) {
                    p.button(b -> {
                        b.image(item.uiIcon).size(32).padRight(5);
                        b.add(item.localizedName).growX().left();
                    }, () -> {
                        dialog.remove();
                        showItemConfigDialog(item, onAdd);
                    }).size(size, 50).pad(5);

                    if (++i % cols == 0)
                        p.row();
                }
            }).grow().maxWidth(800);
        }).grow();

        dialog.show();
    }

    private static void showItemConfigDialog(Item item, ItemAddConsumer onAdd) {
        BaseDialog dialog = new BaseDialog("Configure Item");
        dialog.addCloseButton();

        dialog.cont.table(t -> {
            int[] amount = { 1000 };
            Team[] selectedTeam = { Vars.player.team() };

            t.table(ctrl -> {
                ctrl.defaults().pad(5);
                ctrl.button(b -> {
                    b.image(Tex.whiteui).color(selectedTeam[0].color).size(24).padRight(5);
                    b.label(() -> selectedTeam[0].name);
                }, () -> {
                    showTeamSelectionDialog(team -> selectedTeam[0] = team);
                }).height(50).growX().row();

                TextField field = new TextField(String.valueOf(amount[0]));
                field.setFilter((f, c) -> Character.isDigit(c) || c == '-');
                ctrl.add(field).growX().row();

                Slider slider = new Slider(-10000, 10000, 100, false);
                slider.setValue(amount[0]);

                field.changed(() -> {
                    try {
                        int val = Integer.parseInt(field.getText());
                        amount[0] = val;
                        slider.setValue(val);
                    } catch (NumberFormatException ignored) {
                    }
                });

                slider.moved(val -> {
                    amount[0] = (int) val;
                    if (!field.hasKeyboard()) {
                        field.setText(String.valueOf((int) val));
                    }
                });

                ctrl.add(slider).growX();

            }).growX().maxWidth(800).row();

            dialog.buttons.button("Confirm", Styles.togglet, () -> {
                onAdd.accept(item, amount[0], selectedTeam[0]);
                dialog.remove();
            }).pad(5).maxWidth(300);

        }).growX().maxWidth(800);

        dialog.show();
    }

    public static void showUnitDialog(UnitSpawnConsumer onSpawn, BiConsumer<UnitType, Team> onKill) {
        BaseDialog dialog = new BaseDialog("Select Unit");

        dialog.addCloseButton();

        dialog.cont.table(t -> {
            t.pane(p -> {
                int i = 0;
                int size = 200;
                int cols = Math.max(1, (int) (Math.min(Core.graphics.getWidth() * 0.9, 1200) / (size + 10)));

                for (UnitType unit : Vars.content.units()) {
                    if (unit.isHidden()) {
                        continue;
                    }

                    p.button(u -> {
                        u.background(Tex.underline);
                        u.image(unit.uiIcon).size(32).padRight(5);
                        u.add(unit.localizedName).width(120).left().ellipsis(true);
                    }, () -> {
                        dialog.remove();
                        showUnitConfigDialog(unit, onSpawn, onKill);
                    }).pad(5);

                    if (++i % cols == 0) {
                        p.row();
                    }
                }
            }).grow().maxWidth(1200);
        }).grow().center();

        dialog.show();
    }

    private static void showUnitConfigDialog(UnitType unit, UnitSpawnConsumer onSpawn,
            BiConsumer<UnitType, Team> onKill) {

        BaseDialog dialog = new BaseDialog("Configure Unit Spawn");
        dialog.addCloseButton();

        dialog.cont.table(t -> {
            t.defaults().pad(5);
            int[] amount = { 1 };
            Team[] selectedTeam = { Vars.player.team() };

            t.table(ctrl -> {
                ctrl.defaults().pad(5);
                ctrl.button(b -> {
                    b.image(Tex.whiteui).color(selectedTeam[0].color).size(24).padRight(5);
                    b.label(() -> selectedTeam[0].name);
                }, () -> {
                    showTeamSelectionDialog(team -> selectedTeam[0] = team);
                }).height(40).growX().row();

                TextField field = new TextField(String.valueOf(amount[0]));
                field.setFilter((f, c) -> Character.isDigit(c) || c == '-');
                ctrl.add(field)
                        .growX()
                        .row();

                Slider slider = new Slider(-100, 100, 1, false);
                slider.setValue(amount[0]);

                field.changed(() -> {
                    try {
                        int val = Integer.parseInt(field.getText());
                        amount[0] = val;
                        slider.setValue(val);
                    } catch (NumberFormatException ignored) {
                    }
                });

                slider.moved(val -> {
                    amount[0] = (int) val;
                    if (!field.hasKeyboard()) {
                        field.setText(String.valueOf((int) val));
                    }
                });

                ctrl.add(slider).growX();
            })
                    .top()
                    .left()
                    .growX().row();

            dialog.buttons.button("Accept", Icon.move, () -> {
                dialog.remove();

                if (amount[0] > 0) {
                    TapListener.select((x, y) -> {
                        onSpawn.accept(unit, amount[0], selectedTeam[0], x, y);
                    });
                } else {
                    onKill.accept(unit, selectedTeam[0]);
                }
            });

        }).maxWidth(800).growX();
        dialog.show();
    }

    public static void showEffectDialog(BiConsumer<StatusEffect, Float> onApply, Consumer<StatusEffect> onClear) {
        BaseDialog dialog = new BaseDialog("Effects");
        dialog.addCloseButton();

        dialog.cont.table(t -> {
            float[] duration = { 600f };

            t.table(ctrl -> {
                ctrl.defaults().pad(5);
                ctrl.add("Duration (ticks): ");
                ctrl.field(String.valueOf(duration[0]), s -> {
                    try {
                        duration[0] = Float.parseFloat(s);
                    } catch (NumberFormatException ignored) {
                    }
                }).width(100);
            }).maxWidth(800).row();

            t.pane(p -> {
                int i = 0;
                int size = 200;
                int cols = Math.max(1, (int) (Math.min(Core.graphics.getWidth(), 800) / (size + 10)));

                for (StatusEffect effect : Vars.content.statusEffects()) {
                    p.table(e -> {
                        e.background(Tex.underline);
                        e.image(effect.uiIcon).size(32).padRight(5);
                        e.add(effect.localizedName).width(120).left().ellipsis(true);

                        e.button(Icon.add, Styles.clearNonei, () -> {
                            onApply.accept(effect, duration[0]);
                        }).size(40);

                        e.button(Icon.cancel, Styles.clearNonei, () -> {
                            onClear.accept(effect);
                        }).size(40);
                    }).pad(5);

                    if (++i % cols == 0)
                        p.row();
                }
            }).grow();
        }).growX().maxWidth(800).grow();

        dialog.show();
    }

    public static void showCoreDialog(CorePlaceConsumer onPlace) {
        BaseDialog dialog = new BaseDialog("Select Core");
        dialog.addCloseButton();

        dialog.cont.table(t -> {
            t.pane(p -> {
                int i = 0;
                int size = 200;
                int cols = Math.max(1, (int) (Math.min(Core.graphics.getWidth(), 800) / (size + 10)));

                for (Block block : Vars.content.blocks()) {
                    if (!(block instanceof CoreBlock))
                        continue;
                    if (block.isHidden())
                        continue;

                    p.button(b -> {
                        b.image(block.uiIcon).size(32).padRight(5);
                        b.add(block.localizedName).growX().left();
                    }, () -> {
                        dialog.remove();
                        showCoreConfigDialog(block, onPlace);
                    }).size(size, 50).pad(5);

                    if (++i % cols == 0)
                        p.row();
                }
            }).grow().maxWidth(800);
        }).grow();

        dialog.show();
    }

    private static void showCoreConfigDialog(Block block, CorePlaceConsumer onPlace) {
        BaseDialog dialog = new BaseDialog("Place Core");
        dialog.addCloseButton();

        dialog.cont.table(t -> {
            Team[] selectedTeam = { Vars.player.team() };

            t.button(b -> {
                b.image(Tex.whiteui).color(selectedTeam[0].color).size(24).padRight(5);
                b.label(() -> selectedTeam[0].name);
            }, () -> {
                showTeamSelectionDialog(team -> selectedTeam[0] = team);
            }).size(150, 40).pad(5).row();

            t.button("Place", Icon.hammer, () -> {
                dialog.remove();
                TapListener.select((x, y) -> {
                    onPlace.accept(block, selectedTeam[0], x, y);
                });
            }).growX().pad(5).maxWidth(300);
        }).maxWidth(800);

        dialog.show();
    }
}
