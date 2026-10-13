package mindustrytool.features.smartupgrade;

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

/**
 * Modern declarative Solim settings view for Smart Upgrade.
 */
public class SmartUpgradeSettingsView extends Component {

    private final SmartUpgradeFeature feature;

    public SmartUpgradeSettingsView(SmartUpgradeFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            rulesCard();
            modeAndLimitsCard();
            keybindsCard();
            resetButtonCard();
        });
    }

    private void keybindsCard() {
        SettingKeybindCard.forFeature(feature);
    }

    private Component headerCard() {
        return SettingCard.header(feature, "BUILDING");
    }

    private Component rulesCard() {
        return SettingCard.section(Icon.wrench, Core.bundle.get("feature.smart-upgrade.settings.rules"), () -> {

            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(1.5f)).left().children(() -> {
                    text(Core.bundle.get("feature.smart-upgrade.settings.only-same-type"))
                            .color(Color.white)
                            .left();
                    text(Core.bundle.get("feature.smart-upgrade.settings.only-same-type.desc"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });
                spacer();
                checkbox("", feature.onlySameTypeConfig.signal());
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(1.5f)).left().children(() -> {
                    text(Core.bundle.get("feature.smart-upgrade.settings.traverse-bridges"))
                            .color(Color.white)
                            .left();
                    text(Core.bundle.get("feature.smart-upgrade.settings.traverse-bridges.desc"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });
                spacer();
                checkbox("", feature.traverseBridgesConfig.signal());
            });
        });
    }

    private Component modeAndLimitsCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("feature.smart-upgrade.settings.limits"), () -> {

            column().growX().gap(unit(1.5f)).left().children(() -> {
                column().growX().gap(unit(1.5f)).left().children(() -> {
                    text(Core.bundle.get("feature.smart-upgrade.settings.trigger-mode"))
                            .color(Color.white)
                            .left();
                    text(Core.bundle.get("feature.smart-upgrade.settings.trigger-mode.desc"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });

                row().gap(unit(1.5f)).children(() -> {
                    button(Core.bundle.get("feature.smart-upgrade.settings.trigger-mode.one-shot"),
                            () -> feature.triggerModeConfig.set(SmartUpgradeFeature.MODE_ONE_SHOT))
                            .style(WebStyles.filterChipText())
                            .checked(feature.triggerModeConfig.signal()
                                    .map(SmartUpgradeFeature.MODE_ONE_SHOT::equals))
                            .height(unit(8))
                            .paddingX(unit(2.5f));

                    button(Core.bundle.get("feature.smart-upgrade.settings.trigger-mode.persistent"),
                            () -> feature.triggerModeConfig.set(SmartUpgradeFeature.MODE_PERSISTENT))
                            .style(WebStyles.filterChipText())
                            .checked(feature.triggerModeConfig.signal()
                                    .map(SmartUpgradeFeature.MODE_PERSISTENT::equals))
                            .height(unit(8))
                            .paddingX(unit(2.5f));
                });
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            SettingSlider.of(
                    Core.bundle.get("feature.smart-upgrade.settings.max-upgrades"),
                    Core.bundle.get("feature.smart-upgrade.settings.max-upgrades.desc"),
                    feature.maxUpdatesConfig.signal(),
                    50,
                    2000,
                    50,
                    feature.maxUpdatesConfig.signal().map(v -> String.valueOf(v != null ? v : 500)));
        });
    }

    private Component resetButtonCard() {
        return SettingCard.card().children(() -> {
            button(Core.bundle.get("feature.smart-upgrade.settings.reset"),
                    feature::resetToDefaults)
                    .style(WebStyles.outlineText())
                    .height(unit(11))
                    .growX();
        });
    }
}
