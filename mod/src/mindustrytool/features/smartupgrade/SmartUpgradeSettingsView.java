package mindustrytool.features.smartupgrade;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.ui.Styles;
import solim.core.BaseComponent;
import solim.reactive.Readable;

/**
 * Settings view for configuring Smart Upgrade feature options.
 */
public class SmartUpgradeSettingsView extends BaseComponent {

    private final SmartUpgradeFeature feature;

    public SmartUpgradeSettingsView(SmartUpgradeFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<String> maxUpdatesText = feature.maxUpdatesConfig.signal()
                .map(v -> String.valueOf(v != null ? v : 500));

        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    // Checkbox: Only Replace Exact Type
                    checkbox(Core.bundle.get("feature.smart-upgrade.settings.only-same-type",
                            "Only Replace Exact Block Type"),
                            feature.onlySameTypeConfig.signal()).growX();

                    // Checkbox: Traverse Bridges and Junctions
                    checkbox(Core.bundle.get("feature.smart-upgrade.settings.traverse-bridges",
                            "Traverse Bridges & Junctions"),
                            feature.traverseBridgesConfig.signal()).growX();

                    divider();

                    // Slider: Max Upgrades
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.smart-upgrade.settings.max-upgrades",
                                "Max Upgraded Blocks")).left();
                        spacer();
                        slider(feature.maxUpdatesConfig.signal(), 50, 2000, 50);
                        row().width(unit(12)).children(() -> {
                            text(maxUpdatesText);
                        });
                    });

                    // Trigger Mode: One Shot vs Persistent
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.smart-upgrade.settings.trigger-mode",
                                "Trigger Mode")).left();
                        spacer();
                        row().gap(unit(2)).children(() -> {
                            button(Core.bundle.get("feature.smart-upgrade.settings.trigger-mode.one-shot",
                                            "One Shot"),
                                    () -> feature.triggerModeConfig.set(SmartUpgradeFeature.MODE_ONE_SHOT))
                                            .style(Styles.togglet)
                                            .checked(feature.triggerModeConfig.signal()
                                                    .map(SmartUpgradeFeature.MODE_ONE_SHOT::equals))
                                            .height(unit(8.5f));
                            button(Core.bundle.get("feature.smart-upgrade.settings.trigger-mode.persistent",
                                            "Persistent"),
                                    () -> feature.triggerModeConfig.set(SmartUpgradeFeature.MODE_PERSISTENT))
                                            .style(Styles.togglet)
                                            .checked(feature.triggerModeConfig.signal()
                                                    .map(SmartUpgradeFeature.MODE_PERSISTENT::equals))
                                            .height(unit(8.5f));
                        });
                    });

                    divider();

                    // Reset Defaults Button
                    button(Core.bundle.get("feature.smart-upgrade.settings.reset", "Reset to Defaults"),
                            feature::resetToDefaults)
                            .style(Styles.defaultb).growX();
                });
            });
        }).element();
    }
}
