package mindustrytool.presentation.builder;

import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.core.model.ServerHost;
import mindustrytool.presentation.dialog.AddServerDialog;

public class ServerRowBuilder {
    public static Button build(Table table, ServerHost host, int index, boolean editable, 
            ServerListRenderer.ServerSelectCallback onSelect, AddServerDialog addDialog, 
            arc.struct.ArrayMap<String, String> servers, Runnable onDelete) {
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
        ServerLabelBuilder.build(st, host.name, val);
        st.add(inner);
        if (editable) buildEditButtons(inner, host, index, addDialog, servers, onDelete);
        ServerPingHelper.buildPing(ping, host);
        return btn;
    }

    private static void buildEditButtons(Table inner, ServerHost host, int index, AddServerDialog addDialog, arc.struct.ArrayMap<String, String> servers, Runnable onDelete) {
        if (Vars.mobile) { inner.button(Icon.pencil, Styles.emptyi, () -> addDialog.showEdit(host, index)).size(30f).pad(2, 5, 2, 5).right(); inner.button(Icon.trash, Styles.emptyi, () -> { Vars.ui.showConfirm("@confirm", "@server.delete", () -> { servers.removeKey(host.name); if (onDelete != null) onDelete.run(); }); }).size(30f).pad(2, 5, 2, 5).right(); }
        else { inner.button(Icon.pencilSmall, Styles.emptyi, () -> addDialog.showEdit(host, index)).pad(4).right(); inner.button(Icon.trashSmall, Styles.emptyi, () -> { Vars.ui.showConfirm("@confirm", "@server.delete", () -> { servers.removeKey(host.name); if (onDelete != null) onDelete.run(); }); }).pad(2).right(); }
    }
}
