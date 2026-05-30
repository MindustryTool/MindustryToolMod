# Background Audit

## Findings

### Security

- **MEDIUM** `BackgroundFeature.java:78-79` — Uses `Reflect.set` to replace `MenuRenderer` instance on `Vars.ui.menufrag`; fragile and could break in future Mindustry versions

### Data Loss

- **LOW** `BackgroundFeature.java:89` — On `onDisable`, restores original renderer; but if feature was never enabled, `originalRenderer` is null and no restore happens (correct behavior)

### Anti-Patterns

- **MEDIUM** `BackgroundFeature.java:35-37` — Static setting keys defined in feature class (`SETTING_KEY`, `SETTING_OPACITY_KEY`) — coupled to settings structure
- **MEDIUM** `BackgroundSettingsDialog.java:20-21` — `name` field set manually after constructor; should use constructor parameter
- **MEDIUM** `BackgroundFeature.java:71` — `CustomMenuRenderer.render()` catches and swallows all exceptions silently — user won't know if background fails to render
- **LOW** `CustomMenuRenderer` extends `MenuRenderer` but calls `super()` — parent constructor may do unnecessary work
