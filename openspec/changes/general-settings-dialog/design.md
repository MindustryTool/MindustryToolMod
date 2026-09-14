## Context

The mod currently has no single point for cross-feature, mod-wide settings. Individual features manage their own config groups (e.g. `TranslationFeature.config`, `ChatFeature.config`). There is no facility for options that do not belong to any specific feature—such as the update channel preference (stable vs. prerelease). The `FeatureSettingDialog` exposes feature-level controls only.

The Solim config system (`ConfigGroup` + `ConfigValue`) already provides persistent, reactive configuration with bidirectional signal synchronization backed by `Core.settings`. The pattern is mature and proven across multiple features.

## Goals / Non-Goals

**Goals:**
- Introduce `ModSettings` as the single config group (`ConfigGroup.of("mindustrytool.settings")`) that owns all global mod preferences.
- Add `betaParticipate: ConfigValue<Boolean>` (default `false`) as the first setting.
- Introduce `GeneralSettingsDialog` — a declarative Solim dialog — that renders one toggle row per setting.
- Wire a "Settings" action button into `FeatureSettingDialog` to open the dialog.
- Integrate `ModSettings.betaParticipate` into `UpdateService` to filter GitHub prereleases.
- Provide i18n keys for all user-visible text.

**Non-Goals:**
- Do not convert individual feature configs to use `ModSettings`; each feature retains its own `ConfigGroup`.
- Do not add a separate settings screen outside `FeatureSettingDialog`; the button is the only entry point.
- Do not implement update download / install logic — only the release-list filter changes.

## Decisions

### Decision 1 — `ModSettings` is a standalone class, not part of `Config.java`
**Why**: `Config.java` holds static constants (URLs, IDs). Mixing persistent reactive state into a constants class would violate the single-responsibility principle and create a confusing layering. A dedicated `ModSettings` class mirrors the existing per-feature pattern (e.g. `TranslationFeature.config`).

**Alternative considered**: Extend `Config.java` with a static `ConfigGroup` field and static `ConfigValue` fields. Rejected because it entangles URL constants with mutable preference state.

### Decision 2 — `GeneralSettingsDialog` renders rows declaratively; no separate row component
**Why**: There is currently only one setting. A dedicated `SettingsRow` component abstraction would be premature. A simple `column()` of `row()` entries inside `GeneralSettingsDialog.build()` is enough and follows the "no unnecessary layers" project rule. If settings grow, this can be extracted then.

**Alternative considered**: A `SettingsRowComponent` with label + checkbox. Deferred until there are ≥ 3 settings.

### Decision 3 — `checkBox` binds directly to `ConfigValue.signal()`
**Why**: `ConfigValue.signal()` is a `Signal<Boolean>` with bidirectional persistence. Binding the Solim `checkBox` directly means no extra state — toggling the checkbox immediately persists the value via `Core.settings`. No OK/Cancel buttons needed; changes are live.

**Alternative considered**: Buffer changes in a local `Signal<Boolean>` and flush on OK. Rejected because it adds complexity with no clear benefit for a simple toggle.

### Decision 4 — `UpdateService` filters by `prerelease == true` in JSON response
**Why**: The GitHub releases API returns both stable and prerelease entries in the same response. Filtering client-side avoids a separate API call and keeps the network layer unchanged.

**Alternative considered**: Use `?prerelease=true` as a query param. Not a real GitHub API parameter; the API does not support this filter server-side.

## Risks / Trade-offs

- **Risk**: `Core.settings` persists to disk; a key collision with another mod could corrupt the flag.  
  → **Mitigation**: Namespace the key under `mindustrytool.settings.betaParticipate` — specific enough to avoid collision.

- **Risk**: User enables beta, receives a prerelease, and is confused by instability.  
  → **Mitigation**: The toggle tooltip (bundle key `setting.beta.participate.tooltip`) warns explicitly that prereleases may be unstable.

- **Trade-off**: Live-save (no OK/Cancel) means accidental toggles are immediately persisted. Acceptable given the low risk of this particular setting.

## Migration Plan

No data migration needed. The `betaParticipate` key does not exist yet in any user's `Core.settings`, so its absence is treated as the default (`false`). No rollback steps required.
