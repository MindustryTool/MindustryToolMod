package mindustrytool.plugins.playerconnect;

import arc.scene.ui.layout.*;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.plugins.playerconnect.ServerUI.SelectCallback;

public class ServerSelectDialog extends BaseDialog {
    private final AddServerDialog addDialog;
    private final SelectCallback onSelect;

    private Table settingsTable = new Table();
    private Table serversTable = new Table();
    private boolean[] customShown = { true }, onlineShown = { true };
    private boolean refreshingOnline;

    public ServerSelectDialog(SelectCallback onSelect) {
        super("Select Server");
        this.onSelect = onSelect;
        this.addDialog = new AddServerDialog(this::refreshCustom);

        addCloseButton();

        cont.pane(t -> {
            t.top();
            ServerUI.buildCustomSection(t, settingsTable, addDialog, this::refreshCustom, customShown);
            ServerUI.buildOnlineSection(t, serversTable, this::refreshOnline, onlineShown);
        }).grow();

        shown(() -> {
            refreshCustom();
            refreshOnline();
        });
    }

    void refreshCustom() {
        PlayerConnectProviders.loadCustom();
        ServerUI.render(PlayerConnectProviders.custom, settingsTable, true, (h, b) -> {
            if (onSelect != null)
                onSelect.onSelect(h, b);
            hide();
        }, addDialog, () -> {
            PlayerConnectProviders.saveCustom();
            refreshCustom();
        });
    }

    void refreshOnline() {
        if (refreshingOnline)
            return;
        refreshingOnline = true;
        PlayerConnectProviders.refreshOnline(() -> {
            refreshingOnline = false;
            ServerUI.render(PlayerConnectProviders.online, serversTable, false, (h, b) -> {
                if (onSelect != null)
                    onSelect.onSelect(h, b);
                hide();
            }, null, null);
        }, e -> {
            refreshingOnline = false;
        });
    }
}
