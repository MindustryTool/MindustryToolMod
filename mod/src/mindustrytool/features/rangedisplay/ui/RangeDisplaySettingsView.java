package mindustrytool.features.rangedisplay.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import java.util.Locale;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.world.Block;
import mindustrytool.components.WebStyles;
import mindustrytool.features.rangedisplay.RangeDisplayFeature;
import solim.core.BaseComponent;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Declarative Solim settings view for Range Display options.
 */
public class RangeDisplaySettingsView extends BaseComponent {

    private final RangeDisplayFeature feature;

    public RangeDisplaySettingsView(RangeDisplayFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<String> opacityText = feature.opacityConfig.signal()
                .map(v -> Math.round((v != null ? v : 1f) * 100) + "%");
        Readable<String> strokeWidthText = feature.strokeWidthConfig.signal()
                .map(v -> String.format(Locale.US, "%.1fx", v != null ? v : 1f));
        Readable<String> proximityText = feature.proximityRadiusConfig.signal()
                .map(v -> Math.round(v != null ? v : 30f) + " tiles");

        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    // Opacity Slider
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.range-display.settings.opacity")).left();
                        spacer();
                        slider(feature.opacityConfig.signal(), 0.1f, 1.0f, 0.05f);
                        row().width(unit(12)).children(() -> {
                            text(opacityText);
                        });
                    });

                    // Stroke Width Slider
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.range-display.settings.stroke-width")).left();
                        spacer();
                        slider(feature.strokeWidthConfig.signal(), 1.0f, 3.0f, 0.5f);
                        row().width(unit(12)).children(() -> {
                            text(strokeWidthText);
                        });
                    });

                    divider();

                    // Hover-Only Mode Toggle
                    checkbox(Core.bundle.get("feature.range-display.settings.hover-only"),
                            feature.hoverOnlyConfig.signal()).growX();

                    // Dashed Lines Toggle
                    checkbox(Core.bundle.get("feature.range-display.settings.dashed"),
                            feature.dashedConfig.signal()).growX();

                    divider();

                    // --- Filters Section ---
                    row().growX().children(() -> {
                        text(Core.bundle.get("feature.range-display.settings.section.filters")).left();
                    });

                    // Proximity Filter
                    checkbox(Core.bundle.get("feature.range-display.settings.proximity-filter"),
                            feature.proximityFilterConfig.signal()).growX();

                    // Proximity Radius Slider
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.range-display.settings.proximity-radius")).left();
                        spacer();
                        slider(feature.proximityRadiusConfig.signal(), 10f, 60f, 5f);
                        row().width(unit(14)).children(() -> {
                            text(proximityText);
                        });
                    });

                    // Turret Target & Ammo Filters
                    checkbox(Core.bundle.get("feature.range-display.settings.filter-target-air"),
                            feature.filterTargetAirConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.range-display.settings.filter-target-ground"),
                            feature.filterTargetGroundConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.range-display.settings.only-with-ammo"),
                            feature.onlyWithAmmoConfig.signal()).growX();

                    divider();

                    // Player Unit Range
                    checkbox(Core.bundle.get("feature.range-display.settings.draw-player-unit"),
                            feature.drawUnitRangePlayerConfig.signal()).growX();

                    // Turret Ranges
                    checkbox(Core.bundle.get("feature.range-display.settings.draw-ally-turrets"),
                            feature.drawTurretRangeAllyConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-enemy-turrets"),
                            feature.drawTurretRangeEnemyConfig.signal()).growX();

                    divider();

                    // Unit Ranges
                    checkbox(Core.bundle.get("feature.range-display.settings.draw-ally-units"),
                            feature.drawUnitRangeAllyConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-enemy-units"),
                            feature.drawUnitRangeEnemyConfig.signal()).growX();

                    divider();

                    // Support Block Ranges
                    checkbox(Core.bundle.get("feature.range-display.settings.draw-ally-blocks"),
                            feature.drawBlockRangeAllyConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-enemy-blocks"),
                            feature.drawBlockRangeEnemyConfig.signal()).growX();

                    divider();

                    // Spawner Drop Zones
                    checkbox(Core.bundle.get("feature.range-display.settings.draw-spawners"),
                            feature.drawSpawnerRangeConfig.signal()).growX();

                    // --- Turrets Granular Toggles ---
                    divider();
                    row().growX().gap(unit(2)).center().children(() -> {
                        text(Core.bundle.get("feature.range-display.settings.section.turrets")).left();
                        spacer();
                        row().gap(unit(1)).children(() -> {
                            button(() -> feature.setCategoryEnabled(true, true))
                                    .style(WebStyles.outline())
                                    .height(unit(7))
                                    .padding(unit(1), unit(2.5f), unit(1), unit(2.5f))
                                    .children(() -> text(Core.bundle.get("feature.range-display.settings.all")));

                            button(() -> feature.setCategoryEnabled(true, false))
                                    .style(WebStyles.ghost())
                                    .height(unit(7))
                                    .padding(unit(1), unit(2.5f), unit(1), unit(2.5f))
                                    .children(() -> text(Core.bundle.get("feature.range-display.settings.none")));
                        });
                    });

                    wrap().growX().left().gap(unit(1.5f)).children(() -> {
                        if (Vars.content != null && Vars.content.blocks() != null) {
                            for (Block block : Vars.content.blocks()) {
                                if (block != null && feature.isTurretBlock(block)) {
                                    blockChip(block);
                                }
                            }
                        }
                    });

                    // --- Support Blocks Granular Toggles ---
                    divider();
                    row().growX().gap(unit(2)).center().children(() -> {
                        text(Core.bundle.get("feature.range-display.settings.section.support-blocks")).left();
                        spacer();
                        row().gap(unit(1)).children(() -> {
                            button(() -> feature.setCategoryEnabled(false, true))
                                    .style(WebStyles.outline())
                                    .height(unit(7))
                                    .padding(unit(1), unit(2.5f), unit(1), unit(2.5f))
                                    .children(() -> text(Core.bundle.get("feature.range-display.settings.all")));

                            button(() -> feature.setCategoryEnabled(false, false))
                                    .style(WebStyles.ghost())
                                    .height(unit(7))
                                    .padding(unit(1), unit(2.5f), unit(1), unit(2.5f))
                                    .children(() -> text(Core.bundle.get("feature.range-display.settings.none")));
                        });
                    });

                    wrap().growX().left().gap(unit(1.5f)).children(() -> {
                        if (Vars.content != null && Vars.content.blocks() != null) {
                            for (Block block : Vars.content.blocks()) {
                                if (block != null && feature.isSupportBlock(block)) {
                                    blockChip(block);
                                }
                            }
                        }
                    });
                });
            });
        }).element();
    }

    private void blockChip(Block block) {
        Signal<Boolean> signal = feature.getBlockSignal(block);
        TextureRegion region = block.uiIcon != null ? block.uiIcon : block.fullIcon;
        Drawable drawable = region != null ? new TextureRegionDrawable(region) : Tex.clear;

        button()
                .style(WebStyles.filterChip())
                .size(unit(11))
                .padding(unit(1))
                .tooltip(block.localizedName)
                .checked(signal)
                .onClick(() -> feature.setBlockEnabled(block, !feature.isBlockEnabled(block)))
                .children(() -> {
                    icon(drawable)
                            .size(unit(6))
                            .color(signal.map(enabled ->
                                    Boolean.TRUE.equals(enabled) ? Color.white : new Color(1f, 1f, 1f, 0.35f)
                            ));
                });
    }
}
