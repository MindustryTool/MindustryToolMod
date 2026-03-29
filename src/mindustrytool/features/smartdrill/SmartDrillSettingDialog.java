package mindustrytool.features.smartdrill;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.Drill;

public class SmartDrillSettingDialog extends BaseDialog {

    public SmartDrillSettingDialog() {
        super(Core.bundle.get("feature.smart-drill.settings", "Smart Drill Settings"));
        addCloseButton();
        setup();
    }

    private void setup() {
        cont.clear();
        cont.pane(t -> {
            t.margin(10);
            for (Block block : Vars.content.blocks()) {
                if (block instanceof Drill || block instanceof BeamDrill) {
                    buildDrillSetting(t, block);
                }
            }
        }).grow();
    }

    private void buildDrillSetting(Table table, Block drill) {
        table.table(Styles.black6, t -> {
            t.top().left().margin(10);

            // Header
            t.table(header -> {
                header.left();
                header.image(drill.uiIcon).size(32).padRight(10);
                header.add(drill.localizedName).growX().left();
            }).growX().row();

            // Divider
            t.image().color(Color.gray).growX().height(2f).padTop(5).padBottom(5).row();

            // Configs
            t.table(configs -> {
                configs.left();

                configs.add(Core.bundle.get("feature.smart-drill.max-tiles", "Max Tiles")).left().padRight(10);
                configs.label(() -> String.valueOf(SmartDrillFeature.getMaxTiles(drill))).padRight(10).width(40);

                configs.slider(20, 200, 1, SmartDrillFeature.getMaxTiles(drill), slider -> {
                    Core.settings.put("mindustrytool.feature.smart-drill.maxtiles." + drill.name, (int) slider);
                }).growX();
                configs.row();
            }).growX();
        }).width(Math.min(Core.graphics.getWidth() * 0.9f, 450f)).pad(5).row();
    }
}
