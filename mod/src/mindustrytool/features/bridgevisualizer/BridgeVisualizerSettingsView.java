package mindustrytool.features.bridgevisualizer;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.ui.Styles;
import solim.core.BaseComponent;
import solim.reactive.Readable;

/**
 * Declarative Solim settings view for Bridge Visualizer options.
 */
public class BridgeVisualizerSettingsView extends BaseComponent {

    private final BridgeVisualizerFeature feature;

    public BridgeVisualizerSettingsView(BridgeVisualizerFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<String> scaleText = feature.itemScaleConfig.signal()
                .map(v -> Math.round((v != null ? v : 1f) * 100) + "%");

        Readable<String> opacityText = feature.opacityConfig.signal()
                .map(v -> Math.round((v != null ? v : 1f) * 100) + "%");

        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    // Item Bridges Checkbox
                    checkbox(Core.bundle.get("feature.bridge-visualizer.settings.show-item-bridges", "Show Item Bridges"),
                            feature.showItemBridgesConfig.signal()).growX();

                    // Duct Bridges Checkbox
                    checkbox(Core.bundle.get("feature.bridge-visualizer.settings.show-duct-bridges", "Show Duct Bridges"),
                            feature.showDuctBridgesConfig.signal()).growX();

                    // Liquid Bridges Checkbox
                    checkbox(Core.bundle.get("feature.bridge-visualizer.settings.show-liquid-bridges", "Show Liquid Bridges"),
                            feature.showLiquidBridgesConfig.signal()).growX();

                    divider();

                    // Item Scale Slider
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.bridge-visualizer.settings.item-scale", "Item Scale")).left();
                        spacer();
                        slider(feature.itemScaleConfig.signal(), 0.5f, 2.0f, 0.1f);
                        row().width(unit(12)).children(() -> {
                            text(scaleText);
                        });
                    });

                    // Opacity Slider
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.bridge-visualizer.settings.opacity", "Opacity")).left();
                        spacer();
                        slider(feature.opacityConfig.signal(), 0.2f, 1.0f, 0.05f);
                        row().width(unit(12)).children(() -> {
                            text(opacityText);
                        });
                    });

                    divider();

                    // Reset Defaults Button
                    button(Core.bundle.get("feature.bridge-visualizer.settings.reset", "Reset to Defaults"), feature::resetToDefaults)
                            .style(Styles.defaultb).growX();
                });
            });
        }).element();
    }
}
