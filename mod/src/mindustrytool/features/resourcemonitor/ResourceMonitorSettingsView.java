package mindustrytool.features.resourcemonitor;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import mindustrytool.components.WebStyles.Colors;
import solim.core.Component;

public class ResourceMonitorSettingsView extends Component {

    private final ResourceMonitorFeature feature;

    public ResourceMonitorSettingsView(ResourceMonitorFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            previewCard();
            layoutCard();
            metricsCard();
            appearanceCard();
            actionsCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, Core.bundle.get("team-resources.settings.badge"));
    }

    private Component previewCard() {
        return SettingCard.section(Icon.eye,
                Core.bundle.get("team-resources.settings.section.preview"), () -> {
                    text(Core.bundle.get("team-resources.settings.preview.desc"))
                            .props(color(Colors.GHOST_FG), left(), textWrap(true), growX());

                    new ResourceMonitorPreviewView(feature);
                });
    }

    private Component layoutCard() {
        return SettingCard.section(Icon.settings,
                Core.bundle.get("team-resources.settings.section.layout"), () -> {

                    SettingSlider.of(
                            Core.bundle.get("team-resources.opacity"),
                            feature.opacityConfig.signal(),
                            0.1f,
                            1.0f,
                            0.05f,
                            feature.opacityConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%"));

                    when(feature.hideBackgroundConfig.signal().map(v -> !Boolean.TRUE.equals(v)))
                            .thenDo(() -> SettingSlider.of(
                                    Core.bundle.get("team-resources.background-opacity"),
                                    feature.backgroundOpacityConfig.signal(),
                                    0.0f,
                                    1.0f,
                                    0.05f,
                                    feature.backgroundOpacityConfig.signal()
                                            .map(v -> Math.round((v != null ? v : 0.65f) * 100) + "%")))
                            .props(growX());

                    SettingSlider.of(
                            Core.bundle.get("team-resources.size"),
                            feature.scaleConfig.signal(),
                            0.3f,
                            2.0f,
                            0.05f,
                            feature.scaleConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%"));

                    SettingSlider.of(
                            Core.bundle.get("team-resources.row-spacing"),
                            feature.rowSpacingConfig.signal(),
                            0f,
                            8f,
                            1f,
                            feature.rowSpacingConfig.signal()
                                    .map(v -> Math.round(v != null ? v : 2f) + "px"));

                    checkbox(Core.bundle.get("team-resources.auto-width"),
                            feature.autoWidthConfig.signal()).props(growX());

                    when(feature.autoWidthConfig.signal().map(v -> !Boolean.TRUE.equals(v)))
                            .thenDo(() -> SettingSlider.of(
                                    Core.bundle.get("team-resources.width"),
                                    feature.overlayWidthConfig.signal(),
                                    0.15f,
                                    1.0f,
                                    0.05f,
                                    feature.overlayWidthConfig.signal()
                                            .map(v -> Math.round((v != null ? v : 0.28f) * 100) + "%")))
                            .props(growX());

                    checkbox(Core.bundle.get("team-resources.limit-height"),
                            feature.limitHeightConfig.signal()).props(growX());

                    when(feature.limitHeightConfig.signal().map(Boolean.TRUE::equals))
                            .thenDo(() -> SettingSlider.of(
                                    Core.bundle.get("team-resources.height"),
                                    feature.overlayHeightConfig.signal(),
                                    0.20f,
                                    0.95f,
                                    0.05f,
                                    feature.overlayHeightConfig.signal()
                                            .map(v -> Math.round((v != null ? v : 0.60f) * 100) + "%")))
                            .props(growX());

                    SettingSlider.of(
                            Core.bundle.get("team-resources.columns"),
                            feature.columnsConfig.signal(),
                            1,
                            8,
                            1,
                            feature.columnsConfig.signal()
                                    .map(v -> (v == null || v <= 1)
                                            ? Core.bundle.get("team-resources.columns.auto")
                                            : String.valueOf(v)));

                    SettingSlider.of(
                            Core.bundle.get("team-resources.unit-columns"),
                            feature.unitColumnsConfig.signal(),
                            1,
                            10,
                            1,
                            feature.unitColumnsConfig.signal()
                                    .map(v -> (v == null || v <= 1)
                                            ? Core.bundle.get("team-resources.columns.auto")
                                            : String.valueOf(v)));

                    checkbox(Core.bundle.get("team-resources.compact-units"),
                            feature.compactUnitsConfig.signal()).props(growX());
                });
    }

    private Component metricsCard() {
        return SettingCard.section(Icon.tree,
                Core.bundle.get("team-resources.settings.section.metrics"), () -> {
                    checkbox(Core.bundle.get("team-resources.show-items"),
                            feature.showItemsConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("team-resources.show-units"),
                            feature.showUnitsConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("team-resources.show-power"),
                            feature.showPowerConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("team-resources.show-stored-power"),
                            feature.showStoredPowerConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("team-resources.always-show-flow-rate"),
                            feature.alwaysShowFlowRateConfig.signal()).props(growX());
                });
    }

    private Component appearanceCard() {
        return SettingCard.section(Icon.eye,
                Core.bundle.get("team-resources.settings.section.appearance"), () -> {
                    checkbox(Core.bundle.get("team-resources.hide-background"),
                            feature.hideBackgroundConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("team-resources.hide-header"),
                            feature.hideHeaderConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("team-resources.hide-zero-units"),
                            feature.hideZeroUnitsConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("team-resources.hide-zero-items"),
                            feature.hideZeroItemsConfig.signal()).props(growX());

                    checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle"),
                            feature.hideDragHandleConfig.signal()).props(growX());
                });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            row().props(growX(), gap(unit(2)), center()).children(() -> {
                button(Core.bundle.get("feature.quick-access.settings.reset-position"),
                        feature::resetPosition)
                        .props(style(WebStyles.outlineText()), height(unit(11)), prefHeight(unit(11)), growX());

                button(Core.bundle.get("team-resources.reset-to-defaults"),
                        feature::resetToDefaults)
                        .props(style(WebStyles.outlineText()), height(unit(11)), prefHeight(unit(11)), growX());
            });
        });
    }
}
