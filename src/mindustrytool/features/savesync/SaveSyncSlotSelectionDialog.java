package mindustrytool.features.savesync;

import arc.Core;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.features.savesync.dto.StorageSlotDto;

public class SaveSyncSlotSelectionDialog extends BaseDialog {
    public SaveSyncSlotSelectionDialog(SaveSyncFeature feature) {
        super("Select Save Slot");
        addCloseButton();

        Table container = cont;
        container.add("Loading slots...").row();

        StorageService.listSlots().thenAccept(slots -> {
            Core.app.post(() -> {
                container.clear();
                if (slots.isEmpty()) {
                    container.add("No slots found.").row();
                    container.button("Create New Slot", Icon.add, () -> new SaveSyncCreateSlotDialog(feature, this).show())
                            .size(200, 50)
                            .row();
                } else {
                    container.add("Select a slot to sync with:").padBottom(10).row();
                    Table slotsTable = new Table();
                    for (StorageSlotDto slot : slots) {
                        slotsTable.button(
                                slot.name + "\n[lightgray]"
                                        + (slot.updatedAt != null ? slot.updatedAt.toString() : "Never synced"),
                                Icon.save, () -> {
                                    Core.settings.put(SaveSyncFeature.SETTING_SLOT_ID, slot.id);
                                    hide();
                                    feature.performSync(slot.id);
                                }).size(300, 60).padBottom(5).row();
                    }
                    container.pane(slotsTable).height(300).row();
                    container.button("Create New Slot", Icon.add, () -> new SaveSyncCreateSlotDialog(feature, this).show())
                            .size(200, 50)
                            .padTop(10)
                            .row();
                }
            });
        }).exceptionally(e -> {
            Core.app.post(() -> {
                container.clear();
                container.add("Error loading slots: " + e.getMessage()).color(mindustry.graphics.Pal.remove).row();
                container.button("Retry", () -> {
                    hide();
                    new SaveSyncSlotSelectionDialog(feature).show();
                }).size(100, 50);
            });
            return null;
        });
    }
}
