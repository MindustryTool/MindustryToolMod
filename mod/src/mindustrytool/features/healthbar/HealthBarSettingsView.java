package mindustrytool.features.healthbar;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import solim.core.BaseComponent;

/**
 * Declarative Solim settings view for Health Bar options.
 */
public class HealthBarSettingsView extends BaseComponent {

    private final HealthBarFeature feature;

    public HealthBarSettingsView(HealthBarFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    checkbox(Core.bundle.get("feature.health-bar.settings.show-friendly-units"),
                            feature.showFriendlyUnitsConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.health-bar.settings.show-enemy-units"),
                            feature.showEnemyUnitsConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.health-bar.settings.show-friendly-blocks"),
                            feature.showFriendlyBlocksConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.health-bar.settings.show-enemy-blocks"),
                            feature.showEnemyBlocksConfig.signal()).growX();

                    divider();

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.health-bar.settings.min-zoom")).left();
                        spacer();
                        slider(feature.zoomThresholdConfig.signal(), 0f, 2f, 0.1f);
                        row().width(unit(14)).children(() -> {
                            text(feature.zoomThresholdConfig.signal().map(v -> (v != null ? v : 0.5f) <= 0.01f
                                    ? Core.bundle.get("feature.health-bar.settings.off")
                                    : String.format("%.1fx", v)));
                        });
                    });

                    divider();

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.health-bar.settings.opacity")).left();
                        spacer();
                        slider(feature.opacityConfig.signal(), 0f, 1f, 0.05f);
                        row().width(unit(14)).children(() -> {
                            text(feature.opacityConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));
                        });
                    });

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.health-bar.settings.scale")).left();
                        spacer();
                        slider(feature.scaleConfig.signal(), 0.5f, 1.5f, 0.1f);
                        row().width(unit(14)).children(() -> {
                            text(feature.scaleConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));
                        });
                    });

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.health-bar.settings.width")).left();
                        spacer();
                        slider(feature.widthConfig.signal(), 0.5f, 2f, 0.1f);
                        row().width(unit(14)).children(() -> {
                            text(feature.widthConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));
                        });
                    });
                });
            });
        }).element();
    }
}
