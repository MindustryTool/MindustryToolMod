package mindustrytool.features.rangedisplay.ui;

import static solim.UI.*;
import static solim.modifier.Values.checked;
import static solim.modifier.Values.color;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.margin;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;
import static solim.modifier.Values.tooltip;

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
import mindustrytool.components.SettingKeybindCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import mindustrytool.features.rangedisplay.RangeDisplayFeature;
import solim.core.Component;
import solim.reactive.Signal;

public class RangeDisplaySettingsView extends Component {

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
            keybindsCard();
            actionsCard();
        });
    }

    private void keybindsCard() {
        SettingKeybindCard.forFeature(feature);
    }

    private Component headerCard() {
        return SettingCard.header(feature, "RANGE");
    }

    private Component visualStylingCard() {
        return SettingCard.section(Icon.image,
                Core.bundle.get("feature.range-display.settings.section.visual", "Visual Styling"), () -> {

                    SettingSlider.of(
                            Core.bundle.get("feature.range-display.settings.opacity"),
                            feature.opacityConfig.signal(),
                            0.1f,
                            1.0f,
                            0.05f,
                            feature.opacityConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%"));

                    SettingSlider.of(
                            Core.bundle.get("feature.range-display.settings.stroke-width"),
                            feature.strokeWidthConfig.signal(),
                            1.0f,
                            3.0f,
                            0.5f,
                            feature.strokeWidthConfig.signal()
                                    .map(v -> String.format(Locale.US, "%.1fx", v != null ? v : 1f)));

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    SettingSlider.of(
                            Core.bundle.get("feature.range-display.settings.min-zoom"),
                            feature.zoomThresholdConfig.signal(),
                            0.0f,
                            5.0f,
                            0.1f,
                            feature.zoomThresholdConfig.signal().map(v -> {
                                float zoom = v != null ? v : 0.5f;
                                return zoom <= 0.01f
                                        ? Core.bundle.get("feature.range-display.settings.off", "Off")
                                        : String.format(Locale.US, "%.1fx", zoom);
                            }));

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    checkbox(Core.bundle.get("feature.range-display.settings.hover-only"),
                            feature.hoverOnlyConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("feature.range-display.settings.dashed"),
                            feature.dashedConfig.signal()).props(growX());
                });
    }

    private Component proximityAndFiltersCard() {
        return SettingCard.section(Icon.filter,
                Core.bundle.get("feature.range-display.settings.section.filters", "Filters"), () -> {
                    checkbox(Core.bundle.get("feature.range-display.settings.proximity-filter"),
                            feature.proximityFilterConfig.signal()).props(growX());

                    SettingSlider.of(
                            Core.bundle.get("feature.range-display.settings.proximity-radius"),
                            feature.proximityRadiusConfig.signal(),
                            10f,
                            60f,
                            5f,
                            feature.proximityRadiusConfig.signal()
                                    .map(v -> Math.round(v != null ? v : 30f) + " tiles"));

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    checkbox(Core.bundle.get("feature.range-display.settings.filter-target-air"),
                            feature.filterTargetAirConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("feature.range-display.settings.filter-target-ground"),
                            feature.filterTargetGroundConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("feature.range-display.settings.only-with-ammo"),
                            feature.onlyWithAmmoConfig.signal()).props(growX());
                });
    }

    private Component rangeTypesCard() {
        return SettingCard.section(Icon.layers,
                Core.bundle.get("feature.range-display.settings.section.types", "Range Types"), () -> {
                    checkbox(Core.bundle.get("feature.range-display.settings.draw-player-unit"),
                            feature.drawUnitRangePlayerConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-ally-turrets"),
                            feature.drawTurretRangeAllyConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-enemy-turrets"),
                            feature.drawTurretRangeEnemyConfig.signal()).props(growX());

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-ally-units"),
                            feature.drawUnitRangeAllyConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-enemy-units"),
                            feature.drawUnitRangeEnemyConfig.signal()).props(growX());

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-ally-blocks"),
                            feature.drawBlockRangeAllyConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-enemy-blocks"),
                            feature.drawBlockRangeEnemyConfig.signal()).props(growX());

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-spawners"),
                            feature.drawSpawnerRangeConfig.signal()).props(growX());

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-overdrive-range"),
                            feature.drawOverdriveRangeConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-mender-range"),
                            feature.drawMenderRangeConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-point-defense-range"),
                            feature.drawPointDefenseRangeConfig.signal()).props(growX());

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    checkbox(Core.bundle.get("feature.range-display.settings.use-team-color-for-special"),
                            feature.useTeamColorForSpecialConfig.signal()).props(growX());
                });
    }

    private Component turretsFilterCard() {
        return SettingCard.card().children(() -> {
            row().props(growX(), gap(unit(2)), center()).children(() -> {
                icon(Icon.turret).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5)), color(Pal.accent));
                text(Core.bundle.get("feature.range-display.settings.section.turrets", "Turrets"))
                        .props(color(Pal.accent), growX(), left());

                row().props(gap(unit(1))).children(() -> {
                    button(Core.bundle.get("feature.range-display.settings.all", "All"),
                            () -> feature.setCategoryEnabled(true, true))
                            .props(style(WebStyles.outlineText()), height(unit(8)), prefHeight(unit(8)), padding(unit(1), unit(2.5f), unit(1), unit(2.5f)));

                    button(Core.bundle.get("feature.range-display.settings.none", "None"),
                            () -> feature.setCategoryEnabled(true, false))
                            .props(style(WebStyles.ghostText()), height(unit(8)), prefHeight(unit(8)), padding(unit(1), unit(2.5f), unit(1), unit(2.5f)));
                });
            });

            if (Vars.state != null && Vars.state.isGame()) {
                row().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                    button(Core.bundle.get("feature.range-display.settings.filter.all", "All"),
                            () -> turretActiveOnMapOnly.set(false))
                            .props(style(WebStyles.filterChipText()), checked(turretActiveOnMapOnly.map(active -> !Boolean.TRUE.equals(active))));

                    button(Core.bundle.get("feature.range-display.settings.filter.active-on-map", "Active on Map"),
                            () -> turretActiveOnMapOnly.set(true))
                            .props(style(WebStyles.filterChipText()), checked(turretActiveOnMapOnly.map(active -> Boolean.TRUE.equals(active))));
                });
            }

            dynamic(turretActiveOnMapOnly, activeOnly -> {
                Seq<Block> blocks = getFilteredBlocks(true, Boolean.TRUE.equals(activeOnly));
                if (blocks.isEmpty()) {
                    text(Core.bundle.get("feature.range-display.settings.filter.empty-map", "No blocks on current map"))
                            .props(color(WebStyles.Colors.GHOST_FG), margin(unit(2)));
                } else {
                    wrap().props(growX(), left(), gap(unit(1.5f))).children(() -> {
                        for (Block block : blocks) {
                            blockChip(block);
                        }
                    });
                }
            }).props(growX());
        });
    }

    private Component supportBlocksFilterCard() {
        return SettingCard.card().children(() -> {
            row().props(growX(), gap(unit(2)), center()).children(() -> {
                icon(Icon.defense).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5)), color(Pal.accent));
                text(Core.bundle.get("feature.range-display.settings.section.support-blocks", "Support Blocks"))
                        .props(color(Pal.accent), growX(), left());

                row().props(gap(unit(1))).children(() -> {
                    button(Core.bundle.get("feature.range-display.settings.all", "All"),
                            () -> feature.setCategoryEnabled(false, true))
                            .props(style(WebStyles.outlineText()), height(unit(8)), prefHeight(unit(8)), padding(unit(1), unit(2.5f), unit(1), unit(2.5f)));

                    button(Core.bundle.get("feature.range-display.settings.none", "None"),
                            () -> feature.setCategoryEnabled(false, false))
                            .props(style(WebStyles.ghostText()), height(unit(8)), prefHeight(unit(8)), padding(unit(1), unit(2.5f), unit(1), unit(2.5f)));
                });
            });

            if (Vars.state != null && Vars.state.isGame()) {
                row().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                    button(Core.bundle.get("feature.range-display.settings.filter.all", "All"),
                            () -> supportActiveOnMapOnly.set(false))
                            .props(style(WebStyles.filterChipText()), checked(supportActiveOnMapOnly.map(active -> !Boolean.TRUE.equals(active))));

                    button(Core.bundle.get("feature.range-display.settings.filter.active-on-map", "Active on Map"),
                            () -> supportActiveOnMapOnly.set(true))
                            .props(style(WebStyles.filterChipText()), checked(supportActiveOnMapOnly.map(active -> Boolean.TRUE.equals(active))));
                });
            }

            dynamic(supportActiveOnMapOnly, activeOnly -> {
                Seq<Block> blocks = getFilteredBlocks(false, Boolean.TRUE.equals(activeOnly));
                if (blocks.isEmpty()) {
                    text(Core.bundle.get("feature.range-display.settings.filter.empty-map", "No blocks on current map"))
                            .props(color(WebStyles.Colors.GHOST_FG), margin(unit(2)));
                } else {
                    wrap().props(growX(), left(), gap(unit(1.5f))).children(() -> {
                        for (Block block : blocks) {
                            blockChip(block);
                        }
                    });
                }
            }).props(growX());
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
                    .props(style(WebStyles.outlineText()), height(unit(11)), prefHeight(unit(11)), growX());
        });
    }

    private void blockChip(Block block) {
        Signal<Boolean> signal = feature.getBlockSignal(block);
        TextureRegion region = block.uiIcon != null ? block.uiIcon : block.fullIcon;
        Drawable drawable = region != null ? new TextureRegionDrawable(region) : Tex.clear;

        button()
                .props(style(WebStyles.filterChip()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), padding(unit(1)), tooltip(block.localizedName), checked(signal))
                .onClick(() -> feature.setBlockEnabled(block, !feature.isBlockEnabled(block)))
                .children(() -> {
                    icon(drawable)
                            .props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), color(signal.map(enabled -> Boolean.TRUE.equals(enabled) ? Color.white
                                    : new Color(1f, 1f, 1f, 0.35f))));
                });
    }
}
