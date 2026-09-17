package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.Element;
import arc.util.Align;
import arc.util.Time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import mindustry.gen.Icon;
import mindustrytool.components.FileIcon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.playerconnect.PlayerConnectFeature;
import mindustrytool.models.response.PlayerConnectRoom;
import solim.core.BaseComponent;
import solim.reactive.Computed;
import solim.reactive.Signal;

public class RoomBrowserView extends BaseComponent {

    public static class ProviderRoomGroup {
        public final String providerName;
        public final List<PlayerConnectRoom> rooms;

        public ProviderRoomGroup(String providerName, List<PlayerConnectRoom> rooms) {
            this.providerName = providerName;
            this.rooms = rooms;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ProviderRoomGroup that = (ProviderRoomGroup) o;
            return Objects.equals(providerName, that.providerName)
                    && Objects.equals(rooms, that.rooms);
        }

        @Override
        public int hashCode() {
            return Objects.hash(providerName, rooms);
        }
    }

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

        Computed<List<ProviderRoomGroup>> groupedRooms = Signal.computed(() -> {
            List<PlayerConnectRoom> list = filteredRooms.get();
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            Map<String, List<PlayerConnectRoom>> map = new LinkedHashMap<>();
            for (PlayerConnectRoom room : list) {
                String pName = room.getName() != null && !room.getName().trim().isEmpty()
                        ? room.getName().trim()
                        : Core.bundle.get("feature.player-connect.unknown-provider", "Relay Network");
                List<PlayerConnectRoom> group = map.get(pName);
                if (group == null) {
                    group = new ArrayList<>();
                    map.put(pName, group);
                }
                group.add(room);
            }
            List<ProviderRoomGroup> result = new ArrayList<>();
            for (Map.Entry<String, List<PlayerConnectRoom>> entry : map.entrySet()) {
                result.add(new ProviderRoomGroup(entry.getKey(), entry.getValue()));
            }
            return result;
        });

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
                                        collapsed.map(c -> Boolean.TRUE.equals(c)
                                                ? FileIcon.of("chevron-right.png", Icon.rightOpen)
                                                : FileIcon.of("chevron-down.png", Icon.downOpen)))
                                                        .width(unit(6)));

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
                                .children(() -> icon(Icon.refresh).origin(Align.center).size(unit(7))
                                        .update((element) -> {
                                            if (feature.isFetching().peek()) {
                                                element.rotation -= 5 * Time.delta;
                                                return;
                                            }

                                            if (element.rotation != 0) {
                                                float last = element.rotation;
                                                element.rotation %= 360;
                                                element.rotation -= 5 * Time.delta;

                                                if (last < element.rotation) {
                                                    element.rotation = 0;
                                                }
                                            }
                                        }));

                        // Join via Link button
                        button(Core.bundle.get("feature.player-connect.join-link-title", "Join via Link"), () -> {
                            new JoinRoomDialog().show();
                        })
                                .style(WebStyles.outline())
                                .height(unit(11))
                                .paddingX(unit(3));
                    });

                    divider();

                    // Collapsible room list
                    dynamic(collapsed, isCollapsed -> {
                        if (Boolean.TRUE.equals(isCollapsed)) {
                            return row();
                        }
                        return column().growX().children(() -> {
                            dynamic(groupedRooms, groups -> {
                                if (groups == null || groups.isEmpty()) {
                                    return column().growX().margin(unit(3)).center().children(() -> {
                                        text(Core.bundle.get("feature.player-connect.no-rooms",
                                                "No active PlayerConnect rooms found."))
                                                        .color(Color.lightGray);
                                    });
                                }

                                return column().growX().gap(unit(3.5f)).children(() -> {
                                    for (ProviderRoomGroup group : groups) {
                                        column().growX().gap(unit(2)).left().children(() -> {
                                            // Provider group header
                                            row().growX().gap(unit(2)).children(() -> {
                                                text(group.providerName).left();
                                                text("(" + group.rooms.size() + ")");
                                            });

                                            // Grid of rooms for this provider
                                            grid(columnCount, Signal.of(group.rooms), PlayerConnectRoom::getLink,
                                                    r -> new RoomCard(r))
                                                            .gap(unit(2));
                                        });
                                    }
                                });
                            }).growX();
                        });
                    }).growX();

                    divider();
                }).element();
    }
}
