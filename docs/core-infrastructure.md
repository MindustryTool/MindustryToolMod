# Core Infrastructure Audit

## Findings

### Security

- **CRITICAL** `Config.java:39-40` — Hardcoded PROJECT_URL (`your-choice-seven.vercel.app`) and PROJECT_ID; looks like placeholder/leaked dev credentials
- **HIGH** `Main.java:154` — Reflection access to `Vars.net.packetProvs` private field; fragile to Mindustry updates, could break or behave unexpectedly
- **HIGH** `Utils.java:291-312` — Generic `setField` uses `setAccessible(true)` without any security checks; allows bypassing access control on any object

### Data Loss

- **CRITICAL** `Main.java:100-110` — `checkDirVersion` + `writeDirVersion` calls `emptyDirectory(false)` which wipes the directory if version doesn't match; existing user data (schematics, map previews, images) can be silently deleted on version mismatch
- **HIGH** `Main.java:51-52` — `imageDir`, `mapsDir`, `schematicDir` have version checks; `backgroundsDir:53` and `musicsDir:54` do NOT — inconsistent data migration

### Concurrency

- **MEDIUM** `Utils.java:219-234` — `icons()` method: TOCTOU race between `containsKey` (line 220) and `put` (line 227); ConcurrentHashMap doesn't protect compound check-then-act
- **MEDIUM** `FeatureManager.java:80-101` — `setEnabled()`: `Core.settings.put` happens synchronously but `onEnable`/`onDisable` via `Core.app.post` is deferred; rapid toggling can cause out-of-order execution
- **LOW** `Main.java:56` — Static mutable `ObjectMap<packetReplacements>` shared across threads without synchronization

### Anti-Patterns

- **MEDIUM** `Config.java:18` — `API_v4_URL` is a duplicate of `API_URL:17` (same computed value); dead config field
- **MEDIUM** `Main.java:151-170` — `initFeatures()` uses `Reflect.get` + `packetProvs.replace`; replaces packets during iteration via side-effect in lambda
- **MEDIUM** `FeatureManager.java:51-61` — `init()` calls `feature.setting()` and `feature.dialog()` but ignores return values; dialogs created but never shown
- **LOW** `FeatureManager.java:42-45` — `register()` re-sorts full list on each batch; O(n log n) per batch

### Dead Code / Tech Debt

- **MEDIUM** `Config.java:11-14` — Commented-out localhost/dev URL variants accumulate dead config
- **MEDIUM** `WebFeature.java` — All URLs hardcoded with `/vi/` locale; no internationalization support for web links
- **LOW** `MdtInitEvent.java` — Empty marker event class; could be simplified to an interface or annotation
- **LOW** `Feature.java:33` — `getSettingKey()` hardcodes "mindustrytool." prefix as string literal; duplicated in multiple feature config classes

### Performance

- **LOW** `Utils.java:46-47` — Static ConcurrentHashMap caches (`iconCache`, `scalableIconCache`) with no eviction policy; memory grows unbounded
- **LOW** `Utils.java:56-58` — `readSchematic` caches schematics in `schematicData` per key but never evicts

### Architectural Violations

- **HIGH** `Main.java` — Entry point does setup, feature registration, UI button creation, packet reflection, and crash handling; violates Single Responsibility Principle
- **MEDIUM** Services use singleton pattern inconsistently: `ServerService`, `PlayerConnectService` use `getInstance()` but `MapService`, `SchematicService` use static methods directly
