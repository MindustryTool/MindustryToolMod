# Browser (Map & Schematic) Audit

## Findings

### Security

- **MEDIUM** `NetworkImage.java:55` — URL cache keying uses string replacement (`replace(":", "-")` etc.) — simplistic sanitization; image URLs could collide
- **MEDIUM** `NetworkImage.java:78` — `Http.get(url + "?format=jpeg")` appends query param to user-provided URL; if URL already has query params, malformed URL

### Data Loss

- **MEDIUM** `NetworkImage.java:67` — On decode error, calls `file.delete()` — if file was valid but decoding failed due to transient issue, next load re-downloads

### Concurrency

- **HIGH** `NetworkImage.java:40-52` — `draw()` is called every frame, accesses shared `ConcurrentHashMap` cache; `lastTexture` comparison and `setDrawable` are not synchronized — could cause visual glitches
- **MEDIUM** `NetworkImage.java:76-78` — Multiple threads may enter the `if (!cache.containsKey(url))` block simultaneously before the put; redundant HTTP requests
- **MEDIUM** `FilterDialog.java:87` — `rebuildToken` counter used as stale-check; ++token race if `rebuild()` called from multiple threads

### Anti-Patterns

- **MEDIUM** `FilterDialog.java:100-104` — Nested callbacks (`modService.getMod` -> `tagProvider.get` -> inner rebuild) create deeply nested async chains
- **MEDIUM** `FilterDialog.java:183-185` — `GlyphLayout` obtained from Pools but exception path may not free it if `layout.setText` throws
- **MEDIUM** `SearchConfig.java:22` — Mutable `Seq` fields exposed via `getBlocks()` and `getSelectedTags()` — callers can modify internal state
- **LOW** `MapBrowserFeature.java:28-33` — `onEnable` comment says "MenuFragment is static-ish" — indicates the feature is wired to a static UI structure that may change in updates

### Dead Code / Tech Debt

- **MEDIUM** `FilterDialog.java:54` — `modIds` is stored but the variable `modIds` shadows method-scoped usage; `modIds` is only used in `ModSelector` to filter tags — not persisted between dialog opens
- **LOW** `MapBrowserFeature.java:22` — `order(1)` hardcoded; better to use constants for ordering

### Performance

- **MEDIUM** `NetworkImage.java:40` — `draw()` method does HTTP check and heavy texture loading synchronously in the render loop — can cause frame drops
- **MEDIUM** `FilterDialog.java:230-250` — TagSelector uses `GlyphLayout` per tag to estimate widths; on mobile with many tags, this is expensive every rebuild
- **LOW** `FilterDialog.java` — Full UI rebuild each time any filter changes; no partial update
