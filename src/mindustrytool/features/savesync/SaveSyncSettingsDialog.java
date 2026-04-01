package mindustrytool.features.savesync;

import arc.Core;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

public class SaveSyncSettingsDialog extends BaseDialog {
    public SaveSyncSettingsDialog(SaveSyncFeature feature) {
        super("Save Sync Settings");
        addCloseButton();

        String slotId = Core.settings.getString(SaveSyncFeature.SETTING_SLOT_ID, "None");
        cont.add("Current Slot ID: " + (slotId.equals("None") ? "[lightgray]None" : "[accent]" + slotId)).row();

        cont.button("Select Slot", Icon.save, feature::showSlotSelectionDialog).size(200, 50).padTop(10).row();
        cont.button("Force Sync", Icon.refresh,
                () -> feature.performSync(Core.settings.getString(SaveSyncFeature.SETTING_SLOT_ID, null)))
                .size(200, 50)
                .padTop(10)
                .disabled(button -> Core.settings.getString(SaveSyncFeature.SETTING_SLOT_ID, null) == null)
                .row();
    }
}
