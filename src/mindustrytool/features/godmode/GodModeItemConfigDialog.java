package mindustrytool.features.godmode;

import arc.scene.ui.Slider;
import arc.scene.ui.TextField;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Tex;
import mindustry.type.Item;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class GodModeItemConfigDialog extends BaseDialog {
    public GodModeItemConfigDialog(Item item, GodModeDialogs.ItemAddConsumer onAdd) {
        super("Configure Item");
        addCloseButton();

        cont.table(t -> {
            int[] amount = { 1000 };
            Team[] selectedTeam = { Vars.player.team() };

            t.table(ctrl -> {
                ctrl.defaults().pad(5);
                ctrl.button(b -> {
                    b.image(Tex.whiteui).color(selectedTeam[0].color).size(24).padRight(5);
                    b.label(() -> selectedTeam[0].name);
                }, () -> {
                    new GodModeTeamSelectionDialog(team -> selectedTeam[0] = team).show();
                }).height(50).growX().row();

                TextField field = new TextField(String.valueOf(amount[0]));
                field.setFilter((f, c) -> Character.isDigit(c) || c == '-');
                ctrl.add(field).growX().row();

                Slider slider = new Slider(-50000, 50000, 100, false);
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

            buttons.button("Confirm", Styles.togglet, () -> {
                onAdd.accept(item, amount[0], selectedTeam[0]);
                remove();
            }).pad(5).maxWidth(300);

        }).growX().maxWidth(800);
    }
}
