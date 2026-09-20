## Context

`FreeCameraFeature` (`mod/src/mindustrytool/features/freecamera/FreeCameraFeature.java`) currently signals its state only through the QuickAccess icon active state. There is no in-world status banner, so players who pan far from their unit cannot tell at a glance that the camera is detached. Exploration confirmed two separate concerns: (a) missing enabled indicator, (b) occasional snapback failure. This change covers (a) only.

Established patterns in the codebase:
- `JoystickFeature` mounts/unmounts a Solim `HudView` on enable/disable and adds it to `Vars.ui.hudGroup` via `Core.app.post`, gating visibility on `hudfrag.shown && state.isGame()`.
- `JoinApprovalHudView` is the closest visual precedent: a top-center banner via `hud().position(Units.screenWidth() / 2, Units.screenHeight() - 50)`, `background(Styles.black8)`, accent-colored content.
- `Hud` root defaults to `Touchable.childrenOnly` (background touches pass through); `TimeControlHudView`/`QuickSchematicGridHudView` follow the same overlay conventions with `ResizeEvent → keepInScreen()`.

Constraints: Solim-first declarative UI, automatic reactive bindings (never `signal.get()` in `build()`), `Pal.accent` for the accent color, all user-visible text via `bundle.properties`, Java 8 runtime APIs only, `mod` module must not reference `:solim-runtime`.

## Goals / Non-Goals

**Goals:**
- Persistent pill banner reading "Free camera enabled" while free camera is on during gameplay.
- Top-center anchor, dark translucent background, `Pal.accent` label, non-interactive (never blocks game input).
- Reactive show/hide tied to feature enabled state + HUD visibility + game state; correct across resize/rotation.
- Fully translatable label.

**Non-Goals:**
- Snapback reliability (`snapToPlayer`, input handlers, re-pan suppression, respawn re-snap) — separate follow-up.
- Bottom-middle placement variant, draggable/persisted position, opacity/scale settings.
- Toast/auto-hide behavior, click-to-snap action on the banner, sounds or animations.
- Changes to keybinds, settings, QuickAccess icon behavior, or camera logic.

## Decisions

### Decision 1: New `FreeCameraHudView` Solim component owned by the feature
- **Choice**: Add `mindustrytool.features.freecamera.FreeCameraHudView extends BaseComponent`, mounted in `onEnable()` and unmounted in `onDisable()`, mirroring `JoystickFeature.mountHud/unmountHud` (element name `free-camera-indicator-hud`, `Vars.ui.hudGroup.addChild` inside `Core.app.post`, `dispose()` on remove).
- **Rationale**: Follows the proven feature-HUD lifecycle; automatic Solim ownership/disposal; no new framework code.
- **Alternatives**: Reuse QuickAccess icon tooltip only (rejected — not visible while scouting); vanilla `hudfrag.showToast` (rejected — transient, misses the "persistent state" requirement decided in Q&A).

### Decision 2: Top-center anchor, fixed offset, no dragging
- **Choice**: `hud.position(Units.screenWidth() / 2f, Units.screenHeight() - 50f)` at build, plus `ResizeEvent` listener → `keepInScreen()` (same as `JoinApprovalHudView` + `JoystickHudView`). Not draggable, no persisted coordinates.
- **Rationale**: Q&A decision moved from middle-bottom to top-center to avoid joystick (bottom-left), draggable QuickAccess bar, and mobile thumbs. Fixed offset keeps scope minimal; resize handling prevents stranding on rotation.
- **Alternatives**: Bottom-center fixed (rejected per Q&A — occlusion risk); draggable + persisted `x/y` config like QuickAccess (rejected — overkill for a status pill, adds config surface).

### Decision 3: Reactive visibility, pass-through input
- **Choice**: `hud.visible(...)` bound to a computed combining `feature.enabled()` + `hudfrag.shown` + `state.isGame()` (evaluated per-frame via signals/events, no manual subscriptions). Root stays `Touchable.childrenOnly`; container holds only a `text` (no buttons), so the banner never consumes clicks/pans.
- **Rationale**: Matches `JoystickHudView` visibility gating; guarantees the pill disappears in menus/editor/chat-closed-HUD states without lifecycle hacks.
- **Alternatives**: Manual `Events.on` show/hide with `setVisible` calls (rejected — imperative, leaks listeners, fights Solim ownership).

### Decision 4: Styling — `Styles.black8` + `Pal.accent`, compact pill
- **Choice**: Container `rounded(unit(2))`, `background(Styles.black8)`, symmetric padding (`margin/padding ~ unit(1)/unit(2)` scale), single `text(bundleKey).color(Pal.accent).center()`.
- **Rationale**: Visually consistent with `JoinApprovalHudView` banner; `Pal.accent` is the Q&A-chosen native accent (vs `WebStyles.Colors.PRIMARY` purple used for browser UI). Compact pill minimizes occlusion.
- **Alternatives**: `WebStyles` purple chip (rejected per Q&A); bordered/large banner (rejected — heavier than a status pill needs).

### Decision 5: i18n key under `status.*`
- **Choice**: New key `status.free-camera.enabled` ("Free camera enabled") with translator comment (where shown, when), resolved via `Core.bundle.get` inside a reactive text binding.
- **Rationale**: Satisfies mandatory i18n rule; `status.*` category fits a mode indicator; reactive binding picks up language changes.
- **Alternatives**: Reusing `feature.free-camera.name` ("Free Camera") alone (rejected — "enabled" state needs explicit wording).

## Risks / Trade-offs

- [Risk] Overlap with the JoinApproval top-center banner when both show → Mitigation: offset Y below the approval banner zone (approval sits at `screenHeight - 50`); indicator uses a slightly lower anchor and compact height; both are short-lived/rarely concurrent.
- [Risk] `Units.screenWidth/Height` snapshot goes stale on resize → Mitigation: `ResizeEvent` listener + `Core.app.post(keepInScreen)` (same as existing HUDs); visibility binding re-evaluates on HUD/game-state signals.
- [Risk] `signal.get()` in `build()` severing reactivity (AGENTS.md violation) → Mitigation: pass `Readable` directly to `text()`/`visible()` and use `.map()` transforms; verify in review.
- [Risk] Banner perceived as clickable but does nothing → Mitigation: keep it visually a label (no button style, no hover), pass-through touchable; snap stays on QuickAccess long-press per existing spec.
