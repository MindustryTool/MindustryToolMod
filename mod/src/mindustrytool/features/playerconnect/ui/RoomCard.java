package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;
import static solim.modifier.Values.background;
import static solim.modifier.Values.border;
import static solim.modifier.Values.ellipsis;
import static solim.modifier.Values.fontScale;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.left;
import static solim.modifier.Values.color;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.right;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;
import static solim.modifier.Values.enabled;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import java.util.ArrayList;
import java.util.List;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.ui.Styles;
import mindustrytool.features.playerconnect.net.NetworkProxy;
import mindustrytool.features.playerconnect.net.PlayerConnectClient;
import mindustrytool.features.playerconnect.net.PlayerConnectLink;
import mindustrytool.models.response.PlayerConnectRoom;
import mindustrytool.models.response.PlayerConnectRoom.PlayerConnectRoomData;
import mindustrytool.models.response.PlayerConnectRoom.PlayerConnectRoomPlayer;
import solim.core.Component;
import solim.reactive.Signal;
import mindustrytool.components.WebStyles;

public class RoomCard extends Component {

    private final PlayerConnectRoom room;
    private final boolean displayPlayerList;

    public RoomCard(PlayerConnectRoom room) {
        this(room, true);
    }

    public RoomCard(PlayerConnectRoom room, boolean displayPlayerList) {
        this.room = room;
        this.displayPlayerList = displayPlayerList;
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
                .name("pc-card")
                .props(background(Styles.black8), border(1.5f, Color.darkGray), padding(unit(2)), gap(unit(1.5f)), left(), grow())
                .children(() -> {

                    row().props(growX(), height(unit(6)), prefHeight(unit(6)), gap(unit(2))).children(() -> {
                        text(title)
                                .props(style(Styles.outlineLabel), fontScale(displayPlayerList ? 1.1f : 0.95f), ellipsis(true), growX(), left());
                        spacer();

                        if (displayPlayerList) {
                            button(() -> {
                                Core.app.setClipboardText(room.getLink());
                                Vars.ui.showInfoFade("@copied");
                            })
                                    .props(style(WebStyles.ghost()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)))
                                    .children(() -> icon(Icon.copy).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));
                        }
                    });

                    if (!displayPlayerList) {

                        row().props(growX(), height(unit(4.5f)), prefHeight(unit(4.5f))).children(() -> {
                            text(!mapMode.isEmpty() ? mapMode : Core.bundle.get("none", "None"))
                                    .props(growX(), ellipsis(true), left());
                        });

                        row().props(growX(), height(unit(4.5f)), prefHeight(unit(4.5f))).children(() -> {
                            String verStr = room.getData() != null && room.getData().getVersion() != null
                                    ? room.getData().getVersion()
                                    : Version.combined();
                            text(verStr).props(color(Color.lightGray), fontScale(0.9f), growX(), ellipsis(true), left());
                        });

                        String compatBadge = !protocolMatch
                                ? "[scarlet]" + Core.bundle.get("feature.chat.ui.incompatible-protocol", "Incompatible")
                                : (!missingMods.isEmpty() || !unneededMods.isEmpty()
                                        ? "[orange]" + Core.bundle.get("feature.chat.ui.mods-required", "Mods Required")
                                        : "");
                        row().props(growX(), height(unit(4.5f)), prefHeight(unit(4.5f))).children(() -> {
                            text(playerInfo).props(color(Color.lightGray), ellipsis(true), growX(), left());
                            if (!compatBadge.isEmpty()) {
                                text(compatBadge).props(fontScale(0.85f), right());
                            }
                        });
                    } else {

                        if (!mapMode.isEmpty()) {
                            text(mapMode).props(growX(), ellipsis(true), left());
                        }

                        if (room.getData() != null && room.getData().getVersion() != null
                                && !Version.combined().equals(room.getData().getVersion())) {
                            text(room.getData().getVersion()).props(growX(), ellipsis(true));
                        }

                        text(playerInfo).props(color(Color.lightGray), left());

                        if (room.getData() != null && room.getData().getPlayers() != null) {
                            column(() -> {
                                for (PlayerConnectRoomPlayer player : room.getData().getPlayers()) {
                                    text("- " + player.getName());
                                }
                            });
                        }

                        if (!missingMods.isEmpty()) {
                            text("[scarlet]Missing mods: " + String.join(", ", missingMods)).props(solim.modifier.Values.wrap(true), growX(), left());
                        }
                        if (!unneededMods.isEmpty()) {
                            text("[orange]Unneeded mods: " + String.join(", ", unneededMods)).props(solim.modifier.Values.wrap(true), growX(), left());
                        }

                        if (!protocolMatch) {
                            text("[scarlet]Protocol mismatch: expected v" + NetworkProxy.PROTOCOL_VERSION).props(left());
                        }

                        spacer();
                    }

                    if (!protocolMatch) {
                        button(Core.bundle.get("feature.player-connect.incompatible", "Incompatible"), () -> {
                        })
                                .props(style(WebStyles.outline()), growX(), height(unit(11)), prefHeight(unit(11)), enabled(Signal.of(false)));
                    } else {
                        row().props(growX(), height(unit(11)), prefHeight(unit(11)), gap(unit(1))).children(() -> {
                            button(Core.bundle.get("join", "Join"),
                                    () -> promptJoin(secured, missingMods, unneededMods))
                                    .props(style(displayPlayerList ? WebStyles.secondary() : WebStyles.primary()), growX(), height(unit(11)), prefHeight(unit(11)));

                            if (!displayPlayerList) {
                                button(() -> {
                                    Core.app.setClipboardText(room.getLink());
                                    Vars.ui.showInfoFade("@copied");
                                })
                                        .props(style(WebStyles.outline()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)))
                                        .children(() -> icon(Icon.copy).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5))));
                            }
                        });
                    }
                }).element();
    }

    private void promptJoin(boolean secured, List<String> missingMods, List<String> unneededMods) {
        if (secured) {
            try {
                PlayerConnectLink link = PlayerConnectLink.fromString(room.getLink());
                String roomId = link.roomId;
                String saved = PlayerConnectClient.getSavedPassword(roomId);

                Vars.ui.showTextInput(
                        Core.bundle.get("feature.player-connect.enter-password", "Enter Password"),
                        Core.bundle.get("feature.player-connect.password", "Password:"),
                        saved,
                        password -> handleJoinWithPassword(password != null ? password : "", missingMods,
                                unneededMods));
            } catch (Exception e) {
                Vars.ui.showException(e);
            }
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
