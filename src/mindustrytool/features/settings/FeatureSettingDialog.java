package mindustrytool.features.settings;

import arc.graphics.Color;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
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

            int cols = Math.max(1, (int) (arc.Core.graphics.getWidth() / Scl.scl() * 0.9f / 340f));
            float cardWidth = ((float) arc.Core.graphics.getWidth() / Scl.scl() * 0.9f) / cols;

            int i = 0;

            for (Feature feature : FeatureManager.getInstance().getFeatures()) {
                buildFeatureCard(table, feature, cardWidth);
                if (++i % cols == 0) {
                    table.row();
                }
            }
        }).grow();
    }

    private void buildFeatureCard(Table parent, Feature feature, float cardWidth) {
        boolean enabled = FeatureManager.getInstance().isEnabled(feature);
        var metadata = feature.getMetadata();

        parent.table(Styles.black8, card -> {
            card.top().left();

            // Status border
            card.image().color(enabled ? Color.green : Color.red).growX().height(4f).row();

            card.table(c -> {
                c.top().left().margin(12);

                // Header
                c.table(header -> {
                    header.left();
                    header.add(metadata.name()).style(Styles.defaultLabel).color(Color.white).growX().left();

                    // Settings button
                    if (feature.setting().isPresent()) {
                        header.button(Icon.settings, Styles.clearNonei,
                                () -> feature.setting().ifPresent(dialog -> dialog.show())).size(32)
                                .padLeft(8);
                    }

                    // Status icon (visual only)
                    header.image(enabled ? Icon.eyeSmall : Icon.eyeOffSmall).size(24).padLeft(4)
                            .color(enabled ? Color.white : Color.gray);
                }).growX().row();

                // Description
                c.add(metadata.description())
                        .color(Color.lightGray)
                        .fontScale(0.9f)
                        .wrap()
                        .growX()
                        .padTop(10)
                        .row();

                // Spacer to push status to bottom
                c.add().growY().row();

                // Status Footer
                c.add(enabled ? "Enabled" : "Disabled")
                        .color(enabled ? Color.green : Color.red)
                        .left();
            }).grow();

        })
                .growX()
                .minWidth(cardWidth)
                .height(180f).pad(10f).get().clicked(() -> {
                    FeatureManager.getInstance().setEnabled(feature, !enabled);
                    rebuild();
                });
    }
}
