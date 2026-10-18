package mindustrytool.features.armycontrol;

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

public class ArmyControlSettingsView extends Component {

    private final ArmyControlFeature feature;

    public ArmyControlSettingsView(ArmyControlFeature feature) {
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
        return SettingCard.header(feature, "COMBAT");
    }

    private Component modeCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("feature.army-control.settings.display-mode", "Display Mode"), () -> {
            row().props(growX(), gap(unit(2)), center()).children(() -> {
                text(Core.bundle.get("feature.army-control.settings.display-mode", "Display Mode")).props(color(Color.white), left());

                spacer();
                row().props(gap(unit(1.5f))).children(() -> {
                    button(Core.bundle.get("feature.army-control.settings.display-mode.popup", "Popup"),
                            () -> feature.displayModeConfig.set(ArmyControlFeature.DISPLAY_POPUP))
                            .props(checked(feature.displayModeConfig.signal()
                                     .map(ArmyControlFeature.DISPLAY_POPUP::equals)),
                                    style(WebStyles.filterChipText()), height(unit(8)), prefHeight(unit(8)), paddingX(unit(2.5f)));

                    button(Core.bundle.get("feature.army-control.settings.display-mode.hud", "HUD"),
                            () -> feature.displayModeConfig.set(ArmyControlFeature.DISPLAY_HUD))
                            .props(checked(feature.displayModeConfig.signal()
                                     .map(ArmyControlFeature.DISPLAY_HUD::equals)),
                                    style(WebStyles.filterChipText()), height(unit(8)), prefHeight(unit(8)), paddingX(unit(2.5f)));
                });
            });
        });
    }

    private Component appearanceCard() {
        return SettingCard.section(Icon.eye, Core.bundle.get("team-resources.settings.section.appearance", "Appearance"), () -> {
            SettingSlider.of(
                    Core.bundle.get("feature.army-control.settings.scale", "Scale"),
                    feature.scaleConfig.signal(),
                    0.5f,
                    1.5f,
                    0.1f,
                    feature.scaleConfig.signal().map(v -> Math.round((v != null ? v : 1f) * 100) + "%")
            );
        });
    }

    private Component actionsCard() {
        return SettingCard.card().props(padding(unit(2.5f)), gap(unit(2))).children(() -> {
            row().props(growX(), gap(unit(2)), center()).children(() -> {
                button(Core.bundle.get("feature.army-control.settings.reset-position", "Reset HUD Position"), () -> {
                    feature.xConfig.reset();
                    feature.yConfig.reset();
                }).props(style(WebStyles.secondaryText()), growX());
            });
        });
    }
}
