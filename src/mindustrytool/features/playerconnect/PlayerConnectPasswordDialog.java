package mindustrytool.features.playerconnect;

import arc.Core;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;

public class PlayerConnectPasswordDialog extends BaseDialog {
    public PlayerConnectPasswordDialog(PlayerConnectLink link) {
        super("@message.type-password.title");
        name = "playerConnectPasswordDialog";

        String[] password = { "" };

        cont.table(table -> {
            table.add("@message.password").padRight(5f).right();
            table.field(password[0], text -> password[0] = text)
                    .size(320f, 54f)
                    .valid(t -> t.length() > 0 && t.length() <= 100)
                    .maxTextLength(100)
                    .left()
                    .get();
            table.row().add();
        }).row();

        buttons.button("@cancel", this::hide).minWidth(210);
        buttons.button("@ok", () -> {
            try {
                PlayerConnect.join(link, password[0], this::hide);
            } catch (Exception e) {
                hide();
                Vars.ui.showException("@message.connect.fail", e);
            }
        }).minWidth(210);

        Core.app.post(this::show);
    }
}
