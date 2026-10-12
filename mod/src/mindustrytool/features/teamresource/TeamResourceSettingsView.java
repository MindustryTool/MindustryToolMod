package mindustrytool.features.teamresource;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.core.Component;

/**
 * Modern declarative Solim settings view for Team Resource Tracker.
 */
public class TeamResourceSettingsView extends Component {

    private final TeamResourceFeature feature;

    public TeamResourceSettingsView(TeamResourceFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            layoutCard();
            metricsCard();
            appearanceCard();
            actionsCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, "HUD OVERLAY");
    }

    private Component layoutCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("team-resources.settings.section.layout", "Overlay Layout"), () -> {
            // Opacity Slider
            SettingSlider.of(
                    Core.bundle.get("team-resources.opacity", "Opacity"),
                    feature.opacityConfig.signal(),
                    0.1f,
                    1.0f,
                    0.05f,
                    feature.opacityConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%")
            );

            // Scale Slider
            SettingSlider.of(
                    Core.bundle.get("team-resources.size", "Scale"),
                    feature.scaleConfig.signal(),
                    0.3f,
                    2.0f,
                    0.05f,
                    feature.scaleConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%")
            );

            // Overlay Width Slider
            SettingSlider.of(
                    Core.bundle.get("team-resources.width", "Overlay Width"),
                    feature.overlayWidthConfig.signal(),
                    0.15f,
                    1.0f,
                    0.05f,
                    feature.overlayWidthConfig.signal().map(v -> Math.round((v != null ? v : 0.28f) * 100) + "%")
            );
        });
    }

    private Component metricsCard() {
        return SettingCard.section(Icon.tree, Core.bundle.get("team-resources.settings.section.metrics", "Tracked Metrics"), () -> {
            checkbox(Core.bundle.get("team-resources.show-items", "Show Core Items"),
                    feature.showItemsConfig.signal()).growX();

            checkbox(Core.bundle.get("team-resources.show-units", "Show Unit Counts"),
                    feature.showUnitsConfig.signal()).growX();

            checkbox(Core.bundle.get("team-resources.show-power", "Show Power Graph"),
                    feature.showPowerConfig.signal()).growX();

            checkbox(Core.bundle.get("team-resources.show-stored-power", "Show Stored Power"),
                    feature.showStoredPowerConfig.signal()).growX();

            checkbox(Core.bundle.get("team-resources.always-show-flow-rate", "Always Show Flow Rate"),
                    feature.alwaysShowFlowRateConfig.signal()).growX();
        });
    }

    private Component appearanceCard() {
        return SettingCard.section(Icon.eye, Core.bundle.get("team-resources.settings.section.appearance", "Appearance & Behavior"), () -> {
            checkbox(Core.bundle.get("team-resources.hide-background", "Hide Background"),
                    feature.hideBackgroundConfig.signal()).growX();

            checkbox(Core.bundle.get("team-resources.hide-header", "Hide Title Bar"),
                    feature.hideHeaderConfig.signal()).growX();

            checkbox(Core.bundle.get("team-resources.hide-zero-units", "Hide Inactive Units (0 count)"),
                    feature.hideZeroUnitsConfig.signal()).growX();

            checkbox(Core.bundle.get("team-resources.hide-zero-items", "Hide Empty Items (0 count)"),
                    feature.hideZeroItemsConfig.signal()).growX();

            checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle", "Hide Drag Handle"),
                    feature.hideDragHandleConfig.signal()).growX();
        });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                button(Core.bundle.get("feature.quick-access.settings.reset-position", "Reset Position"), feature::resetPosition)
                        .style(WebStyles.outlineText())
                        .height(unit(11))
                        .growX();

                button(Core.bundle.get("team-resources.reset-to-defaults", "Reset to Defaults"), feature::resetToDefaults)
                        .style(WebStyles.outlineText())
                        .height(unit(11))
                        .growX();
            });
        });
    }
}
