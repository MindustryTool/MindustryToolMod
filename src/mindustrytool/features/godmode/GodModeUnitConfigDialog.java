package mindustrytool.features.godmode;

import arc.scene.ui.Slider;
import arc.scene.ui.TextField;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.type.UnitType;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.services.TapListener;

import java.util.function.BiConsumer;

public class GodModeUnitConfigDialog extends BaseDialog {
    public GodModeUnitConfigDialog(UnitType unit, GodModeDialogs.UnitSpawnConsumer onSpawn,
            BiConsumer<UnitType, Team> onKill) {
        super("Configure Unit Spawn");
        addCloseButton();

        cont.table(t -> {
            t.defaults().pad(5);
            int[] amount = { 1 };
            Team[] selectedTeam = { Vars.player.team() };

            t.table(ctrl -> {
                ctrl.defaults().pad(5);
                ctrl.button(b -> {
                    b.image(Tex.whiteui).color(selectedTeam[0].color).size(24).padRight(5);
                    b.label(() -> selectedTeam[0].name);
                }, () -> {
                    new GodModeTeamSelectionDialog(team -> selectedTeam[0] = team).show();
                }).height(40).growX().row();

                TextField field = new TextField(String.valueOf(amount[0]));
                field.setFilter((f, c) -> Character.isDigit(c) || c == '-');
                ctrl.add(field)
                        .growX()
                        .row();

                Slider slider = new Slider(-1000, 1000, 1, false);
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

            buttons.button("Accept", Icon.move, () -> {
                remove();

                if (amount[0] > 0) {
                    TapListener.getInstance().select((x, y) -> {
                        onSpawn.accept(unit, amount[0], selectedTeam[0], x, y);
                    });
                } else {
                    onKill.accept(unit, selectedTeam[0]);
                }
            });

        }).maxWidth(800).growX();
    }
}
