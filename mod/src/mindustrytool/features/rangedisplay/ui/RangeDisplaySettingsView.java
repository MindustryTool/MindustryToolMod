package mindustrytool.features.rangedisplay.ui;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustrytool.features.rangedisplay.RangeDisplayFeature;
import solim.core.BaseComponent;
import solim.signal.Readable;

/**
 * Declarative Solim settings view for Range Display options.
 */
public class RangeDisplaySettingsView extends BaseComponent {

    private final RangeDisplayFeature feature;

    public RangeDisplaySettingsView(RangeDisplayFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<String> opacityText = feature.opacityConfig.signal()
                .map(v -> Math.round((v != null ? v : 1f) * 100) + "%");

        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    // Opacity Slider
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.range-display.settings.opacity")).left();
                        spacer();
                        slider(feature.opacityConfig.signal(), 0.1f, 1.0f, 0.05f);
                        row().width(unit(12)).children(() -> {
                            text(opacityText);
                        });
                    });

                    divider();

                    // Dashed Lines Toggle
                    checkbox(Core.bundle.get("feature.range-display.settings.dashed"),
                            feature.dashedConfig.signal()).growX();

                    divider();

                    // Turret Ranges
                    checkbox(Core.bundle.get("feature.range-display.settings.draw-ally-turrets"),
                            feature.drawTurretRangeAllyConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-enemy-turrets"),
                            feature.drawTurretRangeEnemyConfig.signal()).growX();

                    divider();

                    // Unit Ranges
                    checkbox(Core.bundle.get("feature.range-display.settings.draw-ally-units"),
                            feature.drawUnitRangeAllyConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-enemy-units"),
                            feature.drawUnitRangeEnemyConfig.signal()).growX();

                    divider();

                    // Support Block Ranges
                    checkbox(Core.bundle.get("feature.range-display.settings.draw-ally-blocks"),
                            feature.drawBlockRangeAllyConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.range-display.settings.draw-enemy-blocks"),
                            feature.drawBlockRangeEnemyConfig.signal()).growX();

                    divider();

                    // Spawner Drop Zones
                    checkbox(Core.bundle.get("feature.range-display.settings.draw-spawners"),
                            feature.drawSpawnerRangeConfig.signal()).growX();
                });
            });
        }).element();
    }
}
