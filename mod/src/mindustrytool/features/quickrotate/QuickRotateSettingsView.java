package mindustrytool.features.quickrotate;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;

import arc.Core;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.WebStyles;
import solim.core.Component;

public class QuickRotateSettingsView extends Component {

    private final QuickRotateFeature feature;

    public QuickRotateSettingsView(QuickRotateFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            optionsCard();
            actionsCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, "CONTROLS");
    }

    private Component optionsCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("feature.quick-rotate.settings.title"), () -> {
            checkbox(Core.bundle.get("feature.quick-rotate.settings.auto-dismiss"),
                    feature.autoDismissConfig.signal()).props(growX());

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkbox(Core.bundle.get("feature.quick-rotate.settings.offset-above"),
                    feature.offsetAboveConfig.signal()).props(growX());
        });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(feature::resetToDefaults)
                    .style(WebStyles.outline())
                    .props(growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(2)))
                    .children(() -> {
                        icon(Icon.refresh).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                        text(Core.bundle.get("feature.quick-rotate.settings.reset"));
                    });
        });
    }
}
