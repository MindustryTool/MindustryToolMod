# Music Audit

## Findings

### Anti-Patterns

- **MEDIUM** `MusicFeature.java:143-149` — `getMusicFile` uses `Reflect.get(music, "file")` to get the backing `Fi` from arc's `Music` class — fragile, relies on internal field name
- **MEDIUM** `MusicFeature.java:62-66` — `captureOriginalMusic()` checks `!originalMusic.containsKey(type)` but only for non-empty lists; if `ambientMusic` is empty when first captured but has music later, the replacement logic is inconsistent
- **MEDIUM** `MusicFeature.java:39-40` — Events.on `MusicRegisterEvent` in `init()` AND `captureOriginalMusic()` called at end of init; `captureOriginalMusic` may capture original music before MusicRegisterEvent fires
- **MEDIUM** `MusicFeature.java:102-107` — On file load failure, deletes path from config; if failure was transient (e.g. file locked), user loses config permanently
- **MEDIUM** `MusicType.java` — Enum values map to settings keys via `getKey()`; adding new enum values requires updating config schema

### Data Loss

- **MEDIUM** `MusicFeature.java:135-141` — `removeAllCustom` disposes music instances and clears paths from config; no undo confirmation for bulk removal
- **MEDIUM** `MusicFeature.java:153-161` — `toggleTrack` adds/removes from `disabled` list; if music is playing while toggled off, it's stopped — user may lose position in long tracks

### Dead Code / Tech Debt

- **LOW** `MusicFeature.java:50` — `validExtensions` includes `mp3`, `ogg`, `wav` but loading uses `new Music(file)` which may not support all these formats on all platforms
