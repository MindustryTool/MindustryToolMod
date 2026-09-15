## 1. Bundle keys (i18n gate)

- [x] 1.1 Add `update.*` keys to `assets/bundles/bundle.properties` per spec — `update.dialog.title`, `update.message.new-version` (with `{0}`/`{1}` slots), `update.button.cancel`, `update.button.update`, `update.changelog.download-count` (`{0}`), `update.error.fetch-releases`, `update.error.fetch-releases-with-status` (`{0}`), `update.error.parse-releases`, `update.label.no-description`, `update.status.up-to-date`, `update.status.require-update`; verify `AGENTS.md` checklist (reuse existing keys if any, `Core.bundle.format` for dynamic values, lowercase dot naming).
- [x] 1.2 Grep `src/old/mindustrytool/services/UpdateService.java` and `UpdateAvailableDialog.java` for hard-coded user strings and map each to the new bundle key; ensure no translated string remains hard-coded in new `update/` code.

## 2. Build & test harness

- [x] 2.1 Configure JUnit 5 in `build.gradle` — add `testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'` (and `testRuntimeOnly` if needed) and `tasks.test { useJUnitPlatform() }`; create test source set `src/test/java` (or project-standard location) so `./gradlew test` discovers `mindustrytool.update.*Test`.
- [x] 2.2 Verify `./gradlew test` runs (green on empty suite) before implementing utils.

## 3. VersionUtils (pure, bug-fixed port)

- [x] 3.1 Create `src/mindustrytool/update/VersionUtils.java` — `public final` with `parseVersion(String)`, `isGreater(int[],int[])` (or `compare`), `format(int[])` ported from `UpdateService.extractVersionNumber:146`/`isVersionGreater:164`/`versionToString:176`; fix `v` without `-` crash (`indexOf` guard / regex), no `arc.*` imports, Javadoc the stripping rule (`v` prefix, `-`/`+` suffix, `[^0-9.]` strip).
- [x] 3.2 Write `src/test/java/mindustrytool/update/VersionUtilsTest.java` — cases: `null`, `""`, `"abc"`, `"1.2.3"`, `"v8"`, `"v8-136"`/`"1.2.3-beta"`/`"2.0+build"`, malformed `"1..2"`; `isGreater` (equal, major/minor greater, shorter-not-greater, empty), `format` (empty, single, multi); assert no throws; keep imports to JUnit + `VersionUtils`.

## 4. Changelog formatting (pure, bundle-aware)

- [x] 4.1 Create `src/mindustrytool/update/ChangelogFormatter.java` — pure `format(List<Release>)` (or `format(Jval)` shim) that caps at 20, builds `[accent]tag`, optional date via `DateTimeFormatter("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())`, summed `[gold]` download count from `assets[].download_count`, and `renderMarkdown` (link/header/list/bold/italic/code) copied from `old.mindustrytool.Utils.renderMarkdown:174` without `import old.*`; labels via `Core.bundle.get("update.*")` or caller-supplied bundle lookup for testability.
- [x] 4.2 Write `src/test/java/mindustrytool/update/ChangelogFormatterTest.java` (or extend existing test) — verify markdown transforms, download-count sum, 20-cap, empty/null body uses `update.label.no-description`, date branch with fixed `Instant`/formatter.

## 5. HTTP boundary (Request delegation)

- [x] 5.1 Create `src/mindustrytool/update/UpdateClient.java` interface — `CompletableFuture<String> fetchModHjson()`, `CompletableFuture<String> fetchReleases()` (and paged overload if needed) with no `HttpClient` mention.
- [x] 5.2 Create `src/mindustrytool/update/GithubUpdateClient.java` (or `UpdateApi`) — implements `UpdateClient` by delegating to `Github.getModHjson()` / `Github.getReleases()` and `MindustryTool.ping("mod-v8")`; uses `Config.MOD_HJSON_URL`/`Config.GITHUB_API_URL` only, no `arc.util.Http`, no `old.*`, explicit imports.
- [x] 5.3 Verify `grep -R "HttpClient.newBuilder\|arc.util.Http" src/mindustrytool/update` is empty and all URLs resolve per spec.

## 6. View / dialog (bundle-backed)

- [x] 6.1 Create `src/mindustrytool/update/UpdateView.java` interface — `void show(String currentVer, String latestVer, String changelog, Runnable done)`.
- [x] 6.2 Create `src/mindustrytool/update/UpdateDialog.java` — refactored from `old.mindustrytool.services.UpdateAvailableDialog:13`, uses `Core.bundle.get/format` for title/buttons/banner, posts via `Core.app.post` if needed, no hard-coded English.
- [x] 6.3 Create `DialogUpdateView` (or make `UpdateDialog` implement `UpdateView`) so `UpdateService` depends on the interface (DIP).

## 7. Orchestrator — UpdateService (SOLID)

- [x] 7.1 Create `src/mindustrytool/update/UpdateService.java` — constructor-injected `UpdateClient + ChangelogFormatter + UpdateView` (plus `VersionUtils` static), keeps legacy `checkForUpdate(Runnable done)` semantics: ping, fetch `mod.hjson` version, compare via `VersionUtils.isGreater`, fetch releases on update-required, format via `ChangelogFormatter`, `Core.app.post` to `UpdateView.show`; error branches show bundle error strings and always call `done`; no `arc.util.Http`, no `Jval` outside client if possible, no `old.*`.
- [x] 7.2 Ensure `UpdateService` has DIP窄 interfaces (ISP): `UpdateClient`/`UpdateView` are minimal; keep a static `getInstance()` facade only if `Main` needs zero-arg access, otherwise require DI.
- [x] 7.3 Run `./gradlew test` and manual smoke: `VersionUtils`/`ChangelogFormatter` tests green, `grep -R "old\\.mindustrytool" src/mindustrytool/update` empty, `grep -R "Core\\.bundle" src/mindustrytool/update` present for all display strings.

## 8. Wiring & retirement of old code

- [x] 8.1 Wire `src/mindustrytool/Main.java:30` (`ClientLoadEvent`) to `mindustrytool.update.UpdateService` (DI or `getInstance().checkForUpdate(...)`), replacing any `old.mindustrytool.services.UpdateService` call; ensure `src/mindustrytool/**` no longer imports `old.mindustrytool.services.*`.
- [x] 8.2 Deprecate/remove `src/old/mindustrytool/services/UpdateService.java` and `UpdateAvailableDialog.java` — either delete (after QA) or annotate `@Deprecated` with Javadoc `Use mindustrytool.update.UpdateService` and keep until next release; record decision in PR notes.
- [x] 8.3 Final verification: `./gradlew test`, `./gradlew jar`, grep checks (`no old.* in src/mindustrytool/update`, no `HttpClient` construction, bundle keys present), and `AGENTS.md` i18n checklist (all user-visible text bundled, `format` for dynamic values, keys added).

