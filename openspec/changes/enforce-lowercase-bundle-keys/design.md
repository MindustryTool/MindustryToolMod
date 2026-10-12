## Context

`assets/bundles/bundle.properties` holds 1,274 keys, mirrored one-for-one across 11 `bundle_*.properties` locale files. `gradle/check-bundles.gradle.kts` already verifies duplicates, locale completeness, obsolete keys, placeholder consistency, and untranslated values. It has no rule about key format.

Feature keybinds are registered through `Feature.bindToggle` / `bindAction` / `bindDialog`, which pass a Java string literal into `arc.input.KeyBind.add(name, defaultValue, category)`. Mindustry's `KeybindDialog` then resolves the display label at runtime as:

```java
bundle.get("keybind." + keybind.name + ".name", Strings.capitalize(keybind.name))
bundle.get("category." + keybind.category + ".name", Strings.capitalize(keybind.category))
```

The bundle key is therefore *derived* from the Java identifier, not chosen independently. The two namespaces are coupled: renaming the literal changes the key the game looks up, and `bundle.get(key, default)` falls back silently rather than failing.

The current coupling has drifted. Of 38 registered keybind literals, 9 have no `keybind.<name>.name` entry and render through the `Strings.capitalize` fallback. The category key was authored with a `section.` prefix, but v160.7 reads `category.`, so `section.MindustryTool.name` is dead and the controls dialog header falls back to "Mindustrytool".

## Goals / Non-Goals

**Goals:**
- Fail the build on any base-bundle key that does not match `^[a-z0-9]+([.-][a-z0-9]+)*$`.
- Bring all 1,274 existing keys into conformance.
- Restore correct display labels for every registered keybind.
- Use the `category.` prefix Mindustry v160.7 actually reads.

**Non-Goals:**
- Verifying that Java references resolve to existing bundle keys. `checkBundles` remains a bundle-file linter with no Java source parsing.
- Enforcing any naming convention on *values*. Only keys are constrained.
- Adding a translation pipeline. Translations are produced by hand.

## Decisions

**Regex is `^[a-z0-9]+([.-][a-z0-9]+)*$`.** Dots and dashes separate and join segments; digits are legal so `feature.player-connect.step1-title` stays valid. Underscores are excluded, which is what forces the 10 `feature.map-rules.save_*` keys to dash form.

**Matching is case-sensitive and must stay that way.** `Pattern.matches` is case-sensitive by default and the check must not introduce a `CASE_INSENSITIVE` flag. A case-insensitive variant would pass all 39 existing violations silently while reporting zero errors — the rule would look correct and enforce nothing. This is noted at the regex itself because the failure mode is invisible: the build goes green with the rule fully disabled.

**Validate the base bundle only.** The existing locale-completeness and obsolete-key checks already guarantee every locale file carries exactly the base key set, so a format rule over the base is sufficient and avoids duplicate error reporting for one root cause.

**Extend the existing `checkBundles` task rather than adding a task or a `buildSrc` plugin.** The task already parses every bundle and accumulates into a shared `errors` list and markdown report. A sibling task or new module would split localization verification across two code paths for a single additional rule.

**No exemption mechanism.** Not a reuse of `untranslated-allowlist.txt` (which carries a separate anti-laziness guard and is scoped to values), and not a new exemption file. Every key is either conforming or the build fails.

**Java literals and bundle keys are renamed as one atomic pair.** They are a single coupled namespace; renaming one without the other reintroduces the `Strings.capitalize` fallback. The renames and the new rule must land together — no intermediate state is valid on its own.

**The rule lands in the same change as the renames.** Adding the check first would fail on all 39 pre-existing keys; renaming first leaves the inconsistency undetected. They are only meaningful together.

## Risks / Trade-offs

- **Case-insensitivity regression silently disables the rule** → All 39 violations pass with zero reported errors. Mitigated by a comment at the regex stating the dependency on default case sensitivity. Not covered by a test, since `checkBundles` behaviour is verified by running the task, not by the test suite.
- **Roughly 250 hand-authored translations are the critical path.** They cannot be scripted, parallelised, or verified by the build, and they gate completion of 23 new keys across 11 locales.
- **Rule and renames must not be cherry-picked apart.** Any partial application fails on its own rule or reintroduces fallback labels.
- **Large single diff** — approximately 468 key renames, 276 new bundle entries, and 38 Java literal changes. Review should separate the mechanical renames from the authored translations.