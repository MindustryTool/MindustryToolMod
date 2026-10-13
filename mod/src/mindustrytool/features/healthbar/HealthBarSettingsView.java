package mindustrytool.features.healthbar;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import java.util.Locale;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingKeybindCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.reactive.Signal;

/**
 * Modern declarative Solim settings view for Health Bar options.
 */
public class HealthBarSettingsView extends Component {

    private final HealthBarFeature feature;

    public HealthBarSettingsView(HealthBarFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            targetLayersCard();
            visualStylingCard();
            keybindsCard();
            actionsCard();
        });
    }

    private void keybindsCard() {
        SettingKeybindCard.forFeature(feature);
    }

    private Component headerCard() {
        return SettingCard.header(feature, "TACTICAL");
    }

    private Component targetLayersCard() {
        return SettingCard.section(Icon.units, Core.bundle.get("feature.health-bar.settings.section.targets"), () -> {
            checkboxRow(Core.bundle.get("feature.health-bar.settings.show-friendly-units"),
                    feature.showFriendlyUnitsConfig.signal());

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkboxRow(Core.bundle.get("feature.health-bar.settings.show-enemy-units"),
                    feature.showEnemyUnitsConfig.signal());

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkboxRow(Core.bundle.get("feature.health-bar.settings.show-friendly-blocks"),
                    feature.showFriendlyBlocksConfig.signal());

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkboxRow(Core.bundle.get("feature.health-bar.settings.show-enemy-blocks"),
                    feature.showEnemyBlocksConfig.signal());
        });
    }

    private Component checkboxRow(String label, Signal<Boolean> signal) {
        return row().growX().gap(unit(2)).center().children(() -> {
            text(label).color(Color.white).growX().left();
            spacer();
            checkbox("", signal);
        });
    }

    private Component visualStylingCard() {
        return SettingCard.section(Icon.image, Core.bundle.get("feature.health-bar.settings.section.visual"), () -> {

            SettingSlider.of(
                    Core.bundle.get("feature.health-bar.settings.min-zoom"),
                    feature.zoomThresholdConfig.signal(),
                    0f,
                    2f,
                    0.1f,
                    feature.zoomThresholdConfig.signal().map(v -> {
                        float val = v != null ? v : 0.5f;
                        return val <= 0.01f
                                ? Core.bundle.get("feature.health-bar.settings.off")
                                : String.format(Locale.US, "%.1fx", val);
                    }));

            SettingSlider.of(
                    Core.bundle.get("feature.health-bar.settings.opacity"),
                    feature.opacityConfig.signal(),
                    0f,
                    1f,
                    0.05f,
                    feature.opacityConfig.signal()
                            .map(v -> String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100)));

            SettingSlider.of(
                    Core.bundle.get("feature.health-bar.settings.scale"),
                    feature.scaleConfig.signal(),
                    0.5f,
                    1.5f,
                    0.1f,
                    feature.scaleConfig.signal()
                            .map(v -> String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100)));

            SettingSlider.of(
                    Core.bundle.get("feature.health-bar.settings.width"),
                    feature.widthConfig.signal(),
                    0.5f,
                    2f,
                    0.1f,
                    feature.widthConfig.signal()
                            .map(v -> String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100)));
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
                        text(Core.bundle.get("feature.health-bar.settings.reset"));
                    });
        });
    }
}
