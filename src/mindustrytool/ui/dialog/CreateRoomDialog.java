package mindustrytool.ui.dialog;

import arc.Core;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustrytool.core.model.ServerHost;
import mindustrytool.network.*;
import mindustrytool.ui.browser.PausedMenuInjector;
import mindustrytool.ui.server.*;

public class CreateRoomDialog extends mindustry.ui.dialogs.BaseDialog {
    private PlayerConnectLink link;
    private ServerHost selected;
    private Table custom = new Table(), online = new Table();
    private boolean[] customShown = {true}, onlineShown = {true};
    private boolean refreshingOnline;
    private final AddServerDialog addDialog = new AddServerDialog(this::refreshCustom);
    private CreateServerDialog createDialog;

    public CreateRoomDialog() {
        super("@message.manage-room.title");
        createDialog = new CreateServerDialog(() -> createRoom());
        arc.Events.run(mindustry.game.EventType.HostEvent.class, this::closeRoom);
        cont.defaults().width(Vars.mobile ? 480f : 850f);
        makeButtonOverlay();
        addCloseButton();
        buttons.button("@message.manage-room.create-room", Icon.add, createDialog::show).disabled(b -> !PlayerConnect.isRoomClosed() || selected == null);
        if (Vars.mobile) buttons.row();
        buttons.button("@message.manage-room.close-room", Icon.cancel, this::closeRoom).disabled(b -> PlayerConnect.isRoomClosed());
        buttons.button("@message.manage-room.copy-link", Icon.copy, this::copyLink).disabled(b -> link == null);
        cont.pane(h -> { ServerUI.buildCustomSection(h, custom, addDialog, this::refreshCustom, customShown); ServerUI.buildOnlineSection(h, online, this::refreshOnline, onlineShown); }).get().setScrollingDisabled(true, false);
        PausedMenuInjector.inject(this);
        shown(() -> Time.run(7f, () -> { refreshCustom(); refreshOnline(); }));
    }

    private void createRoom() {
        if (selected == null) return;
        Vars.ui.loadfrag.show("@message.manage-room.create-room");
        Timer.Task t = Timer.schedule(PlayerConnect::closeRoom, 10);
        PlayerConnect.createRoom(selected.ip, selected.port, createDialog.getPassword(), l -> { Vars.ui.loadfrag.hide(); t.cancel(); link = l; },
            e -> { Vars.net.handleException(e); t.cancel(); },
            r -> { Vars.ui.loadfrag.hide(); t.cancel(); if (r != null) Vars.ui.showText("", "@message.room." + Strings.camelToKebab(r.name())); else if (link == null) Vars.ui.showErrorMessage("@message.manage-room.create-room.failed"); });
    }
    void refreshCustom() { PlayerConnectProviders.loadCustom(); ServerUI.render(PlayerConnectProviders.custom, custom, true, (h, b) -> selected = h, addDialog, () -> { PlayerConnectProviders.saveCustom(); refreshCustom(); }); }
    void refreshOnline() { if (refreshingOnline) return; refreshingOnline = true; PlayerConnectProviders.refreshOnline(() -> { refreshingOnline = false; ServerUI.render(PlayerConnectProviders.online, online, false, (h, b) -> selected = h, null, null); }, e -> { refreshingOnline = false; Vars.ui.showException("@message.room.fetch-failed", e); }); }
    public void closeRoom() { PlayerConnect.closeRoom(); link = null; }
    public void copyLink() { if (link == null) return; Core.app.setClipboardText(link.toString()); Vars.ui.showInfoFade("@copied"); }
}
