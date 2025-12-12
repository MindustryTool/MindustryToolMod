package mindustrytool.ui.room;

import arc.Core;
import arc.scene.ui.layout.*;
import arc.struct.Seq;
import mindustry.gen.Tex;
import mindustrytool.core.model.PlayerConnectRoom;
import mindustrytool.ui.browser.JoinDialogInjector;

/** Renders room list in Player Connect section. */
public class RoomRenderer {
    public static void render(Table container, Seq<PlayerConnectRoom> roomList, boolean loading, String search) {
        if (container == null) return;
        container.clear();

        if (loading) {
            container.table(Tex.button, t -> t.add("[accent]" + Core.bundle.get("message.loading")).pad(10f)).growX().row();
            return;
        }

        if (roomList.isEmpty()) {
            container.table(Tex.button, t -> t.add("@hosts.none").pad(10f)).growX().row();
            return;
        }

        Seq<PlayerConnectRoom> filtered = filterRooms(roomList, search);
        if (filtered.isEmpty()) {
            container.table(Tex.button, t -> t.add("@hosts.none").pad(10f)).growX().row();
            return;
        }

        float w = JoinDialogInjector.targetWidth();
        int cols = JoinDialogInjector.columns();
        int count = 0;

        for (PlayerConnectRoom room : filtered) {
            container.add(RoomButtonBuilder.build(room)).width(w).padBottom(7).padRight(4f).top().left().growY().uniformY();
            if (++count % cols == 0) container.row();
        }
    }

    private static Seq<PlayerConnectRoom> filterRooms(Seq<PlayerConnectRoom> rooms, String search) {
        if (search.isEmpty()) return rooms;
        String s = search.toLowerCase();
        return rooms.select(r -> {
            String name = r.data().name().toLowerCase();
            String map = r.data().mapName().toLowerCase();
            String mode = r.data().gamemode().toLowerCase();
            return name.contains(s) || map.contains(s) || mode.contains(s);
        });
    }
}
