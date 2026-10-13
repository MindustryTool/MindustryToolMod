package mindustrytool.features.timecontrol;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingKeybindCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.core.Component;

public class TimeControlSettingsView extends Component {

    private final TimeControlFeature feature;

    public TimeControlSettingsView(TimeControlFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            modeCard();
            appearanceCard();
            keybindsCard();
            actionsCard();
        });
    }

    private void keybindsCard() {
        SettingKeybindCard.forFeature(feature);
    }

    private Component headerCard() {
        return SettingCard.header(feature, "SIMULATION");
    }

    private Component modeCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("feature.time-control.settings.mode"), () -> {
            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("feature.time-control.settings.mode")).wrap().left().color(Color.white);

                spacer();
                row().gap(unit(1.5f)).children(() -> {
                    button(Core.bundle.get("feature.time-control.settings.mode.presets"),
                            () -> feature.modeConfig.set(TimeControlFeature.MODE_PRESETS))
                            .style(WebStyles.filterChipText())
                            .checked(feature.modeConfig.signal()
                                    .map(TimeControlFeature.MODE_PRESETS::equals))
                            .height(unit(8))
                            .paddingX(unit(2.5f));

                    button(Core.bundle.get("feature.time-control.settings.mode.slider"),
                            () -> feature.modeConfig.set(TimeControlFeature.MODE_SLIDER))
                            .style(WebStyles.filterChipText())
                            .checked(feature.modeConfig.signal()
                                    .map(TimeControlFeature.MODE_SLIDER::equals))
                            .height(unit(8))
                            .paddingX(unit(2.5f));
                });
            });

            text(Core.bundle.get("feature.time-control.settings.mode.hint")).left().wrap()
                    .color(WebStyles.Colors.GHOST_FG);

            divider().color(WebStyles.Colors.SECTION_BORDER);

            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("feature.time-control.settings.display-mode")).left().color(Color.white);

                spacer();
                row().gap(unit(1.5f)).children(() -> {
                    button(Core.bundle.get("feature.time-control.settings.display-mode.hud"),
                            () -> feature.displayModeConfig.set(TimeControlFeature.DISPLAY_HUD))
                            .style(WebStyles.filterChipText())
                            .checked(feature.displayModeConfig.signal()
                                    .map(TimeControlFeature.DISPLAY_HUD::equals))
                            .height(unit(8))
                            .paddingX(unit(2.5f));

                    button(Core.bundle.get("feature.time-control.settings.display-mode.popup"),
                            () -> feature.displayModeConfig.set(TimeControlFeature.DISPLAY_POPUP))
                            .style(WebStyles.filterChipText())
                            .checked(feature.displayModeConfig.signal()
                                    .map(TimeControlFeature.DISPLAY_POPUP::equals))
                            .height(unit(8))
                            .paddingX(unit(2.5f));
                });
            });
        });
    }

    private Component appearanceCard() {
        return SettingCard.section(Icon.eye,
                Core.bundle.get("team-resources.settings.section.appearance", "Appearance"), () -> {

                    SettingSlider.of(
                            Core.bundle.get("feature.time-control.settings.scale"),
                            feature.scaleConfig.signal(),
                            0.5f,
                            1.5f,
                            0.1f,
                            feature.scaleConfig.signal().map(v -> (int) ((v != null ? v : 1f) * 100) + "%"));

                    divider().color(WebStyles.Colors.SECTION_BORDER);

                    checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle"),
                            feature.hideDragHandleConfig.signal()).growX();

                    divider().color(WebStyles.Colors.SECTION_BORDER);

                    text(Core.bundle.get("feature.time-control.settings.safety")).left().wrap().growX()
                            .color(WebStyles.Colors.GHOST_FG);
                });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                button(Core.bundle.get("feature.time-control.settings.reset-speed"), feature::resetSpeed)
                        .style(WebStyles.outlineText())
                        .height(unit(11))
                        .growX();

                button(Core.bundle.get("feature.time-control.settings.reset-position"), feature::resetPosition)
                        .style(WebStyles.outlineText())
                        .height(unit(11))
                        .growX();
            });
        });
    }
}
