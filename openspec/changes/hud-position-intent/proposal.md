## Why

HUD overlays (team resources, quick access, chat, god mode, time control, autoplay, army control, joystick, quick schematic grid) persist their position through `ContextualConfigValue` signals. `Hud.keepInScreen` clamps the displayed position against the viewport and then writes the clamped value back into those same signals, which `ConfigValue` immediately persists.

That write path treats a presentation adjustment as user intent. On Android rotation it is actively destructive: `Signals.isPortrait` is updated in a posted task, so the synchronous `ResizeEvent` handlers clamp the outgoing orientation's coordinates into the incoming viewport and save them under the outgoing orientation's config key. Rotating back therefore shows a clamped/edge position. The same mechanism resets desktop positions when the window is minimized (iconic window around `160x31`), resized, split-screened, or when the on-screen keyboard shrinks the viewport.

A previous fix, `e60d8a58`, guarded tiny viewports and only persisted when a clamp actually occurred. It was removed by `b1543948` ("Revert 'Fixed crash (#261)'") together with its regression test, and current `HEAD` has neither the guard nor any test coverage for minimize or rotation.

## What Changes

- **Display position becomes derived, not stored.** `Hud` displays `clamp(desired, viewport)`; viewport and content-size changes reposition the display only. `keepInScreen` no longer writes `xSignal`/`ySignal`.
- **User intent has one owner.** Only `Hud.makeDraggable` (per drag frame) and each feature's explicit `resetPosition()` mutate the position signals.
- **Drag uses a clamp-only path.** The drag sequence (`moveBy` → clamp → write signal) must not re-apply the desired position before the new value is written.
- **Degenerate viewports skip clamping.** When the viewport is below the historical `100`-unit threshold or the root has zero size, the display position is left untouched, so minimize/IME never snaps a panel to the corner.
- **Orientation discriminant updates synchronously** on `ResizeEvent`, so the config key switches before any HUD resize handler runs. `Signals.init()` is called explicitly from `Main.init()` to make the listener-before-Hud ordering a guarantee rather than an accident.
- **Per-context keys stay unchanged.** Portrait/landscape keys, chat's expanded/collapsed keys, their default factories, and the existing settings migration fallbacks are untouched. Missing contexts continue to use their own defaults.
- **Tests and spec.** Restore the minimize regression test, add rotation and content-grow/shrink tests, and rewrite the resize test that currently asserts clamped values are written into signals.
- **Non-goals.** Consolidating the per-view `ResizeEvent`/`Effect(isPortrait)` handlers (tracked as a follow-up), switching to normalized/fractional coordinates, seeding missing contexts from the current position, and any UI or default-position change.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `solim-widgets`: the Hud viewport boundary-clamping requirement becomes presentation-only. Viewport-driven clamping SHALL NOT mutate position signals; only user drag and explicit reset persist coordinates.

## Impact

- `solim-core`:
  - `src/solim/overlay/Hud.java`: separate desired-position application from viewport clamping, add the clamp-only drag path, drop the now-meaningless `keepInScreen(boolean)` overload, add the degenerate-viewport guard.
  - `src/solim/reactive/Signals.java`: update the portrait discriminant synchronously with `ResizeEvent`.
- `mod`:
  - `src/mindustrytool/Main.java`: explicit `Signals.init()` before feature registration.
  - HUD views keep their existing `keepInScreen()` calls; those calls become harmless (display-only) until the follow-up cleanup.
- Tests:
  - `mod/src/test/java/mindustrytool/features/teamresource/TeamResourcePositionTest.java`: restore the deleted minimize/restore regression test.
  - `solim-core/src/test/java/solim/overlay/HudTest.java`: rewrite `resizeEventClampingWithSignals` to assert display clamps while signals keep their saved values; add rotation ordering and content-grow/shrink coverage.
- Specs:
  - `openspec/specs/solim-widgets/spec.md`: modified clamping requirement via this change's delta.
- Runtime: no new dependencies, no Java 9+ runtime APIs, no per-frame allocation added; the change removes writes from resize paths.
