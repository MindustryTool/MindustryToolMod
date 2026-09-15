## Why

`src/old/mindustrytool/services/UpdateService.java` is a legacy singleton that mixes version parsing, HTTP fetching, changelog building, dialog display, and translation concerns in a single class with hardcoded user-visible strings, direct `arc.util.Http` calls, and no unit tests. Migrating it into the new `src/mindustrytool/update/` module with SOLID decomposition, proper i18n, and testable utils is required to retire `src/old` and keep update-check behavior maintainable.

## What Changes

- Create `src/mindustrytool/update/` package replacing `old.mindustrytool.services.UpdateService`:
  - `UpdateService` — orchestrator (check-for-update flow, dependency-injected HTTP + version + changelog + dialog collaborators).
  - `VersionUtils` — pure version parsing/comparison/formatting extracted from private methods (`extractVersionNumber`, `isVersionGreater`, `versionToString`).
  - `ChangelogFormatter` — builds display changelog from release JSON (tag, date, download count, markdown rendering), extracted from `fetchReleasesAndShowDialog`.
  - `UpdateDialog` — dialog/view layer (renamed/refactored from `UpdateAvailableDialog`), uses `Core.bundle` keys for all user-visible text.
  - `UpdateApi` (or collaborators on `Github`/`MindustryTool` services) — HTTP boundary that delegates to instance-based `Request` clients; no direct `HttpClient`/`Http` construction inside `UpdateService`.
- Remove or deprecate `src/old/mindustrytool/services/UpdateService.java` and `UpdateAvailableDialog.java` once new module is wired (keep old until fully cut over if needed, but mark deprecated).
- Apply SOLID cleanup:
  - **SRP**: version utils, changelog formatting, HTTP fetching, and dialog are separate classes.
  - **OCP/DIP**: `UpdateService` depends on interfaces (`VersionComparator`, `ChangelogProvider`, `UpdateView`, `UpdateClient`) so sources (GitHub releases vs. `mod.hjson` via `Config.MOD_HJSON_URL`/`Config.GITHUB_API_URL`) can change without modifying `UpdateService`.
  - **ISP**: narrow interfaces per collaborator.
- Add `assets/bundles/bundle.properties` keys for all update-related user-visible text (dialog title, new-version message, buttons, changelog labels, error messages for release-notes fetch/parse — replacing hard-coded English strings in `UpdateService` and `UpdateAvailableDialog`).
- Add unit tests for `VersionUtils` (and `ChangelogFormatter` if pure) — JUnit 5, no Mindustry runtime required; tests live under `tests/` or `src/test` per project conventions and verify edge cases (v-prefix, suffixes, empty/invalid, length-mismatched comparisons).

## Capabilities

### New Capabilities
- `update-service`: Update-check orchestration, version utilities, changelog formatting, and update dialog under `mindustrytool.update` with SOLID boundaries, instance-based `Request` HTTP, bundle-backed i18n, and unit-tested utils.

### Modified Capabilities
- None — existing specs (`http-client`, `github-service`, `mindustrytool-api`, `api-models`) are reused, not changed. This change does not alter their requirements; it consumes their contracts.

## Impact

- **Code**: `src/old/mindustrytool/services/UpdateService.java:1`, `src/old/mindustrytool/services/UpdateAvailableDialog.java:1`, new `src/mindustrytool/update/**`, `assets/bundles/bundle.properties`, `src/mindustrytool/Config.java:1` (already has `MOD_HJSON_URL`/`GITHUB_API_URL`), `src/mindustrytool/services/Request.java:1` and `Github.java:1` (consumed, not modified).
- **Tests**: New unit tests for `mindustrytool.update.VersionUtils` (and formatter); no existing test suite to regress (project currently has no `test/` dir).
- **i18n**: New keys in `assets/bundles/bundle.properties` per `AGENTS.md`.
- **Runtime**: No breaking API; old service is replaced internally. Callers (e.g., `Main.init`) switch import from `old.mindustrytool.services.UpdateService` to `mindustrytool.update.UpdateService`.
