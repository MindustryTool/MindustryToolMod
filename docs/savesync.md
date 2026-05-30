# Save Sync Audit

## Findings

### Data Loss

- **CRITICAL** `SaveSyncFeature.java:102-107` — `performSyncOnExit()` calls `future.get().join()` which blocks the shutdown thread; if sync fails, exception is logged but `join()` may hang indefinitely if server is unreachable — preventing mod/save from closing properly
- **CRITICAL** `SyncService.java:75-76` — Sync deletes server files THEN uploads local files; if upload fails, server-side files are already deleted — data loss
- **HIGH** `FileService.java:22-25` — `listFiles()` includes `Core.settings.getSettingsFile()` and `getBackupSettingsFile()` — syncing settings could overwrite user's configuration across devices unexpectedly
- **HIGH** `SyncService.java:100-103` — `deleteExtraFiles()` deletes files that are on server but not in local file list; if user plays on Device A (has saves S1, S2) then Device B (has only S1), syncing from B deletes S2 from server — data loss
- **MEDIUM** `FileService.java:50` — SHA-256 computed for every file on every sync; for large saves, this is CPU-intensive and may cause frame drops during gameplay
- **MEDIUM** `FileService.java:67` — `writeFile` overwrites local files without backup; if download fails mid-write, file is corrupted

### Concurrency

- **HIGH** `SyncService.java:24` — `AtomicBoolean isSyncing` prevents concurrent syncs but `snapshotLocalFiles()` is called in `onEnable()` while sync may be running from Timer
- **MEDIUM** `SyncService.java:41` — `runSync` returns `Optional.empty()` if already syncing; caller silently ignores — no retry mechanism
- **MEDIUM** `SaveSyncFeature.java:63-65` — `Timer.schedule` fires regardless of feature's enabled state; `performPeriodicSync()` checks `isEnabled()` but the Timer itself is never stopped

### Anti-Patterns

- **MEDIUM** `SyncService.java:54-62` — File change detection based on comparing `initialFiles` (snapshot at enable time) with current files; if files change while feature is disabled, `snapshotLocalFiles()` is not updated
- **MEDIUM** `SyncService.java:91-99` — `uploadNext` and `downloadNext` recursive async pattern — deep stack of CompletableFuture chains for many files
- **MEDIUM** `StorageService.java` — Implicitly couples to API structure; no fallback if API changes

### Dead Code / Tech Debt

- **LOW** `SaveSyncFeature.java:97` — `handleSyncFailure` unwraps `Throwable.getCause()` once; `CompletionException` may be nested multiple levels

### Performance

- **MEDIUM** `SyncService.java:101` — Sequential uploads and downloads (`uploadNext`/`downloadNext`); could parallelize for speed
- **LOW** `FileService.java:50` — SHA-256 of all files on every sync; could cache hashes and only re-hash modified files
