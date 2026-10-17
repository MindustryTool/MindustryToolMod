package mindustrytool.features.griefalert;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.world.Block;
import mindustry.world.blocks.power.PowerGenerator;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingKeybindCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.core.Component;

public class GriefAlertSettingsView extends Component {
    private final GriefAlertFeature feature;

    public GriefAlertSettingsView(GriefAlertFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            generalCard();
            monitoredBlocksCard();
            keybindsCard();
        });
    }

    private void keybindsCard() {
        SettingKeybindCard.forFeature(feature);
    }

    private Component headerCard() {
        return SettingCard.header(feature, "SECURITY");
    }

    private Component generalCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("feature.grief-alert.settings.section.general"),
                () -> {
                    SettingSlider.of(
                            Core.bundle.get("feature.grief-alert.settings.radius"),
                            feature.radiusConfig.signal(),
                            1f,
                            30f,
                            1f,
                            feature.radiusConfig.signal().map(v -> String.format("%.0f tiles",
                                    v != null ? v : GriefAlertFeature.DEFAULT_RADIUS)));

                    SettingSlider.of(
                            Core.bundle.get("feature.grief-alert.settings.duration"),
                            feature.durationConfig.signal(),
                            5f,
                            30f,
                            1f,
                            feature.durationConfig.signal()
                                    .map(v -> Core.bundle.format("feature.grief-alert.settings.seconds",
                                            Math.round(v != null ? v : GriefAlertFeature.DEFAULT_DURATION))));
                });
    }

    private Component monitoredBlocksCard() {
        return SettingCard.section(Icon.warning, Core.bundle.get("feature.grief-alert.settings.monitored-blocks"),
                () -> {
                    row().props(growX(), gap(unit(2)), center()).children(() -> {
                        text(Core.bundle.get("feature.grief-alert.settings.monitored-blocks")).left();
                        spacer();
                        row().props(gap(unit(1))).children(() -> {
                            button(Core.bundle.get("feature.grief-alert.settings.all"),
                                    () -> feature.setAllBlocksEnabled(true))
                                    .style(WebStyles.outlineText())
                                    .props(height(unit(8)), prefHeight(unit(8)), padding(unit(1), unit(2.5f), unit(1), unit(2.5f)));

                            button(Core.bundle.get("feature.grief-alert.settings.none"),
                                    () -> feature.setAllBlocksEnabled(false))
                                    .style(WebStyles.ghostText())
                                    .props(height(unit(8)), prefHeight(unit(8)), padding(unit(1), unit(2.5f), unit(1), unit(2.5f)));
                        });
                    });

                    divider().color(WebStyles.Colors.SECTION_BORDER);

                    for (Block block : feature.explosiveBlocks()) {
                        blockRow(block);
                    }
                });
    }

    private void blockRow(Block block) {
        TextureRegion region = block.uiIcon != null ? block.uiIcon : block.fullIcon;
        Drawable drawable = region != null ? new TextureRegionDrawable(region) : Tex.clear;
        String label = block instanceof PowerGenerator
                ? block.localizedName + " ("
                        + Core.bundle.format("feature.grief-alert.settings.tiles",
                                ((PowerGenerator) block).explosionRadius)
                        + ")"
                : block.localizedName;

        row().props(growX(), gap(unit(2)), center()).children(() -> {
            icon(drawable).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
            checkbox(label, feature.getBlockSignal(block)).props(growX());
        });
    }
}
