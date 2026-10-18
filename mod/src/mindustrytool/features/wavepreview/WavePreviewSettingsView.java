package mindustrytool.features.wavepreview;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;

import arc.Core;
import arc.scene.Element;
import java.util.Locale;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingKeybindCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.core.Component;

public class WavePreviewSettingsView extends Component {

    private final WavePreviewFeature feature;

    public WavePreviewSettingsView(WavePreviewFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            settingsCard();
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

    private Component settingsCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("feature.wave-preview.settings.section.settings"),
                () -> {

                    SettingSlider.of(
                            Core.bundle.get("feature.wave-preview.settings.opacity"),
                            feature.opacityConfig.signal(),
                            0.1f,
                            1.0f,
                            0.05f,
                            feature.opacityConfig.signal()
                                    .map(v -> String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100)));

                    SettingSlider.of(
                            Core.bundle.get("feature.wave-preview.settings.scale"),
                            feature.scaleConfig.signal(),
                            0.5f,
                            2.0f,
                            0.1f,
                            feature.scaleConfig.signal()
                                    .map(v -> String.format(Locale.US, "%.0f%%", (v != null ? v : 1f) * 100)));

                    SettingSlider.of(
                            Core.bundle.get("feature.wave-preview.settings.depth"),
                            feature.depthConfig.signal(),
                            WavePreviewFeature.MIN_DEPTH,
                            WavePreviewFeature.MAX_DEPTH,
                            1,
                            feature.depthConfig.signal()
                                    .map(v -> String.valueOf(v != null ? v : WavePreviewFeature.DEFAULT_DEPTH)));
                });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(feature::resetToDefaults)
                    .props(style(WebStyles.outline()), growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(2)))
                    .children(() -> {
                        icon(Icon.refresh).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                        text(Core.bundle.get("feature.wave-preview.settings.reset"));
                    });
        });
    }
}
