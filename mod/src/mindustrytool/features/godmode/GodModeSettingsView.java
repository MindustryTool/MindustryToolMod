package mindustrytool.features.godmode;

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
 * Modern declarative Solim settings view for God Mode sandbox controls.
 */
public class GodModeSettingsView extends Component {

    private final GodModeFeature feature;

    public GodModeSettingsView(GodModeFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            displayLayoutCard();
            providerCard();
            actionsCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, Core.bundle.get("feature.god-mode.badge.sandbox", "SANDBOX"));
    }

    private Component displayLayoutCard() {
        return SettingCard.section(Icon.layers, Core.bundle.get("feature.god-mode.settings.display-layout", "Display & Layout"), () -> {
            // Display Mode Chips
            row().growX().center().children(() -> {
                text(Core.bundle.get("feature.god-mode.settings.display-mode", "Display Mode"))
                        .color(WebStyles.Colors.GHOST_FG)
                        .left();
                spacer();
                row().gap(unit(1.5f)).children(() -> {
                    button(Core.bundle.get("feature.god-mode.settings.display-mode.hud", "HUD"),
                            () -> feature.displayModeConfig.set(GodModeFeature.DISPLAY_HUD))
                            .style(WebStyles.filterChipText())
                            .checked(feature.displayModeConfig.signal()
                                    .map(GodModeFeature.DISPLAY_HUD::equals))
                            .height(unit(8))
                            .paddingX(unit(2.5f));

                    button(Core.bundle.get("feature.god-mode.settings.display-mode.popup", "Popup"),
                            () -> feature.displayModeConfig.set(GodModeFeature.DISPLAY_POPUP))
                            .style(WebStyles.filterChipText())
                            .checked(feature.displayModeConfig.signal()
                                    .map(GodModeFeature.DISPLAY_POPUP::equals))
                            .height(unit(8))
                            .paddingX(unit(2.5f));
                });
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Scale Slider
            SettingSlider.of(
                    Core.bundle.get("feature.god-mode.settings.scale", "HUD Scale"),
                    feature.scaleConfig.signal(),
                    0.5f,
                    1.5f,
                    0.1f,
                    feature.scaleConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100))
            );

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Hide Drag Handle Toggle
            checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle", "Hide Drag Handle"),
                    feature.hideDragHandleConfig.signal()).growX();
        });
    }

    private Component providerCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("feature.god-mode.settings.provider", "Execution Provider"), () -> {
            text(Core.bundle.get("feature.god-mode.settings.provider-desc", "Select how sandbox commands and actions are executed in the game."))
                    .color(WebStyles.Colors.GHOST_FG)
                    .wrap()
                    .growX()
                    .left();

            row().gap(unit(1.5f)).children(() -> {
                button(Core.bundle.get("feature.god-mode.settings.provider.auto", "Auto"),
                        () -> feature.providerModeConfig.set(GodModeFeature.PROVIDER_AUTO))
                        .style(WebStyles.filterChipText())
                        .checked(feature.providerModeConfig.signal()
                                .map(GodModeFeature.PROVIDER_AUTO::equals))
                        .height(unit(8))
                        .paddingX(unit(2f));

                button(Core.bundle.get("feature.god-mode.settings.provider.internal", "Internal"),
                        () -> feature.providerModeConfig.set(GodModeFeature.PROVIDER_INTERNAL))
                        .style(WebStyles.filterChipText())
                        .checked(feature.providerModeConfig.signal()
                                .map(GodModeFeature.PROVIDER_INTERNAL::equals))
                        .height(unit(8))
                        .paddingX(unit(2f));

                button(Core.bundle.get("feature.god-mode.settings.provider.js", "JS (/js)"),
                        () -> feature.providerModeConfig.set(GodModeFeature.PROVIDER_JS))
                        .style(WebStyles.filterChipText())
                        .checked(feature.providerModeConfig.signal()
                                .map(GodModeFeature.PROVIDER_JS::equals))
                        .height(unit(8))
                        .paddingX(unit(2f));
            });
        });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(feature::resetPosition)
                    .style(WebStyles.outline())
                    .growX()
                    .height(unit(11))
                    .gap(unit(2))
                    .children(() -> {
                        icon(Icon.refresh).size(unit(6));
                        text(Core.bundle.get("feature.god-mode.settings.reset-position", "Reset HUD Position"));
                    });
        });
    }
}
