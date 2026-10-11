package mindustrytool.features.blockstats;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import java.util.Locale;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitAssembler;
import mindustry.world.blocks.units.UnitFactory;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.reactive.Signal;

/**
 * Modern declarative Solim settings view for Block Stats Display options: grouped
 * per-block chip cards plus global zoom, opacity, and scale sliders.
 */
public class BlockStatsDisplaySettingsView extends BaseComponent {

    private final BlockStatsDisplayFeature feature;

    public BlockStatsDisplaySettingsView(BlockStatsDisplayFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            globalSettingsCard();

            blockCategoryCard(
                    Core.bundle.get("feature.blockstats-display.settings.section.unit-factories"),
                    Icon.units,
                    UnitFactory.class);

            blockCategoryCard(
                    Core.bundle.get("feature.blockstats-display.settings.section.reconstructors"),
                    Icon.up,
                    Reconstructor.class);

            blockCategoryCard(
                    Core.bundle.get("feature.blockstats-display.settings.section.unit-assemblers"),
                    Icon.wrench,
                    UnitAssembler.class);

            blockCategoryCard(
                    Core.bundle.get("feature.blockstats-display.settings.section.crafters"),
                    Icon.crafting,
                    GenericCrafter.class);

            actionsCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, "LOGISTICS");
    }

    private Component globalSettingsCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("feature.blockstats-display.settings.section.global"), () -> {
            // Zoom Threshold
            SettingSlider.of(
                    Core.bundle.get("feature.blockstats-display.settings.zoom-threshold"),
                    feature.zoomThresholdConfig.signal(),
                    0f,
                    2f,
                    0.1f,
                    feature.zoomThresholdConfig.signal().map(v -> {
                        float val = v != null ? v : 0.5f;
                        return val <= 0.01f
                                ? Core.bundle.get("feature.blockstats-display.settings.off")
                                : String.format(Locale.US, "%.1fx", val);
                    })
            );

            // Opacity
            SettingSlider.of(
                    Core.bundle.get("feature.blockstats-display.settings.opacity"),
                    feature.opacityConfig.signal(),
                    0f,
                    1f,
                    0.05f,
                    feature.opacityConfig.signal().map(v ->
                            String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100))
            );

            // Scale
            SettingSlider.of(
                    Core.bundle.get("feature.blockstats-display.settings.scale"),
                    feature.scaleConfig.signal(),
                    0.5f,
                    1.5f,
                    0.1f,
                    feature.scaleConfig.signal().map(v ->
                            String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100))
            );
        });
    }

    private <T extends Block> void blockCategoryCard(String title, Drawable iconDrawable, Class<T> type) {
        Seq<Block> blocks = Vars.content != null ? Vars.content.blocks() : null;
        if (blocks == null) {
            return;
        }

        boolean present = false;
        for (int i = 0; i < blocks.size; i++) {
            if (type.isInstance(blocks.get(i))) {
                present = true;
                break;
            }
        }
        if (!present) {
            return;
        }

        SettingCard.section(iconDrawable, title, () -> {
            wrap().growX().gap(unit(1.5f)).children(() -> {
                for (int i = 0; i < blocks.size; i++) {
                    Block block = blocks.get(i);
                    if (type.isInstance(block)) {
                        new BlockChip(feature, block);
                    }
                }
            });
        });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(feature::resetToDefaults)
                    .style(WebStyles.outline())
                    .growX()
                    .height(unit(11))
                    .gap(unit(2))
                    .children(() -> {
                        icon(Icon.refresh).size(unit(6));
                        text(Core.bundle.get("feature.blockstats-display.settings.reset"));
                    });
        });
    }

    private static final class BlockChip extends BaseComponent {

        private final BlockStatsDisplayFeature feature;
        private final Block block;
        private final Signal<Boolean> enabled;

        BlockChip(BlockStatsDisplayFeature feature, Block block) {
            this.feature = feature;
            this.block = block;
            this.enabled = Signal.of(feature.isBlockEnabled(block));
        }

        @Override
        protected Element build() {
            TextureRegion region = block.uiIcon != null ? block.uiIcon : block.fullIcon;
            Drawable drawable = region != null ? new TextureRegionDrawable(region) : Tex.clear;

            return button()
                    .style(WebStyles.filterChip())
                    .size(unit(11))
                    .padding(unit(1))
                    .tooltip(block.localizedName)
                    .checked(enabled)
                    .onClick(() -> {
                        boolean next = !Boolean.TRUE.equals(enabled.peek());
                        enabled.set(next);
                        feature.setBlockEnabled(block, next);
                    })
                    .children(() -> {
                        icon(drawable)
                                .size(unit(6))
                                .color(enabled.map(en ->
                                        Boolean.TRUE.equals(en) ? Color.white : new Color(1f, 1f, 1f, 0.35f)
                                       ));
                    }).element();
        }
    }
}
