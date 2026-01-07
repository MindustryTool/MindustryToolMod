package mindustrytool.features.settings;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;

public class FeatureSettingDialog extends BaseDialog {

    public FeatureSettingDialog() {
        super("Feature Settings");
        addCloseButton();

        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        cont.pane(table -> {
            table.top().left();

            float cardWidth = 340f;
            int cols = Math.max(1, (int) (arc.Core.graphics.getWidth() / cardWidth));
            int i = 0;

            for (Feature feature : FeatureManager.getInstance().getFeatures()) {
                buildFeatureCard(table, feature);
                if (++i % cols == 0) {
                    table.row();
                }
            }
        }).grow();
    }

    private void buildFeatureCard(Table parent, Feature feature) {
        boolean enabled = FeatureManager.getInstance().isEnabled(feature);
        var metadata = feature.getMetadata();

        parent.table(Tex.button, card -> {
            card.top().left().margin(12);

            // Header
            card.table(header -> {
                header.left();
                header.add(metadata.name()).style(Styles.defaultLabel).color(Color.white).growX().left();

                // Settings button
                if (feature.setting().isPresent()) {
                    header.button(Icon.settings, Styles.clearNonei,
                            () -> feature.setting().ifPresent(dialog -> dialog.show())).size(32);
                }

                // Status icon (visual only)
                header.image(enabled ? Icon.eyeSmall : Icon.eyeOffSmall).size(24).padLeft(4)
                        .color(enabled ? Color.white : Color.gray);
            }).growX().row();

            // Description
            card.add(metadata.description())
                    .color(Color.lightGray)
                    .fontScale(0.9f)
                    .wrap()
                    .growX()
                    .padTop(10)
                    .row();

            // Spacer to push status to bottom
            card.add().growY().row();

            // Status Footer
            card.add(enabled ? "Enabled" : "Disabled")
                    .color(enabled ? Color.green : Color.red)
                    .left();

        }).size(320f, 180f).pad(10f).get().clicked(() -> {
            FeatureManager.getInstance().setEnabled(feature, !enabled);
            rebuild();
        });
    }
}
