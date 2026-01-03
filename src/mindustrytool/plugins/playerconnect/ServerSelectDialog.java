package mindustrytool.plugins.playerconnect;

import arc.Core;
import mindustry.Vars;

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

            Table customWrapper = new Table();
            ServerUI.buildCustomSection(customWrapper, this.settingsTable, addDialog, this::refreshCustom, customShown);
            t.add(customWrapper).width(Vars.mobile ? Core.graphics.getWidth() : 500f).padBottom(10).row();

            Table onlineWrapper = new Table();
            ServerUI.buildOnlineSection(onlineWrapper, this.serversTable, this::refreshOnline, onlineShown);
            t.add(onlineWrapper).width(Vars.mobile ? Core.graphics.getWidth() : 500f).row();
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
