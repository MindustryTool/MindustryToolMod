package mindustrytool.features.playerconnect;

import mindustry.ui.dialogs.BaseDialog;

public class PlayerConnectSettingDialog extends BaseDialog {
    public PlayerConnectSettingDialog() {
        super("Player Connect Settings");
        addCloseButton();

        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        cont.table(table -> {
            table.add("Room Name").padRight(5).left();
            table.field(PlayerConnectConfig.getRoomName(), text -> {
                PlayerConnectConfig.setRoomName(text);
            }).size(320f, 54f).maxTextLength(100).left().row();

            table.add("Password").padRight(5).left();
            table.field(PlayerConnectConfig.getPassword(), text -> {
                PlayerConnectConfig.setPassword(text);
            }).size(320f, 54f).maxTextLength(100).left().row();
        });
    }
}
