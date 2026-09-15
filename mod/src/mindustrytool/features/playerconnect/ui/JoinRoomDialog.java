package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustrytool.features.playerconnect.net.PlayerConnectClient;
import mindustrytool.features.playerconnect.net.PlayerConnectLink;
import solim.core.BaseComponent;
import solim.overlay.SolimDialog;
import solim.signal.Computed;
import solim.signal.Signal;

public class JoinRoomDialog extends SolimDialog {

    private static final String LAST_LINK_KEY = "mindustrytool.player-connect.last-link";

    public JoinRoomDialog() {
        super(Core.bundle.get("feature.player-connect.join-link-title", "Join via Link"));

        name("joinRoomDialog");
        addCloseButton();
        closeOnBack();

        children(() -> new JoinRoomView(this));
    }

    private static class JoinRoomView extends BaseComponent {
        private final JoinRoomDialog dialog;
        private final Signal<String> linkSignal;
        private final Signal<String> passwordSignal = Signal.of("");

        public JoinRoomView(JoinRoomDialog dialog) {
            this.dialog = dialog;

            String clipboard = Core.app.getClipboardText();
            String initial = (clipboard != null && PlayerConnectLink.isValid(clipboard))
                    ? clipboard
                    : Core.settings.getString(LAST_LINK_KEY, "player-connect://");

            this.linkSignal = Signal.of(initial);
            this.linkSignal.subscribe(val -> {
                if (val != null) {
                    Core.settings.put(LAST_LINK_KEY, val);
                }
            });
        }

        @Override
        protected Element build() {
            Computed<Boolean> isValid = linkSignal.map(PlayerConnectLink::isValid);
            Computed<String> statusMessage = isValid.map(valid -> valid
                    ? Core.bundle.get("feature.player-connect.link-valid", "Valid link format")
                    : Core.bundle.get("feature.player-connect.link-invalid", "Invalid link (expected player-connect://host:port/roomId)"));

            return column()
                    .growX()
                    .margin(unit(4))
                    .gap(unit(3))
                    .center()
                    .children(() -> {
                        // Link input
                        row().growX().gap(unit(2)).children(() -> {
                            text(Core.bundle.get("feature.player-connect.join-link", "Link:")).width(unit(20)).left();
                            textField(linkSignal)
                                    .growX()
                                    .height(unit(9));
                        });

                        // Password input
                        row().growX().gap(unit(2)).children(() -> {
                            text(Core.bundle.get("feature.player-connect.password", "Password:")).width(unit(20)).left();
                            textField(passwordSignal)
                                    .growX()
                                    .height(unit(9));
                        });

                        // Validation text
                        text(statusMessage)
                                .color(isValid.map(v -> v ? Color.green : Color.scarlet));

                        // Join button
                        button(Core.bundle.get("join", "Join"), this::join)
                                .style(Styles.defaultb)
                                .color(Color.royal)
                                .size(unit(36), unit(10))
                                .enabled(isValid);
                    }).element();
        }

        private void join() {
            if (Vars.player == null || Vars.player.name == null || Vars.player.name.trim().isEmpty()) {
                Vars.ui.showInfo("@noname");
                return;
            }

            try {
                PlayerConnectLink link = PlayerConnectLink.fromString(linkSignal.get());
                dialog.hide();
                PlayerConnectClient.join(link, passwordSignal.get(), () -> {
                });
            } catch (Exception e) {
                Vars.ui.showErrorMessage(e.getMessage());
            }
        }
    }
}
