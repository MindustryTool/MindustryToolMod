## ADDED Requirements

### Requirement: Update package owns update-check orchestration with SOLID boundaries

`mindustrytool.update` SHALL own all update-check logic previously in `old.mindustrytool.services.UpdateService` and be decomposed into single-responsibility collaborators: `VersionUtils` (pure version math), `ChangelogFormatter` (pure release-to-string), `UpdateClient` (HTTP boundary delegating to `Request`/`Github`), `UpdateView` (dialog/UI), and `UpdateService` (orchestrator). `UpdateService` SHALL depend on collaborator interfaces (DIP) and be constructible via dependency injection, not hard-wired singletons.

#### Scenario: Package structure exists without old imports
- **WHEN** `src/mindustrytool/update/` is listed and each file is inspected
- **THEN** it contains `UpdateService.java`, `VersionUtils.java`, `ChangelogFormatter.java`, `UpdateClient.java` (or `GithubUpdateClient`), `UpdateView.java`, and `UpdateDialog.java`, each with `package mindustrytool.update` and no `import old.*`

#### Scenario: UpdateService depends on interfaces
- **WHEN** `UpdateService.java` fields and constructor are inspected
- **THEN** it holds `UpdateClient`/`ChangelogFormatter`/`UpdateView` (interfaces) injected via constructor, and contains no direct `HttpClient`/`HttpRequest` construction and no inline `arc.util.Http`

#### Scenario: Single-responsibility class split
- **WHEN** responsibilities are mapped
- **THEN** `VersionUtils` contains only `parseVersion`/`compare`/`format` statics, `ChangelogFormatter` only formats changelog text, `UpdateClient` only performs HTTP fetches, `UpdateView` only shows UI, and `UpdateService` only orchestrates `checkForUpdate`

### Requirement: VersionUtils is pure and covers legacy behavior with bug fix

`VersionUtils` SHALL expose pure static helpers derived from `UpdateService.extractVersionNumber:146`, `isVersionGreater:164`, and `versionToString:176`: `int[] parseVersion(String)` (strips leading `v`, removes suffix after `-`/`+`, keeps digits/dots, splits on `.`, falls back to `int[0]`), `boolean isGreater(int[],int[])` (lexicographic, longer wins), and `String format(int[])` (dot-join). It SHALL fix the `indexOf("v")`/`indexOf("-")` crash when version contains `v` without `-` and SHALL have no Arc/Mindustry dependencies.

#### Scenario: parseVersion handles v-prefix and suffix
- **WHEN** `parseVersion("v8-136")` is called
- **THEN** it returns `int[]{8,136}` or `int[]{8}` depending on stripping rule (documented), and `parseVersion("1.2.3-beta")` returns `int[]{1,2,3}`

#### Scenario: parseVersion is null/empty/invalid safe
- **WHEN** `parseVersion(null)` or `parseVersion("")` or `parseVersion("abc")` is called
- **THEN** it returns `int[0]` without throwing

#### Scenario: isGreater compares lexicographically and by length
- **WHEN** `isGreater(new int[]{1,3,0}, new int[]{1,2,9})` then `isGreater(new int[]{1,2}, new int[]{1,2,0})` then `isGreater(new int[]{1,2}, new int[]{1,2})`
- **THEN** results are `true`, `false` (shorter is not greater), `false` (equal)

#### Scenario: format round-trips
- **WHEN** `format(new int[]{1,2,3})` is called
- **THEN** it returns `"1.2.3"` and `format(new int[0])` returns `""`

### Requirement: HTTP via instance Request delegation

Update HTTP SHALL be performed only via instance-based `mindustrytool.services.Request` (through `Github` or `MindustryTool`), not `arc.util.Http` or direct `java.net.http.HttpClient` inside `UpdateService`. Version is fetched from `Config.MOD_HJSON_URL`, releases from `Config.GITHUB_API_URL` (via `Github.getModHjson()`/`Github.getReleases()` or equivalent `UpdateClient` methods), and ping via `MindustryTool.ping("mod-v8")`.

#### Scenario: No direct HttpClient in update package
- **WHEN** `src/mindustrytool/update/*.java` are searched for `HttpClient.newBuilder`, `HttpRequest.newBuilder`, and `arc.util.Http`
- **THEN** no matches are found; all HTTP goes through `Github`/`MindustryTool`/`Request.builder()`

#### Scenario: Endpoints are correct
- **WHEN** `UpdateClient`/`GithubUpdateClient` is inspected
- **THEN** it references `Config.MOD_HJSON_URL` for version and `Config.GITHUB_API_URL` for releases, and no `Config.API_URL` GitHub confusion (per `github-service` spec)

#### Scenario: Error paths invoke done and show dialog
- **WHEN** release fetch fails (non-2xx) or JSON parse fails
- **THEN** `UpdateService` posts to `Core.app.post` and shows `UpdateView` with an error changelog string from bundle keys, then invokes the `done` runnable exactly as legacy `fetchReleasesAndShowDialog:70` does

