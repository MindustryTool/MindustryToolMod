package mindustrytool.features.social.multiplayer;

import arc.Core;
import arc.scene.ui.Button;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.core.Version;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Room rendering for both dialog and injector views. */
public class RoomCard {
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(.+?) build (\\d+)(?:\\.(\\d+))?$");

    // Simple card for PlayerConnectRoomsDialog
    public static Table render(PlayerConnectRoom room, Runnable onJoin) {
        Table t = new Table(Styles.black5);
        t.margin(8);
        t.table(left -> {
            String locked = room.data().isSecured() ? " [white]" + Iconc.lock : "";
            String version = getVersionString(room.data().version());
            left.add(room.data().name() + " (" + room.data().locale() + ") " + version + locked)
                    .fontScale(1.35f).left().wrap().growX().row();
            left.add(Strings.format("@ @[lightgray] / @", Iconc.map, room.data().mapName(), room.data().gamemode()))
                    .left().row();
            left.add(Strings.format("@ @", Iconc.players, Core.bundle.format("players", room.data().players().size)))
                    .left().row();
            if (room.data().mods().size > 0)
                left.add(Iconc.book + " " + Strings.join(", ", room.data().mods())).left().wrap().growX();
        }).left().top().growX();
        t.add().growX();
        t.table(right -> right.button(Iconc.play + " " + Core.bundle.get("join"), onJoin).size(160f, 60f)).right()
                .top();
        return t;
    }

    // Detailed list for JoinDialogInjector
    public static void renderList(Table container, Seq<PlayerConnectRoom> roomList, boolean loading, String search) {
        if (container == null)
            return;
        container.clear();

        if (loading) {
            container.table(Tex.button, t -> t.add("[accent]" + Core.bundle.get("mdt.message.loading")).pad(10f)).growX()
                    .row();
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
        int cols = JoinDialogInjector.columns(), count = 0;
        for (PlayerConnectRoom room : filtered) {
            container.add(buildButton(room, w)).width(w).padBottom(7).padRight(4f).top().left().growY().uniformY();
            if (++count % cols == 0)
                container.row();
        }
    }

    private static Button buildButton(PlayerConnectRoom room, float width) {
        Button[] btn = { null };
        btn[0] = new Button(new Button.ButtonStyle() {
            {
                up = Styles.black5;
                down = Styles.flatDown;
                over = Styles.flatOver;
            }
        });
        btn[0].clicked(() -> {
            if (!btn[0].childrenPressed())
                PasswordDialog.show(room, () -> Vars.ui.join.hide());
        });
        float tw = width - 40f;
        String ver = "[accent]v" + room.data().version(),
                lock = room.data().isSecured() ? " [scarlet]" + Iconc.lock : "";
        Table hdr = new Table(Tex.whiteui);
        hdr.setColor(Pal.gray);
        btn[0].add(hdr).height(45f).growX().row();
        hdr.add(room.data().name() + "   " + ver + lock).left().padLeft(10f).wrap().style(Styles.outlineLabel).growX();
        hdr.button(Icon.copy, Styles.emptyi, () -> {
            Core.app.setClipboardText(room.data().name());
            Vars.ui.showInfoFade("@copied");
        }).margin(3f).pad(8f).padRight(4f).top().right().tooltip("Copy");
        btn[0].table(c -> c.table(Tex.whitePane, t -> {
            t.top().left().setColor(Pal.gray);
            t.add("[lightgray]" + Core.bundle.format("save.map", room.data().mapName()) + "[lightgray] / "
                    + room.data().gamemode()).width(tw).left().ellipsis(true).row();
            int p = room.data().players().size;
            t.add("[lightgray]" + Core.bundle.format("players" + (p == 1 ? ".single" : ""),
                    (p == 0 ? "[lightgray]" : "[accent]") + p + "[lightgray]")).left().row();
            t.add("[gray]" + Iconc.commandRally + " " + room.data().locale()).left().row();
            if (room.data().mods().size > 0)
                t.add("[gray]" + Iconc.book + " " + String.join(", ", room.data().mods().toArray(String.class)))
                        .width(tw).left().wrap().row();
        }).growY().growX().left().bottom()).grow();
        return btn[0];
    }

    private static Seq<PlayerConnectRoom> filterRooms(Seq<PlayerConnectRoom> rooms, String search) {
        if (search.isEmpty())
            return rooms;
        String s = search.toLowerCase();
        return rooms.select(r -> {
            String name = r.data().name().toLowerCase(), map = r.data().mapName().toLowerCase(),
                    mode = r.data().gamemode().toLowerCase();
            return name.contains(s) || map.contains(s) || mode.contains(s);
        });
    }

    private static String getVersionString(String s) {
        int build = -1;
        String type = "custom";
        if (!"custom build".equals(s)) {
            Matcher m = VERSION_PATTERN.matcher(s);
            if (m.matches()) {
                type = m.group(1);
                build = Integer.parseInt(m.group(2));
            }
        }
        if (build == -1)
            return Core.bundle.format("server.custombuild");
        if (build == 0)
            return Core.bundle.get("server.outdated");
        if (Version.build != -1) {
            if (build < Version.build)
                return Core.bundle.get("server.outdated") + "\n" + Core.bundle.format("server.version", build, "");
            if (build > Version.build)
                return Core.bundle.get("server.outdated.client") + "\n"
                        + Core.bundle.format("server.version", build, "");
        }
        return (build == Version.build && Version.type.equals(type)) ? ""
                : Core.bundle.format("server.version", build, type);
    }
}
