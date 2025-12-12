package mindustrytool.ui.dialog;

import mindustry.Vars;
import mindustrytool.Main;
import mindustrytool.network.PlayerConnect;
import mindustrytool.network.PlayerConnectLink;
import mindustrytool.ui.common.LinkValidator;

public class JoinRoomDialog extends mindustry.ui.dialogs.BaseDialog {
    String lastLink = "player-connect://", password = "", output;
    boolean isValid;

    public JoinRoomDialog() {
        super("@message.join-room.title");
        cont.defaults().width(Vars.mobile ? 350f : 550f);
        cont.table(table -> {
            table.add("@message.join-room.link").padRight(5f).left();
            table.field(lastLink, this::setLink).maxTextLength(100).valid(this::setLink).height(54f).growX().row();
            table.add("@message.password").padRight(5f).left();
            table.field(password, text -> password = text).maxTextLength(100).height(54f).growX().row();
            table.add();
            table.labelWrap(() -> output).left().growX().row();
        }).row();
        buttons.defaults().size(140f, 60f).pad(4f);
        buttons.button("@cancel", this::hide);
        buttons.button("@ok", this::joinRoom).disabled(b -> !isValid || lastLink.isEmpty() || Vars.net.active());
        Main.playerConnectRoomsDialog.buttons.button("@message.join-room.title", mindustry.gen.Icon.play, this::show);
    }

    public void joinRoom() {
        if (Vars.player.name.trim().isEmpty()) { Vars.ui.showInfo("@noname"); return; }
        try {
            PlayerConnectLink link = PlayerConnectLink.fromString(lastLink);
            Vars.ui.loadfrag.show("@connecting");
            Vars.ui.loadfrag.setButton(() -> { Vars.ui.loadfrag.hide(); Vars.netClient.disconnectQuietly(); });
            arc.util.Time.runTask(2f, () -> PlayerConnect.joinRoom(link, password, () -> { Main.playerConnectRoomsDialog.hide(); hide(); }));
        } catch (Exception e) { isValid = false; Vars.ui.showErrorMessage(output); }
    }

    public boolean setLink(String link) {
        if (lastLink.equals(link)) return isValid;
        lastLink = link;
        LinkValidator.Result r = LinkValidator.validate(lastLink);
        output = r.message;
        return isValid = r.isValid;
    }
}
