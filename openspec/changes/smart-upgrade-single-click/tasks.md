## 1. Trigger state and config

- [ ] 1.1 Replace `tapIntervalConfig` with a persisted `trigger-mode` value (`one-shot` default, `persistent` alternative) including signal cache and `resetToDefaults`, with no migration of old interval values
- [ ] 1.2 Add armed state with arm/disarm helpers, disarming on `onDisable()` and menu-state change

## 2. Tap and QuickAccess behavior

- [ ] 2.1 Rewrite the `TapEvent` handler to drop the double-tap pair state and open the menu on one valid tap gated by enabled AND armed plus existing guards, disarming after trigger in one-shot mode
- [ ] 2.2 Override `onQuickAccessClick` to toggle armed state (auto-enabling when disabled) while keeping long-press settings behavior
- [ ] 2.3 Add armed feedback (toast on arm plus QuickAccess highlight via an opt-in signal) and wire all cancel paths (outside tap, Escape/Back, re-tap, state change)

## 3. Settings and localization

- [ ] 3.1 Update `SmartUpgradeSettingsView` to remove the interval slider and add the trigger-mode control with reactive binding
- [ ] 3.2 Update `bundle.properties` and `bundle_vi.properties` (drop/replace interval keys, add mode/armed/toast keys with translator comments, rewrite help text for armed single-click plus `U`)

## 4. Tests and verification

- [ ] 4.1 Update `SmartUpgradeFeatureTest` for new defaults, reset behavior, mode transitions, and bundle assertions
- [ ] 4.2 Run the SmartUpgrade tests and project build, verifying Java 8 runtime compatibility, no hardcoded user-visible strings, and no fully qualified class names
