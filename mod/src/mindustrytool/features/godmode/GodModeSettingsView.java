package mindustrytool.features.godmode;

import static solim.UI.*;
import static solim.modifier.Values.checked;
import static solim.modifier.Values.color;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.paddingX;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;

import arc.Core;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.core.Component;

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
        return SettingCard.section(Icon.layers,
                Core.bundle.get("feature.god-mode.settings.display-layout", "Display & Layout"), () -> {

                    row().props(growX(), center()).children(() -> {
                        text(Core.bundle.get("feature.god-mode.settings.display-mode", "Display Mode"))
                                .props(color(WebStyles.Colors.GHOST_FG), left());
                        spacer();
                        row().props(gap(unit(1.5f))).children(() -> {
                            button(Core.bundle.get("feature.god-mode.settings.display-mode.hud", "HUD"),
                                    () -> feature.displayModeConfig.set(GodModeFeature.DISPLAY_HUD))
                                    .props(
                                            style(WebStyles.filterChipText()),
                                            checked(feature.displayModeConfig.signal()
                                                    .map(GodModeFeature.DISPLAY_HUD::equals)),
                                            height(unit(8)),
                                            prefHeight(unit(8)),
                                            paddingX(unit(2.5f)));

                            button(Core.bundle.get("feature.god-mode.settings.display-mode.popup", "Popup"),
                                    () -> feature.displayModeConfig.set(GodModeFeature.DISPLAY_POPUP))
                                    .props(
                                            style(WebStyles.filterChipText()),
                                            checked(feature.displayModeConfig.signal()
                                                    .map(GodModeFeature.DISPLAY_POPUP::equals)),
                                            height(unit(8)),
                                            prefHeight(unit(8)),
                                            paddingX(unit(2.5f)));
                        });
                    });

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    SettingSlider.of(
                            Core.bundle.get("feature.god-mode.settings.scale", "HUD Scale"),
                            feature.scaleConfig.signal(),
                            0.5f,
                            1.5f,
                            0.1f,
                            feature.scaleConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle", "Hide Drag Handle"),
                            feature.hideDragHandleConfig.signal()).props(growX());
                });
    }

    private Component providerCard() {
        return SettingCard.section(Icon.settings,
                Core.bundle.get("feature.god-mode.settings.provider", "Execution Provider"), () -> {
                    text(Core.bundle.get("feature.god-mode.settings.provider-desc",
                            "Select how sandbox commands and actions are executed in the game."))
                            .props(color(WebStyles.Colors.GHOST_FG), solim.modifier.Values.wrap(true), growX(), left());

                    row().props(gap(unit(1.5f))).children(() -> {
                        button(Core.bundle.get("feature.god-mode.settings.provider.auto", "Auto"),
                                () -> feature.providerModeConfig.set(GodModeFeature.PROVIDER_AUTO))
                                .props(
                                        style(WebStyles.filterChipText()),
                                        checked(feature.providerModeConfig.signal()
                                                .map(GodModeFeature.PROVIDER_AUTO::equals)),
                                        height(unit(8)),
                                        prefHeight(unit(8)),
                                        paddingX(unit(2f)));

                        button(Core.bundle.get("feature.god-mode.settings.provider.internal", "Internal"),
                                () -> feature.providerModeConfig.set(GodModeFeature.PROVIDER_INTERNAL))
                                .props(
                                        style(WebStyles.filterChipText()),
                                        checked(feature.providerModeConfig.signal()
                                                .map(GodModeFeature.PROVIDER_INTERNAL::equals)),
                                        height(unit(8)),
                                        prefHeight(unit(8)),
                                        paddingX(unit(2f)));

                        button(Core.bundle.get("feature.god-mode.settings.provider.js", "JS (/js)"),
                                () -> feature.providerModeConfig.set(GodModeFeature.PROVIDER_JS))
                                .props(
                                        style(WebStyles.filterChipText()),
                                        checked(feature.providerModeConfig.signal()
                                                .map(GodModeFeature.PROVIDER_JS::equals)),
                                        height(unit(8)),
                                        prefHeight(unit(8)),
                                        paddingX(unit(2f)));
                    });
                });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(feature::resetPosition)
                    .props(style(WebStyles.outline()), growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(2)))
                    .children(() -> {
                        icon(Icon.refresh).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                        text(Core.bundle.get("feature.god-mode.settings.reset-position", "Reset HUD Position"));
                    });
        });
    }
}
