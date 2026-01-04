package mindustrytool.features.social.multiplayer;

import arc.input.KeyCode;
import arc.scene.ui.TextField;
import mindustry.Vars;
import mindustry.ui.dialogs.BaseDialog;

/** Dialog for entering password to join a secured room. */
public class PasswordDialog {
    public static void show(PlayerConnectRoom room, Runnable onSuccess) {
        if (!room.data().isSecured()) {
            tryJoin(room, "", null, onSuccess);
            return;
        }

        BaseDialog dialog = new BaseDialog("@mdt.message.type-password.title");

        // Use a container for the field reference
        final TextField[] fieldRef = { null };

        dialog.cont.table(t -> {
            t.add("@mdt.message.password").padRight(5);
            fieldRef[0] = t.field("", s -> {
            }).size(320, 54).left().get();
            fieldRef[0].setPasswordMode(true);
            fieldRef[0].setFilter(TextField.TextFieldFilter.digitsOnly);
            fieldRef[0].setMaxLength(6);

            // Allow Enter key to submit
            fieldRef[0].keyDown(key -> {
                if (key == KeyCode.enter) {
                    String p = fieldRef[0].getText();
                    if (p.length() >= 4 && p.length() <= 6) {
                        tryJoin(room, p, dialog, onSuccess);
                    }
                }
            });
        });

        dialog.buttons.button("@cancel", dialog::hide);
        dialog.buttons.button("@ok", () -> {
            String p = fieldRef[0].getText();
            tryJoin(room, p, dialog, onSuccess);
        }).disabled(b -> {
            if (fieldRef[0] == null)
                return true;
            String p = fieldRef[0].getText();
            return p.length() < 4 || p.length() > 6;
        });

        dialog.show();
    }

    private static void tryJoin(PlayerConnectRoom room, String password, BaseDialog dialogToHide, Runnable onSuccess) {
        try {
            PlayerConnect.joinRoom(PlayerConnectLink.fromString(room.link()), password, () -> {
                if (dialogToHide != null)
                    dialogToHide.hide();
                onSuccess.run();
            });
        } catch (Throwable e) {
            if (dialogToHide != null)
                dialogToHide.hide();
            Vars.ui.showException("@mdt.message.connect.fail", e);
        }
    }
}
