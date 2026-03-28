package mindustrytool.features.savesync;

import arc.ApplicationListener;
import arc.Core;
import arc.files.Fi;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;
import arc.util.Http.HttpStatusException;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Main;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.auth.AuthService;
import mindustrytool.features.savesync.dto.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class SaveSyncFeature implements Feature {
    private static final String SETTING_SLOT_ID = "mindustrytool.savesync.slotId";
    private static final String SETTING_LAST_SYNC = "mindustrytool.savesync.lastSync";

    private final Set<String> initialFilePaths = new HashSet<>();

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

    private List<ClientFileDto> listFiles() {
        Seq<Fi> files = new Seq<>();
        files.add(Core.settings.getSettingsFile());
        files.add(Core.settings.getBackupSettingsFile());
        files.addAll(Vars.customMapDirectory.list());
        files.addAll(Vars.saveDirectory.list());
        // files.addAll(Vars.modDirectory.list());
        files.addAll(Vars.schematicDirectory.list());

        String base = Vars.dataDirectory.absolutePath();

        List<ClientFileDto> result = new ArrayList<>();
        for (var file : files) {
            if (file.isDirectory()) {
                continue;
            }
            try {
                var relativePath = file.absolutePath().substring(base.length());
                // normalize path
                relativePath = relativePath.replace('\\', '/');
                if (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }

                String hash = Utils.sha256(file.file());
                result.add(new ClientFileDto(relativePath, hash, Instant.ofEpochMilli(file.lastModified())));
            } catch (Exception e) {
                Log.err(e);
            }
        }

        return result;
    }

    private Set<String> listFilePaths() {
        List<ClientFileDto> files = listFiles();
        Set<String> result = new HashSet<>();
        for (var file : files) {
            result.add(file.getPath());
        }
        return result;
    }

    public Fi getFile(String path) {
        return Vars.dataDirectory.child(path);
    }

    @Override
    public void init() {
        var feature = this;

        Core.app.addListener(new ApplicationListener() {
            @Override
            public void exit() {
                if (feature.isEnabled()) {
                    String slotId = Core.settings.getString(SETTING_SLOT_ID, null);
                    if (slotId != null && AuthService.getInstance().isLoggedIn()) {
                        performSyncOnExit(slotId);
                    }
                }
            }
        });
    }

    private void performSyncOnExit(String slotId) {
        Log.info("Performing save sync on exit...");

        try {
            Set<String> currentPaths = listFilePaths();
            List<String> deletedFiles = new ArrayList<>();
            for (String path : initialFilePaths) {
                if (!currentPaths.contains(path)) {
                    deletedFiles.add(path);
                }
            }

            if (!deletedFiles.isEmpty()) {
                Log.info("Deleting " + deletedFiles.size() + " files on server...");
                List<CompletableFuture<Void>> deletions = new ArrayList<>();
                for (String path : deletedFiles) {
                    deletions.add(StorageService.deleteFile(slotId, path));
                }
                CompletableFuture.allOf(deletions.toArray(new CompletableFuture[0])).join();
            }

            List<ClientFileDto> files = listFiles();
            SyncSlotDto syncData = new SyncSlotDto();
            syncData.clientFiles = files;

            SyncSlotResponseDto response = StorageService.syncSlot(slotId, syncData).join();

            if (response.missingHashes != null && !response.missingHashes.isEmpty()) {
                List<CompletableFuture<Void>> uploads = new ArrayList<>();
                List<Fi> changedFiles = new ArrayList<>();
                for (String hash : response.missingHashes) {
                    for (ClientFileDto file : files) {
                        if (file.hash.equals(hash)) {
                            Fi fileToUpload = getFile(file.path);
                            if (fileToUpload.exists()) {
                                changedFiles.add(fileToUpload);
                            }
                            break;
                        }
                    }
                }

                for (var file : changedFiles) {
                    uploads.add(StorageService.uploadFile(file));
                }

                if (changedFiles.size() > 0) {
                    Log.info("Uploading " + changedFiles.size() + " files on exit...");
                    CompletableFuture.allOf(uploads.toArray(new CompletableFuture[0])).join();
                    Log.info("Upload complete.");
                }
            }

            Log.info("Save sync on exit completed.");
        } catch (Exception e) {
            Log.err("Failed to sync on exit", e);
        }
    }

    @Override
    public void onEnable() {
        initialFilePaths.addAll(listFilePaths());
        checkAndSync(false);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public Optional<Dialog> setting() {
        BaseDialog dialog = new BaseDialog("Save Sync Settings");
        dialog.addCloseButton();

        Table cont = dialog.cont;

        String slotId = Core.settings.getString(SETTING_SLOT_ID, "None");
        cont.add("Current Slot ID: " + (slotId.equals("None") ? "[lightgray]None" : "[accent]" + slotId)).row();

        cont.button("Select Slot", Icon.save, this::showSlotSelectionDialog).size(200, 50).padTop(10).row();
        cont.button("Force Sync", Icon.refresh, () -> performSync(Core.settings.getString(SETTING_SLOT_ID, null)))
                .size(200, 50).padTop(10).disabled(b -> Core.settings.getString(SETTING_SLOT_ID, null) == null).row();

        return Optional.of(dialog);
    }

    private void checkAndSync(boolean showWarning) {
        if (!AuthService.getInstance().isLoggedIn()) {
            if (showWarning) {
                Vars.ui.showInfo("You must be logged in to use Save Sync.");
            }
            return;
        }

        String slotId = Core.settings.getString(SETTING_SLOT_ID, null);
        if (slotId == null) {
            if (showWarning) {
                showSlotSelectionDialog();
            }
        } else {
            performSync(slotId);
        }
    }

    private void showSlotSelectionDialog() {
        BaseDialog dialog = new BaseDialog("Select Save Slot");
        dialog.addCloseButton();

        Table cont = dialog.cont;
        cont.add("Loading slots...").row();

        StorageService.listSlots().thenAccept(slots -> {
            Core.app.post(() -> {
                cont.clear();
                if (slots.isEmpty()) {
                    cont.add("No slots found.").row();
                    cont.button("Create New Slot", Icon.add, () -> showCreateSlotDialog(dialog)).size(200, 50).row();
                } else {
                    cont.add("Select a slot to sync with:").padBottom(10).row();
                    Table slotsTable = new Table();
                    for (StorageSlotDto slot : slots) {
                        slotsTable.button(
                                slot.name + "\n[lightgray]"
                                        + (slot.updatedAt != null ? slot.updatedAt.toString() : "Never synced"),
                                Icon.save, () -> {
                                    Core.settings.put(SETTING_SLOT_ID, slot.id);
                                    dialog.hide();
                                    performSync(slot.id);
                                }).size(300, 60).padBottom(5).row();
                    }
                    cont.pane(slotsTable).height(300).row();
                    cont.button("Create New Slot", Icon.add, () -> showCreateSlotDialog(dialog)).size(200, 50)
                            .padTop(10).row();
                }
            });
        }).exceptionally(e -> {
            Core.app.post(() -> {
                cont.clear();
                cont.add("Error loading slots: " + e.getMessage()).color(mindustry.graphics.Pal.remove).row();
                cont.button("Retry", this::showSlotSelectionDialog).size(100, 50);
            });
            return null;
        });

        dialog.show();
    }

    private void showCreateSlotDialog(BaseDialog parentDialog) {
        BaseDialog dialog = new BaseDialog("Create Slot");
        dialog.addCloseButton();

        Table cont = dialog.cont;
        final String[] name = { "New Slot" };

        cont.add("Slot Name:").padRight(10);
        cont.field(name[0], val -> name[0] = val).width(200).row();

        cont.button("Create", Icon.ok, () -> {
            if (name[0].isEmpty())
                return;

            StorageService.createSlot(name[0]).thenAccept(slot -> {
                Core.app.post(() -> {
                    dialog.hide();
                    if (parentDialog != null)
                        parentDialog.hide();
                    Core.settings.put(SETTING_SLOT_ID, slot.id);
                    performSync(slot.id);
                });
            }).exceptionally(e -> {
                Core.app.post(() -> Vars.ui.showException(e));
                return null;
            });
        }).size(150, 50).padTop(10);

        dialog.show();
    }

    private void performSync(String slotId) {
        if (slotId == null) {
            Log.warn("SaveSync: slotId is null!");
            return;
        }

        BaseDialog dialog = new BaseDialog("Syncing Saves");
        dialog.addCloseButton();
        Table cont = dialog.cont;
        cont.add("Preparing sync...").row();
        dialog.show();

        try {
            List<ClientFileDto> files = listFiles();
            SyncSlotDto syncData = new SyncSlotDto();
            syncData.clientFiles = files;
            try {
                var lastSyncTime = Core.settings.getLong(SETTING_LAST_SYNC, 0);
                if (lastSyncTime != 0) {
                    syncData.lastSync = Instant.ofEpochMilli(lastSyncTime);
                }
            } catch (Exception e) {
                Core.settings.put(SETTING_LAST_SYNC, 0);
            }

            cont.clear();
            cont.add("Syncing with server...").row();

            StorageService.syncSlot(slotId, syncData).thenAccept(response -> {
                Core.app.post(() -> {
                    processSyncResponse(response, dialog, cont);
                });
            }).exceptionally(e -> {
                Core.app.post(() -> {
                    dialog.hide();
                    Log.err(e);

                    if (e.getCause() instanceof HttpStatusException h) {
                        Vars.ui.showErrorMessage("Http response error: " + h.response.getResultAsString());
                    } else {
                        Vars.ui.showException(e);
                    }
                });
                return null;
            });

        } catch (Exception e) {
            dialog.hide();
            Vars.ui.showException(e);
        }
    }

    private ClientFileDto findFileByHash(List<ClientFileDto> files, String hash) {
        for (ClientFileDto file : files) {
            if (file.hash.equals(hash)) {
                return file;
            }
        }
        return null;
    }

    private void processSyncResponse(SyncSlotResponseDto response, BaseDialog dialog,
            Table cont) {
        cont.clear();
        cont.add("Processing changes...").row();

        List<ClientFileDto> localFiles = listFiles();

        if (response.missingHashes != null && !response.missingHashes.isEmpty()) {
            List<String> toUpload = new ArrayList<>();
            for (String hash : response.missingHashes) {
                var file = findFileByHash(localFiles, hash);
                if (file != null) {
                    toUpload.add(file.path);

                }
            }

            int total = toUpload.size();

            if (total > 0) {
                cont.add("Uploading " + total + " files...").row();
                uploadNext(localFiles, toUpload, 0, dialog, cont, response);
                return;
            }
        }

        processDownloads(localFiles, response, dialog, cont);

        Core.settings.put(SETTING_LAST_SYNC, System.currentTimeMillis());
    }

    private void uploadNext(List<ClientFileDto> localFiles, List<String> files, int index, BaseDialog dialog,
            Table cont,
            SyncSlotResponseDto response) {
        if (index >= files.size()) {
            processDownloads(localFiles, response, dialog, cont);
            return;
        }

        cont.clear();
        cont.add("Uploading " + (index + 1) + " of " + files.size() + " files...").row();

        String path = files.get(index);
        Fi file = getFile(path);

        if (file == null || !file.exists()) {
            Log.err("File not found for upload: " + path);
            uploadNext(localFiles, files, index + 1, dialog, cont, response);
            return;
        }

        StorageService.uploadFile(file).thenRun(() -> {
            Log.info("Uploaded " + path);
            Core.app.post(() -> uploadNext(localFiles, files, index + 1, dialog, cont, response));
        }).exceptionally(e -> {
            Core.app.post(() -> {
                Log.err("Failed to upload " + path, e);
                uploadNext(localFiles, files, index + 1, dialog, cont, response);
            });
            return null;
        });
    }

    private void processDownloads(List<ClientFileDto> localFiles, SyncSlotResponseDto response, BaseDialog dialog,
            Table cont) {
        if (response.downloads != null && !response.downloads.isEmpty()) {
            cont.clear();
            cont.add("Downloading " + response.downloads.size() + " files...").row();
            downloadNext(localFiles, response.downloads, 0, dialog, cont, response);
        } else {
            processDeletes(localFiles, response, dialog, cont);
        }
    }

    private void downloadNext(List<ClientFileDto> localFiles, List<DownloadDto> downloads, int index, BaseDialog dialog,
            Table cont,
            SyncSlotResponseDto response) {
        if (index >= downloads.size()) {
            processDeletes(localFiles, response, dialog, cont);
            return;
        }

        DownloadDto download = downloads.get(index);

        cont.clear();
        cont.add("Downloading " + download.path).row();
        Http.get(download.url, res -> {
            try {
                if (Main.self.file.path().endsWith(download.path)) {
                    Log.info("Skipping download of " + download.path);
                } else {
                    byte[] bytes = res.getResult();
                    getFile(download.path).writeBytes(bytes);
                }

                Core.app.post(() -> downloadNext(localFiles, downloads, index + 1, dialog, cont, response));
            } catch (Exception e) {
                Core.app.post(() -> {
                    Log.err("Failed to save " + download.path, e);
                    downloadNext(localFiles, downloads, index + 1, dialog, cont, response);
                });
            }
        }, err -> {
            Core.app.post(() -> {
                Log.err("Failed to download " + download.path, err);
                downloadNext(localFiles, downloads, index + 1, dialog, cont, response);
            });
        });
    }

    private void processDeletes(List<ClientFileDto> localFiles, SyncSlotResponseDto response, BaseDialog dialog,
            Table cont) {
        if (response.extraHashes != null && !response.extraHashes.isEmpty()) {
            cont.clear();
            cont.add("Deleting " + response.extraHashes.size() + " files...").row();
            for (String hash : response.extraHashes) {
                var file = findFileByHash(localFiles, hash);
                if (file != null) {
                    getFile(file.path).delete();
                }
            }
        }

        cont.clear();
        cont.add("Sync Complete!").color(mindustry.graphics.Pal.accent).row();
        cont.button("Close", dialog::hide).size(100, 50).padTop(10);

        // Update initialFilePaths after successful sync
        initialFilePaths.clear();
        initialFilePaths.addAll(listFilePaths());
    }
}
