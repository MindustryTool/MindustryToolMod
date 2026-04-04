package mindustrytool.features.savesync;

import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

public class SaveSyncSettingsDialog extends BaseDialog {
    public SaveSyncSettingsDialog(SaveSyncFeature feature) {
        super("Save Sync Settings");
        addCloseButton();

        String slotId = feature.getSelectedSlotId();
        cont.add("Current Slot ID: " + (slotId == null ? "[lightgray]None" : "[accent]" + slotId)).row();

        cont.button("Select Slot", Icon.save, feature::showSlotSelectionDialog).size(200, 50).padTop(10).row();
        cont.button("Force Sync", Icon.refresh, feature::syncSelectedSlot)
                .size(200, 50)
                .padTop(10)
                .disabled(button -> feature.getSelectedSlotId() == null)
                .row();
    }
}
