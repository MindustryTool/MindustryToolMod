package mindustrytool.features.savesync;

import arc.Core;
import arc.scene.ui.Dialog;
import arc.util.Log;
import arc.util.Timer;
import arc.util.Http.HttpStatusException;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.auth.AuthService;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SaveSyncFeature implements Feature {
    static final String SETTING_SLOT_ID = "mindustrytool.save-sync.slot-id";
    static final String SETTING_LAST_SYNC = "mindustrytool.save-sync.last-sync";
    private static final float AUTO_SYNC_INTERVAL_SECONDS = 60f * 5f;

    private final FileService fileService = new FileService();
    private final SyncService syncService = new SyncService(fileService);

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("save-sync")
                .description("Sync your saves with the cloud.")
                .icon(Icon.save)
                .order(10)
                .enabledByDefault(false)
                .build();
    }

    @Override
    public void init() {
        Timer.schedule(this::performPeriodicSync, AUTO_SYNC_INTERVAL_SECONDS, AUTO_SYNC_INTERVAL_SECONDS);
        Utils.onAppExit(this::performSyncOnExit);
    }

    @Override
    public void onEnable() {
        syncService.snapshotLocalFiles();
        checkAndSync(false);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public Optional<Dialog> setting() {
        return Optional.of(new SaveSyncSettingsDialog(this));
    }

    void showSlotSelectionDialog() {
        new SaveSyncSlotSelectionDialog(this).show();
    }

    void performSync(String slotId) {
        if (slotId == null) {
            Log.warn("SaveSync: slotId is null!");
            return;
        }

        SaveSyncProgressDialog dialog = new SaveSyncProgressDialog();
        dialog.show();

        Optional<CompletableFuture<Void>> future = syncService.syncWithServer(slotId,
                status -> Core.app.post(() -> dialog.setStatus(status)));
        if (!future.isPresent()) {
            dialog.hide();
            return;
        }

        future.get()
                .thenRun(() -> Core.app.post(() -> handleSyncSuccess(dialog)))
                .exceptionally(error -> {
                    Core.app.post(() -> handleSyncFailure(dialog, error));
                    return null;
                });
    }

    void syncSelectedSlot() {
        performSync(getSelectedSlotId());
    }

    void selectSlotAndSync(String slotId) {
        Core.settings.put(SETTING_SLOT_ID, slotId);
        performSync(slotId);
    }

    String getSelectedSlotId() {
        return Core.settings.getString(SETTING_SLOT_ID, null);
    }

    private void checkAndSync(boolean showWarning) {
        if (!AuthService.getInstance().isLoggedIn()) {
            if (showWarning) {
                Vars.ui.showInfo("You must be logged in to use Save Sync.");
            }
            return;
        }

        String slotId = getSelectedSlotId();
        if (slotId == null) {
            if (showWarning) {
                showSlotSelectionDialog();
            }
            return;
        }

        performSync(slotId);
    }

    private void performPeriodicSync() {
        if (!isEnabled() || !AuthService.getInstance().isLoggedIn()) {
            return;
        }

        Optional<CompletableFuture<Void>> future = syncService.syncLocalChanges(getSelectedSlotId());
        if (!future.isPresent()) {
            return;
        }

        Log.info("Performing periodic save sync...");
        future.get()
                .thenRun(() -> Log.info("Periodic save sync completed."))
                .exceptionally(error -> {
                    Log.err("Failed periodic save sync", error);
                    return null;
                });
    }

    private void performSyncOnExit() {
        if (!isEnabled() || !AuthService.getInstance().isLoggedIn()) {
            return;
        }

        String slotId = getSelectedSlotId();
        if (slotId == null) {
            return;
        }

        Optional<CompletableFuture<Void>> future = syncService.syncLocalChanges(slotId);
        if (!future.isPresent()) {
            Log.info("Skipping save sync on exit because another sync is already in progress.");
            return;
        }

        Log.info("Performing save sync on exit...");
        try {
            future.get().join();
            Log.info("Save sync on exit completed.");
        } catch (Exception e) {
            Log.err("Failed to sync on exit", e);
        }
    }

    private void handleSyncSuccess(SaveSyncProgressDialog dialog) {
        dialog.hide();
        Vars.ui.showInfoFade("[accent]Sync Complete!");
    }

    private void handleSyncFailure(SaveSyncProgressDialog dialog, Throwable error) {
        dialog.hide();
        Throwable failure = unwrap(error);
        Log.err(failure);
        if (failure instanceof HttpStatusException) {
            HttpStatusException statusException = (HttpStatusException) failure;
            Vars.ui.showErrorMessage("Http response error: " + statusException.response.getResultAsString());
            return;
        }
        Vars.ui.showException(failure);
    }

    private Throwable unwrap(Throwable error) {
        if (error.getCause() != null) {
            return error.getCause();
        }
        return error;
    }
}
