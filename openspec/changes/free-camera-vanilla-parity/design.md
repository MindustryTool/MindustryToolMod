## Context

Vanilla Mindustry's "free camera" is the `detach-camera` setting (toggled by the `detach_camera` keybind, unbound by default). Ground truth from the v160.1 `DesktopInput` source the mod compiles against:

- `update()` pans the camera with WASD while detached, lerps it back to the player/spectating/core target otherwise, and clears `panning` on any WASD/mouseMove input.
- `updateMovement()` while detached drives the unit **toward the camera position** (camera leads, unit follows, stopping within 15f).
- Toggling detach OFF also sets `panning = false` and `spectating = null`.
- Vanilla `MobileInput` has no detach concept, so mobile is unaffected by this change.

`ModDesktopInput` currently only branches on the mod's own `isFreeCam()` flag: while ON it forces `detach-camera=true` around `super.update()` (reusing vanilla's pan path) and freezes the unit; while OFF it never consults the vanilla setting, so stock detach loses its unit-chase half. `snapToPlayer()` resets `panning` but never clears `spectating`, while vanilla's camera lerp prefers a non-null `spectating` target — a stale target can drag the camera away right after a snap.

Constraints: desktop-only change, no input-handler swapping (per `unified-input-handling` decoupling requirement), Java 8 runtime APIs, `arc.util.Nullable` for nullable values.

## Goals / Non-Goals

**Goals:**
- Mod OFF + vanilla detach ON → stock behavior: camera pans via vanilla path (already true) **and** unit chases the camera (restored).
- `snapToPlayer()` leaves no stale camera target: clears `spectating` alongside `panning`, mirroring vanilla's toggle-off.
- Zero behavior change for players who never enable vanilla detach, and none on mobile.

**Non-Goals:**
- Making the vanilla `detach_camera` key do something while the mod's free camera is ON (accepted silent no-op).
- Reducing the per-frame force/restore `settings.put()` chatter.
- Snapback hardening beyond the `spectating` clear (mid-gesture races stay a separate concern).
- New settings, keybinds, HUD, or user-visible text.

## Decisions

### Decision 1: Replicate the vanilla chase branch when mod is off and vanilla detach is on
- **Choice**: In `ModDesktopInput.updateMovement()`, when `!isFreeCam()` and `Core.settings.getBool("detach-camera", false)` is true, drive the unit toward `Core.camera.position` with the stock 15f arrival radius and velocity damping, bypassing keyboard/joystick/mouseMove branches for that frame.
- **Rationale**: Restores the exact stock feel players expect from the original feature; keeps the mod's freeze behavior strictly scoped to mod-ON.
- **Alternatives**: Delegating to `super.updateMovement()` for that case (rejected — would discard the mod's joystick blending and autoplay yielding for the whole frame); leaving the divergence (rejected by explicit user decision).

### Decision 2: Clear `spectating` in `snapToPlayer()`
- **Choice**: Set the input handler's `spectating` target to null next to the existing `panning = false` / `cancelPanDelay()` resets.
- **Rationale**: One line that mirrors vanilla's own detach-toggle-off (`panning = false; spectating = null`), closing the stale-target drift after snap.
- **Alternatives**: Clearing only on `onDisable` but not long-press snap (rejected — both paths share `snapToPlayer`, consistency wins).

### Decision 3: Guard ordering — mod flag first, vanilla setting second
- **Choice**: Check `isFreeCam()` before reading the `detach-camera` setting, so mod-ON behavior (frozen unit) is unchanged even if the user also flipped the vanilla setting.
- **Rationale**: Preserves the precedence the mod already relies on in `update()`'s force/restore; avoids a settings read on the hot path when the mod is active.

## Risks / Trade-offs

- [Risk] Replicated chase logic drifts from future vanilla tuning (15f radius, accel damping) → Mitigation: keep the branch small and commented with the vanilla source reference so updates are easy to spot.
- [Risk] `spectating` field access differs between `ModDesktopInput`/`ModMobileInput`/vanilla handlers → Mitigation: clear it only where the field exists on the concrete handler type with an instanceof guard, mirroring the existing `panning` reset pattern.
- [Risk] Autoplay's `followUnit` lerp could fight the restored chase for one frame → Mitigation: autoplay already yields its camera lerp while freeCam is on and owns its own unit control; no shared-state change here.
