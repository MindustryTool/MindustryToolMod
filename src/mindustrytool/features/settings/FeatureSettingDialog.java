package mindustrytool.features.settings;

import arc.graphics.Color;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Feature;
import mindustrytool.FeatureManager;

public class FeatureSettingDialog extends BaseDialog {

    public FeatureSettingDialog() {
        super("Feature Settings");
        addCloseButton();
        
        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        cont.pane(table -> {
            table.top();
            for (Feature feature : FeatureManager.getInstance().getFeatures()) {
                buildFeatureCard(table, feature);
            }
        }).grow();
    }

    private void buildFeatureCard(Table parent, Feature feature) {
        boolean enabled = FeatureManager.getInstance().isEnabled(feature);
        
        parent.table(Tex.button, card -> {
            card.left();
            // Status indicator
            card.image(enabled ? Icon.ok : Icon.cancel)
                .color(enabled ? Color.green : Color.red)
                .padRight(10).size(32);
            
            // Name and description
            card.table(info -> {
                info.left();
                info.add(feature.getMetadata().name()).style(Styles.outlineLabel).left().row();
                info.add(feature.getMetadata().description()).fontScale(0.8f).color(Color.lightGray).left().wrap().width(300f);
            }).growX().padRight(10);

            // Settings button if available
            Dialog settings = feature.setting();
            if (settings != null) {
                card.button(Icon.settings, Styles.clearNonei, () -> {
                    settings.show();
                }).size(48).padRight(5);
            }
            
            // Toggle action
            card.clicked(() -> {
                FeatureManager.getInstance().setEnabled(feature, !enabled);
                rebuild();
            });

        }).growX().pad(5).margin(10).row();
    }
}
