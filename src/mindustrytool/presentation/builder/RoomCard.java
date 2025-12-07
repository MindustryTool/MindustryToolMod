package mindustrytool.presentation.builder;

import arc.Core;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.gen.Iconc;
import mindustry.ui.Styles;
import mindustrytool.core.model.PlayerConnectRoom;

public class RoomCard {
    public static Table render(PlayerConnectRoom room, Runnable onJoin) {
        Table t = new Table(Styles.black5);
        t.margin(8);
        t.table(left -> {
            String locked = room.data().isSecured() ? " [white]" + Iconc.lock : "";
            String version = VersionParser.getVersionString(room.data().version());
            left.add(room.data().name() + " (" + room.data().locale() + ") " + version + locked)
                .fontScale(1.35f).left().wrap().growX().row();
            left.add(Strings.format("@ @[lightgray] / @", Iconc.map, room.data().mapName(), room.data().gamemode())).left().row();
            left.add(Strings.format("@ @", Iconc.players, Core.bundle.format("players", room.data().players().size))).left().row();
            if (room.data().mods().size > 0) left.add(Iconc.book + " " + Strings.join(", ", room.data().mods())).left().wrap().growX();
        }).left().top().growX();
        t.add().growX();
        t.table(right -> right.button(Iconc.play + " " + Core.bundle.get("join"), onJoin).size(160f, 60f)).right().top();
        return t;
    }
}
