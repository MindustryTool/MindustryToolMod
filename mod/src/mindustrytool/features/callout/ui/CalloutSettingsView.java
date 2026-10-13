package mindustrytool.features.callout.ui;

import static solim.UI.*;

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

            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(0.5f)).left().children(() -> {
                    text(Core.bundle.get("feature.callout.settings.team-chat"))
                            .color(Color.white)
                            .left();
                    text(Core.bundle.get("feature.callout.settings.team-chat.description"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });
                spacer();
                checkbox("", feature.teamChatConfig.signal());
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(0.5f)).left().children(() -> {
                    text(Core.bundle.get("feature.callout.settings.close-on-send"))
                            .color(Color.white)
                            .left();
                    text(Core.bundle.get("feature.callout.settings.close-on-send.description"))
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });
                spacer();
                checkbox("", feature.closeOnSendConfig.signal());
            });
        });
    }

    private Component resetButton() {
        return row().growX().children(() -> {
            button(feature::resetToDefaults)
                    .style(WebStyles.outline())
                    .height(unit(8.5f))
                    .growX()
                    .children(() -> {
                        row().gap(unit(1.5f)).center().children(() -> {
                            icon(Icon.refresh).size(unit(4));
                            text(Core.bundle.get("feature.callout.settings.reset"));
                        });
                    });
        });
    }
}
