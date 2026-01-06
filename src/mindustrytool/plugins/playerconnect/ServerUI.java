package mindustrytool.plugins.playerconnect;

import arc.graphics.Color;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.ArrayMap;
import arc.util.*;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;

/** Combined server list UI utilities. */
public class ServerUI {
    public static void render(ArrayMap<String, String> servers, Table table, boolean editable, SelectCallback onSelect, AddServerDialog addDialog, Runnable onDelete) {
        table.clear();
        for (int i = 0; i < servers.size; i++) {
            ServerHost host = new ServerHost();
            host.name = servers.getKeyAt(i);
            host.set(servers.getValueAt(i));
            buildServerRow(table, host, i, editable, onSelect, addDialog, servers, onDelete);
        }
    }

    // Row building
    private static Button buildServerRow(Table table, ServerHost host, int index, boolean editable, 
            SelectCallback onSelect, AddServerDialog addDialog, ArrayMap<String, String> servers, Runnable onDelete) {
        Button btn = new Button();
        btn.getStyle().checkedOver = btn.getStyle().checked = btn.getStyle().over;
        btn.setProgrammaticChangeEvents(true);
        btn.clicked(() -> onSelect.onSelect(host, btn));
        table.add(btn).growX().padTop(5).padBottom(5).row();
        Stack st = new Stack();
        Table inner = new Table();
        inner.setColor(Pal.gray);
        btn.clearChildren();
        btn.add(st).growX().row();
        Table ping = inner.table(t -> {}).margin(0).pad(0).left().fillX().get();
        inner.add().expandX();
        String val = servers.size > 0 ? servers.getValueAt(Math.min(index, servers.size - 1)) : "";
        buildLabel(st, host.name, val);
        st.add(inner);
        if (editable) buildEditButtons(inner, host, index, addDialog, servers, onDelete);
        buildPing(ping, host);
        return btn;
    }

    private static void buildPing(Table ping, ServerHost host) {
        ping.label(() -> Strings.animated(Time.time, 4, 11, ".")).pad(2).color(Pal.accent).left();
        PlayerConnect.pingHost(host.ip, host.port, ms -> {
            ping.clear(); ping.image(Icon.ok).color(Color.green).padLeft(5).padRight(5).left();
            if (Vars.mobile) ping.row().add(ms + "ms").color(Color.lightGray).padLeft(5).padRight(5).left();
            else ping.add(ms + "ms").color(Color.lightGray).padRight(5).left();
        }, e -> { ping.clear(); ping.image(Icon.cancel).color(Color.red).padLeft(5).padRight(5).left(); });
    }

    private static void buildEditButtons(Table inner, ServerHost host, int index, AddServerDialog addDialog, ArrayMap<String, String> servers, Runnable onDelete) {
        if (Vars.mobile) {
            inner.button(Icon.pencil, Styles.emptyi, () -> addDialog.showEdit(host, index)).size(30f).pad(2, 5, 2, 5).right();
            inner.button(Icon.trash, Styles.emptyi, () -> Vars.ui.showConfirm("@confirm", "@server.delete", () -> { servers.removeKey(host.name); if (onDelete != null) onDelete.run(); })).size(30f).pad(2, 5, 2, 5).right();
        } else {
            inner.button(Icon.pencilSmall, Styles.emptyi, () -> addDialog.showEdit(host, index)).pad(4).right();
            inner.button(Icon.trashSmall, Styles.emptyi, () -> Vars.ui.showConfirm("@confirm", "@server.delete", () -> { servers.removeKey(host.name); if (onDelete != null) onDelete.run(); })).pad(2).right();
        }
    }

    public static void buildLabel(Stack st, String name, String val) {
        Table label = new Table().center();
        if (Vars.mobile || (name + " (" + val + ')').length() > 54) {
            label.add(name).pad(5, 5, 0, 5).expandX().row();
            label.add(" [lightgray](" + val + ')').pad(5, 0, 5, 5).expandX();
        } else label.add(name + " [lightgray](" + val + ')').pad(5).expandX();
        st.add(label);
    }

    // Section building
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

    public interface SelectCallback { void onSelect(ServerHost host, Button btn); }
}
