package mindustrytool.ui.room;

import arc.Core;
import arc.scene.ui.Button;
import arc.scene.ui.layout.*;
import mindustry.Vars;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.core.model.PlayerConnectRoom;
import mindustrytool.ui.browser.JoinDialogInjector;
import mindustrytool.ui.dialog.PasswordDialog;

/** Builds room button with JoinDialog community server theme. */
public class RoomButtonBuilder {
    public static Button build(PlayerConnectRoom room) {
        float width = JoinDialogInjector.targetWidth();
        Button[] btn = {null};
        btn[0] = new Button(new Button.ButtonStyle() {{ up = Styles.black5; down = Styles.flatDown; over = Styles.flatOver; }});
        btn[0].clicked(() -> { if (!btn[0].childrenPressed()) PasswordDialog.show(room, () -> Vars.ui.join.hide()); });

        float tw = width - 40f;
        String ver = "[accent]v" + room.data().version();
        String lock = room.data().isSecured() ? " [scarlet]" + Iconc.lock : "";

        Table hdr = new Table(Tex.whiteui);
        hdr.setColor(Pal.gray);
        btn[0].add(hdr).height(45f).growX().row();
        hdr.add(room.data().name() + "   " + ver + lock).left().padLeft(10f).wrap().style(Styles.outlineLabel).growX();
        hdr.button(Icon.copy, Styles.emptyi, () -> { Core.app.setClipboardText(room.data().name()); Vars.ui.showInfoFade("@copied"); })
           .margin(3f).pad(8f).padRight(4f).top().right().tooltip("Copy");

        btn[0].table(c -> c.table(Tex.whitePane, t -> {
            t.top().left().setColor(Pal.gray);
            t.add("[lightgray]" + Core.bundle.format("save.map", room.data().mapName()) + "[lightgray] / " + room.data().gamemode()).width(tw).left().ellipsis(true).row();
            int p = room.data().players().size;
            t.add("[lightgray]" + Core.bundle.format("players" + (p == 1 ? ".single" : ""), (p == 0 ? "[lightgray]" : "[accent]") + p + "[lightgray]")).left().row();
            t.add("[gray]" + Iconc.commandRally + " " + room.data().locale()).left().row();
            if (room.data().mods().size > 0) t.add("[gray]" + Iconc.book + " " + String.join(", ", room.data().mods().toArray(String.class))).width(tw).left().wrap().row();
        }).growY().growX().left().bottom()).grow();

        return btn[0];
    }
}
