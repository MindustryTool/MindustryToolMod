package mindustrytool.features.rangedisplay.ui;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustrytool.features.rangedisplay.RangeDisplayFeature;
import solim.core.BaseComponent;
import solim.reactive.Readable;

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

        float contentWidth = Core.graphics != null
                ? Math.min(Core.graphics.getWidth() / 1.2f, 540f)
                : 540f;

        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().width(contentWidth).growX().gap(unit(2)).children(() -> {
                    // Opacity Slider
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.range-display.settings.opacity")).left();
                        spacer();
                        slider(feature.opacityConfig.signal(), 0.1f, 1.0f, 0.05f);
                        row().width(unit(12)).children(() -> {
                            text(opacityText);
                        });
                    });

                    divider();

                    // Dashed Lines Toggle
                    checkbox(Core.bundle.get("feature.range-display.settings.dashed"),
                            feature.dashedConfig.signal()).growX();

                    divider();

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
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.range-display.settings.section.turrets")).left();
                        spacer();
                        row().gap(unit(1)).children(() -> {
                            button(Core.bundle.get("feature.range-display.settings.all"),
                                    () -> feature.setCategoryEnabled(true, true))
                                    .style(Styles.defaultb)
                                    .height(unit(6))
                                    .margin(unit(1), unit(2), unit(1), unit(2));
                            button(Core.bundle.get("feature.range-display.settings.none"),
                                    () -> feature.setCategoryEnabled(true, false))
                                    .style(Styles.defaultb)
                                    .height(unit(6))
                                    .margin(unit(1), unit(2), unit(1), unit(2));
                        });
                    });

                    wrap().growX().left().gap(unit(1)).children(() -> {
                        if (Vars.content != null && Vars.content.blocks() != null) {
                            for (Block block : Vars.content.blocks()) {
                                if (block != null && feature.isTurretBlock(block)) {
                                    checkbox(block.localizedName, feature.getBlockSignal(block));
                                }
                            }
                        }
                    });

                    // --- Support Blocks Granular Toggles ---
                    divider();
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.range-display.settings.section.support-blocks")).left();
                        spacer();
                        row().gap(unit(1)).children(() -> {
                            button(Core.bundle.get("feature.range-display.settings.all"),
                                    () -> feature.setCategoryEnabled(false, true))
                                    .style(Styles.defaultb)
                                    .height(unit(6))
                                    .margin(unit(1), unit(2), unit(1), unit(2));
                            button(Core.bundle.get("feature.range-display.settings.none"),
                                    () -> feature.setCategoryEnabled(false, false))
                                    .style(Styles.defaultb)
                                    .height(unit(6))
                                    .margin(unit(1), unit(2), unit(1), unit(2));
                        });
                    });

                    wrap().growX().left().gap(unit(1)).children(() -> {
                        if (Vars.content != null && Vars.content.blocks() != null) {
                            for (Block block : Vars.content.blocks()) {
                                if (block != null && feature.isSupportBlock(block)) {
                                    checkbox(block.localizedName, feature.getBlockSignal(block));
                                }
                            }
                        }
                    });
                });
            });
        }).element();
    }
}
