package mindustrytool.plugins.playerconnect;

import arc.scene.ui.TextField;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;

/** Dialog for entering password to join a secured room. */
public class PasswordDialog {
    public static void show(PlayerConnectRoom room, Runnable onSuccess) {
        if (!room.data().isSecured()) { tryJoin(room, "", null, onSuccess); return; }
        BaseDialog dialog = new BaseDialog("@message.type-password.title");
        String[] pass = {""};
        dialog.cont.table(t -> {
            t.add("@message.password").padRight(5);
            TextField f = t.field("", s -> pass[0] = s).size(320, 54).left().get();
            f.setPasswordMode(true);
        });
        dialog.buttons.button("@cancel", dialog::hide);
        dialog.buttons.button("@ok", () -> tryJoin(room, pass[0], dialog, onSuccess)).disabled(b -> pass[0].isEmpty());
        dialog.show();
    }

    private static void tryJoin(PlayerConnectRoom room, String password, BaseDialog dialogToHide, Runnable onSuccess) {
        try {
            PlayerConnect.joinRoom(PlayerConnectLink.fromString(room.link()), password, () -> {
                if (dialogToHide != null) dialogToHide.hide();
                onSuccess.run();
            });
        } catch (Throwable e) {
            if (dialogToHide != null) dialogToHide.hide();
            Vars.ui.showException("@message.connect.fail", e);
        }
    }
}
