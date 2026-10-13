package mindustrytool.features.smartdrill.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import mindustrytool.features.smartdrill.SmartDrillFeature;
import solim.core.Component;

public class SmartDrillSettingsView extends Component {

    private final SmartDrillFeature feature;

    public SmartDrillSettingsView(SmartDrillFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            settingsCard();
            resetButtonCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, "AUTOMATION");
    }

    private Component settingsCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("feature.smart-drill.settings.title"), () -> {

            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(1.5f)).left().children(() -> {
                    text(Core.bundle.get("feature.smart-drill.settings.auto-disable"))
                            .color(Color.white)
                            .left();
                    text(Core.bundle.get("feature.smart-drill.settings.auto-disable.desc"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });
                spacer();
                checkbox("", feature.autoDisableConfig.signal());
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            SettingSlider.of(
                    Core.bundle.get("feature.smart-drill.settings.max-tiles"),
                    Core.bundle.get("feature.smart-drill.settings.max-tiles.desc"),
                    feature.maxTilesConfig.signal(),
                    20,
                    300,
                    10,
                    feature.maxTilesConfig.signal().map(v -> String.valueOf(v != null ? v : 100)));
        });
    }

    private Component resetButtonCard() {
        return SettingCard.card().children(() -> {
            button(Core.bundle.get("feature.smart-drill.settings.reset"),
                    feature::resetToDefaults)
                    .style(WebStyles.outlineText())
                    .height(unit(11))
                    .growX();
        });
    }
}
