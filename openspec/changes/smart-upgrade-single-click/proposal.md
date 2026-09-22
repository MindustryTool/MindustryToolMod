## Why

Double-tap timing (150–600ms) is inaccessible for motor-impaired and mobile players, undiscoverable in-game, and conflicts with vanilla building selection. Mobile has no alternative trigger besides double-tap (desktop has the `U` keybind).

## What Changes

- **BREAKING**: Replace the double-tap gesture with an armed single-click trigger for the Smart Upgrade menu.
- **BREAKING**: Remove the `tap-interval` setting (no migration of persisted values, matching the earlier `hold-duration` removal precedent).
- Add a `trigger-mode` setting (`one-shot` default, `persistent` alternative): one-shot disarms after one menu opens; persistent stays armed across triggers until manually disarmed.
- QuickAccess tap arms/disarms single-click (long-press still opens settings); single world tap fires only when the feature is both enabled AND armed.
- Armed feedback: toast instruction on arm plus highlighted QuickAccess icon while armed; arming cancels on outside tap, Escape/Back, QuickAccess re-tap, menu-state change, and `onDisable()`.
- The `U` hover keybind keeps working on enabled alone and ignores armed state.
- Menu stacking above vanilla selection, outside-tap/Escape dismissal, candidate computation, chain upgrade, max-updates / same-type / traverse-bridges settings, and toasts behave as before.
- New user-visible strings resolve from `bundle.properties` with translator comments plus matching VI entries.

## Capabilities

### New Capabilities

- None — this change modifies existing trigger behavior rather than introducing a new capability.

### Modified Capabilities

- `smart-upgrade-trigger`: trigger changes from double-tap pair window to armed single-click; `tap-interval` requirement removed and replaced by `trigger-mode` plus armed-state requirements.

## Impact

- `mod/src/mindustrytool/features/smartupgrade/SmartUpgradeFeature.java` (TapEvent path, armed state, QuickAccess hooks, config values).
- `mod/src/mindustrytool/features/smartupgrade/SmartUpgradeSettingsView.java` (remove interval slider, add trigger-mode control).
- `assets/bundles/bundle.properties` + `bundle_vi.properties` (remove interval keys or repurpose, add mode/armed strings).
- `mod/src/test/java/mindustrytool/features/smartupgrade/SmartUpgradeFeatureTest.java` (defaults, reset, bundle assertions).
- Spec `openspec/specs/smart-upgrade-trigger/spec.md` (requirements rewritten via delta spec).
