## Context

TimeControl renders a standalone draggable Solim `hud()` overlay with per-orientation persisted position, scale (0.5–1.5), and presets/slider modes; its spec states it is not a QuickAccess entry. GodMode is a development placeholder with metadata only and no signals, views, or logic. QuickAccess item buttons toggle on tap and open settings on 300ms long-press, skipping development and non-quick-access features. Solim `Popup` is a transient scene-rooted menu (`show(data, stageX, stageY)`, content rebuilt per show and disposed on hide) that auto-dismisses on outside touch, escape, and resize. All decisions below were made by the user during exploration; unresolved points are listed under Open Questions, not decided here.

## Goals / Non-Goals

**Goals:**

- Opt-in popup control surface for TimeControl and GodMode when QuickAccess is on, defaulting to current HUD behavior.
- QuickAccess membership for both features; GodMode de-devved as a logic-free shell.
- Silent HUD fallback when QuickAccess is off.

**Non-Goals:**

- GodMode cheat logic (explicitly deferred to its rewrite).
- Drag-and-drop or additional popup triggers beyond QuickAccess tap.
- Changes to default-mode HUD behavior, sizing spec, or speed/preset semantics.
- QuickAccess HUD ordering changes.

## Decisions

All decisions in this section are user decisions gathered before proposal writing. Alternatives listed are the ones considered and rejected by the user.

### 1. Tap toggles popup visibility instead of toggling feature enablement (user-decided)

In popup mode with QuickAccess on, tapping the feature's QuickAccess button toggles popup visibility: opens the popup if closed, and hides it if currently showing (or if clicked again during active popup display). Tapping never toggles feature enablement. Rejected: tap toggles feature enablement; clicking again keeps popup open or re-opens it.

### 2. Enable/disable lives only in settings and FeatureCard (user-decided)

The popup contains no enable switch and opening it never auto-enables. Rejected: enable switch inside the popup; opening auto-enables.

### 3. Shared horizontal row layout in static HudView function with black rounded container (user-decided)

Popup content reuses the HUD's horizontal row layout directly via a static function in each feature's `HudView` (`TimeControlHudView`, `GodModeHudView`) rather than duplicating UI logic in a vertical stack:
- **Mirror HUD row**: TimeControl and GodMode maintain horizontal row layouts mirroring the HUD control layout.
- **Icon-only everywhere**: GodMode HUD is icon-only with tooltips; popup drops text labels and uses the exact same icon-only buttons with tooltips everywhere.
- **Static in HudView**: The shared layout function lives as a static method in each feature's `HudView` (e.g., `TimeControlHudView.buildControls(...)`, `GodModeHudView.buildControls(...)`).
- **No title header**: The popup has no title header at the top.
- **Black background & styling**: The popup wraps the shared layout in a container with a black background (`Styles.black6` or black rounded background), `rounded(unit(2))`, `padding(unit(1))`, and `gap(unit(1))`.
- **Toggle-hide on click**: When the popup is currently showing, clicking the feature's QuickAccess button again hides the popup.
- **`canEdit` parameter**: The shared layout function accepts a `Readable<Boolean> canEdit` parameter to govern interactivity. The HUD passes always-editable (or `null`/always true), while the popup passes a reactive signal disabling controls when the feature is disabled or when the player is a net client.
- **Exclude drag handle**: Draggable move handles bound to position signals are strictly excluded from the shared layout function and remain in `HudView` only.
Rejected: vertical stacked popup duplicating layout logic; text labels in GodMode popup; title header in popup; shared layout including drag handle; separate direction flag or adaptive layout.

### 4. Anchor above or below the whole QuickAccess bar by bar position (user-decided)

Placement is relative to the QuickAccess bar as a whole (above it or below it depending on where the bar sits), not anchored at the individual tapped button. Rejected: per-button anchoring with overflow flip.

### 5. Per-feature string display-mode setting defaulting to HUD (user-decided)

Each feature owns a persisted `ConfigValue<String>` display mode with HUD/popup values defaulting to HUD. Rejected: boolean use-popup flag.

### 6. Fully suppressed standalone HUD while popup-active (user-decided)

In popup mode with QuickAccess on there is no standalone HUD even when enabled. Rejected: HUD plus popup simultaneously.

### 7. Silent fallback to standalone HUD (user-decided)

In popup mode with QuickAccess off, the standalone HUD returns with no message and the setting stays at popup. Rejected: fallback plus toast notice.

### 8. Long-press unchanged (user-decided)

Long-press still opens the feature settings dialog. Rejected: long-press takes over toggling.

### 9. Clients see disabled controls (user-decided)

Net clients opening the TimeControl popup see the normal controls visibly disabled. Rejected: unavailable-note replacement; refusing to open.

### 10. Scale applies to popup content (user-decided)

The feature UI scale setting resizes popup content as it does HUD elements. Rejected: fixed popup size.

### 11. GodMode scope is plumbing plus shell, no logic (user-decided)

This change de-devs GodMode, gives it QuickAccess membership, a display-mode setting, and a popup shell with no cheat controls. Rejected: full cheat controls now; deferring GodMode entirely to later.

## Risks / Trade-offs

- [Risk] Tap-toggles is the grid-wide QuickAccess contract; two features breaking it may confuse → Mitigation: long-press and card paths unchanged; popup content should make its non-toggling nature evident (exact treatment undecided — see Open Questions).
- [Risk] Popup content is rebuilt on every show and disposed on hide while HUD state lives in feature signals → Mitigation: popup binds the same signals (speed, preset, mode); no view-local state.
- [Risk] GodMode de-devving shifts stub-registry counts and badge behavior owned by the `app-settings` spec → Mitigation: delta specs cover registry, count, and badge expectations.
- [Risk] Whole-bar anchoring must track a draggable bar across orientations → Mitigation: anchor computed at open time from the bar's current stage position.
- [Risk] Suppressed-HUD plus disabled-feature plus settings-only-enable could strand a new user (no visible path from gray icon to working popup) → Mitigation: undecided — see Open Questions.

## Open Questions

The following were not decided by the user and must be resolved before or during implementation (user explicitly reserved all decisions):

- Setting key names and `ConfigGroup` placement for each display-mode entry.
- Exact popup metrics (widths, gaps, touch targets) and bundle key names for new labels/tooltips.
- What the GodMode popup shell displays given no cheat logic exists (empty state content and wording).
- How a first-time user discovers enablement (settings/card path) from a gray QuickAccess icon in popup mode.
- Exact above/below rule articulation (which bar position maps to which side) and behavior on orientation change while open.
- Reactive wiring shape for suppressing the standalone HUD and branching tap handling in `QuickAccessHudView`.
