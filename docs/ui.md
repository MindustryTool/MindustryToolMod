# UI Components Audit

## Findings

### ChangelogDialog

#### Anti-Patterns
- **MEDIUM** `ChangelogDialog.java:106-109` — `copy-debug-detail` button reads entire `last_log.txt`, all mod strings, full UI tree — potentially very large clipboard content that could freeze the UI
- **MEDIUM** `ChangelogDialog.java:108` — `last_log.txt` could be gigabytes if logging is verbose; `readString()` loads entire file into memory
- **MEDIUM** `ChangelogDialog.java:124-127` — UI tree serialization for arbitrary scene elements; if scene has cycles, `flattenUiTree` will stack-overflow (no cycle detection)

#### Performance
- **MEDIUM** `ChangelogDialog.java:37-38` — Fetches GitHub releases on dialog show; paginated at 10 per page but no caching — re-fetches every time dialog opens

### NetworkImage

#### Performance
- **HIGH** `NetworkImage.java:40-52` — `draw()` method runs every frame; does disk I/O check, HTTP request check, texture creation — in the render loop. This can cause severe frame drops.
- **MEDIUM** `NetworkImage.java:76-78` — HTTP request made without checking if file cache already exists; on cache miss, fires request every frame until cache populated

#### Concurrency
- **MEDIUM** `NetworkImage.java:40` — `draw()` can be called before previous HTTP response is processed; multiple requests for same URL stacked
- **MEDIUM** `NetworkImage.java:82-83` — Post-processes result in `Core.app.post` which is queued; but `cache.put` happens inside the post callback, so multiple callbacks may interleave

### UserCard

#### Performance
- **LOW** `UserCard.java:10-11` — Shows "Loading..." until `UserService.findUserById` completes; if user not found, shows "Loading..." permanently (error case not handled in UI)

### Utils (State.java)

#### Anti-Patterns
- **LOW** `utils/State.java` — Generic state machine; not used consistently across features (some features use their own state tracking)
