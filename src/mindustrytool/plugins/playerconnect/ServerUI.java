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
    public static void render(ArrayMap<String, String> servers, Table table, boolean editable, SelectCallback onSelect,
            AddServerDialog addDialog, Runnable onDelete) {
        table.clear();
        for (int i = 0; i < servers.size; i++) {
            ServerHost host = new ServerHost();
            host.name = servers.getKeyAt(i);
            host.set(servers.getValueAt(i));
            buildServerRow(table, host, i, editable, onSelect, addDialog, servers, onDelete);
        }
    }

    // Row building
    // Row building
    private static Button buildServerRow(Table table, ServerHost host, int index, boolean editable,
            SelectCallback onSelect, AddServerDialog addDialog, ArrayMap<String, String> servers, Runnable onDelete) {
        Button btn = new Button(Styles.flatBordert);
        btn.setProgrammaticChangeEvents(true);
        btn.clicked(() -> onSelect.onSelect(host, btn));

        table.add(btn).growX().height(60f).padTop(5).padBottom(5).row();

        btn.table(t -> {
            t.left();
            // Ping
            t.table(p -> buildPing(p, host)).width(80f).left().padRight(10);

            // Info
            t.table(info -> {
                info.left().defaults().left();
                info.add(host.name).color(Color.white).fontScale(1f).ellipsis(true).row();
                String val = servers.size > 0 ? servers.getValueAt(Math.min(index, servers.size - 1)) : "";
                info.add(val).color(Color.lightGray).fontScale(0.75f).ellipsis(true);
            }).growX();

            // Edit
            if (editable) {
                t.table(e -> buildEditButtons(e, host, index, addDialog, servers, onDelete)).right().padLeft(10);
            }
        }).grow().pad(5);

        return btn;
    }

    private static void buildPing(Table ping, ServerHost host) {
        ping.label(() -> Strings.animated(Time.time, 4, 11, ".")).pad(2).color(Pal.accent).left();
        PlayerConnect.pingHost(host.ip, host.port, ms -> {
            ping.clear();
            ping.image(Icon.ok).color(Color.green).size(20f).padRight(5);
            ping.add(ms + "ms").color(Color.white).fontScale(0.85f);
        }, e -> {
            ping.clear();
            ping.image(Icon.cancel).color(Color.red).size(20f).padRight(5);
            ping.add("Error").color(Color.lightGray).fontScale(0.85f);
        });
    }

    private static void buildEditButtons(Table inner, ServerHost host, int index, AddServerDialog addDialog,
            ArrayMap<String, String> servers, Runnable onDelete) {
        inner.defaults().size(30f).pad(2);
        inner.button(Icon.pencil, Styles.emptyi, () -> addDialog.showEdit(host, index));
        inner.button(Icon.trash, Styles.emptyi, () -> Vars.ui.showConfirm("@confirm", "@server.delete", () -> {
            servers.removeKey(host.name);
            if (onDelete != null)
                onDelete.run();
        }));
    }

    // Section building
    public static void buildCustomSection(Table h, Table custom, AddServerDialog addDialog, Runnable refresh,
            boolean[] shown) {
        buildHeader(h, "@message.manage-room.custom-servers", t -> {
            t.button(Icon.add, Styles.emptyi, addDialog::show).size(32f).right().padRight(5);
            t.button(Icon.refresh, Styles.emptyi, refresh).size(32f).right();
        });
        h.add(custom).growX().row();
        h.add().height(20f).row(); // Spacer
    }

    public static void buildOnlineSection(Table h, Table online, Runnable refresh, boolean[] shown) {
        buildHeader(h, "@message.manage-room.public-servers", t -> {
            t.button(Icon.refresh, Styles.emptyi, refresh).size(32f).right();
        });
        h.add(online).growX().row();
        h.marginBottom(Vars.mobile ? 140f : 70f);
    }

    private static void buildHeader(Table h, String text, arc.func.Cons<Table> buttonBuilder) {
        h.table(t -> {
            t.add(text).color(Pal.accent).left().padBottom(5).fontScale(1f);
            t.add().growX();
            buttonBuilder.get(t);
        }).growX().padBottom(5).row();
        h.image().color(Pal.accent).height(3f).growX().padBottom(10).row();
    }

    public interface SelectCallback {
        void onSelect(ServerHost host, Button btn);
    }
}
