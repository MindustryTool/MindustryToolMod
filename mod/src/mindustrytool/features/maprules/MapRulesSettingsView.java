package mindustrytool.features.maprules;

import static solim.UI.checkbox;
import static solim.UI.column;
import static solim.UI.scroll;
import static solim.UI.unit;

import arc.Core;
import arc.scene.Element;
import solim.core.BaseComponent;

/**
 * Settings view for Map Rules feature.
 */
public class MapRulesSettingsView extends BaseComponent {

    private final MapRulesFeature feature;

    public MapRulesSettingsView(MapRulesFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    checkbox(Core.bundle.get("feature.map-rules.settings.show-permission"),
                            feature.showPermissionConfig.signal()).growX();
                    checkbox(Core.bundle.get("feature.map-rules.settings.show-descriptions"),
                            feature.showDescriptionsConfig.signal()).growX();
                });
            });
        }).element();
    }
}
