package mindustrytool.features.progressdisplay;

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
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitAssembler;
import mindustry.world.blocks.units.UnitFactory;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.Card;
import solim.reactive.Signal;

/**
 * Modern declarative Solim settings view for Progress Display options: grouped
 * per-block chip cards plus global zoom, opacity, and scale sliders.
 */
public class ProgressDisplaySettingsView extends BaseComponent {

    private static final float MAX_CONTENT_WIDTH = 500f;

    private final ProgressDisplayFeature feature;

    public ProgressDisplaySettingsView(ProgressDisplayFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().maxWidth(MAX_CONTENT_WIDTH).children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().gap(unit(2.5f)).padding(unit(2)).center().children(() -> {
                    headerCard();
                    globalSettingsCard();

                    blockCategoryCard(
                            Core.bundle != null
                                    ? Core.bundle.get("feature.progress-display.settings.section.unit-factories", "Unit Factories")
                                    : "Unit Factories",
                            Icon.units,
                            UnitFactory.class);

                    blockCategoryCard(
                            Core.bundle != null
                                    ? Core.bundle.get("feature.progress-display.settings.section.reconstructors", "Reconstructors")
                                    : "Reconstructors",
                            Icon.up,
                            Reconstructor.class);

                    blockCategoryCard(
                            Core.bundle != null
                                    ? Core.bundle.get("feature.progress-display.settings.section.unit-assemblers", "Unit Assemblers")
                                    : "Unit Assemblers",
                            Icon.wrench,
                            UnitAssembler.class);

                    blockCategoryCard(
                            Core.bundle != null
                                    ? Core.bundle.get("feature.progress-display.settings.section.crafters", "Crafters")
                                    : "Crafters",
                            Icon.crafting,
                            GenericCrafter.class);

                    actionsCard();
                });
            });
        }).element();
    }

    private Card sectionCard() {
        return card()
                .growX()
                .padding(unit(2.5f))
                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                .border(1f, WebStyles.Colors.SECTION_BORDER)
                .gap(unit(2));
    }

    private Component headerCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.crafting).size(unit(5)).color(Pal.accent);
                text(Core.bundle != null
                        ? Core.bundle.get("feature.progress-display.name", "Progress Display")
                        : "Progress Display")
                        .color(Pal.accent)
                        .growX()
                        .left();
                badge("LOGISTICS").color(WebStyles.Colors.PRIMARY);
            });

            text(Core.bundle != null
                    ? Core.bundle.get("feature.progress-display.description",
                            "Show a countdown of remaining production time above factories and crafters.")
                    : "Show a countdown of remaining production time above factories and crafters.")
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();
        });
    }

    private Component globalSettingsCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.settings).size(unit(5)).color(Pal.accent);
                text(Core.bundle != null
                        ? Core.bundle.get("feature.progress-display.settings.section.global", "Global Settings")
                        : "Global Settings")
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            // Zoom Threshold
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle != null
                        ? Core.bundle.get("feature.progress-display.settings.zoom-threshold", "Zoom Threshold")
                        : "Zoom Threshold")
                        .color(Color.white)
                        .left();
                spacer();
                slider(feature.zoomThresholdConfig.signal(), 0f, 2f, 0.1f);
                row().width(unit(14)).right().children(() -> {
                    badge(feature.zoomThresholdConfig.signal().map(v -> {
                        float val = v != null ? v : 0.5f;
                        return val <= 0.01f
                                ? (Core.bundle != null ? Core.bundle.get("feature.progress-display.settings.off", "Off") : "Off")
                                : String.format(Locale.US, "%.1fx", val);
                    })).color(WebStyles.Colors.SECONDARY);
                });
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Opacity
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle != null
                        ? Core.bundle.get("feature.progress-display.settings.opacity", "Opacity")
                        : "Opacity")
                        .color(Color.white)
                        .left();
                spacer();
                slider(feature.opacityConfig.signal(), 0f, 1f, 0.05f);
                row().width(unit(14)).right().children(() -> {
                    badge(feature.opacityConfig.signal().map(v ->
                            String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100)))
                            .color(WebStyles.Colors.SECONDARY);
                });
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Scale
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle != null
                        ? Core.bundle.get("feature.progress-display.settings.scale", "Scale")
                        : "Scale")
                        .color(Color.white)
                        .left();
                spacer();
                slider(feature.scaleConfig.signal(), 0.5f, 1.5f, 0.1f);
                row().width(unit(14)).right().children(() -> {
                    badge(feature.scaleConfig.signal().map(v ->
                            String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100)))
                            .color(WebStyles.Colors.SECONDARY);
                });
            });
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

        sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(iconDrawable).size(unit(5)).color(Pal.accent);
                text(title).color(Pal.accent).growX().left();
            });

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
        return sectionCard().children(() -> {
            button(feature::resetToDefaults)
                    .style(WebStyles.outline())
                    .growX()
                    .height(unit(11))
                    .gap(unit(2))
                    .children(() -> {
                        icon(Icon.refresh).size(unit(6));
                        text(Core.bundle != null
                                ? Core.bundle.get("feature.progress-display.settings.reset", "Reset to Defaults")
                                : "Reset to Defaults");
                    });
        });
    }

    private static final class BlockChip extends BaseComponent {

        private final ProgressDisplayFeature feature;
        private final Block block;
        private final Signal<Boolean> enabled;

        BlockChip(ProgressDisplayFeature feature, Block block) {
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
