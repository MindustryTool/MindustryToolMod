package mindustrytool.features.savesync;

import arc.Core;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

public class SaveSyncCreateSlotDialog extends BaseDialog {
    public SaveSyncCreateSlotDialog(SaveSyncFeature feature, BaseDialog parentDialog) {
        super("Create Slot");
        addCloseButton();

        final String[] name = { "New Slot" };

        cont.add("Slot Name:").padRight(10);
        cont.field(name[0], value -> name[0] = value).width(200).row();

        cont.button("Create", Icon.ok, () -> {
            if (name[0].isEmpty()) {
                return;
            }

            StorageService.createSlot(name[0]).thenAccept(slot -> {
                Core.app.post(() -> {
                    hide();
                    if (parentDialog != null) {
                        parentDialog.hide();
                    }
                    Core.settings.put(SaveSyncFeature.SETTING_SLOT_ID, slot.id);
                    feature.performSync(slot.id);
                });
            }).exceptionally(e -> {
                Core.app.post(() -> Vars.ui.showException(e));
                return null;
            });
        }).size(150, 50).padTop(10);
    }
}
