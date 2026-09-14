## Why

The beta participation toggle (`ModSettings.betaParticipate`) is currently channel-blind: the update gate compares only the stable `mod.hjson` version, so beta users are never offered prereleases (e.g. installed `v5.0.1-v8` with beta `v5.0.3-v8-beta` published shows no dialog). The flag only filters changelog lines after a stable-driven gate.

## What Changes

- **MODIFY** `UpdateService` — when beta participation is enabled, skip the `mod.hjson` fetch and determine the latest version solely from the GitHub releases list (max tag over all entries, stable and prerelease). When disabled, the stable `mod.hjson` flow is unchanged.
- **MODIFY** `UpdateDialog` — on the beta path the dialog shows the latest version as the raw release tag (e.g. `v5.0.3-v8-beta`) and its Update action installs that exact tag via the `githubImportMod(repo, isJava, release, forceEnable)` overload.
- **DEFINE** beta-path edge behavior: release fetch failure, empty release list, and same-number ties after suffix stripping (e.g. `v5.0.3-v8` vs `v5.0.3-v8-beta`) all resolve to silent (log and finish, no dialog).
- **NO** network-fetch tests: version fetching is not unit-tested (no mocked HTTP for the gate); tests cover only pure logic (latest-tag selection, prerelease filtering, version math).

## Capabilities

### New Capabilities

None — this change redefines behavior of existing capabilities.

### Modified Capabilities

- `general-settings-dialog`: the "Beta Participation Drives Update Channel" requirement changes from changelog-only filtering to a channel-aware update gate (releases-only latest, raw tag display, exact-tag install, silent edge behavior).
- `update-service`: `UpdateService` gains a releases-driven latest check and exact-tag install path used by the beta channel; stable-channel orchestration is unchanged.

## Impact

- `mod/src/mindustrytool/services/update/UpdateService.java` — beta-path branch (releases-only gate).
- `mod/src/mindustrytool/services/update/UpdateDialog.java` — exact-tag install action for beta results.
- Pure helper for latest-tag selection alongside `ChangelogFormatter`/`VersionUtils` (location at implementer's discretion); no new external dependencies.
- No new bundle keys expected (raw tag display needs no i18n); no `mod.hjson` or API changes.
- No network-dependent unit tests; existing `ChangelogFormatterTest` / `ChangelogFormatterPrereleaseTest` / `ModSettingsTest` remain green.
