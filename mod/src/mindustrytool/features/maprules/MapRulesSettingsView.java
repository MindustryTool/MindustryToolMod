package mindustrytool.features.maprules;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingKeybindCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.WebStyles;
import solim.core.Component;

public class MapRulesSettingsView extends Component {

    private final MapRulesFeature feature;

    public MapRulesSettingsView(MapRulesFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            optionsCard();
            keybindsCard();
        });
    }

    private void keybindsCard() {
        SettingKeybindCard.forFeature(feature);
    }

    private Component headerCard() {
        return SettingCard.header(feature, "GAMEPLAY");
    }

    private Component optionsCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("feature.map-rules.settings.title"), () -> {
            checkbox(Core.bundle.get("feature.map-rules.settings.show-permission"),
                    feature.showPermissionConfig.signal()).growX();

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkbox(Core.bundle.get("feature.map-rules.settings.show-descriptions"),
                    feature.showDescriptionsConfig.signal()).growX();
        });
    }
}
