## Context

`old.mindustrytool.services.UpdateService` (`src/old/mindustrytool/services/UpdateService.java:1`) currently owns the entire update-check flow: ping (`Config.API_URL + "ping?client=mod-v8"`), version fetch from `Config.API_REPO_URL` via `Jval`, `Github`-style release fetch via `Config.GITHUB_API_URL`, changelog string building (tag, `Instant`/`DateTimeFormatter`, download counts, `Utils.renderMarkdown`), and dialog display through `UpdateAvailableDialog`. It uses `arc.util.Http`, hard-coded English strings, a singleton via `getInstance()`, and private helpers (`extractVersionNumber:146`, `isVersionGreater:164`, `versionToString:176`, `fetchReleasesAndShowDialog:69`, `showUpdateDialog:142`). The new codebase already provides `mindustrytool.Config:1`, `mindustrytool.services.Request:1` (instance-based, `java.net.http`), `mindustrytool.services.Github:1`, `mindustrytool.utils.JsonUtils:1`, and `mindustrytool.Main:1` as the replacement entry point. No test harness exists yet and `assets/bundles/bundle.properties` is empty.

Constraints: keep `Request`/`Github`/`MindustryTool` contracts intact (per `openspec/specs/http-client`, `github-service`, `mindustrytool-api`), respect `AGENTS.md` i18n rules, avoid `old.*` imports in `src/mindustrytool`, use Java 17.

## Goals / Non-Goals

**Goals:**
- Relocate update logic from `src/old/**` into `src/mindustrytool/update/**` with SOLID separation so each concern is independently testable and replaceable.
- Make version utilities pure and unit-tested (no Arc/Mindustry runtime).
- Replace hard-coded UI strings with `bundle.properties` keys via `Core.bundle.get/format`.
- Replace `arc.util.Http` + `Jval` fetching in `UpdateService` with instance `Request` (`Github`/`MindustryTool` or dedicated `UpdateClient`), preserving current endpoints (`MOD_HJSON_URL`, `GITHUB_API_URL`, ping via `MindustryTool.ping`).

**Non-Goals:**
- Changing update UX beyond moving to bundle keys (no new screens/flows).
- Reworking `Github`/`MindustryTool` APIs or introducing new external deps.
- Desktop/Android packaging changes.
- Translating the bundle file itself — only English default keys are in scope.

## Decisions

### 1. Package + class split — SRP over single-class port

**Decision:** Create `mindustrytool.update` with:
- `VersionUtils` — static pure helpers: `parseVersion(String) -> int[]`, `compare(int[],int[]) -> int`, `isGreater`, `format(int[])`. Favours testability; closes bug in `extractVersionNumber:149` (`indexOf("v")`/`indexOf("-")` crashes when `-` absent).
- `ChangelogFormatter` — `format(List<ReleaseDto>) -> String` (or `format(Jval)` shim), injected markdown renderer + date formatter + i18n lookups.
- `UpdateClient` interface + `GithubUpdateClient` impl — `fetchVersion()`, `fetchReleases()` via `Github`/`Request`; isolates HTTP for mocking.
- `UpdateView` interface + `DialogUpdateView` impl — `show(current, latest, changelog, onDone)`; isolates Arc UI thread (`Core.app.post`).
- `UpdateService` — orchestrator, constructor-injected `UpdateClient + ChangelogFormatter + UpdateView + VersionUtils` (or static utils), exposes `checkForUpdate(Runnable done)` matching old signature for drop-in wiring in `Main`.

**Alternative considered:** Direct 1:1 line port into `mindustrytool.update.UpdateService`. Rejected — leaves SRP violation, untestable statics, and re-introduces `arc.util.Http` debt.

### 2. HTTP boundary — delegate to existing `Github`/`MindustryTool` instead of raw `Request` inside service

**Decision:** `UpdateService`/`UpdateClient` delegates to `Github.getModHjson()`/`Github.getReleases()` (or `MindustryTool.ping`) which already wrap `Request`. Keeps import hygiene per `github-service` spec and avoids duplicating URL logic. If `Github` lacks paging needed (`getReleases(page,per)`), use it; otherwise extend `Github` before inline `Request`.

**Alternative:** New `UpdateApi` with its own `Request` instances. Viable but duplicates `Github` coverage; prefer thin delegation.

### 3. Version parsing — fix and spec via unit tests

**Decision:** `VersionUtils.parseVersion` defined as: strip leading `v`, trim after `-`/`+`, `replaceAll("[^0-9.]","")`, split `\\.`, parse ints, return `int[0]` on failure (preserving old fallback) but without `StringIndexOutOfBoundsException` risk. Behavior is enumerated in spec scenarios and becomes the test oracle.

