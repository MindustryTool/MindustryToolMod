package mindustrytool.features.playerconnect.ui;

import static solim.UI.*;
import static solim.modifier.Values.border;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.margin;
import static solim.modifier.Values.paddingX;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.rounded;
import static solim.modifier.Values.size;
import static solim.modifier.Values.update;
import static solim.modifier.Values.width;

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
import solim.core.Units;
import solim.core.Component;
import solim.reactive.Computed;
import solim.reactive.Signal;

public class RoomBrowserView extends Component {

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
    private final Computed<Integer> columnCount = Units.dvw.map(w -> Mathf.clamp((int) (w * 90f / 550f), 1, 4));

    public RoomBrowserView(PlayerConnectFeature feature) {
        this.feature = feature;
    }

    private Component roomsLoading() {
        return column().props(growX(), margin(unit(3)), center()).children(() -> {
            icon(Icon.refresh).props(size(unit(8)), prefWidth(unit(8)), prefHeight(unit(8)));
            text(Core.bundle.get("loading", "Loading..."));
        });
    }

    @Override
    protected Element build() {
        Computed<List<PlayerConnectRoom>> filteredRooms = Signal.computed(() -> {
            List<PlayerConnectRoom> all = feature.getRooms().get();
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
                .props(growX(), margin(unit(2)), gap(unit(2)), left())
                .children(() -> {

                    row().props(growX(), gap(unit(2)), center()).children(() -> {
                        button(() -> collapsed.set(!Boolean.TRUE.equals(collapsed.get())))
                                .style(WebStyles.outline())
                                .props(size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)))
                                .children(() -> icon(
                                        collapsed.map(c -> Boolean.TRUE.equals(c)
                                                ? FileIcon.of("chevron-right.png", Icon.rightOpen)
                                                : FileIcon.of("chevron-down.png", Icon.downOpen)))
                                        .props(width(unit(6)), prefWidth(unit(6))));

                        row().props(growX(), height(unit(11)), prefHeight(unit(11)), border(1.5f, Color.darkGray), paddingX(unit(2)), rounded(unit(2)))
                                .children(() -> {
                                    textField(searchQuery)
                                            .props(grow())
                                            .style(WebStyles.clearInput());

                                });

                        button(feature.getRoomsQuery()::refetch)
                                .style(WebStyles.outline())
                                .props(size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)))
                                .children(() -> icon(Icon.refresh).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)), update((element) -> {
                                            element.setOrigin(Align.center);

                                            if (feature.isFetching().peek()) {
                                                element.rotation -= 6f * Time.delta;
                                                if (element.rotation <= -360f) {
                                                    element.rotation += 360f;
                                                }
                                                return;
                                            }

                                            if (element.rotation < 0f) {
                                                element.rotation -= 6f * Time.delta;
                                                if (element.rotation <= -360f) {
                                                    element.rotation = 0f;
                                                }
                                            }
                                        })));

                        button(Core.bundle.get("feature.player-connect.join-link-title", "Join via Link"), () -> {
                            feature.getJoinRoomDialog().show();
                        })
                                .style(WebStyles.outline())
                                .props(height(unit(11)), prefHeight(unit(11)), paddingX(unit(3)));
                    });

                    divider();

                    when(collapsed)
                            .thenDo(() -> row())
                            .elseDo(() -> column().props(growX()).children(() -> {
                                query(feature.getRoomsQuery())
                                        .props(grow())
                                        .loading(this::roomsLoading)
                                        .error(err -> column().props(growX(), margin(unit(3)), center()).children(() -> {
                                            text(err != null ? err.getMessage() : Core.bundle.get("error", "Error"))
                                                    .color(Color.scarlet);
                                            button(Core.bundle.get("retry", "Retry"), feature.getRoomsQuery()::refetch)
                                                    .style(WebStyles.outline())
                                                    .props(height(unit(10)), prefHeight(unit(10)));
                                        }))
                                        .data((allRooms, fetching) -> Boolean.TRUE.equals(fetching)
                                                ? roomsLoading()
                                                : column().props(growX()).children(() -> {
                                                    dynamic(groupedRooms, groups -> {
                                                        if (groups == null || groups.isEmpty()) {
                                                            column().props(growX(), margin(unit(3)), center())
                                                                    .children(() -> {
                                                                        text(Core.bundle.get(
                                                                                "feature.player-connect.no-rooms",
                                                                                "No active PlayerConnect rooms found."))
                                                                                .color(Color.lightGray);
                                                                    });

                                                        } else {
                                                            column().props(growX(), gap(unit(3.5f))).children(() -> {
                                                                for (ProviderRoomGroup group : groups) {
                                                                    column().props(growX(), gap(unit(2)), left())
                                                                            .children(() -> {

                                                                                row().props(growX(), gap(unit(2)))
                                                                                        .children(() -> {
                                                                                            text(group.providerName)
                                                                                                    .left();
                                                                                            text("(" + group.rooms
                                                                                                    .size() + ")");
                                                                                        });

                                                                                reactiveGrid(group.rooms)
                                                                                        .props(gap(unit(2)))
                                                                                        .columns(columnCount)
                                                                                        .key(PlayerConnectRoom::getLink)
                                                                                        .items((
                                                                                                PlayerConnectRoom r) -> new RoomCard(
                                                                                                        r));
                                                                            });
                                                                }
                                                            });

                                                        }
                                                    }).props(growX());
                                                }));
                            })).props(growX());

                    divider();
                }).element();
    }
}