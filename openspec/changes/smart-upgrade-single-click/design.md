## Context

`SmartUpgradeFeature` currently opens its upgrade menu on a double-tap pair window: two `TapEvent`s on the same `tile.build` within `tapIntervalConfig` (150–600ms, default 300, cached in `cachedTapInterval` with `lastTapBuild`/`lastTapTime` pair state). A `U` hover keybind (`triggerHoveredUpgrade`) offers a desktop alternative, and the menu stacks above vanilla selection in `hudGroup`. The previous migration (hold-duration → tap-interval) set the precedent of removing the old timing setting without migrating persisted values. A TODO on the feature asks for single-click accessibility with disable-after-click semantics.

QuickAccess integration today uses `Feature` defaults: tap toggles enabled (`onQuickAccessClick` → `setEnabled(!isEnabled())`), long-press opens the settings dialog, and `QuickAccessHudView` highlights icons from `f.enabled()` (white vs darkGray) and routes taps to `onQuickAccessClick(anchor)`. `MapPositionPicker.pick()` is the established one-shot tap pattern (arm → next tap fires → auto-cancel with toast instruction).

## Goals / Non-Goals

**Goals:**

- Replace the timing-based double-tap with an explicit armed single-click that is motor-accessible and discoverable.
- Prevent menu spam on routine building inspection without reintroducing a timing window.
- Keep the `U` keybind, menu layout/stacking/dismissal, chain upgrade, candidate computation, and remaining settings behaving as before.
- Give users a one-shot vs persistent choice with a safe default.

**Non-Goals:**

- Injecting an upgrade button into vanilla's selection panel (rejected alternative, not spiked).
- Changing upgrade candidates, chain traversal, max-updates / same-type / traverse-bridges logic, or toasts.
- Changing QuickAccess semantics for any other feature.
- Migrating persisted `tap-interval` values.

## Decisions

**Decision 1: Armed single-click replaces double-tap (no timing window).**

- *Choice*: One valid world tap while armed opens the menu; the `lastTapBuild`/`lastTapTime` pair state and `cachedTapInterval` go away.
- *Rationale*: Removes the motor timing barrier entirely while the armed gate solves the every-tap spam that naive single-click would cause on conveyors/walls/drills.
- *Alternatives considered*: selected-then-click without timer (still two taps, selection edge cases); naive always-on single-click (maximum spam); vanilla panel injection (best discoverability but unknown injection point, needs a spike).

**Decision 2: Arm/disarm via QuickAccess tap override.**

- *Choice*: Override `onQuickAccessClick(anchor)` to toggle an `armed` flag instead of toggling enabled; long-press keeps opening settings. Taps require enabled AND armed; arming while disabled auto-enables so the gate never dead-ends.
- *Rationale*: Visible HUD entry point, mirrors the picker arming pattern, keeps enable semantics on the settings card.
- *Alternatives considered*: enable-equals-armed (conflates two states); settings-only switch (no in-game entry point for mobile).

**Decision 3: `trigger-mode` setting replaces `tap-interval` (default one-shot).**

- *Choice*: Persisted `ConfigValue` with `one-shot` (disarm after one menu opens) vs `persistent` (stay armed until manually disarmed). `tapIntervalConfig`, its slider, and bundle keys are removed with no migration.
- *Rationale*: Honors the requested one-shot/toggle option while keeping both modes spam-gated behind the armed flag; one-shot default is the safest first impression.
- *Alternatives considered*: one-shot only (smaller scope, but a second change would follow when persistent is requested); always-on toggle mode (reintroduces spam).

**Decision 4: `U` keybind bypasses armed state.**

- *Choice*: `triggerHoveredUpgrade` keeps its enabled-only gate and ignores `armed`.
- *Rationale*: Preserves the desktop flow with zero extra steps; mobile and desktop triggers stay documented as separate paths.
- *Alternatives considered*: `U` requires armed (single consistent rule, but punishes keyboard users).

**Decision 5: Toast plus highlight while armed.**

- *Choice*: Toast instruction on arm (god-mode picker precedent) plus QuickAccess icon highlight while armed. `QuickAccessHudView` consults an opt-in armed signal (default: current enabled-based highlight) so no other feature's rendering changes.
- *Rationale*: Persistent highlight prevents forgotten-armed spam in persistent mode; toast teaches first-time users.
- *Alternatives considered*: toast-only (forgetting risk); highlight-only (first-use confusion).

**Decision 6: Cancel paths mirror menu dismissal.**

- *Choice*: Disarm on outside tap, Escape/Back, QuickAccess re-tap, state change to menu, and `onDisable()` (which also closes the menu and clears tap state, as today).
- *Rationale*: Reuses the existing dismissal mental model; no new gestures to learn.

## Risks / Trade-offs

- [Risk] QuickAccess tap no longer toggles enabled → users who used it as an on/off switch lose that shortcut → Mitigation: arming auto-enables, enable/disable stays on the settings card, help text documents the new meaning.
- [Risk] Persistent mode stays armed if the user forgets → repeated menus → Mitigation: one-shot default, persistent highlight, one-tap disarm.
- [Risk] Highlight needs a generic `QuickAccessHudView` change → regression surface for all QuickAccess icons → Mitigation: opt-in armed signal with defaulted behavior; enabled-based rendering unchanged for other features.
- [Risk] Double-tap muscle memory breaks → Mitigation: help text rewritten for armed single-click plus `U`; `U` path untouched.
- [Risk] The triggering single tap also reaches vanilla selection → menu stacks above selection as today (accepted, unchanged behavior) → Mitigation: keep stacking/dismissal logic verbatim; no vanilla suppression attempted.

## Migration Plan

1. Land feature change with `tap-interval` removal (no data migration) and new `trigger-mode` defaulting to one-shot.
2. Update help text and bundle entries (EN + VI) in the same change.
3. Update `SmartUpgradeFeatureTest` defaults/reset/bundle assertions in the same change.
4. Archive the delta spec over `smart-upgrade-trigger`; no rollback data needed since removed keys carry no migrated values.
