package mindustrytool.plugins.playerconnect;

import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

public class HostOptionDialog extends BaseDialog {

    public HostOptionDialog(BaseDialog createRoomDialog) {
        super("@message.manage-room.host-title");

        addCloseButton();

        cont.defaults().size(220f, 60f).pad(10f);

        cont.button("@server.host", Icon.host, () -> {
            hide();
            Vars.ui.host.show();
        }).row();

        cont.button("@message.manage-room.title", Icon.planet, () -> {
            hide();
            createRoomDialog.show();
        }).row();
    }
}
