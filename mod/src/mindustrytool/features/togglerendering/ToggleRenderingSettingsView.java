package mindustrytool.features.togglerendering;

import static solim.UI.column;
import static solim.UI.checkbox;
import static solim.UI.scroll;
import static solim.UI.unit;

import arc.Core;
import arc.scene.Element;
import solim.core.BaseComponent;

/**
 * Declarative Solim settings view for Toggle Rendering options.
 */
public class ToggleRenderingSettingsView extends BaseComponent {

    private final ToggleRenderingFeature feature;

    public ToggleRenderingSettingsView(ToggleRenderingFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    checkbox(Core.bundle != null
                                    ? Core.bundle.get("feature.toggle-rendering.settings.draw-units-allies")
                                    : "Draw Allied Units",
                            feature.drawUnitsAlliesConfig.signal()).growX();

                    checkbox(Core.bundle != null
                                    ? Core.bundle.get("feature.toggle-rendering.settings.draw-units-enemies")
                                    : "Draw Enemy Units",
                            feature.drawUnitsEnemiesConfig.signal()).growX();

                    checkbox(Core.bundle != null
                                    ? Core.bundle.get("feature.toggle-rendering.settings.draw-blocks")
                                    : "Draw Blocks & Buildings",
                            feature.drawBlocksConfig.signal()).growX();
                });
            });
        }).element();
    }
}
