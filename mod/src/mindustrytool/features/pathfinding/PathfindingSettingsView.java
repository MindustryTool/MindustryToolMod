package mindustrytool.features.pathfinding;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingKeybindCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.core.Component;

/**
 * Modern declarative Solim settings view for unit pathfinding visualization.
 */
public class PathfindingSettingsView extends Component {

    private final PathfindingFeature feature;

    private static final String[] COST_KEY_SUFFIXES = {
        "ground", "legs", "naval", "neoplasm", "none", "hover"
    };

    public PathfindingSettingsView(PathfindingFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            layersCard();
            visualStylingCard();
            costTypesCard();
            keybindsCard();
            actionsCard();
        });
    }

    private void keybindsCard() {
        SettingKeybindCard.forFeature(feature);
    }

    private Component headerCard() {
        return SettingCard.header(feature, Core.bundle.get("feature.pathfinding.badge.navigation", "NAVIGATION"));
    }

    private Component layersCard() {
        return SettingCard.section(Icon.layers, Core.bundle.get("feature.pathfinding.settings.layers", "Display Layers"), () -> {
            checkbox(Core.bundle.get("feature.pathfinding.settings.draw-unit-path", "Draw Unit Paths"),
                    feature.drawUnitPathConfig.signal()).growX();

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkbox(Core.bundle.get("feature.pathfinding.settings.draw-spawn-path", "Draw Spawn Point Paths"),
                    feature.drawSpawnPathConfig.signal()).growX();

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkbox(Core.bundle.get("feature.pathfinding.settings.draw-allies", "Draw Ally Unit Paths"),
                    feature.drawAlliesConfig.signal()).growX();
        });
    }

    private Component visualStylingCard() {
        return SettingCard.section(Icon.spray, Core.bundle.get("feature.pathfinding.settings.visual-styling", "Visual Styling"), () -> {
            // Min Zoom Slider
            SettingSlider.of(
                    Core.bundle.get("feature.pathfinding.settings.min-zoom", "Min Zoom"),
                    feature.zoomThresholdConfig.signal(),
                    0.0f,
                    5.0f,
                    0.1f,
                    feature.zoomThresholdConfig.signal().map(v -> {
                        float zoom = v != null ? v : 0.5f;
                        return zoom <= 0.01f
                                ? Core.bundle.get("feature.pathfinding.settings.off", "Off")
                                : String.format("%.1fx", zoom);
                    })
            );

            // Opacity Slider
            SettingSlider.of(
                    Core.bundle.get("feature.pathfinding.settings.opacity", "Opacity"),
                    feature.opacityConfig.signal(),
                    0.05f,
                    1.0f,
                    0.05f,
                    feature.opacityConfig.signal().map(v -> {
                        float op = v != null ? v : 1.0f;
                        return Math.round(op * 100) + "%";
                    })
            );
        });
    }

    private Component costTypesCard() {
        return SettingCard.section(Icon.terrain, Core.bundle.get("feature.pathfinding.settings.cost-types", "Movement Cost Types"), () -> {
            wrap().gap(unit(1.5f)).children(() -> {
                for (int i = 0; i < feature.costTypeConfigs.length; i++) {
                    final int index = i;
                    String bundleKey = "feature.pathfinding.settings.cost." + COST_KEY_SUFFIXES[index];
                    button(Core.bundle.get(bundleKey, COST_KEY_SUFFIXES[index]),
                            () -> feature.costTypeConfigs[index].set(!Boolean.TRUE.equals(feature.costTypeConfigs[index].signal().peek())))
                            .style(WebStyles.filterChipText())
                            .checked(feature.costTypeConfigs[index].signal().map(v -> Boolean.TRUE.equals(v)))
                            .height(unit(8))
                            .paddingX(unit(2f));
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
                        text(Core.bundle.get("feature.pathfinding.settings.reset", "Reset to Defaults"));
                    });
        });
    }
}
