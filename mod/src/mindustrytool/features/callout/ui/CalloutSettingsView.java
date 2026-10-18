package mindustrytool.features.callout.ui;

import static solim.UI.*;
import static solim.modifier.Values.color;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingKeybindCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.WebStyles;
import mindustrytool.features.callout.CalloutFeature;
import solim.core.Component;

public class CalloutSettingsView extends Component {

    private final CalloutFeature feature;

    public CalloutSettingsView(CalloutFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            settingsCard();
            keybindsCard();
            resetButton();
        });
    }

    private void keybindsCard() {
        SettingKeybindCard.forFeature(feature);
    }

    private Component headerCard() {
        return SettingCard.header(feature, "TACTICAL");
    }

    private Component settingsCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("feature.callout.settings.general"), () -> {

            row().props(growX(), gap(unit(2)), center()).children(() -> {
                column().props(growX(), gap(unit(0.5f)), left()).children(() -> {
                    text(Core.bundle.get("feature.callout.settings.team-chat"))
                            .props(color(Color.white), left());
                    text(Core.bundle.get("feature.callout.settings.team-chat.description"))
                            .props(color(WebStyles.Colors.GHOST_FG), left(), solim.modifier.Values.wrap(true), growX());
                });
                spacer();
                checkbox("", feature.teamChatConfig.signal());
            });

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            row().props(growX(), gap(unit(2)), center()).children(() -> {
                column().props(growX(), gap(unit(0.5f)), left()).children(() -> {
                    text(Core.bundle.get("feature.callout.settings.close-on-send"))
                            .props(color(Color.white), left());
                    text(Core.bundle.get("feature.callout.settings.close-on-send.description"))
                            .props(color(WebStyles.Colors.GHOST_FG), left(), solim.modifier.Values.wrap(true), growX());
                });
                spacer();
                checkbox("", feature.closeOnSendConfig.signal());
            });
        });
    }

    private Component resetButton() {
        return row().props(growX()).children(() -> {
            button(feature::resetToDefaults)
                    .props(style(WebStyles.outline()), height(unit(8.5f)), prefHeight(unit(8.5f)), growX())
                    .children(() -> {
                        row().props(gap(unit(1.5f)), center()).children(() -> {
                            icon(Icon.refresh).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)));
                            text(Core.bundle.get("feature.callout.settings.reset"));
                        });
                    });
        });
    }
}
