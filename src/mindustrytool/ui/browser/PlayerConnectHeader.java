package mindustrytool.ui.browser;

import arc.Core;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.ui.dialog.CreateRoomDialog;

/** Builds section header for Player Connect in JoinDialog. */
public class PlayerConnectHeader {
    public static void build(Table hosts, Collapser coll, Runnable onShowHidden) {
        hosts.table(name -> {
            name.add("Player Connect").pad(10).growX().left().color(Pal.accent);
            name.button(Icon.eyeSmall, Styles.emptyi, onShowHidden)
                .update(i -> i.getStyle().imageUp = Icon.eyeSmall).size(40f).right().padRight(3).tooltip("@servers.showhidden");
            name.button(Icon.add, Styles.emptyi, () -> new CreateRoomDialog().show()).size(40f).right().padRight(3).tooltip("Create Room");
            name.button(Icon.downOpen, Styles.emptyi, () -> {
                coll.toggle(false);
                Core.settings.put("collapsed-playerconnect", coll.isCollapsed());
            }).update(i -> i.getStyle().imageUp = !coll.isCollapsed() ? Icon.upOpen : Icon.downOpen).size(40f).right().padRight(10f);
        }).growX().row();
        hosts.image().growX().pad(5).padLeft(10).padRight(10).height(3).color(Pal.accent).row();
    }
}
