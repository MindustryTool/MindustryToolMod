package mindustrytool.features.social.multiplayer;

import arc.Core;
import mindustry.ui.dialogs.BaseDialog;

/** Dialog for creating a new Player Connect room with name and password. */
public class CreateServerDialog extends BaseDialog {
    private final String[] config = {
        Core.settings.getString("playerConnectRoomName", ""),
        Core.settings.getString("playerConnectRoomPassword", "")
    };
    private final Runnable onCreate;

    public CreateServerDialog(Runnable onCreate) {
        super("@mdt.message.create-room.title");
        this.onCreate = onCreate;
        buttons.defaults().size(140f, 60f).pad(4f);
        
        cont.table(t -> {
            t.add("@mdt.message.create-room.server-name").padRight(5f).right();
            t.field(config[0], x -> { config[0] = x; Core.settings.put("playerConnectRoomName", x); })
                .size(320f, 54f).valid(x -> x.length() > 0 && x.length() <= 100).maxTextLength(100).left();
            t.row().add("@mdt.message.password").padRight(5f).right();
            t.field(config[1], x -> { config[1] = x; Core.settings.put("playerConnectRoomPassword", x); })
                .size(320f, 54f).maxTextLength(100).left();
            t.row().add();
        }).row();

        buttons.button("@cancel", this::hide);
        buttons.button("@ok", this::confirm)
            .disabled(b -> config[0].isEmpty() || config[0].length() > 100 || config[1].length() > 100);
    }

    private void confirm() {
        onCreate.run();
        hide();
    }

    public String getPassword() { return config[1]; }
}
