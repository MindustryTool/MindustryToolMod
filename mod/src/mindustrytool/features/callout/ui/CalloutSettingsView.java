package mindustrytool.features.callout.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustrytool.components.WebStyles;
import mindustrytool.features.callout.CalloutFeature;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.Card;

/**
 * Modern declarative Solim settings view for Tactical Callouts.
 */
public class CalloutSettingsView extends BaseComponent {

    private final CalloutFeature feature;

    public CalloutSettingsView(CalloutFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().maxWidth(500f).children(() -> {
            scroll().grow().scrollX(false).children(() -> {
                column().growX().gap(unit(2.5f)).padding(unit(2)).center().children(() -> {
                    headerCard();
                    settingsCard();
                    resetButton();
                });
            });
        }).element();
    }

    private Card sectionCard() {
        return card()
                .growX()
                .padding(unit(2.5f))
                .rounded(8, WebStyles.Colors.SECTION_BG)
                .border(1.5f, WebStyles.Colors.SECTION_BORDER)
                .gap(unit(2));
    }

    private Component headerCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.chat).size(unit(5)).color(Pal.accent);
                text(Core.bundle != null
                        ? Core.bundle.get("feature.callout.name", "Callout")
                        : "Callout")
                        .color(Pal.accent)
                        .growX()
                        .left();
                badge("TACTICAL").color(WebStyles.Colors.PRIMARY);
            });

            text(Core.bundle != null
                    ? Core.bundle.get("feature.callout.description",
                            "Quick tactical in-game callouts and resource requests with a single click.")
                    : "Quick tactical in-game callouts and resource requests with a single click.")
                    .color(WebStyles.Colors.GHOST_FG)
                    .left()
                    .wrap()
                    .growX();
        });
    }

    private Component settingsCard() {
        return sectionCard().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                icon(Icon.settings).size(unit(5)).color(Pal.accent);
                text(Core.bundle != null
                        ? Core.bundle.get("feature.callout.settings.general", "General Settings")
                        : "General Settings")
                        .color(Pal.accent)
                        .growX()
                        .left();
            });

            // Team Chat by default
            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(0.5f)).left().children(() -> {
                    text(Core.bundle != null
                            ? Core.bundle.get("feature.callout.settings.team-chat", "Team Chat Default")
                            : "Team Chat Default")
                            .color(Color.white)
                            .left();
                    text(Core.bundle != null
                            ? Core.bundle.get("feature.callout.settings.team-chat.description",
                                    "Send callouts to team chat (/t) by default instead of public chat.")
                            : "Send callouts to team chat (/t) by default instead of public chat.")
                            .color(WebStyles.Colors.GHOST_FG)
                            .left()
                            .wrap()
                            .growX();
                });
                spacer();
                checkbox("", feature.teamChatConfig.signal());
            });

            divider().color(WebStyles.Colors.SECTION_BORDER);

            // Close on send
            row().growX().gap(unit(2)).center().children(() -> {
                column().growX().gap(unit(0.5f)).left().children(() -> {
                    text(Core.bundle != null
                            ? Core.bundle.get("feature.callout.settings.close-on-send", "Close on Send")
                            : "Close on Send")
                            .color(Color.white)
                            .left();
                    text(Core.bundle != null
                            ? Core.bundle.get("feature.callout.settings.close-on-send.description",
                                    "Automatically close the callout dialog after sending a message.")
                            : "Automatically close the callout dialog after sending a message.")
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
                            text(Core.bundle != null
                                    ? Core.bundle.get("feature.callout.settings.reset", "Reset to Defaults")
                                    : "Reset to Defaults");
                        });
                    });
        });
    }
}
