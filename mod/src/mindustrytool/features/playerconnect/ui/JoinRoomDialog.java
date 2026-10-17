package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.margin;
import static solim.modifier.Values.maxHeight;
import static solim.modifier.Values.minWidth;
import static solim.modifier.Values.paddingX;
import static solim.modifier.Values.paddingY;
import static solim.modifier.Values.prefHeight;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustry.Vars;
import mindustrytool.components.WebStyles;
import mindustrytool.features.playerconnect.net.PlayerConnectClient;
import mindustrytool.features.playerconnect.net.PlayerConnectLink;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import solim.core.Units;
import solim.modifier.Values;
import solim.core.Component;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Signal;

public class JoinRoomDialog extends SolimDialog {
    private static final String LAST_LINK_KEY = "mindustrytool.player-connect.last-link";

    private final PlayerConnectFeature feature;
    private final Signal<String> passwordSignal;
    private final Signal<String> linkSignal;

    public JoinRoomDialog(PlayerConnectFeature feature) {
        super(Core.bundle.get("feature.player-connect.join-link-title", "Join via Link"));

        this.feature = feature;

        name("joinRoomDialog");
        addCloseButton();
        closeOnBack();
        cont().center();

        String clipboard = Core.app.getClipboardText();
        passwordSignal = Signal.of("");
        linkSignal = Signal.of((clipboard != null && PlayerConnectLink.isValid(clipboard))
                ? clipboard
                : Core.settings.getString(LAST_LINK_KEY, "player-connect://"));

        Effect.of(linkSignal, val -> {
            if (val != null) {
                Core.settings.put(LAST_LINK_KEY, val);
            }
        });

        shown(this::update);

        children(() -> new JoinRoomView());
    }

    private void update() {
        String clipboard = Core.app.getClipboardText();
        if (clipboard != null && PlayerConnectLink.isValid(clipboard)) {
            linkSignal.set(clipboard);
        }
    }

    private class JoinRoomView extends Component {
        @Override
        protected Element build() {

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
                    .props(growX(), Values.maxWidth(Units.dvw.map(w -> Math.min(w * 0.9f, 1000f))), maxHeight(Units.dvh.map(h -> Math.min(h * 0.85f, 800f))), margin(unit(5)), gap(unit(4)), center())
                    .children(() -> {

                        column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                            text(Core.bundle.get("feature.player-connect.join-link", "Link:")).left();
                            row().props(growX(), height(unit(11)), prefHeight(unit(11)), Values.border(1.5f, Color.darkGray), paddingX(unit(2)), Values.rounded(unit(2)))
                                    .children(() -> {
                                        textField(linkSignal)
                                                .props(grow())
                                                .style(WebStyles.clearInput());
                                    });
                        });

                        when(hasPassword)
                                .props(growX())
                                .thenDo(() -> {
                                    column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                                        text(Core.bundle.get("feature.player-connect.password", "Password:")).left();
                                        row().props(growX(), height(unit(11)), prefHeight(unit(11)), Values.border(1.5f, Color.darkGray), paddingX(unit(2)), Values.rounded(unit(2)))
                                                .children(() -> {
                                                    textField(passwordSignal)
                                                            .props(grow())
                                                            .style(WebStyles.clearInput());
                                                });
                                    });
                                });

                        text(statusMessage)
                                .color(isValid.map(v -> v ? Color.green : Color.scarlet));

                        button(Core.bundle.get("join", "Join"), () -> join(linkSignal.peek(), passwordSignal.peek()))
                                .style(WebStyles.primary())
                                .props(height(unit(11)), prefHeight(unit(11)), paddingX(unit(8)), paddingY(unit(2.5f)), minWidth(unit(36)))
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