**Alternative:** Semantic-version library. Rejected — extra dep for 3-method helper; keep stdlib.

### 4. Changelog formatting — pure, bundle-aware

**Decision:** Date formatting stays `DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())` but wrapped so tests can inject a fixed clock/formatter. Labels (`download-count`, fetch-error messages) come from bundle keys, not literals like `[gold]Download count: ` or `Could not fetch…` (`UpdateService:75`, `UpdateAvailableDialog:21`). Markdown uses `Utils.renderMarkdown` logic copied as `MarkdownRenderer` or `ChangelogFormatter.renderMarkdown` to avoid `old.*` import.

**Alternative:** Keep `old.mindustrytool.Utils.renderMarkdown` via import. Rejected — violates `api-models` spec hygiene (`no old.*`).

### 5. Dialog / i18n

**Decision:** `UpdateDialog` (refactored from `old.mindustrytool.services.UpdateAvailableDialog:13`) uses keys such as `update.dialog.title`, `update.dialog.new-version`, `update.button.cancel`, `update.button.update`, `update.changelog.download-count`, `update.error.fetch-releases`, `update.error.parse-releases`, etc. Added to `assets/bundles/bundle.properties`. Title was hard-coded `"Update Available"`; values like Discord/donate keep existing constants (`Config.DISCORD_INVITE_URL`) but labels are bundled.

### 6. Testing — JUnit 5 for utils, no Arc runtime

**Decision:** Add `testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'` (and `mockito` optional for `UpdateService` if async tests added later). `VersionUtilsTest` is the mandatory gate: covers `extractVersionNumber`, `isVersionGreater`, `versionToString` edge cases. `ChangelogFormatterTest` covers empty/null markdown, header/list/bold/italic/code transforms, and date branch. `UpdateService` orchestration is verified manually or with lightweight `CompletableFuture` fakes; not required for this change's unit-test gate.

**Alternative:** `arc` headless test harness. Overkill for pure utils; defer.

## Risks / Trade-offs

- [Version string drift] `mod.hjson` version format (`v8-...`) changes → `VersionUtils` misparses → Mitigation: tests cover `v`+`-`+suffix stripping and fallback to `int[0]`; log on failure as before.
- [Missing bundle keys] new keys not loaded on older client builds → `Core.bundle.get` throws/misses → Mitigation: add all keys to `bundle.properties` in same commit; guard with try/fallback like `old.Utils.getString`.
- [Async error swallowing] `Http`→`Request` refactor changes error types (`HttpStatusException` vs `HttpResponse.statusCode`) → Mitigation: map non-2xx to same dialog fallback strings via bundle keys; keep `done.run()` / `Core.app.post` semantics identical.
- [Singleton vs DI] old `getInstance()` singleton complicates testing → Mitigation: new `UpdateService` is instantiable with DI; keep a static `getInstance()` facade delegating to DI instance only if `Main` needs zero-arg access.
- [No existing test harness] first test setup may need Gradle config tweak → Mitigation: mirror `src` layout with `src/test/java` and add `useJUnitPlatform()`; verify `./gradlew test` passes.

## Migration Plan

1. Add `bundle.properties` keys (no code yet).
2. Create `src/mindustrytool/update/{VersionUtils,ChangelogFormatter,UpdateClient,UpdateView,UpdateService,UpdateDialog}.java` and wire `Request`/`Github` delegation.
3. Add unit tests for `VersionUtils` (and formatter); run `./gradlew test`.
4. Switch `mindustrytool.Main` (new `Main.java:30` client-load hook) to call `new UpdateService(...).checkForUpdate(...)` or `UpdateService.getInstance().checkForUpdate(...)`; keep old class deprecated until cutover proven.
5. Remove `src/old/mindustrytool/services/UpdateService.java` / `UpdateAvailableDialog.java` after QA (separate commit, not required to land this change).
6. Rollback: revert package + bundle keys; old service remains functional.

## Open Questions

- Should `ping?client=mod-v8` remain in `UpdateService` or move to `MindustryTool.ping("mod-v8")`? — Leaning `MindustryTool.ping`.
- Keep `UpdateService.checkForUpdate(Runnable)` signature or migrate to `CompletableFuture<Void>`? — Keep `Runnable` for compat; add overload later if desired.
- Exact bundle key prefix (`update.*` vs `dialog.update.*`) — propose `update.*` for brevity; bikeshed in review.
