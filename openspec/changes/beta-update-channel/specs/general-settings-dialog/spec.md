# general-settings-dialog Delta Specification

## MODIFIED Requirements

### Requirement: Beta Participation Drives Update Channel
When `ModSettings.betaParticipate.get()` is `false`, `UpdateService` SHALL consult it when processing the GitHub releases response as before: prereleases (entries where `"prerelease": true` in the JSON) SHALL be excluded from the changelog, and the stable `mod.hjson` version gate is unchanged. When the flag is `true`, `UpdateService` SHALL skip the `mod.hjson` fetch and determine the latest version solely from the GitHub releases list as the maximum tag over all entries (stable and prerelease) compared with `VersionUtils` semantics. If that latest tag is greater than the installed version, the update dialog SHALL be shown with the latest version displayed as the raw release tag (e.g. `v5.0.3-v8-beta`), a prerelease-inclusive changelog, and an Update action that installs that exact tag via the `githubImportMod(repo, isJava, release, forceEnable)` overload. Release fetch failure, an empty release list, and same-number ties after suffix stripping (e.g. `v5.0.3-v8` vs `v5.0.3-v8-beta`) SHALL resolve to silent (log and finish with no dialog).

#### Scenario: Beta off — prerelease excluded
- **WHEN** `ModSettings.betaParticipate.get()` is `false` and GitHub returns a prerelease newer than the installed version
- **THEN** the prerelease is ignored and no update dialog is shown (assuming no stable release is newer)

#### Scenario: Beta on — prerelease triggers dialog with raw tag
- **WHEN** `ModSettings.betaParticipate.get()` is `true` and the maximum release tag (e.g. `v5.0.3-v8-beta`) is greater than the installed version (e.g. `v5.0.1-v8`)
- **THEN** the update dialog is shown displaying the raw release tag, the changelog includes prereleases, and activating Update installs that exact tag

#### Scenario: Beta on — nothing newer stays silent
- **WHEN** `ModSettings.betaParticipate.get()` is `true` and no release tag is greater than the installed version
- **THEN** no update dialog is shown

#### Scenario: Beta on — fetch failure or empty releases ignored
- **WHEN** `ModSettings.betaParticipate.get()` is `true` and the releases fetch fails or returns no usable entries
- **THEN** the failure is logged and finished silently with no dialog and no error popup

#### Scenario: Beta on — same-number tie stays silent
- **WHEN** `ModSettings.betaParticipate.get()` is `true` and the newest prerelease tag parses equal to the installed version (e.g. `v5.0.3-v8-beta` vs installed `v5.0.3-v8`)
- **THEN** no update dialog is shown
