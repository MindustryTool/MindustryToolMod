package mindustrytool.features.rangedisplay.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import java.util.Locale;
import mindustry.Vars;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import mindustrytool.features.rangedisplay.RangeDisplayFeature;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.reactive.Signal;

/**
 * Modern declarative Solim settings view for Range Display options.
 */
public class RangeDisplaySettingsView extends BaseComponent {

    private final RangeDisplayFeature feature;
    private final Signal<Boolean> turretActiveOnMapOnly = Signal.of(false);
    private final Signal<Boolean> supportActiveOnMapOnly = Signal.of(false);

    public RangeDisplaySettingsView(RangeDisplayFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            visualStylingCard();
            proximityAndFiltersCard();
            rangeTypesCard();
            turretsFilterCard();
            supportBlocksFilterCard();
            actionsCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, "RANGE");
    }

    private Component visualStylingCard() {
        return SettingCard.section(Icon.image, Core.bundle.get("feature.range-display.settings.section.visual", "Visual Styling"), () -> {
            // Opacity Slider
            SettingSlider.of(
                    Core.bundle.get("feature.range-display.settings.opacity"),
                    feature.opacityConfig.signal(),
                    0.1f,
                    1.0f,
                    0.05f,
                    feature.opacityConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%")
            );

            // Stroke Width Slider
            SettingSlider.of(
                    Core.bundle.get("feature.range-display.settings.stroke-width"),
                    feature.strokeWidthConfig.signal(),
                    1.0f,
                    3.0f,
                    0.5f,
                    feature.strokeWidthConfig.signal().map(v -> String.format(Locale.US, "%.1fx", v != null ? v : 1f))
            );

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkbox(Core.bundle.get("feature.range-display.settings.hover-only"),
                    feature.hoverOnlyConfig.signal()).growX();

            checkbox(Core.bundle.get("feature.range-display.settings.dashed"),
                    feature.dashedConfig.signal()).growX();
        });
    }

    private Component proximityAndFiltersCard() {
        return SettingCard.section(Icon.filter, Core.bundle.get("feature.range-display.settings.section.filters", "Filters"), () -> {
            checkbox(Core.bundle.get("feature.range-display.settings.proximity-filter"),
                    feature.proximityFilterConfig.signal()).growX();

            // Proximity Radius Slider
            SettingSlider.of(
                    Core.bundle.get("feature.range-display.settings.proximity-radius"),
                    feature.proximityRadiusConfig.signal(),
                    10f,
                    60f,
                    5f,
                    feature.proximityRadiusConfig.signal().map(v -> Math.round(v != null ? v : 30f) + " tiles")
            );

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkbox(Core.bundle.get("feature.range-display.settings.filter-target-air"),
                    feature.filterTargetAirConfig.signal()).growX();

            checkbox(Core.bundle.get("feature.range-display.settings.filter-target-ground"),
                    feature.filterTargetGroundConfig.signal()).growX();

            checkbox(Core.bundle.get("feature.range-display.settings.only-with-ammo"),
                    feature.onlyWithAmmoConfig.signal()).growX();
        });
    }

    private Component rangeTypesCard() {
        return SettingCard.section(Icon.layers, Core.bundle.get("feature.range-display.settings.section.types", "Range Types"), () -> {
            checkbox(Core.bundle.get("feature.range-display.settings.draw-player-unit"),
                    feature.drawUnitRangePlayerConfig.signal()).growX();

            checkbox(Core.bundle.get("feature.range-display.settings.draw-ally-turrets"),
                    feature.drawTurretRangeAllyConfig.signal()).growX();

            checkbox(Core.bundle.get("feature.range-display.settings.draw-enemy-turrets"),
                    feature.drawTurretRangeEnemyConfig.signal()).growX();

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkbox(Core.bundle.get("feature.range-display.settings.draw-ally-units"),
                    feature.drawUnitRangeAllyConfig.signal()).growX();

            checkbox(Core.bundle.get("feature.range-display.settings.draw-enemy-units"),
                    feature.drawUnitRangeEnemyConfig.signal()).growX();

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkbox(Core.bundle.get("feature.range-display.settings.draw-ally-blocks"),
                    feature.drawBlockRangeAllyConfig.signal()).growX();

            checkbox(Core.bundle.get("feature.range-display.settings.draw-enemy-blocks"),
                    feature.drawBlockRangeEnemyConfig.signal()).growX();

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkbox(Core.bundle.get("feature.range-display.settings.draw-spawners"),
                    feature.drawSpawnerRangeConfig.signal()).growX();
        });
    }

    private Component turretsFilterCard() {
        return SettingCard.card().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.turret).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.range-display.settings.section.turrets", "Turrets"))
                        .color(Pal.accent)
                        .growX()
                        .left();

                row().gap(unit(1)).children(() -> {
                    button(Core.bundle.get("feature.range-display.settings.all", "All"),
                            () -> feature.setCategoryEnabled(true, true))
                            .style(WebStyles.outlineText())
                            .height(unit(8))
                            .padding(unit(1), unit(2.5f), unit(1), unit(2.5f));

                    button(Core.bundle.get("feature.range-display.settings.none", "None"),
                            () -> feature.setCategoryEnabled(true, false))
                            .style(WebStyles.ghostText())
                            .height(unit(8))
                            .padding(unit(1), unit(2.5f), unit(1), unit(2.5f));
                });
            });

            if (Vars.state != null && Vars.state.isGame()) {
                row().growX().gap(unit(1.5f)).left().children(() -> {
                    button(Core.bundle.get("feature.range-display.settings.filter.all", "All"),
                            () -> turretActiveOnMapOnly.set(false))
                            .style(WebStyles.filterChipText())
                            .checked(turretActiveOnMapOnly.map(active -> !Boolean.TRUE.equals(active)));

                    button(Core.bundle.get("feature.range-display.settings.filter.active-on-map", "Active on Map"),
                            () -> turretActiveOnMapOnly.set(true))
                            .style(WebStyles.filterChipText())
                            .checked(turretActiveOnMapOnly.map(active -> Boolean.TRUE.equals(active)));
                });
            }

            dynamic(turretActiveOnMapOnly, activeOnly -> {
                Seq<Block> blocks = getFilteredBlocks(true, Boolean.TRUE.equals(activeOnly));
                if (blocks.isEmpty()) {
                    text(Core.bundle.get("feature.range-display.settings.filter.empty-map", "No blocks on current map"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .padding(unit(2));
                } else {
                    wrap().growX().left().gap(unit(1.5f)).children(() -> {
                        for (Block block : blocks) {
                            blockChip(block);
                        }
                    });
                }
            }).growX();
        });
    }

    private Component supportBlocksFilterCard() {
        return SettingCard.card().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.defense).size(unit(5)).color(Pal.accent);
                text(Core.bundle.get("feature.range-display.settings.section.support-blocks", "Support Blocks"))
                        .color(Pal.accent)
                        .growX()
                        .left();

                row().gap(unit(1)).children(() -> {
                    button(Core.bundle.get("feature.range-display.settings.all", "All"),
                            () -> feature.setCategoryEnabled(false, true))
                            .style(WebStyles.outlineText())
                            .height(unit(8))
                            .padding(unit(1), unit(2.5f), unit(1), unit(2.5f));

                    button(Core.bundle.get("feature.range-display.settings.none", "None"),
                            () -> feature.setCategoryEnabled(false, false))
                            .style(WebStyles.ghostText())
                            .height(unit(8))
                            .padding(unit(1), unit(2.5f), unit(1), unit(2.5f));
                });
            });

            if (Vars.state != null && Vars.state.isGame()) {
                row().growX().gap(unit(1.5f)).left().children(() -> {
                    button(Core.bundle.get("feature.range-display.settings.filter.all", "All"),
                            () -> supportActiveOnMapOnly.set(false))
                            .style(WebStyles.filterChipText())
                            .checked(supportActiveOnMapOnly.map(active -> !Boolean.TRUE.equals(active)));

                    button(Core.bundle.get("feature.range-display.settings.filter.active-on-map", "Active on Map"),
                            () -> supportActiveOnMapOnly.set(true))
                            .style(WebStyles.filterChipText())
                            .checked(supportActiveOnMapOnly.map(active -> Boolean.TRUE.equals(active)));
                });
            }

            dynamic(supportActiveOnMapOnly, activeOnly -> {
                Seq<Block> blocks = getFilteredBlocks(false, Boolean.TRUE.equals(activeOnly));
                if (blocks.isEmpty()) {
                    text(Core.bundle.get("feature.range-display.settings.filter.empty-map", "No blocks on current map"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .padding(unit(2));
                } else {
                    wrap().growX().left().gap(unit(1.5f)).children(() -> {
                        for (Block block : blocks) {
                            blockChip(block);
                        }
                    });
                }
            }).growX();
        });
    }

    private Seq<Block> getFilteredBlocks(boolean isTurret, boolean activeOnly) {
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
                if (block != null) {
                    boolean matches = isTurret ? feature.isTurretBlock(block) : feature.isSupportBlock(block);
                    if (matches && activeBlocks.contains(block)) {
                        result.add(block);
                    }
                }
            }
            return result;
        }

        for (Block block : Vars.content.blocks()) {
            if (block != null) {
                boolean matches = isTurret ? feature.isTurretBlock(block) : feature.isSupportBlock(block);
                if (matches) {
                    result.add(block);
                }
            }
        }
        return result;
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(Core.bundle.get("feature.range-display.settings.reset", "Reset to Defaults"),
                    feature::resetToDefaults)
                    .style(WebStyles.outlineText())
                    .height(unit(11))
                    .growX();
        });
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
