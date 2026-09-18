package mindustrytool.features.pathfinding;

import static solim.UI.button;
import static solim.UI.checkbox;
import static solim.UI.column;
import static solim.UI.divider;
import static solim.UI.row;
import static solim.UI.scroll;
import static solim.UI.slider;
import static solim.UI.spacer;
import static solim.UI.text;
import static solim.UI.unit;

import arc.Core;
import arc.scene.Element;
import mindustry.ui.Styles;
import solim.core.BaseComponent;
import solim.reactive.Readable;

public class PathfindingSettingsView extends BaseComponent {

    private final PathfindingFeature feature;

    private static final String[] COST_KEY_SUFFIXES = {
        "ground", "legs", "naval", "neoplasm", "none", "hover"
    };

    public PathfindingSettingsView(PathfindingFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<String> zoomText = feature.zoomThresholdConfig.signal().map(v -> {
            float zoom = v != null ? v : 0.5f;
            return zoom <= 0.01f
                    ? Core.bundle.get("feature.pathfinding.settings.off", "Off")
                    : String.format("%.1fx", zoom);
        });

        Readable<String> opacityText = feature.opacityConfig.signal().map(v -> {
            float op = v != null ? v : 1.0f;
            return Math.round(op * 100) + "%";
        });

        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    checkbox(Core.bundle.get("feature.pathfinding.settings.draw-unit-path", "Draw Unit Paths"),
                            feature.drawUnitPathConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.pathfinding.settings.draw-spawn-path", "Draw Spawn Point Paths"),
                            feature.drawSpawnPathConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.pathfinding.settings.draw-allies", "Draw Ally Unit Paths"),
                            feature.drawAlliesConfig.signal()).growX();

                    divider();

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.pathfinding.settings.min-zoom", "Min Zoom")).left();
                        spacer();
                        slider(feature.zoomThresholdConfig.signal(), 0.0f, 5.0f, 0.1f);
                        row().width(unit(12)).children(() -> {
                            text(zoomText);
                        });
                    });

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.pathfinding.settings.opacity", "Opacity")).left();
                        spacer();
                        slider(feature.opacityConfig.signal(), 0.05f, 1.0f, 0.05f);
                        row().width(unit(12)).children(() -> {
                            text(opacityText);
                        });
                    });

                    divider();

                    text(Core.bundle.get("feature.pathfinding.settings.cost-types", "Cost Types")).left();

                    for (int i = 0; i < feature.costTypeConfigs.length; i++) {
                        String bundleKey = "feature.pathfinding.settings.cost." + COST_KEY_SUFFIXES[i];
                        checkbox(Core.bundle.get(bundleKey, COST_KEY_SUFFIXES[i]),
                                feature.costTypeConfigs[i].signal()).growX();
                    }

                    divider();

                    button(Core.bundle.get("feature.pathfinding.settings.reset", "Reset to Defaults"),
                            feature::resetToDefaults)
                            .style(Styles.defaultb)
                            .growX();
                });
            });
        }).element();
    }
}
