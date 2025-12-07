package mindustrytool.presentation.builder;

import arc.scene.ui.layout.*;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.presentation.dialog.AddServerDialog;

public class ServerListSection {
    public static void buildCustomSection(Table h, Table custom, AddServerDialog addDialog, Runnable refresh, boolean[] shown) {
        h.table(t -> {
            t.add("@message.manage-room.custom-servers").pad(10).padLeft(0).color(Pal.accent).growX().left();
            t.button(Icon.add, Styles.emptyi, addDialog::show).size(40f).right().padRight(3);
            t.button(Icon.refresh, Styles.emptyi, refresh).size(40f).right().padRight(3);
            t.button(Icon.downOpen, Styles.emptyi, () -> shown[0] = !shown[0])
                .update(i -> i.getStyle().imageUp = !shown[0] ? Icon.upOpen : Icon.downOpen).size(40f).right();
        }).pad(0, 5, 0, 5).growX().row();
        h.image().pad(5).height(3).color(Pal.accent).growX().row();
        h.collapser(t -> { custom.clear(); t.add(custom); }, false, () -> shown[0]).pad(0, 5, 10, 5).growX().row();
    }

    public static void buildOnlineSection(Table h, Table online, Runnable refresh, boolean[] shown) {
        h.table(t -> {
            t.add("@message.manage-room.public-servers").pad(10).padLeft(0).color(Pal.accent).growX().left();
            t.button(Icon.refresh, Styles.emptyi, refresh).size(40f).right().padRight(3);
            t.button(Icon.downOpen, Styles.emptyi, () -> shown[0] = !shown[0])
                .update(i -> i.getStyle().imageUp = !shown[0] ? Icon.upOpen : Icon.downOpen).size(40f).right();
        }).pad(0, 5, 0, 5).growX().row();
        h.image().pad(5).height(3).color(Pal.accent).growX().row();
        h.collapser(t -> { online.clear(); t.add(online); }, false, () -> shown[0]).pad(0, 5, 10, 5).growX().row();
        h.marginBottom(Vars.mobile ? 140f : 70f);
    }
}
