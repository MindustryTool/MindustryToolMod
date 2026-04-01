package mindustrytool.features.savesync;

import mindustry.ui.dialogs.BaseDialog;

public class SaveSyncProgressDialog extends BaseDialog {
    public SaveSyncProgressDialog() {
        super("Syncing Saves");
        addCloseButton();
        cont.add("Preparing sync...").row();
    }
}
