package mindustrytool.gui;

import mindustry.Vars;
import mindustrytool.Main;
import mindustrytool.playerconnect.PlayerConnect;
import mindustrytool.playerconnect.PlayerConnectLink;

public class JoinRoomDialog extends mindustry.ui.dialogs.BaseDialog {
    String lastLink = "player-connect://";
    boolean isValid;
    String output;

    public JoinRoomDialog() {
        super("@message.join-room.title");

        cont.defaults().width(Vars.mobile ? 350f : 550f);

        cont.table(table -> {
            table.add("@message.join-room.link")
                    .padRight(5f)
                    .left();

            table.field(lastLink, this::setLink)
                    .maxTextLength(100)
                    .valid(this::setLink)
                    .height(54f)
                    .growX()
                    .row();

            table.add();
            table.labelWrap(() -> output)
                    .left()
                    .growX()
                    .row();
        }).row();

        buttons.defaults()
                .size(140f, 60f)
                .pad(4f);

        buttons.button("@cancel", this::hide);

        buttons.button("@ok", this::joinRoom)
                .disabled(button -> !isValid || lastLink.isEmpty() || Vars.net.active());

        Main.playerConnectRoomsDialog.buttons
                .button("@message.join-room.title", mindustry.gen.Icon.play, this::show);
    }

    public void joinRoom() {
        if (Vars.player.name.trim().isEmpty()) {
            Vars.ui.showInfo("@noname");
            return;
        }

        PlayerConnectLink link;
        try {
            link = PlayerConnectLink.fromString(lastLink);
        } catch (Exception e) {
            isValid = false;
            Vars.ui.showErrorMessage(arc.Core.bundle.get("message.join-room.invalid") + ' ' + e.getLocalizedMessage());
            return;
        }

        Vars.ui.loadfrag.show("@connecting");
        Vars.ui.loadfrag.setButton(() -> {
            Vars.ui.loadfrag.hide();
            Vars.netClient.disconnectQuietly();
        });

        arc.util.Time.runTask(2f, () -> PlayerConnect.joinRoom(link, () -> {
            Main.playerConnectRoomsDialog.hide();
            hide();
        }));
    }

    public boolean setLink(String link) {
        if (lastLink.equals(link)) {
            return isValid;
        }

        lastLink = link;
        try {
            PlayerConnectLink.fromString(lastLink);
            output = "@message.join-room.valid";
            return isValid = true;

        } catch (Exception e) {
            output = arc.Core.bundle.get("message.join-room.invalid") + ' ' + e.getLocalizedMessage();
            return isValid = false;
        }
    }
}
