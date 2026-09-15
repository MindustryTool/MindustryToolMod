## 1. Display-mode settings

- [ ] 1.1 Declare a persisted per-feature display-mode option (HUD/popup values, default HUD) for TimeControl in its `ConfigGroup`, surfaced in `TimeControlSettingsView`.
- [ ] 1.2 Declare the same display-mode option for GodMode in its `ConfigGroup`, surfaced in its settings entry.
- [ ] 1.3 Add bundle keys with translator comments for all new display-mode labels; verify no hardcoded user-visible text.

## 2. TimeControl popup surface

- [ ] 2.1 Build the stacked vertical mini-panel popup content bound to the existing speed, preset, boost, and mode signals with automatic ownership and no view-local state.
- [ ] 2.2 Anchor the popup above or below the whole QuickAccess bar based on the bar's screen position at open time; apply the feature UI scale mappings to popup content.
- [ ] 2.3 Render popup controls visibly disabled for net clients.
- [ ] 2.4 Suppress the standalone HUD entirely in popup mode while QuickAccess is on; restore it silently when QuickAccess is off with the setting left at popup.

## 3. QuickAccess tap branching

- [ ] 3.1 Mark TimeControl quick-access capable so its button renders in the QuickAccess grid under existing visibility rules.
- [ ] 3.2 Branch QuickAccess tap handling: popup-mode features open their popup without toggling; HUD-mode features toggle exactly as today; long-press opens settings in both modes.
- [ ] 3.3 Ensure popup activation never propagates to the toggle path.

## 4. GodMode shell

- [ ] 4.1 De-dev GodMode into a real placeholder feature (enable signal, no cheat logic, no game-state effects) with QuickAccess membership.
- [ ] 4.2 Provide the GodMode popup shell under the same placement, suppression, fallback, and localization rules as the TimeControl popup.
- [ ] 4.3 Update stub-registry and count-sensitive expectations for GodMode leaving the development set.

## 5. Verification

- [ ] 5.1 Verify default-mode behavior is pixel- and flow-identical to today (HUD, toggle, long-press, client hiding).
- [ ] 5.2 Verify popup-mode matrix: tap opens without toggling, enable only via settings/card, fallback silent with setting retained, clients see disabled controls, scale follows setting.
- [ ] 5.3 Verify Java 8 runtime compatibility, `arc.util.Nullable` usage, no hardcoded display text, and no direct HTTP clients.
