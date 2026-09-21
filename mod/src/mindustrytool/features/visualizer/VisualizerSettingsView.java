package mindustrytool.features.visualizer;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.world.Block;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Declarative Solim settings view for unified Visualizer options.
 */
public class VisualizerSettingsView extends BaseComponent {

    private final VisualizerFeature feature;

    public VisualizerSettingsView(VisualizerFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<String> bScaleText = feature.bridgeItemScaleConfig.signal()
                .map(v -> Math.round((v != null ? v : 1f) * 100) + "%");

        Readable<String> bOpacityText = feature.bridgeOpacityConfig.signal()
                .map(v -> Math.round((v != null ? v : 1f) * 100) + "%");

        Readable<String> tScaleText = feature.turretBadgeScaleConfig.signal()
                .map(v -> Math.round((v != null ? v : 1f) * 100) + "%");

        Readable<String> tOpacityText = feature.targetLineOpacityConfig.signal()
                .map(v -> Math.round((v != null ? v : 0.6f) * 100) + "%");

        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    // Section: Bridges
                    text(Core.bundle.get("feature.visualizer.settings.section.bridges", "Bridges")).left();

                    checkbox(Core.bundle.get("feature.visualizer.settings.show-item-bridges", "Show Item Bridges"),
                            feature.showItemBridgesConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.visualizer.settings.show-duct-bridges", "Show Duct Bridges"),
                            feature.showDuctBridgesConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.visualizer.settings.show-liquid-bridges", "Show Liquid Bridges"),
                            feature.showLiquidBridgesConfig.signal()).growX();

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.visualizer.settings.bridge-item-scale", "Bridge Item Scale")).left();
                        spacer();
                        slider(feature.bridgeItemScaleConfig.signal(), 0.5f, 2.0f, 0.1f);
                        row().width(unit(12)).children(() -> {
                            text(bScaleText);
                        });
                    });

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.visualizer.settings.bridge-opacity", "Bridge Opacity")).left();
                        spacer();
                        slider(feature.bridgeOpacityConfig.signal(), 0.2f, 1.0f, 0.05f);
                        row().width(unit(12)).children(() -> {
                            text(bOpacityText);
                        });
                    });

                    divider();

                    // Section: Turrets
                    text(Core.bundle.get("feature.visualizer.settings.section.turrets", "Turrets")).left();

                    checkbox(Core.bundle.get("feature.visualizer.settings.show-ammo-badge", "Show Ammo Badge"),
                            feature.showAmmoBadgeConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.visualizer.settings.show-target-line", "Show Target Line"),
                            feature.showTargetLineConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.visualizer.settings.target-line-ally", "Show Allied Turret Lines"),
                            feature.targetLineAllyConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.visualizer.settings.target-line-enemy", "Show Enemy Turret Lines"),
                            feature.targetLineEnemyConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.visualizer.settings.only-when-shooting", "Only When Shooting"),
                            feature.onlyWhenShootingConfig.signal()).growX();

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.visualizer.settings.turret-badge-scale", "Turret Badge Scale")).left();
                        spacer();
                        slider(feature.turretBadgeScaleConfig.signal(), 0.5f, 2.0f, 0.1f);
                        row().width(unit(12)).children(() -> {
                            text(tScaleText);
                        });
                    });

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.visualizer.settings.target-line-opacity", "Target Line Opacity")).left();
                        spacer();
                        slider(feature.targetLineOpacityConfig.signal(), 0.1f, 1.0f, 0.05f);
                        row().width(unit(12)).children(() -> {
                            text(tOpacityText);
                        });
                    });

                    divider();

                    // Granular Turret Filter
                    row().growX().gap(unit(2)).center().children(() -> {
                        text(Core.bundle.get("feature.visualizer.settings.turret-filter", "Turret Filter")).left();
                        spacer();
                        row().gap(unit(1)).children(() -> {
                            button(() -> feature.setAllTurretsEnabled(true))
                                    .style(WebStyles.outline())
                                    .height(unit(7))
                                    .padding(unit(1), unit(2.5f), unit(1), unit(2.5f))
                                    .children(() -> text(Core.bundle.get("feature.visualizer.settings.all", "All")));

                            button(() -> feature.setAllTurretsEnabled(false))
                                    .style(WebStyles.ghost())
                                    .height(unit(7))
                                    .padding(unit(1), unit(2.5f), unit(1), unit(2.5f))
                                    .children(() -> text(Core.bundle.get("feature.visualizer.settings.none", "None")));
                        });
                    });

                    wrap().growX().left().gap(unit(1.5f)).children(() -> {
                        if (Vars.content != null && Vars.content.blocks() != null) {
                            for (Block block : Vars.content.blocks()) {
                                if (block != null && feature.isTurretBlock(block)) {
                                    turretChip(block);
                                }
                            }
                        }
                    });
                });
            });
        }).element();
    }

    private void turretChip(Block block) {
        Signal<Boolean> signal = feature.getTurretSignal(block);
        TextureRegion region = block.uiIcon != null ? block.uiIcon : block.fullIcon;
        Drawable drawable = region != null ? new TextureRegionDrawable(region) : Tex.clear;

        button()
                .style(WebStyles.filterChip())
                .size(unit(11))
                .padding(unit(1))
                .tooltip(block.localizedName)
                .checked(signal)
                .onClick(() -> feature.setTurretEnabled(block, !feature.isTurretEnabled(block)))
                .children(() -> {
                    icon(drawable)
                            .size(unit(6))
                            .color(signal.map(enabled ->
                                    Boolean.TRUE.equals(enabled) ? Color.white : new Color(1f, 1f, 1f, 0.35f)
                            ));
                });
    }
}
