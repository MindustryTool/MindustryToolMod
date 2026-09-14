# general-settings-dialog Specification

## Purpose
A declarative Solim dialog that aggregates mod-wide preferences as labeled toggle rows, each row backed by a `ConfigValue`-derived reactive signal. Currently exposes the beta participation toggle; designed for extension without structural changes.

## Requirements
### Requirement: Mod-Wide Settings Config Group
The mod SHALL expose a dedicated `ModSettings` class that holds a `ConfigGroup` namespaced under `mindustrytool.settings` and declares all global `ConfigValue` entries as public static fields, starting with `betaParticipate: ConfigValue<Boolean>` (default `false`).

#### Scenario: Beta flag is false by default
- **WHEN** `ModSettings.betaParticipate` is read without any prior user interaction
- **THEN** `ModSettings.betaParticipate.get()` returns `false`

#### Scenario: Beta flag persists across sessions
- **WHEN** the user sets `ModSettings.betaParticipate` to `true` and the game restarts
- **THEN** `ModSettings.betaParticipate.get()` returns `true` on the next load

### Requirement: General Settings Dialog Rendering
The `GeneralSettingsDialog` SHALL extend `SolimDialog` and render a scrollable vertical list of setting rows. Each row SHALL display a label (from `Core.bundle`) and a `checkBox` bound directly to the corresponding `ConfigValue.signal()`. The dialog SHALL have a "Settings" title (bundle key `dialog.general-settings.title`) and a close button.

#### Scenario: Dialog opens with correct initial toggle state
- **WHEN** `GeneralSettingsDialog` is shown and `ModSettings.betaParticipate.get()` is `false`
- **THEN** the beta participation checkbox is rendered unchecked

#### Scenario: Toggling checkbox persists immediately
- **WHEN** the user clicks the beta participation checkbox
- **THEN** `ModSettings.betaParticipate.get()` reflects the new value without any additional confirm action, and the value is persisted to `Core.settings`

#### Scenario: Tooltip visible on hover
- **WHEN** the user hovers over the beta participation row
- **THEN** a tooltip with localized text (`setting.beta.participate.tooltip`) is displayed warning that prereleases may be unstable

### Requirement: Beta Participation Drives Update Channel
`UpdateService` SHALL consult `ModSettings.betaParticipate.get()` when processing the GitHub releases response. When the flag is `true`, prereleases (entries where `"prerelease": true` in the JSON) SHALL be included in the candidate list for the update dialog. When `false`, prereleases SHALL be excluded.

#### Scenario: Beta off — prerelease excluded
- **WHEN** `ModSettings.betaParticipate.get()` is `false` and GitHub returns a prerelease newer than the installed version
- **THEN** the prerelease is ignored and no update dialog is shown (assuming no stable release is newer)

#### Scenario: Beta on — prerelease included
- **WHEN** `ModSettings.betaParticipate.get()` is `true` and GitHub returns a prerelease newer than the installed version
- **THEN** the prerelease appears in the changelog list in the update dialog
