package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;

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
import mindustrytool.features.chat.ChatMessageHeightCalculator;
import mindustrytool.features.playerconnect.net.NetworkProxy;
import mindustrytool.features.playerconnect.net.PlayerConnectClient;
import mindustrytool.features.playerconnect.net.PlayerConnectLink;
import mindustrytool.models.response.PlayerConnectRoom;
import mindustrytool.models.response.PlayerConnectRoom.PlayerConnectRoomData;
import mindustrytool.models.response.PlayerConnectRoom.PlayerConnectRoomPlayer;
import solim.core.BaseComponent;
import solim.layout.Card;
import solim.signal.Signal;
import mindustrytool.components.WebStyles;

public class RoomCard extends BaseComponent {

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

        Card cardComp = card()
                .name("pc-card")
                .background(Styles.black8)
                .border(1.5f, Color.darkGray)
                .gap(unit(1.5f))
                .padding(unit(2))
                .left()
                .grow()
                .margin(unit(2.5f))
                .minHeight(ChatMessageHeightCalculator.INVITE_CARD_HEIGHT);

        return cardComp.children(() -> {
            // Header row: Title + Copy link button
            row().growX().height(unit(6)).gap(unit(2)).children(() -> {
                text(title)
                        .style(Styles.outlineLabel)
                        .fontScale(displayPlayerList ? 1.1f : 0.95f)
                        .ellipsis()
                        .growX()
                        .left();
                spacer();

                if (displayPlayerList) {
                    button(() -> {
                        Core.app.setClipboardText(room.getLink());
                        Vars.ui.showInfoFade("@copied");
                    })
                            .style(WebStyles.ghost())
                            .size(unit(11))
                            .children(() -> icon(Icon.copy).size(unit(6)));
                }
            });

            // Map and mode
            if (!mapMode.isEmpty()) {
                text(mapMode).growX().ellipsis().left();
            }

            if (!Version.combined().equals(room.getData().getVersion())) {
                text(room.getData().getVersion()).growX().ellipsis();
            }

            if (!displayPlayerList) {
                String compatBadge = !protocolMatch
                        ? "[scarlet]" + Core.bundle.get("feature.chat.ui.incompatible-protocol", "Incompatible")
                        : (!missingMods.isEmpty() || !unneededMods.isEmpty()
                                ? "[orange]" + Core.bundle.get("feature.chat.ui.mods-required", "Mods Required")
                                : "");
                row().growX().height(unit(4)).children(() -> {
                    text(playerInfo).color(Color.lightGray).ellipsis().growX().left();
                    text(compatBadge).fontScale(0.85f).right();
                });
            } else {
                // Players
                text(playerInfo).color(Color.lightGray).left();

                if (room.getData() != null && room.getData().getPlayers() != null) {
                    column(() -> {
                        for (PlayerConnectRoomPlayer player : room.getData().getPlayers()) {
                            text("- " + player.getName());
                        }
                    });
                }

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
            }

            spacer();

            // Join action button
            if (!protocolMatch) {
                button(Core.bundle.get("feature.player-connect.incompatible", "Incompatible"), () -> {
                })
                        .style(WebStyles.outline())
                        .growX()
                        .height(unit(7))
                        .enabled(Signal.of(false));
            } else {
                row().gap(unit(1)).growX().children(() -> {
                    button(Core.bundle.get("join", "Join"), () -> promptJoin(secured, missingMods, unneededMods))
                            .style(WebStyles.primary())
                            .growX()
                            .height(unit(11));

                    if (!displayPlayerList) {
                        button(() -> {
                            Core.app.setClipboardText(room.getLink());
                            Vars.ui.showInfoFade("@copied");
                        })
                                .style(WebStyles.outline())
                                .size(unit(11))
                                .children(() -> icon(Icon.copy).size(unit(5)));
                    }
                });
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
