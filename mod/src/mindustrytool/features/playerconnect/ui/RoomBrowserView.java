package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import java.util.ArrayList;
import java.util.List;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import mindustrytool.models.response.PlayerConnectRoom;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.signal.Computed;
import solim.signal.Signal;

public class RoomBrowserView extends BaseComponent {

    private final PlayerConnectFeature feature;
    private final Signal<String> searchQuery = Signal.of("");
    private final Signal<Boolean> collapsed = Signal.of(false);

    public RoomBrowserView(PlayerConnectFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Computed<List<PlayerConnectRoom>> filteredRooms = Signal.computed(() -> {
            List<PlayerConnectRoom> all = feature.roomsSignal().get();
            String q = searchQuery.get();
            if (q == null || q.trim().isEmpty() || all == null) {
                return all != null ? all : new ArrayList<>();
            }
            String queryLower = q.trim().toLowerCase();
            List<PlayerConnectRoom> result = new ArrayList<>();
            for (PlayerConnectRoom room : all) {
                String name = room.getData() != null && room.getData().getName() != null
                        ? room.getData().getName()
                        : room.getName();
                if (name != null && name.toLowerCase().contains(queryLower)) {
                    result.add(room);
                }
            }
            return result;
        });

        return column()
                .growX()
                .margin(unit(2))
                .left()
                .children(() -> {
                    // Header Bar
                    row().growX().gap(unit(2)).center().children(() -> {
                        button(() -> collapsed.set(!Boolean.TRUE.equals(collapsed.get())))
                                .style(Styles.clearNonei)
                                .size(unit(8))
                                .children(() -> icon(collapsed.map(c -> Boolean.TRUE.equals(c) ? Icon.rightOpen : Icon.downOpen)));

                        text(Core.bundle.get("feature.player-connect.title", "PlayerConnect Rooms"))
                                .style(Styles.outlineLabel)
                                .color(Pal.accent)
                                .left();

                        spacer();

                        // Search field
                        textField(searchQuery)
                                .height(unit(8))
                                .width(unit(36));

                        // Refresh button
                        button(Icon.refresh, feature::fetchRoomsRest)
                                .style(Styles.clearNonei)
                                .size(unit(8));

                        // Join via Link button
                        button(Core.bundle.get("feature.player-connect.join-link-title", "Join via Link"), () -> {
                            new JoinRoomDialog().show();
                        }).style(Styles.defaultb).height(unit(8));
                    });

                    divider();

                    // Collapsible room list
                    dynamic(collapsed, isCollapsed -> {
                        if (Boolean.TRUE.equals(isCollapsed)) {
                            return row();
                        }
                        return dynamic(filteredRooms, this::buildRoomGrid).growX();
                    }).growX();

                    divider();
                }).element();
    }

    private Component buildRoomGrid(List<PlayerConnectRoom> roomList) {
        if (roomList == null || roomList.isEmpty()) {
            return column().growX().margin(unit(3)).center().children(() -> {
                text(Core.bundle.get("feature.player-connect.no-rooms", "No active PlayerConnect rooms found."))
                        .color(Color.lightGray);
            });
        }

        return column().growX().gap(unit(2)).children(() -> {
            for (PlayerConnectRoom room : roomList) {
                new RoomCard(room);
            }
        });
    }
}
