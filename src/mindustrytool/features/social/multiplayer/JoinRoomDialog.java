package mindustrytool.features.social.multiplayer;

import arc.Core;
import mindustry.Vars;

/** Dialog for joining a room via Player Connect link. */
public class JoinRoomDialog extends mindustry.ui.dialogs.BaseDialog {
    String lastLink = "player-connect://", password = "", output;
    boolean isValid;
    private final PlayerConnectRoomsDialog roomsDialog;

    public JoinRoomDialog(PlayerConnectRoomsDialog roomsDialog) {
        super("@mdt.message.join-room.title");
        this.roomsDialog = roomsDialog;
        cont.defaults().width(Vars.mobile ? 350f : 550f);
        cont.table(table -> {
            table.add("@mdt.message.join-room.link").padRight(5f).left();
            table.field(lastLink, this::setLink).maxTextLength(100).valid(this::setLink).height(54f).growX().row();
            table.add("@mdt.message.password").padRight(5f).left();
            table.field(password, text -> password = text).maxTextLength(100).height(54f).growX().row();
            table.add();
            table.labelWrap(() -> output).left().growX().row();
        }).row();
        buttons.defaults().size(140f, 60f).pad(4f);
        buttons.button("@cancel", this::hide);
        buttons.button("@ok", this::joinRoom).disabled(b -> !isValid || lastLink.isEmpty() || Vars.net.active());
        roomsDialog.buttons.button("@mdt.message.join-room.title", mindustry.gen.Icon.play, this::show);
    }

    public void joinRoom() {
        if (Vars.player.name.trim().isEmpty()) { Vars.ui.showInfo("@noname"); return; }
        try {
            PlayerConnectLink link = PlayerConnectLink.fromString(lastLink);
            Vars.ui.loadfrag.show("@connecting");
            Vars.ui.loadfrag.setButton(() -> { Vars.ui.loadfrag.hide(); Vars.netClient.disconnectQuietly(); });
            arc.util.Time.runTask(2f, () -> PlayerConnect.joinRoom(link, password, () -> { roomsDialog.hide(); hide(); }));
        } catch (Exception e) { isValid = false; Vars.ui.showErrorMessage(output); }
    }

    public boolean setLink(String link) {
        if (lastLink.equals(link)) return isValid;
        lastLink = link;
        try { PlayerConnectLink.fromString(link); output = "@mdt.message.join-room.valid"; isValid = true; }
        catch (Exception e) { output = Core.bundle.get("mdt.message.join-room.invalid") + ' ' + e.getLocalizedMessage(); isValid = false; }
        return isValid;
    }
}
