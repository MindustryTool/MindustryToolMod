package mindustrytool.presentation.dialog;

import arc.Core;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustrytool.core.model.ServerHost;
import mindustrytool.feature.playerconnect.network.*;
import mindustrytool.presentation.builder.*;

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
        createDialog = new CreateServerDialog(() -> RoomCreator.create(selected, createDialog.getPassword(), l -> link = l, l -> link = null));
        arc.Events.run(mindustry.game.EventType.HostEvent.class, this::closeRoom);
        cont.defaults().width(Vars.mobile ? 480f : 850f);
        makeButtonOverlay();
        addCloseButton();
        buttons.button("@message.manage-room.create-room", Icon.add, createDialog::show).disabled(b -> !PlayerConnect.isRoomClosed() || selected == null);
        if (Vars.mobile) buttons.row();
        buttons.button("@message.manage-room.close-room", Icon.cancel, this::closeRoom).disabled(b -> PlayerConnect.isRoomClosed());
        buttons.button("@message.manage-room.copy-link", Icon.copy, this::copyLink).disabled(b -> link == null);
        cont.pane(h -> { ServerListSection.buildCustomSection(h, custom, addDialog, this::refreshCustom, customShown); ServerListSection.buildOnlineSection(h, online, this::refreshOnline, onlineShown); }).get().setScrollingDisabled(true, false);
        PausedMenuInjector.inject(this);
        shown(() -> Time.run(7f, () -> { refreshCustom(); refreshOnline(); }));
    }

    void refreshCustom() { PlayerConnectProviders.loadCustom(); ServerListRenderer.render(PlayerConnectProviders.custom, custom, true, (h, b) -> selected = h, addDialog, () -> { PlayerConnectProviders.saveCustom(); refreshCustom(); }); }
    void refreshOnline() { if (refreshingOnline) return; refreshingOnline = true; PlayerConnectProviders.refreshOnline(() -> { refreshingOnline = false; ServerListRenderer.render(PlayerConnectProviders.online, online, false, (h, b) -> selected = h, null, null); }, e -> { refreshingOnline = false; Vars.ui.showException("@message.room.fetch-failed", e); }); }
    public void closeRoom() { PlayerConnect.closeRoom(); link = null; }
    public void copyLink() { if (link == null) return; Core.app.setClipboardText(link.toString()); Vars.ui.showInfoFade("@copied"); }
}