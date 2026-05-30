# Settings Audit

## Findings

### Performance

- **MEDIUM** `FeatureSettingDialog.java` — Renders feature cards for ALL registered features on every open; with 20+ features, this is fine, but if features grow, could slow down dialog open
- **MEDIUM** `FeatureCard.java` — Each card loads feature icon; may trigger `Utils.icons()` which caches but initial call reads from disk

### Anti-Patterns

- **MEDIUM** `FeatureSettingDialog.java` — Coupled to `FeatureManager` singleton directly; no dependency injection
- **MEDIUM** `IconBrowserDialog.java` — Searches all icons including mod-provided ones; icon filtering is client-side and could be slow with many icons

### Architectural Violations

- **MEDIUM** `FeatureSettingDialog.java` — Settings dialog is both the feature settings UI FOR features AND registered as a feature dialog itself; circular dependency through `FeatureManager`
- **LOW** `IconBrowserDialog.java` — Uses icon name filtering with substring matching; may match unrelated icons
