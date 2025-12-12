package mindustrytool.ui.server;

import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustrytool.core.model.ServerHost;
import mindustrytool.ui.dialog.AddServerDialog;

public class ServerListRenderer {
    public static void render(arc.struct.ArrayMap<String, String> servers, Table table, boolean editable, 
                              ServerSelectCallback onSelect, AddServerDialog addDialog, Runnable onDelete) {
        table.clear();
        for (int i = 0; i < servers.size; i++) {
            ServerHost host = new ServerHost();
            host.name = servers.getKeyAt(i);
            host.set(servers.getValueAt(i));
            ServerRowBuilder.build(table, host, i, editable, onSelect, addDialog, servers, onDelete);
        }
    }
    
    public interface ServerSelectCallback { void onSelect(ServerHost host, Button btn); }
}
