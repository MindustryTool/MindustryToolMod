package mindustrytool.features.savesync;

import arc.Core;
import arc.files.Fi;
import arc.util.Http;
import arc.util.Log;
import mindustrytool.features.savesync.dto.ClientFileDto;
import mindustrytool.features.savesync.dto.DownloadDto;
import mindustrytool.features.savesync.dto.SyncSlotDto;
import mindustrytool.features.savesync.dto.SyncSlotResponseDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncService {
    private final FileService fileService;
    private final AtomicBoolean isSyncing = new AtomicBoolean(false);
    private volatile List<ClientFileDto> initialFiles = Collections.emptyList();

    public SyncService(FileService fileService) {
        this.fileService = fileService;
    }

    public void snapshotLocalFiles() {
        initialFiles = new ArrayList<>(fileService.listFiles());
    }

    public Optional<CompletableFuture<Void>> syncWithServer(String slotId, ProgressListener listener) {
        return runSync(slotId, true, listener == null ? ProgressListener.none() : listener);
    }

    public Optional<CompletableFuture<Void>> syncLocalChanges(String slotId) {
        return runSync(slotId, false, ProgressListener.none());
    }

    private Optional<CompletableFuture<Void>> runSync(String slotId, boolean includeLastSync,
            ProgressListener listener) {
        if (slotId == null || !isSyncing.compareAndSet(false, true)) {
            return Optional.empty();
        }

        CompletableFuture<Void> future = sync(slotId, includeLastSync, listener)
                .whenComplete((ignored, throwable) -> isSyncing.set(false));
        return Optional.of(future);
    }

    private CompletableFuture<Void> sync(String slotId, boolean includeLastSync, ProgressListener listener) {
        List<ClientFileDto> localFiles = fileService.listFiles();
        SaveSyncFileChanges fileChanges = fileService.compareFiles(initialFiles, localFiles);
        logFileChanges(fileChanges);

        return deleteServerFiles(slotId, fileChanges.getDeletedFiles())
                .thenCompose(ignored -> syncSlot(slotId, localFiles, includeLastSync, listener))
                .thenCompose(response -> applyServerChanges(localFiles, response, includeLastSync, listener))
                .thenRun(this::completeSync);
    }

    private CompletableFuture<Void> deleteServerFiles(String slotId, List<ClientFileDto> deletedFiles) {
        if (deletedFiles.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Log.info("Deleting " + deletedFiles.size() + " files on server...");
        List<CompletableFuture<Void>> deletions = new ArrayList<>();
        for (ClientFileDto file : deletedFiles) {
            deletions.add(StorageService.deleteFile(slotId, file.getPath()));
        }
        return CompletableFuture.allOf(deletions.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<SyncSlotResponseDto> syncSlot(String slotId, List<ClientFileDto> localFiles,
            boolean includeLastSync, ProgressListener listener) {
        listener.updateStatus("Syncing with server...");
        return StorageService.syncSlot(slotId, createSyncRequest(localFiles, includeLastSync));
    }

    private SyncSlotDto createSyncRequest(List<ClientFileDto> localFiles, boolean includeLastSync) {
        SyncSlotDto syncData = new SyncSlotDto();
        syncData.clientFiles = localFiles;
        if (includeLastSync) {
            Instant lastSync = readLastSync();
            if (lastSync != null) {
                syncData.lastSync = lastSync;
            }
        }
        return syncData;
    }

    private Instant readLastSync() {
        try {
            long lastSyncTime = Core.settings.getLong(SaveSyncFeature.SETTING_LAST_SYNC, 0);
            if (lastSyncTime == 0) {
                return null;
            }
            return Instant.ofEpochMilli(lastSyncTime);
        } catch (Exception e) {
            Core.settings.put(SaveSyncFeature.SETTING_LAST_SYNC, 0);
            return null;
        }
    }

    private CompletableFuture<Void> applyServerChanges(List<ClientFileDto> localFiles, SyncSlotResponseDto response,
            boolean includeLastSync, ProgressListener listener) {
        listener.updateStatus("Processing changes...");
        CompletableFuture<Void> uploads = uploadMissingFiles(localFiles, response.missingHashes, listener);
        if (!includeLastSync) {
            return uploads;
        }

        return uploads
                .thenCompose(ignored -> downloadFiles(response.downloads, listener))
                .thenRun(() -> deleteExtraFiles(localFiles, response.extraHashes, listener));
    }

    private CompletableFuture<Void> uploadMissingFiles(List<ClientFileDto> localFiles, List<String> missingHashes,
            ProgressListener listener) {
        List<Fi> filesToUpload = fileService.resolveFilesForHashes(localFiles, missingHashes);
        if (filesToUpload.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        listener.updateStatus("Uploading " + filesToUpload.size() + " files...");
        return uploadNext(filesToUpload, 0, listener);
    }

    private CompletableFuture<Void> uploadNext(List<Fi> filesToUpload, int index, ProgressListener listener) {
        if (index >= filesToUpload.size()) {
            return CompletableFuture.completedFuture(null);
        }

        Fi file = filesToUpload.get(index);
        listener.updateStatus("Uploading " + (index + 1) + " of " + filesToUpload.size() + " files...");

        return StorageService.uploadFile(file)
                .thenRun(() -> Log.info("Uploaded " + file.path()))
                .exceptionally(error -> {
                    Log.err("Failed to upload " + file.path(), error);
                    return null;
                })
                .thenCompose(ignored -> uploadNext(filesToUpload, index + 1, listener));
    }

    private CompletableFuture<Void> downloadFiles(List<DownloadDto> downloads, ProgressListener listener) {
        if (downloads == null || downloads.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        listener.updateStatus("Downloading " + downloads.size() + " files...");
        return downloadNext(downloads, 0, listener);
    }

    private CompletableFuture<Void> downloadNext(List<DownloadDto> downloads, int index, ProgressListener listener) {
        if (index >= downloads.size()) {
            return CompletableFuture.completedFuture(null);
        }

        DownloadDto download = downloads.get(index);
        listener.updateStatus("Downloading " + download.path);

        return downloadFile(download)
                .exceptionally(error -> {
                    Log.err("Failed to download " + download.path, error);
                    return null;
                })
                .thenCompose(ignored -> downloadNext(downloads, index + 1, listener));
    }

    private CompletableFuture<Void> downloadFile(DownloadDto download) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Http.get(download.url, response -> {
            try {
                if (fileService.shouldSkipDownload(download.path)) {
                    Log.info("Skipping download of " + download.path);
                } else {
                    fileService.writeFile(download.path, response.getResult());
                }
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, future::completeExceptionally);
        return future;
    }

    private void deleteExtraFiles(List<ClientFileDto> localFiles, List<String> extraHashes, ProgressListener listener) {
        if (extraHashes == null || extraHashes.isEmpty()) {
            return;
        }

        listener.updateStatus("Deleting " + extraHashes.size() + " files...");
        for (String hash : extraHashes) {
            ClientFileDto file = fileService.findFileByHash(localFiles, hash);
            if (file != null) {
                fileService.deleteFile(file.getPath());
            }
        }
    }

    private void completeSync() {
        Core.settings.put(SaveSyncFeature.SETTING_LAST_SYNC, System.currentTimeMillis());
        snapshotLocalFiles();
    }

    private void logFileChanges(SaveSyncFileChanges fileChanges) {
        if (!fileChanges.hasChanges()) {
            return;
        }

        Log.info("Save sync changes - created: " + fileChanges.getCreatedFiles().size()
                + ", updated: " + fileChanges.getUpdatedFiles().size()
                + ", deleted: " + fileChanges.getDeletedFiles().size());
    }

    public interface ProgressListener {
        void updateStatus(String status);

        static ProgressListener none() {
            return status -> {
            };
        }
    }
}
