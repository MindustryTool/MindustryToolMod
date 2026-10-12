## Why

Localization bundle keys are inconsistent: 39 of the 1,274 keys in `assets/bundles/bundle.properties` violate a lowercase dot-separated convention — 28 keybind display keys in camelCase, 10 `feature.map-rules.save_*` keys using underscores, and one category key. The inconsistency is silent: nothing in the build detects it, so it accumulates as new features are added.

Chasing the inconsistency also surfaced that the keybind key namespace has drifted from the Java keybind identifiers it mirrors. Nine of the 38 registered keybinds have no `keybind.<name>.name` entry at all and fall back to `Strings.capitalize(name)` in Mindustry's Rebind Keys dialog (rendering as "Logicvisualizer", "Maprules", "Togglerendering", and similar). Separately, `section.MindustryTool.name` uses a prefix Mindustry v160 no longer reads — the game looks up `category.<name>.name` — so the key is dead and the controls dialog falls back to "Mindustrytool".

## What Changes

- **BREAKING**: Introduce a key format rule enforced by the `checkBundles` Gradle task. Every key in the base bundle must match `^[a-z0-9]+([.-][a-z0-9]+)*$`. Violations fail the build; there is no exemption list.
- **BREAKING**: Rename the 37 feature keybind identifiers registered via `bindToggle` / `bindAction` / `bindDialog` from camelCase to lowercase, in both Java call sites and the `keybind.<name>.name` bundle keys they resolve to.
- Rename `KEYBIND_CATEGORY` from `MindustryTool` to `mindustrytool` so the runtime-resolved category key also satisfies the format rule.
- Replace the dead `section.MindustryTool.name` key with `category.mindustrytool.name`, using the prefix Mindustry v160.7 actually reads (`KeybindDialog`).
- Rename the 10 `feature.map-rules.save_*` keys to use dashes instead of underscores.
- Add the 9 missing `keybind.<name>.name` display keys so every registered keybind has a localized label.
- Add 14 mod-owned bundle keys that Java already references but that are absent from the bundle — 8 of which render as a raw key string to the player today.
- Update `untranslated-allowlist.txt` and the keybind names cited in `tactical-keybinds` spec to match the renamed keys.

## Capabilities

### New Capabilities
- `bundle-key-conventions`: Enforced character-set and naming rules for localization bundle keys, verified by the `checkBundles` task.

### Modified Capabilities
- `tactical-keybinds`: The keybind identifiers cited in its requirements are renamed to lowercase.
- `feature-keybinds`: The `MindustryTool` category identifier referenced in its requirements is renamed to `mindustrytool`.

## Impact

- `gradle/check-bundles.gradle.kts`: new key format check added to the existing `checkBundles` task; no new Gradle task or module.
- `assets/bundles/`: 39 keys renamed and 23 keys added across `bundle.properties` and all 11 `bundle_*.properties` locale files.
- `assets/bundles/untranslated-allowlist.txt`: one entry renamed.
- `mod/src/mindustrytool/features/`: 37 keybind name literals and the `KEYBIND_CATEGORY` constant renamed across `Feature.java` and each registering feature.
- `openspec/specs/tactical-keybinds/spec.md`: 6 literal keybind references updated.
- No new dependencies, no new runtime code paths, no new Gradle modules.