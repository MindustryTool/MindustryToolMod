package mindustrytool.features.visualizer;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.layout.Card;
import solim.reactive.Signal;

/**
 * Modern declarative Solim settings view for unified Visualizer options.
 */
public class VisualizerSettingsView extends BaseComponent {

    private final VisualizerFeature feature;
    private final Signal<Boolean> turretActiveOnMapOnly = Signal.of(false);

    public VisualizerSettingsView(VisualizerFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().gap(unit(2.5f)).padding(unit(2)).children(() -> {
                    headerCard();
                    bridgesCard();
                    turretsCard();
                    turretFilterCard();
                    actionsCard();
                });
            });
        }).element();
    }

    private Card sectionCard() {
        return card()
                .growX()
                .padding(unit(2.5f))
                .rounded(8, WebStyles.Colors.SECTION_BG)
                .border(1.5f, WebStyles.Colors.SECTION_BORDER)
                .gap(unit(2));
    }

    private Card headerCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.eye).size(unit(6)).color(Pal.accent);
                text(Core.bundle.get("feature.visualizer.name", "Visualizer"))
                        .color(Pal.accent)
                        .growX()
                        .left();
                badge("TACTICAL").color(WebStyles.Colors.PRIMARY);
            });

            text(Core.bundle.get("feature.visualizer.description"))
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();
        });
    }

    private Card bridgesCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.waves).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.visualizer.settings.section.bridges", "Bridges"))
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            checkbox(Core.bundle.get("feature.visualizer.settings.show-item-bridges", "Show Item Bridges"),
                    feature.showItemBridgesConfig.signal()).growX();

            checkbox(Core.bundle.get("feature.visualizer.settings.show-duct-bridges", "Show Duct Bridges"),
                    feature.showDuctBridgesConfig.signal()).growX();

            checkbox(Core.bundle.get("feature.visualizer.settings.show-liquid-bridges", "Show Liquid Bridges"),
                    feature.showLiquidBridgesConfig.signal()).growX();

            checkbox(Core.bundle.get("feature.visualizer.settings.show-bridge-flow-rate", "Show Bridge Flow Rate"),
                    feature.showBridgeFlowRateConfig.signal()).growX();

            checkbox(Core.bundle.get("feature.visualizer.settings.hide-idle-bridge-flow", "Hide When Idle"),
                    feature.hideIdleBridgeFlowConfig.signal()).growX();

            divider();

            // Bridge Opacity Slider
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("feature.visualizer.settings.bridge-opacity", "Bridge Opacity")).left();
                spacer();
                slider(feature.bridgeOpacityConfig.signal(), 0.2f, 1.0f, 0.05f).width(unit(24));
                badge(feature.bridgeOpacityConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%"))
                        .color(WebStyles.Colors.SECONDARY);
            });

            // Bridge Item Scale Slider
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("feature.visualizer.settings.bridge-item-scale", "Bridge Item Scale")).left();
                spacer();
                slider(feature.bridgeItemScaleConfig.signal(), 0.3f, 2.0f, 0.05f).width(unit(24));
                badge(feature.bridgeItemScaleConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%"))
                        .color(WebStyles.Colors.SECONDARY);
            });

            // Bridge Flow Rate Scale Slider
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("feature.visualizer.settings.bridge-flow-rate-scale", "Bridge Flow Rate Scale")).left();
                spacer();
                slider(feature.bridgeFlowRateScaleConfig.signal(), 0.5f, 2.0f, 0.05f).width(unit(24));
                badge(feature.bridgeFlowRateScaleConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%"))
                        .color(WebStyles.Colors.SECONDARY);
            });
        });
    }

    private Card turretsCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.turret).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.visualizer.settings.section.turrets", "Turrets"))
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

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

            divider();

            // Target Line Opacity Slider
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("feature.visualizer.settings.target-line-opacity", "Target Line Opacity")).left();
                spacer();
                slider(feature.targetLineOpacityConfig.signal(), 0.1f, 1.0f, 0.05f).width(unit(24));
                badge(feature.targetLineOpacityConfig.signal().map(v -> Math.round((v != null ? v : 0.6f) * 100) + "%"))
                        .color(WebStyles.Colors.SECONDARY);
            });

            // Turret Badge Scale Slider
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("feature.visualizer.settings.turret-badge-scale", "Turret Badge Scale")).left();
                spacer();
                slider(feature.turretBadgeScaleConfig.signal(), 0.2f, 2.0f, 0.05f).width(unit(24));
                badge(feature.turretBadgeScaleConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%"))
                        .color(WebStyles.Colors.SECONDARY);
            });
        });
    }

    private Card turretFilterCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.filter).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.visualizer.settings.turret-filter", "Turret Filter"))
                        .color(Pal.accent)
                        .growX()
                        .left();

                row().gap(unit(1)).children(() -> {
                    button(Core.bundle.get("feature.visualizer.settings.all", "All"),
                            () -> feature.setAllTurretsEnabled(true))
                            .style(WebStyles.outlineText())
                            .height(unit(8))
                            .padding(unit(1), unit(2.5f), unit(1), unit(2.5f));

                    button(Core.bundle.get("feature.visualizer.settings.none", "None"),
                            () -> feature.setAllTurretsEnabled(false))
                            .style(WebStyles.ghostText())
                            .height(unit(8))
                            .padding(unit(1), unit(2.5f), unit(1), unit(2.5f));
                });
            });

            if (Vars.state != null && Vars.state.isGame()) {
                row().growX().gap(unit(1.5f)).left().children(() -> {
                    button(Core.bundle.get("feature.visualizer.settings.filter.all", "All"),
                            () -> turretActiveOnMapOnly.set(false))
                            .style(WebStyles.filterChipText())
                            .checked(turretActiveOnMapOnly.map(active -> !Boolean.TRUE.equals(active)));

                    button(Core.bundle.get("feature.visualizer.settings.filter.active-on-map", "Active on Map"),
                            () -> turretActiveOnMapOnly.set(true))
                            .style(WebStyles.filterChipText())
                            .checked(turretActiveOnMapOnly.map(active -> Boolean.TRUE.equals(active)));
                });
            }

            dynamic(turretActiveOnMapOnly, activeOnly -> {
                Seq<Block> blocks = getFilteredTurretBlocks(Boolean.TRUE.equals(activeOnly));
                if (blocks.isEmpty()) {
                    text(Core.bundle.get("feature.range-display.settings.filter.empty-map", "No blocks on current map"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .padding(unit(2));
                } else {
                    wrap().growX().left().gap(unit(1.5f)).children(() -> {
                        for (Block block : blocks) {
                            turretChip(block);
                        }
                    });
                }
            }).growX();
        });
    }

    private Seq<Block> getFilteredTurretBlocks(boolean activeOnly) {
        Seq<Block> result = new Seq<>();
        if (Vars.content == null || Vars.content.blocks() == null) {
            return result;
        }

        if (activeOnly) {
            ObjectSet<Block> activeBlocks = new ObjectSet<>();
            if (Vars.state != null && Vars.state.isGame() && Groups.build != null) {
                Groups.build.each(b -> {
                    if (b != null && b.block != null) {
                        activeBlocks.add(b.block);
                    }
                });
            }
            for (Block block : Vars.content.blocks()) {
                if (block != null && feature.isTurretBlock(block) && activeBlocks.contains(block)) {
                    result.add(block);
                }
            }
            return result;
        }

        for (Block block : Vars.content.blocks()) {
            if (block != null && feature.isTurretBlock(block)) {
                result.add(block);
            }
        }
        return result;
    }

    private Card actionsCard() {
        return sectionCard().children(() -> {
            button(Core.bundle.get("feature.visualizer.settings.reset", "Reset to Defaults"),
                    feature::resetToDefaults)
                    .style(WebStyles.outlineText())
                    .growX();
        });
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
