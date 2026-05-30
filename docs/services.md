# Services Audit

## Findings

### Security

- **CRITICAL** `CrashReportDialog.java:38` — Sends crash report to API on dialog hide without user confirming the send; `hidden()` callback fires even if user unchecked the "send" box? Actually it checks `Core.settings.getBool(sendCrashReportKey, true)` — if user unchecks but the value was previously true, the setting update and the hidden callback may race. Also sends raw potentially sensitive crash data (stack traces with paths).
- **HIGH** `PlayerConnectService.java:36` — Room cache never expires; stale room data persists indefinitely
- **MEDIUM** `UpdateService.java:120-129` — Parses release notes from GitHub API; HTML/markdown content rendered via `Utils.renderMarkdown` — potential injection if markdown parser has issues
- **MEDIUM** `CrashReportDialog.java:55-58` — `Http.post` sends crash data without timeout; could block UI thread if server is slow

### Data Loss

- **HIGH** `CrashReportService.java:15` — `SimpleDateFormat` is not thread-safe; used as instance field but `checkForCrashes` could be called from any thread
- **MEDIUM** `PlayerConnectService.java:28-30` — `roomFuture` field shared across calls; if `findPlayerConnectRooms` is called while another request is in-flight, the second caller gets the same future (potentially wrong results or lost errors)

### Concurrency

- **HIGH** `CrashReportService.java:15` — Instance-level `SimpleDateFormat`; not thread-safe
- **MEDIUM** `PlayerConnectService.java:28-30` — `roomFuture` is a shared mutable field; `synchronized` on method but `CompletableFuture` callbacks may execute on different threads
- **MEDIUM** `PlayerConnectService.java:36` — `ConcurrentHashMap` for roomCache but no eviction/timeout policy
- **MEDIUM** `UserService.java:20-21` — `ConcurrentHashMap` listeners; `batch()` clears all listeners then iterates the copy; if `findUserById` is called during batch, its future is in the new map but won't be processed until next batch tick (200ms delay)

### Anti-Patterns

- **MEDIUM** `ServerService.java:37-38` — Uses `Reflect.invoke` to call private methods `setupRemote` and `refreshRemote` on join dialog; fragile
- **MEDIUM** `ServerService.java:46-47` — `@SuppressWarnings("unchecked")` on deserialization — unchecked cast from settings JSON
- **LOW** `UserService.java:58-64` — Error handling in batch: same logic duplicated for HttpStatusException vs generic Exception
- **MEDIUM** `TagService.java:16-18` — Static `HashMap` for categories with no eviction or refresh mechanism
- **MEDIUM** `ModService.java:17-19` — Static `Seq<ModData>` never refreshed after initial load
- **MEDIUM** `UpdateService.java:78-82` — `Http.get` ping result is submitted but never used; fire-and-forget HTTP request
- **MEDIUM** `UpdateService.java:131-149` — Version comparison logic: `isVersionGreater` doesn't handle the case where v1 is shorter AND all prefix digits match (e.g. v1=[1,0] vs v2=[1,0,1])

### Dead Code / Tech Debt

- **MEDIUM** `UpdateService.java:82` — Empty `Http.get(..).submit(result -> { })` — fire-and-forget ping with no error handling
- **MEDIUM** `TapListener.java:35` — Hold registration uses `order` field and implements `Comparable` but only used for sorting — the `order` field of BiConsumer data is never accessed for its semantic value
- **LOW** `TapListener.java:49` — Cast warning in `invokeCallback` — unchecked type access
- **LOW** `UpdateService.java` — Many string concatenations in changelog building; could use StringBuilder more efficiently

### Performance

- **LOW** `TapListener.java:44-48` — On each frame when touched, iterates all hold listeners linearly; if many listeners registered, could cause frame drops in update loop
- **LOW** `PlayerConnectService.java:45` — `findPlayerConnectRooms` uses `.select(room -> ...)` after deserializing all rooms; filtering should happen server-side via query param `q`
