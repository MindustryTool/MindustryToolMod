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
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingKeybindCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
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
        return SettingScaffold.build(() -> {
            headerCard();
            bridgesCard();
            turretsCard();
            turretFilterCard();
            keybindsCard();
            actionsCard();
        });
    }

    private void keybindsCard() {
        SettingKeybindCard.forFeature(feature);
    }

    private Component headerCard() {
        return SettingCard.header(feature, "TACTICAL");
    }

    private Component bridgesCard() {
        return SettingCard.section(Icon.waves, Core.bundle.get("feature.visualizer.settings.section.bridges", "Bridges"), () -> {
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

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Bridge Opacity Slider
            SettingSlider.of(
                    Core.bundle.get("feature.visualizer.settings.bridge-opacity", "Bridge Opacity"),
                    feature.bridgeOpacityConfig.signal(),
                    0.2f,
                    1.0f,
                    0.05f,
                    feature.bridgeOpacityConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%")
            );

            // Bridge Item Scale Slider
            SettingSlider.of(
                    Core.bundle.get("feature.visualizer.settings.bridge-item-scale", "Bridge Item Scale"),
                    feature.bridgeItemScaleConfig.signal(),
                    0.3f,
                    2.0f,
                    0.05f,
                    feature.bridgeItemScaleConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%")
            );

            // Bridge Flow Rate Scale Slider
            SettingSlider.of(
                    Core.bundle.get("feature.visualizer.settings.bridge-flow-rate-scale", "Bridge Flow Rate Scale"),
                    feature.bridgeFlowRateScaleConfig.signal(),
                    0.5f,
                    2.0f,
                    0.05f,
                    feature.bridgeFlowRateScaleConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%")
            );
        });
    }

    private Component turretsCard() {
        return SettingCard.section(Icon.turret, Core.bundle.get("feature.visualizer.settings.section.turrets", "Turrets"), () -> {
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

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Target Line Opacity Slider
            SettingSlider.of(
                    Core.bundle.get("feature.visualizer.settings.target-line-opacity", "Target Line Opacity"),
                    feature.targetLineOpacityConfig.signal(),
                    0.1f,
                    1.0f,
                    0.05f,
                    feature.targetLineOpacityConfig.signal().map(v -> Math.round((v != null ? v : 0.6f) * 100) + "%")
            );

            // Turret Badge Scale Slider
            SettingSlider.of(
                    Core.bundle.get("feature.visualizer.settings.turret-badge-scale", "Turret Badge Scale"),
                    feature.turretBadgeScaleConfig.signal(),
                    0.2f,
                    2.0f,
                    0.05f,
                    feature.turretBadgeScaleConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%")
            );
        });
    }

    private Component turretFilterCard() {
        return SettingCard.card().children(() -> {
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

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(Core.bundle.get("feature.visualizer.settings.reset", "Reset to Defaults"),
                    feature::resetToDefaults)
                    .style(WebStyles.outlineText())
                    .height(unit(11))
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
