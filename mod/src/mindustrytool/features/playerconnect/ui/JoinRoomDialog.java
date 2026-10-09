package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.Vars;
import mindustrytool.components.WebStyles;
import mindustrytool.features.playerconnect.net.PlayerConnectClient;
import mindustrytool.features.playerconnect.net.PlayerConnectLink;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import solim.core.BaseComponent;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Signal;

public class JoinRoomDialog extends SolimDialog {

    private static final String LAST_LINK_KEY = "mindustrytool.player-connect.last-link";
    private final PlayerConnectFeature feature;

    public JoinRoomDialog(PlayerConnectFeature feature) {
        super(Core.bundle.get("feature.player-connect.join-link-title", "Join via Link"));

        this.feature = feature;

        name("joinRoomDialog");
        addCloseButton();
        closeOnBack();
        cont().center();

        children(() -> new JoinRoomView());
    }

    private class JoinRoomView extends BaseComponent {

        public JoinRoomView() {
        }

        @Override
        protected Element build() {
            String clipboard = Core.app.getClipboardText();

            Signal<String> passwordSignal = Signal.of("");
            Signal<String> linkSignal = Signal.of((clipboard != null && PlayerConnectLink.isValid(clipboard))
                    ? clipboard
                    : Core.settings.getString(LAST_LINK_KEY, "player-connect://"));

            linkSignal.subscribe(val -> {
                if (val != null) {
                    Core.settings.put(LAST_LINK_KEY, val);
                }
            });

            Computed<Boolean> isValid = linkSignal.map(PlayerConnectLink::isValid);
            Computed<String> statusMessage = isValid.map(valid -> valid
                    ? Core.bundle.get("feature.player-connect.link-valid", "Valid link format")
                    : Core.bundle.get("feature.player-connect.link-invalid",
                            "Invalid link (expected player-connect://host:port/roomId)"));

            Computed<Boolean> hasPassword = feature.getRoomsQuery()
                    .data()
                    .map(rooms -> rooms == null ? true
                            : rooms.stream()
                                    .filter(room -> room.getLink().equals(linkSignal.get()))
                                    .findFirst()
                                    .map(room -> room.getData().isSecured())
                                    .orElse(true));

            return column()
                    .growX()
                    .maxWidth(dvw(90f).map(w -> Math.min(w, 1000f)))
                    .maxHeight(dvh(85f).map(h -> Math.min(h, 800f)))
                    .margin(unit(5))
                    .gap(unit(4))
                    .center()
                    .children(() -> {
                        // Link input
                        column().growX().gap(unit(1.5f)).left().children(() -> {
                            text(Core.bundle.get("feature.player-connect.join-link", "Link:")).left();
                            row().growX().height(unit(11)).border(1.5f, Color.darkGray).paddingX(unit(2))
                                    .rounded(unit(2))
                                    .children(() -> {
                                        textField(linkSignal)
                                                .grow()
                                                .style(WebStyles.clearInput());
                                    });
                        });

                        // Password input
                        when(hasPassword)
                                .growX()
                                .thenDo(() -> {
                                    column().growX().gap(unit(1.5f)).left().children(() -> {
                                        text(Core.bundle.get("feature.player-connect.password", "Password:")).left();
                                        row().growX().height(unit(11)).border(1.5f, Color.darkGray).paddingX(unit(2))
                                                .rounded(unit(2))
                                                .children(() -> {
                                                    textField(passwordSignal)
                                                            .grow()
                                                            .style(WebStyles.clearInput());
                                                });
                                    });
                                });

                        // Validation text
                        text(statusMessage)
                                .color(isValid.map(v -> v ? Color.green : Color.scarlet));

                        // Join button
                        button(Core.bundle.get("join", "Join"), () -> join(linkSignal.peek(), passwordSignal.peek()))
                                .style(WebStyles.primary())
                                .height(unit(11))
                                .paddingX(unit(8))
                                .paddingY(unit(2.5f))
                                .minWidth(unit(36))
                                .enabled(isValid);
                    }).element();
        }

        private void join(String link, String password) {
            if (Vars.player == null || Vars.player.name == null || Vars.player.name.trim().isEmpty()) {
                Vars.ui.showInfo("@noname");
                return;
            }

            try {
                PlayerConnectLink l = PlayerConnectLink.fromString(link);
                hide();
                PlayerConnectClient.join(l, password, () -> {
                });
            } catch (Exception e) {
                Vars.ui.showErrorMessage(e.getMessage());
            }
        }
    }
}
