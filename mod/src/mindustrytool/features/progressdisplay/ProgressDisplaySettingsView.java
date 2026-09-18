package mindustrytool.features.progressdisplay;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitAssembler;
import mindustry.world.blocks.units.UnitFactory;
import solim.core.BaseComponent;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Declarative Solim settings view for Progress Display options: grouped
 * per-block checkboxes (with block icons) plus global zoom, opacity, and scale
 * sliders.
 */
public class ProgressDisplaySettingsView extends BaseComponent {

    private static final float MAX_CONTENT_WIDTH = 760f;

    private final ProgressDisplayFeature feature;

    public ProgressDisplaySettingsView(ProgressDisplayFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<Float> contentWidth = dvw(90f).map(w -> Math.min(w, MAX_CONTENT_WIDTH));

        Readable<String> zoomText = feature.zoomThresholdConfig.signal().map(v -> {
            float value = v != null ? v : 0.5f;
            return value <= 0.01f
                    ? Core.bundle.get("feature.progress-display.settings.off")
                    : String.format("%.1fx", value);
        });

        Readable<String> opacityText = feature.opacityConfig.signal()
                .map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100));

        Readable<String> scaleText = feature.scaleConfig.signal()
                .map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100));

        return column().grow().center().maxWidth(contentWidth).padding(unit(3)).children(() -> {
            scroll().center().grow().children(() -> {
                column().growX().maxWidth(contentWidth).gap(unit(2)).padding(unit(2)).children(() -> {

                    sliderRow(Core.bundle.get("feature.progress-display.settings.zoom-threshold"),
                            feature.zoomThresholdConfig.signal(), 0f, 2f, 0.1f, zoomText);

                    sliderRow(Core.bundle.get("feature.progress-display.settings.opacity"),
                            feature.opacityConfig.signal(), 0f, 1f, 0.05f, opacityText);

                    sliderRow(Core.bundle.get("feature.progress-display.settings.scale"),
                            feature.scaleConfig.signal(), 0.5f, 1.5f, 0.1f, scaleText);

                    divider();

                    blockSection(Core.bundle.get("feature.progress-display.settings.section.unit-factories"),
                            UnitFactory.class);
                    blockSection(Core.bundle.get("feature.progress-display.settings.section.reconstructors"),
                            Reconstructor.class);
                    blockSection(Core.bundle.get("feature.progress-display.settings.section.unit-assemblers"),
                            UnitAssembler.class);
                    blockSection(Core.bundle.get("feature.progress-display.settings.section.crafters"),
                            GenericCrafter.class);

                });
            });
        }).element();
    }

    private void sliderRow(String label, Signal<Float> signal, float min, float max, float step,
            Readable<String> valueText) {
        row().growX().gap(unit(2)).paddingX(unit(1)).children(() -> {
            text(label).left();
            spacer();
            slider(signal, min, max, step).width(unit(48));
            row().width(unit(14)).children(() -> text(valueText));
        });
    }

    private <T extends Block> void blockSection(String title, Class<T> type) {
        Seq<Block> blocks = Vars.content.blocks();

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

        column().growX().gap(unit(1)).children(() -> {
            text(title).left().color(Color.lightGray);

            wrap().growX().gap(unit(4)).paddingLeft(unit(1)).children(() -> {
                for (int i = 0; i < blocks.size; i++) {
                    Block block = blocks.get(i);
                    if (type.isInstance(block)) {
                        new BlockToggle(feature, block);
                    }
                }
            });
        });
    }

    private static final class BlockToggle extends BaseComponent {

        private final ProgressDisplayFeature feature;
        private final Block block;

        BlockToggle(ProgressDisplayFeature feature, Block block) {
            this.feature = feature;
            this.block = block;
        }

        @Override
        protected Element build() {
            TextureRegion region = block.uiIcon != null ? block.uiIcon : block.fullIcon;

            return row().center().gap(unit(2)).children(() -> {
                icon(new TextureRegionDrawable(region != null ? region : new TextureRegion()))
                        .size(unit(5));

                checkbox(block.localizedName, feature.isBlockEnabled(block),
                        value -> feature.setBlockEnabled(block, value));
            }).element();
        }
    }
}
