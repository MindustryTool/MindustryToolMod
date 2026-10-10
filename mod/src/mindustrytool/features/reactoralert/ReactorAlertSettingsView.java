package mindustrytool.features.reactoralert;

import static solim.UI.*;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import mindustry.gen.Tex;
import mindustry.world.Block;
import mindustry.world.blocks.power.PowerGenerator;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;

public class ReactorAlertSettingsView extends BaseComponent {
    private final ReactorAlertFeature feature;

    public ReactorAlertSettingsView(ReactorAlertFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).padding(unit(3)).children(() -> {
                    row().growX().gap(unit(2)).center().children(() -> {
                        text(Core.bundle.get("feature.reactor-alert.settings.radius")).left();
                        spacer();
                        slider(feature.radiusConfig.signal(), 1f, 30f, 1f);
                        row().width(unit(14)).children(() -> {
                            text(feature.radiusConfig.signal()
                                    .map(v -> String.format("%.0f", v != null ? v : 10f)));
                        });
                    });

                    divider();

                    row().growX().gap(unit(2)).center().children(() -> {
                        text(Core.bundle.get("feature.reactor-alert.settings.monitored-blocks")).left();
                        spacer();
                        row().gap(unit(1)).children(() -> {
                            button(() -> feature.setAllBlocksEnabled(true))
                                    .style(WebStyles.outline())
                                    .height(unit(7))
                                    .padding(unit(1), unit(2.5f), unit(1), unit(2.5f))
                                    .children(() -> text(Core.bundle.get("feature.reactor-alert.settings.all")));

                            button(() -> feature.setAllBlocksEnabled(false))
                                    .style(WebStyles.ghost())
                                    .height(unit(7))
                                    .padding(unit(1), unit(2.5f), unit(1), unit(2.5f))
                                    .children(() -> text(Core.bundle.get("feature.reactor-alert.settings.none")));
                        });
                    });

                    for (Block block : feature.explosiveBlocks()) {
                        blockRow(block);
                    }
                });
            });
        }).element();
    }

    private void blockRow(Block block) {
        TextureRegion region = block.uiIcon != null ? block.uiIcon : block.fullIcon;
        Drawable drawable = region != null ? new TextureRegionDrawable(region) : Tex.clear;
        String label = block instanceof PowerGenerator
                ? block.localizedName + " ("
                        + Core.bundle.format("feature.reactor-alert.settings.tiles",
                                ((PowerGenerator) block).explosionRadius)
                        + ")"
                : block.localizedName;

        row().growX().gap(unit(2)).center().children(() -> {
            icon(drawable).size(unit(6));
            checkbox(label, feature.getBlockSignal(block)).growX();
        });
    }
}
