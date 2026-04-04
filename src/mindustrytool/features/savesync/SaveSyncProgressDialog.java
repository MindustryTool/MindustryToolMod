package mindustrytool.features.savesync;

import mindustry.ui.dialogs.BaseDialog;

public class SaveSyncProgressDialog extends BaseDialog {
    public SaveSyncProgressDialog() {
        super("Syncing Saves");
        addCloseButton();
        setStatus("Preparing sync...");
    }

    public void setStatus(String status) {
        cont.clear();
        cont.add(status).row();
    }
}
