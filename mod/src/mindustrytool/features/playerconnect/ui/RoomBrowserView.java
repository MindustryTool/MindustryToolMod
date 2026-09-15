package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.Element;
import java.util.ArrayList;
import java.util.List;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import mindustrytool.models.response.PlayerConnectRoom;
import solim.core.BaseComponent;
import solim.signal.Computed;
import solim.signal.Signal;

public class RoomBrowserView extends BaseComponent {

    private final PlayerConnectFeature feature;
    private final Signal<String> searchQuery = Signal.of("");
    private final Signal<Boolean> collapsed = Signal.of(false);
    private final Computed<Integer> columnCount = dvw(90f).map(w -> Mathf.clamp((int) (w / 550f), 1, 4));

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

        Computed<Boolean> hasRooms = filteredRooms.map(r -> r != null && !r.isEmpty());

        return column()
                .growX()
                .margin(unit(2))
                .gap(unit(2))
                .left()
                .children(() -> {
                    // Header Bar
                    row().growX().gap(unit(2)).center().children(() -> {
                        button(() -> collapsed.set(!Boolean.TRUE.equals(collapsed.get())))
                                .style(WebStyles.outline())
                                .size(unit(11))
                                .children(() -> icon(
                                        collapsed.map(c -> Boolean.TRUE.equals(c) ? Icon.rightOpen : Icon.downOpen))
                                                .size(unit(7)));

                        // Search field
                        row().growX().height(unit(11)).border(1.5f, Color.darkGray).paddingX(unit(2)).rounded(unit(2))
                                .children(() -> {
                                    textField(searchQuery)
                                            .grow()
                                            .style(WebStyles.clearInput());

                                });
                        // Refresh button
                        button(feature::fetchRoomsRest)
                                .style(WebStyles.outline())
                                .size(unit(11))
                                .children(() -> icon(Icon.refresh).size(unit(7)));

                        // Join via Link button
                        button(Core.bundle.get("feature.player-connect.join-link-title", "Join via Link"), () -> {
                            new JoinRoomDialog().show();
                        }).style(WebStyles.outline()).height(unit(11));
                    });

                    divider();

                    // Collapsible room list
                    dynamic(collapsed, isCollapsed -> {
                        if (Boolean.TRUE.equals(isCollapsed)) {
                            return row();
                        }
                        return column().growX().children(() -> {
                            column().growX().gap(unit(2)).visible(hasRooms).children(() -> {
                                grid(columnCount, filteredRooms, room -> room.getLink(), RoomCard::new).gap(unit(2));
                            });

                            column().growX().margin(unit(3)).center().visible(hasRooms.map(h -> !h)).children(() -> {
                                text(Core.bundle.get("feature.player-connect.no-rooms",
                                        "No active PlayerConnect rooms found."))
                                                .color(Color.lightGray);
                            });
                        });
                    }).growX();

                    divider();
                }).element();
    }
}
