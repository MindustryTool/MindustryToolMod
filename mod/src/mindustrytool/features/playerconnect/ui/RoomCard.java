package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import java.util.ArrayList;
import java.util.List;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.ui.Styles;
import mindustrytool.features.playerconnect.net.NetworkProxy;
import mindustrytool.features.playerconnect.net.PlayerConnectClient;
import mindustrytool.features.playerconnect.net.PlayerConnectLink;
import mindustrytool.models.response.PlayerConnectRoom;
import mindustrytool.models.response.PlayerConnectRoom.PlayerConnectRoomData;
import solim.core.BaseComponent;
import solim.signal.Signal;
import mindustrytool.components.WebStyles;

public class RoomCard extends BaseComponent {

    private final PlayerConnectRoom room;

    public RoomCard(PlayerConnectRoom room) {
        this.room = room;
    }

    @Override
    protected Element build() {
        PlayerConnectRoomData data = room.getData();
        boolean secured = data != null && data.isSecured();
        boolean protocolMatch = data == null || NetworkProxy.PROTOCOL_VERSION.equals(data.getProtocolVersion());

        String roomName = data != null && data.getName() != null ? data.getName() : room.getName();
        String title = (secured ? "[accent]" + Iconc.lock + "[white] " : "") + roomName;
        String mapMode = data != null
                ? "[lightgray]" + data.getMapName() + " [white]/ [accent]" + data.getGamemode()
                : "";

        int playerCount = data != null && data.getPlayers() != null ? data.getPlayers().size() : 0;
        String playerInfo = Iconc.players + " " + playerCount
                + (data != null && data.getLocale() != null ? " (" + data.getLocale() + ")" : "");

        // Mod difference calculations
        List<String> roomMods = data != null && data.getMods() != null ? data.getMods() : new ArrayList<>();
        Seq<String> localMods = Vars.mods != null ? Vars.mods.getModStrings() : new Seq<>();
        List<String> missingMods = new ArrayList<>();
        for (String m : roomMods) {
            if (!localMods.contains(m)) {
                missingMods.add(m);
            }
        }
        List<String> unneededMods = new ArrayList<>();
        for (String m : localMods) {
            if (!roomMods.contains(m)) {
                unneededMods.add(m);
            }
        }

        return card()
                .background(Styles.black8)
                .border(1.5f, Color.darkGray)
                .grow()
                .margin(unit(2.5f))
                .gap(unit(1.5f))
                .minHeight(unit(40))
                .padding(unit(2))
                .left()
                .children(() -> {
                    // Header row: Title + Copy link button
                    row().growX().children(() -> {
                        text(title).style(Styles.outlineLabel).fontScale(1.1f).left();
                        spacer();
                        button(() -> {
                            Core.app.setClipboardText(room.getLink());
                            Vars.ui.showInfoFade("@copied");
                        })
                                .style(WebStyles.ghost())
                                .size(unit(11))
                                .children(() -> icon(Icon.copy).size(unit(6)));
                    });

                    // Map and mode
                    if (!mapMode.isEmpty()) {
                        text(mapMode).left();
                    }

                    // Players
                    text(playerInfo).color(Color.lightGray).left();

                    // Mod conflicts indicator
                    if (!missingMods.isEmpty()) {
                        text("[scarlet]Missing mods: " + String.join(", ", missingMods)).wrap().growX().left();
                    }
                    if (!unneededMods.isEmpty()) {
                        text("[orange]Unneeded mods: " + String.join(", ", unneededMods)).wrap().growX().left();
                    }

                    if (!protocolMatch) {
                        text("[scarlet]Protocol mismatch: expected v" + NetworkProxy.PROTOCOL_VERSION).left();
                    }

                    spacer();

                    // Join action button
                    if (!protocolMatch) {
                        button(Core.bundle.get("feature.player-connect.incompatible", "Incompatible"), () -> {
                        }).style(WebStyles.outline()).growX().height(unit(8)).enabled(Signal.of(false));
                    } else {
                        button(Core.bundle.get("join", "Join"), () -> promptJoin(secured, missingMods, unneededMods))
                                .style(WebStyles.secondary())
                                .growX()
                                .height(unit(8));
                    }
                }).element();
    }

    private void promptJoin(boolean secured, List<String> missingMods, List<String> unneededMods) {
        if (secured) {
            Vars.ui.showTextInput(
                    Core.bundle.get("feature.player-connect.enter-password", "Enter Password"),
                    Core.bundle.get("feature.player-connect.password", "Password:"),
                    "",
                    password -> handleJoinWithPassword(password != null ? password : "", missingMods, unneededMods));
        } else {
            handleJoinWithPassword("", missingMods, unneededMods);
        }
    }

    private void handleJoinWithPassword(String password, List<String> missingMods, List<String> unneededMods) {
        if (!missingMods.isEmpty() || !unneededMods.isEmpty()) {
            new JoinWarningDialog(room, missingMods, unneededMods, password).show();
        } else {
            try {
                PlayerConnectLink link = PlayerConnectLink.fromString(room.getLink());
                PlayerConnectClient.join(link, password, () -> {
                });
            } catch (Exception e) {
                Vars.ui.showErrorMessage(e.getMessage());
            }
        }
    }
}
