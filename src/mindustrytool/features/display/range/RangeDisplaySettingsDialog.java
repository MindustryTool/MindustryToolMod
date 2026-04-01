package mindustrytool.features.display.range;

import arc.Core;
import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class RangeDisplaySettingsDialog extends BaseDialog {
    public RangeDisplaySettingsDialog() {
        super("@range-display.settings.title");
        name = "rangeDisplaySettingDialog";
        addCloseButton();

        Table container = cont;
        container.defaults().left().pad(5);

        Slider opacitySlider = new Slider(0f, 1f, 0.05f, false);
        opacitySlider.setValue(RangeDisplayConfig.opacity);

        Label opacityValue = new Label(
                String.format("%.0f%%", RangeDisplayConfig.opacity * 100),
                Styles.outlineLabel);
        opacityValue.setColor(Color.lightGray);

        Table opacityContent = new Table();
        opacityContent.touchable = Touchable.disabled;
        opacityContent.margin(3f, 33f, 3f, 33f);
        opacityContent.add("@opacity", Styles.outlineLabel).left().growX();
        opacityContent.add(opacityValue).padLeft(10f).right();

        opacitySlider.changed(() -> {
            RangeDisplayConfig.opacity = opacitySlider.getValue();
            opacityValue.setText(String.format("%.0f%%", RangeDisplayConfig.opacity * 100));
            RangeDisplayConfig.save();
        });

        float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);
        container.stack(opacitySlider, opacityContent).width(width).left().padTop(4f).row();

        addCheck(container, "@range-display.draw-ally-block-range", RangeDisplayConfig.drawBlockRangeAlly, v -> {
            RangeDisplayConfig.drawBlockRangeAlly = v;
            RangeDisplayConfig.save();
        });

        if (Vars.mobile) {
            container.row();
        }

        addCheck(container, "@range-display.draw-enemy-block-range", RangeDisplayConfig.drawBlockRangeEnemy, v -> {
            RangeDisplayConfig.drawBlockRangeEnemy = v;
            RangeDisplayConfig.save();
        });

        container.row();

        addCheck(container, "@range-display.draw-ally-turret-range", RangeDisplayConfig.drawTurretRangeAlly, v -> {
            RangeDisplayConfig.drawTurretRangeAlly = v;
            RangeDisplayConfig.save();
        });

        if (Vars.mobile) {
            container.row();
        }

        addCheck(container, "@range-display.draw-enemy-turret-range", RangeDisplayConfig.drawTurretRangeEnemy, v -> {
            RangeDisplayConfig.drawTurretRangeEnemy = v;
            RangeDisplayConfig.save();
        });

        container.row();

        addCheck(container, "@range-display.draw-ally-unit-range", RangeDisplayConfig.drawUnitRangeAlly, v -> {
            RangeDisplayConfig.drawUnitRangeAlly = v;
            RangeDisplayConfig.save();
        });

        if (Vars.mobile) {
            container.row();
        }

        addCheck(container, "@range-display.draw-enemy-unit-range", RangeDisplayConfig.drawUnitRangeEnemy, v -> {
            RangeDisplayConfig.drawUnitRangeEnemy = v;
            RangeDisplayConfig.save();
        });

        container.row();

        addCheck(container, "@range-display.draw-player-range", RangeDisplayConfig.drawPlayerRange, v -> {
            RangeDisplayConfig.drawPlayerRange = v;
            RangeDisplayConfig.save();
        });

        container.row();

        addCheck(container, "@range-display.draw-spawner-range", RangeDisplayConfig.drawSpawnerRange, v -> {
            RangeDisplayConfig.drawSpawnerRange = v;
            RangeDisplayConfig.save();
        });

        if (Vars.mobile) {
            container.row();
        }
    }

    private void addCheck(Table table, String title, boolean checked, arc.func.Boolc changed) {
        table.check(title, checked, changed).left();
    }
}
