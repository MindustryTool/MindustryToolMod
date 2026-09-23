package mindustrytool.features.smartdrill.ui;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.ui.Styles;
import mindustrytool.features.smartdrill.SmartDrillFeature;
import solim.core.BaseComponent;
import solim.reactive.Readable;

/**
 * Solim settings view for Smart Drill configuration.
 */
public class SmartDrillSettingsView extends BaseComponent {

    private final SmartDrillFeature feature;

    public SmartDrillSettingsView(SmartDrillFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<String> maxTilesText = feature.maxTilesConfig.signal()
                .map(v -> String.valueOf(v != null ? v : 100));

        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    // Checkbox: Auto-disable after placement
                    checkbox(Core.bundle != null
                            ? Core.bundle.get("feature.smart-drill.settings.auto-disable", "Auto-Disable After Placement")
                            : "Auto-Disable After Placement",
                            feature.autoDisableConfig.signal()).growX();

                    divider();

                    // Slider: Max Ore Tiles
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle != null
                                ? Core.bundle.get("feature.smart-drill.settings.max-tiles", "Max Ore Tiles")
                                : "Max Ore Tiles").left();
                        spacer();
                        slider(feature.maxTilesConfig.signal(), 20, 300, 10);
                        row().width(unit(12)).children(() -> {
                            text(maxTilesText);
                        });
                    });

                    divider();

                    // Reset Defaults Button
                    button(Core.bundle != null
                            ? Core.bundle.get("feature.smart-drill.settings.reset", "Reset to Defaults")
                            : "Reset to Defaults",
                            feature::resetToDefaults)
                            .style(Styles.defaultb).growX();
                });
            });
        }).element();
    }
}
