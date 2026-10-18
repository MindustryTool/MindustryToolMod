package mindustrytool.features.chat;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;

import arc.Core;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingKeybindCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.core.Component;

public class ChatSettingsView extends Component {

    private final ChatFeature feature;

    public ChatSettingsView(ChatFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            layoutCard();
            keybindsCard();
            actionsCard();
        });
    }

    private void keybindsCard() {
        SettingKeybindCard.forFeature(feature);
    }

    private Component headerCard() {
        return SettingCard.header(feature, Core.bundle.get("feature.chat.badge.communication", "COMMUNICATION"));
    }

    private Component layoutCard() {
        return SettingCard.section(Icon.layers,
                Core.bundle.get("feature.chat.settings.overlay-layout", "Overlay Layout"), () -> {

                    SettingSlider.of(
                            Core.bundle.get("feature.chat.settings.opacity", "Opacity"),
                            feature.opacityConfig.signal(),
                            0.2f,
                            1.0f,
                            0.05f,
                            feature.opacityConfig.signal()
                                    .map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));

                    SettingSlider.of(
                            Core.bundle.get("feature.chat.settings.width", "Width"),
                            feature.widthRatioConfig.signal(),
                            0.4f,
                            1.0f,
                            0.05f,
                            feature.widthRatioConfig.signal()
                                    .map(v -> String.format("%.0f%%", (v != null ? v : 0.6f) * 100)));

                    SettingSlider.of(
                            Core.bundle.get("feature.chat.settings.height", "Height"),
                            feature.heightRatioConfig.signal(),
                            0.4f,
                            1.0f,
                            0.05f,
                            feature.heightRatioConfig.signal()
                                    .map(v -> String.format("%.0f%%", (v != null ? v : 0.6f) * 100)));
                });
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(feature::resetPosition)
                    .props(style(WebStyles.outline()), growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(2)))
                    .children(() -> {
                        icon(Icon.move).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                        text(Core.bundle.get("feature.chat.settings.reset-position", "Reset Position"));
                    });

            button(feature::resetAppearance)
                    .props(style(WebStyles.outline()), growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(2)))
                    .children(() -> {
                        icon(Icon.refresh).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)));
                        text(Core.bundle.get("feature.chat.settings.reset-appearance", "Reset Appearance"));
                    });
        });
    }
}
