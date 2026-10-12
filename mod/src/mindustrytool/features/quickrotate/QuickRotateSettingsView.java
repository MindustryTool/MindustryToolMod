package mindustrytool.features.quickrotate;

import static solim.UI.*;

import arc.Core;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.core.Component;

/**
 * Modern declarative Solim settings view for quick rotate options.
 */
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
                    feature.autoDismissConfig.signal()).growX();

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkbox(Core.bundle.get("feature.quick-rotate.settings.offset-above"),
                    feature.offsetAboveConfig.signal()).growX();
        });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(feature::resetToDefaults)
                    .style(WebStyles.outline())
                    .growX()
                    .height(unit(11))
                    .gap(unit(2))
                    .children(() -> {
                        icon(Icon.refresh).size(unit(6));
                        text(Core.bundle.get("feature.quick-rotate.settings.reset"));
                    });
        });
    }
}
