package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.Vars;
import mindustry.graphics.Pal;
import mindustrytool.components.WebStyles;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import mindustrytool.features.playerconnect.net.PlayerConnectLink;
import solim.core.Units;
import solim.core.Component;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Signal;

public class ManageRoomDialog extends SolimDialog {

    public ManageRoomDialog(PlayerConnectFeature feature) {
        super(Core.bundle.get("feature.player-connect.manage-room", "Manage Room"));

        name("manageRoomDialog");
        addCloseButton();
        closeOnBack();
        cont().center();

        children(() -> new ManageRoomView(feature, this));
    }

    private static class ManageRoomView extends Component {
        private final PlayerConnectFeature feature;
        private final ManageRoomDialog dialog;

        public ManageRoomView(PlayerConnectFeature feature, ManageRoomDialog dialog) {
            this.feature = feature;
            this.dialog = dialog;
        }

        @Override
        protected Element build() {
            Computed<String> linkText = Signal.computed(() -> {
                PlayerConnectLink link = feature.getActiveLink();
                return link != null ? link.toString()
                        : Core.bundle.get("feature.player-connect.no-link", "No active link");
            });

            Computed<String> pingText = feature.pingSignal().map(p -> (p != null ? p : 0) + "ms");

            return column()
                    .props(growX(), solim.UI.maxWidth(Units.dvw.map(w -> Math.min(w * 0.9f, 900f))), maxHeight(Units.dvh.map(h -> Math.min(h * 0.85f, 800f))), margin(unit(5)), gap(unit(4)), center())
                    .children(() -> {
                        text(Core.bundle.get("feature.player-connect.room-active",
                                "Your room is live on the relay network!"))
                                        .props(color(Color.green));

                        row().props(gap(unit(3)), center()).children(() -> {
                            text(Core.bundle.get("feature.player-connect.relay-ping", "Relay Ping: "))
                                    .props(color(Color.lightGray));
                            text(pingText).props(color(Pal.accent));
                        });

                        row().props(gap(unit(3)), center()).children(() -> {
                            text(Core.bundle.get("feature.player-connect.join-link", "Link: ")).props(color(Color.lightGray));
                            text(linkText).props(color(Color.white));
                        });

                        row().props(gap(unit(3)), center()).children(() -> {
                            button(Core.bundle.get("feature.player-connect.copy-link", "Copy Join Link"), this::copyLink)
                                    .props(style(WebStyles.outline()), height(unit(11)), prefHeight(unit(11)), paddingX(unit(6)), paddingY(unit(2.5f)), minWidth(unit(36)));

                            button(Core.bundle.get("feature.player-connect.close-room", "Close Room"), this::closeRoom)
                                    .props(style(WebStyles.danger()), height(unit(11)), prefHeight(unit(11)), paddingX(unit(6)), paddingY(unit(2.5f)), minWidth(unit(36)));
                        });
                    }).element();
        }

        private void copyLink() {
            PlayerConnectLink link = feature.getActiveLink();
            if (link != null) {
                Core.app.setClipboardText(link.toString());
                Vars.ui.showInfoFade("@copied");
            }
        }

        private void closeRoom() {
            feature.closeRoom();
            dialog.hide();
            Vars.ui.showInfoFade("@feature.player-connect.room-closed");
        }
    }
}