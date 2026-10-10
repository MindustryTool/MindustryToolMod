package mindustrytool.features.quickrotate;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import solim.core.BaseComponent;

/**
 * Declarative Solim settings view for quick rotate options.
 */
public class QuickRotateSettingsView extends BaseComponent {

    private final QuickRotateFeature feature;

    public QuickRotateSettingsView(QuickRotateFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    checkbox(Core.bundle.get("feature.quick-rotate.settings.auto-dismiss"),
                            feature.autoDismissConfig.signal()).growX();

                    checkbox(Core.bundle.get("feature.quick-rotate.settings.offset-above"),
                            feature.offsetAboveConfig.signal()).growX();
                });
            });
        }).element();
    }
}
