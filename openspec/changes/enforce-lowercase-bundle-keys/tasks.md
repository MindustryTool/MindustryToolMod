## 1. Key Format Rule

- [x] 1.1 Add `Pattern.compile("^[a-z0-9]+([.-][a-z0-9]+)*$")` to the existing `checkBundles` task in `gradle/check-bundles.gradle.kts`, with a comment stating that the match MUST remain case-sensitive and must not be given a `CASE_INSENSITIVE` flag.
- [x] 1.2 Validate every key of the parsed base bundle against the pattern, appending an `[KEY-FORMAT]` error listing the key and its line number.
- [x] 1.3 Do not consult `untranslated-allowlist.txt` for key format violations.
- [x] 1.4 Run `./gradlew checkBundles` and confirm it reports exactly 39 violations, as an authoritative worklist for sections 2 to 4.

## 2. Java Keybind Identifier Renames

- [x] 2.1 Rename `KEYBIND_CATEGORY` in `mod/src/mindustrytool/features/Feature.java` from `MindustryTool` to `mindustrytool`.
- [x] 2.2 Lowercase the 37 keybind name literals passed to `bindToggle` / `bindAction` / `bindDialog` across the feature classes. Verify all 38 registered names remain distinct after lowercasing before committing.
- [x] 2.3 `armyControlSettings`, `autoPlay`, `blockStatsSettings`, `blockStatsToggle`, `calloutMenu`, `calloutSettings`, `chatOverlay`, `freeCameraSnap`, `freeCameraToggle`, `griefAlertJump`, `griefAlertSettings`, `healthBar`, `healthBarSettings`, `logicVisualizer`, `logicVisualizerSettings`, `mapBrowser`, `mapRules`, `mapRulesSettings`, `patchBrowser`, `pathfindingSettings`, `pathfindingToggle`, `rangeDisplay`, `rangeDisplaySettings`, `schematicBrowser`, `screenshotCapture`, `screenshotSettings`, `smartUpgradeSettings`, `smartUpgradeToggle`, `smartUpgradeTrigger`, `timecontrolPause`, `timecontrolSpeedDown`, `timecontrolSpeedUp`, `toggleRendering`, `toggleRenderingSettings`, `visualizerSettings`, `wavePreview`, `wavePreviewSettings` (all to their lowercase form). `visualizer` is already compliant and stays as-is.

## 3. Base Bundle Key Renames

- [x] 3.1 Rename the 28 `keybind.<name>.name` keys to match the lowercased identifiers from section 2.
- [x] 3.2 Replace `section.MindustryTool.name` with `category.mindustrytool.name`, keeping the value `MindustryTool`.
- [x] 3.3 Rename the 10 map-rules keys from underscores to dashes: `save_prompt`, `save_section.desc`, `save_error`, `save_name`, `save_overwrite`, `save_folder`, `save_confirm`, `save_success`, `save_location`, `save_map` (prefixed `feature.map-rules.`).
- [x] 3.4 Preserve each key's existing descriptive comment block through every rename.

## 4. New Base Bundle Keys

- [x] 4.1 Add the 9 missing `keybind.<name>.name` entries with English display text, each with a descriptive comment: `keybind.logicvisualizer.name`, `keybind.logicvisualizersettings.name`, `keybind.maprules.name`, `keybind.maprulessettings.name`, `keybind.patchbrowser.name`, `keybind.pathfindingtoggle.name`, `keybind.pathfindingsettings.name`, `keybind.togglerendering.name`, `keybind.togglerenderingsettings.name`.
- [x] 4.2 Author English text for the 8 keys that currently render as a raw key string, reading each call site for UI context. **Deviation:** the 4 `chat.link.dialog.*` keys were not added. An existing `feature.chat.link.dialog.*` family (title/prompt/open/cancel) already exists in the base bundle and is translated in all 11 locales, but was referenced by no code. Per AGENTS.md reuse guidance, `ChatMessageListView` was repointed at the existing keys and the 4 duplicates were discarded, so 4 authored keys remain: `feature.god-mode.core.position` (takes `{0}` = x, `{1}` = y), `feature.god-mode.core.select-position`, `feature.translation.error.timeout`, `feature.translation.error.invalid-api-key`.
- [x] 4.3 Promote the 6 existing `get(key, default)` English strings into real bundle entries, then remove the now-redundant inline default: `browser.detail.type`, `chat.clipboard-empty`, `editor.export`, `feature.player-connect.join-anyway`, `schematic.disabled`, `schematic.edit`.
- [x] 4.4 Give every new key a descriptive comment directly above it, documenting meaning, display location, and any placeholder arguments.

## 5. Locale Propagation

- [x] 5.1 Apply all 39 renames from section 3 to all 11 `bundle_*.properties` locale files, preserving per-file value text and comments.
- [x] 5.2 Add translations for all 23 new keys from section 4 to all 11 locale files.
- [x] 5.3 Verify each new key's placeholder set matches the base entry, since the existing placeholder consistency check will otherwise fail.

## 6. Supporting References

- [x] 6.1 Update `assets/bundles/untranslated-allowlist.txt`: rename `section.MindustryTool.name` to `category.mindustrytool.name`.
- [x] 6.2 Update the 6 literal keybind references in `openspec/specs/tactical-keybinds/spec.md` to their lowercase names.

## 7. Verification

- [x] 7.1 Run `./gradlew checkBundles` and confirm zero errors.
- [x] 7.2 Run `./gradlew check` to confirm the bundle task still passes alongside the full verification suite.
- [x] 7.3 Confirm the Reindent/Controls dialog lists every feature keybind under a "MindustryTool" header with no fallback-derived labels. Verified by lookup completeness rather than an in-game run (no Mindustry MCP available in this environment): `KEYBIND_CATEGORY` compiles to `mindustrytool`, so `KeybindDialog` resolves `category.mindustrytool.name` (present in base and all 11 locales, value `MindustryTool`), and all 38 registered keybinds resolve `keybind.<name>.name` with 0 falling back to `Strings.capitalize`.