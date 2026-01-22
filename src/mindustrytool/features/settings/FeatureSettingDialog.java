package mindustrytool.features.settings;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.util.Scaling;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;

public class FeatureSettingDialog extends BaseDialog {

    public FeatureSettingDialog() {
        super("Feature");
        addCloseButton();

        buttons.button("@feature.report-bug", Icon.infoCircle, () -> {
            Core.app.openURI(Config.DISCORD_INVITE_URL);
        });

        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        cont.pane(table -> {
            table.top().left();

            table.add("@feature").padLeft(10).top().left().row();

            int cols = Math.max(1, (int) (arc.Core.graphics.getWidth() / Scl.scl() * 0.9f / 340f));
            float cardWidth = ((float) arc.Core.graphics.getWidth() / Scl.scl() * 0.9f) / cols;

            int i = 0;

            for (Feature feature : FeatureManager.getInstance().getEnableds().select(f -> f.dialog().isPresent())) {
                buildFeatureButton(table, feature, cardWidth);
                if (++i % cols == 0) {
                    table.row();
                }
            }

            table.row();
            table.image().color(Color.gray).growX().height(4f)
                    .colspan(cols)
                    .pad(10)
                    .row();

            table.add("@settings").padLeft(10).top().left().row();

            i = 0;

            for (Feature feature : FeatureManager.getInstance().getFeatures()) {
                buildFeatureCard(table, feature, cardWidth);
                if (++i % cols == 0) {
                    table.row();
                }
            }
        }).grow();
    }

    private void buildFeatureButton(Table parent, Feature feature, float cardWidth) {
        var metadata = feature.getMetadata();

        parent.table(Tex.button, card -> {
            card.top().left();

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
                c.add().growX();

                c.button(Icon.linkSmall, () -> {
                    feature.dialog().get().show();
                });

            }).grow();

        })
                .growX()
                .minWidth(cardWidth)
                .height(180f).pad(10f).get().clicked(() -> {
                    feature.dialog().get().show();
                });
    }

    private void buildFeatureCard(Table parent, Feature feature, float cardWidth) {
        boolean enabled = FeatureManager.getInstance().isEnabled(feature);
        var metadata = feature.getMetadata();

        parent.table(Styles.black6, card -> {
            card.top().left();

            // Status border
            card.image().color(enabled ? Color.green : Color.red).growX().height(4f).row();

            card.table(c -> {
                c.top().left().margin(12);

                // Header
                c.table(header -> {
                    header.left();
                    header.image(metadata.icon()).scaling(Scaling.fill).size(24).padRight(8);
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
                c.add(enabled ? "@enabled" : "@disabled")
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
