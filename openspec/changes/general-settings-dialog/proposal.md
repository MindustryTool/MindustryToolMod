## Why

The mod currently has no single place for global options that apply outside of any individual feature. Adding a **General Settings** dialog provides a reusable home for cross-cutting mod preferences—starting with the opt-in to beta (prerelease) updates—without cluttering the per-feature cards in `FeatureSettingDialog`.

## What Changes

- **NEW** `GeneralSettingsDialog` — a `SolimDialog` opened from a "Settings" button in `FeatureSettingDialog` that hosts mod-wide preferences.
- **NEW** `ModSettings` — a config group holding all global `ConfigValue` entries; initially one: `betaParticipate: ConfigValue<Boolean>` (default `false`).
- **MODIFY** `FeatureSettingDialog` — adds a bottom-right action button that opens `GeneralSettingsDialog`.
- **MODIFY** `UpdateService` — reads `ModSettings.betaParticipate` when fetching GitHub releases to include prereleases when the flag is enabled.
- **NEW** bundle keys for the dialog title, toggle label, tooltip, and update status strings.

## Capabilities

### New Capabilities
- `general-settings-dialog`: A declarative Solim dialog that lists mod-wide settings as labeled toggle rows, each row backed by a `ConfigValue`-derived reactive signal. Starts with the beta participation toggle.

### Modified Capabilities
- `feature-settings-dialog`: Gains a "Settings" action button at the bottom-right that opens the general settings dialog.

## Impact

- **`mod/src/mindustrytool/features/settings/`** — new `GeneralSettingsDialog.java`, new `ModSettings.java`.
- **`mod/src/mindustrytool/features/settings/FeatureSettingDialog.java`** — one `actionButton` call added.
- **`mod/src/mindustrytool/services/update/UpdateService.java`** — prerelease filtering driven by `ModSettings.betaParticipate`.
- **`assets/bundles/bundle.properties`** — new i18n keys.
- No new external dependencies; uses existing `solim.config.ConfigGroup`, `ConfigValue`, and `SolimDialog` APIs.