### Requirement: ChangelogFormatter is pure and bundle-aware

`ChangelogFormatter` SHALL format up to 20 releases into Mindustry markup: `[accent]tag[white]`, optional `yyyy-MM-dd HH:mm` date in `ZoneId.systemDefault()`, `[gold]` download-count line from summed `assets[].download_count`, and `renderMarkdown(body)` conversion (links→`[sky]`, headers→`[accent]`, lists→`•`, bold/italic/code). Labels SHALL be bundle keys, not hard-coded English.

#### Scenario: Pure formatting without Arc runtime
- **WHEN** `ChangelogFormatter.format(releases)` is called with a parsed release list
- **THEN** it returns a `String` without touching `Core.app` or network, and caps output at 20 releases

#### Scenario: Markdown transforms preserved
- **WHEN** body contains `**bold**`, `*italic*`, `` `code` ``, `[text](url)`, `## header`, `- item`
- **THEN** output contains `[white]bold[white]`, `[lightgray]italic[white]`, `[cyan]code[white]`, `[sky]text[white]`, `[accent]header[white]`, `• item` respectively

### Requirement: Update dialog uses bundle keys for all user-visible text

`UpdateDialog` (refactored from `UpdateAvailableDialog:13`) SHALL use `Core.bundle.get`/`format` for every user-visible string and SHALL NOT contain hard-coded English literals for titles, buttons, or messages. All new keys SHALL be added to `assets/bundles/bundle.properties` per `AGENTS.md` key conventions (`update.*`).

#### Scenario: Dialog has no hard-coded display strings
- **WHEN** `UpdateDialog.java` string literals are inspected
- **THEN** title `"Update Available"`, button labels `"Cancel"`/`"Update"`, and status lines use `Core.bundle.get("update.dialog.title")`, `Core.bundle.get("update.button.cancel")`, etc., and `Core.bundle.format("update.message.new-version", currentVer, latestVer)` for the version banner

#### Scenario: Bundle keys exist and follow naming
- **WHEN** `assets/bundles/bundle.properties` is inspected
- **THEN** it contains keys such as `update.dialog.title=Update Available`, `update.message.new-version=…{0}…{1}…`, `update.button.cancel=Cancel`, `update.button.update=Update`, `update.changelog.download-count=Download count: {0}`, `update.error.fetch-releases=Could not fetch release notes.`, `update.error.fetch-releases-with-status=Could not fetch release notes: {0}`, `update.error.parse-releases=Could not parse release notes.`, `update.label.no-description=No description provided.` and all use lowercase dot-separated `update.*` names

#### Scenario: Dynamic values use format
- **WHEN** a message contains `currentVer`/`latestVer` or `downloadCount`
- **THEN** it uses `Core.bundle.format("update.*", value)` not string concatenation of translated fragments

### Requirement: Unit tests cover VersionUtils (and formatter)

The change SHALL ship JUnit 5 unit tests for `VersionUtils` (required) and `ChangelogFormatter` markdown helpers (recommended) that run via `./gradlew test` without a Mindustry runtime. Tests SHALL live under `src/test/java/mindustrytool/update/` (or project-standard test source set).

#### Scenario: VersionUtilsTest covers edge cases
- **WHEN** `./gradlew test --tests "mindustrytool.update.VersionUtilsTest"` runs
- **THEN** tests for `parseVersion` (null, empty, `"v8"`, `"1.2.3"`, `"v8-1.0"`, `"abc"`, `"1..2"`), `isGreater` (equal, greater major/minor, longer array), and `format` (empty, single, multi) all pass

#### Scenario: Tests do not require Arc/Core
- **WHEN** `VersionUtilsTest.java` imports are inspected
- **THEN** it imports only `org.junit.jupiter.api.*` and `mindustrytool.update.VersionUtils`, with no `arc.*` or `mindustry.*`

#### Scenario: Build is configured for JUnit 5
- **WHEN** `build.gradle:48` dependencies are inspected
- **THEN** it declares `testImplementation 'org.junit.jupiter:junit-jupiter:…'` and `tasks.test { useJUnitPlatform() }` (or equivalent), and `./gradlew test` succeeds

### Requirement: Legacy UpdateService is retired

After wiring, `src/old/mindustrytool/services/UpdateService.java:1` and `UpdateAvailableDialog.java:1` SHALL be removed or marked `@Deprecated` with Javadoc pointing to `mindustrytool.update.UpdateService`, and `mindustrytool.Main` SHALL not import `old.mindustrytool.services.*` for update checks.

#### Scenario: New Main wires new service
- **WHEN** `src/mindustrytool/Main.java:30` client-load path is inspected
- **THEN** it constructs or obtains `mindustrytool.update.UpdateService` and calls `checkForUpdate(Runnable)` instead of `old.mindustrytool.services.UpdateService.getInstance()`

#### Scenario: No old update import remains in new code
- **WHEN** `grep -R "old\\.mindustrytool\\.services\\.Update" src/mindustrytool` is run
- **THEN** it returns no results
