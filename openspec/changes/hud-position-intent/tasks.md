## 1. Hud Position Semantics

- [x] 1.1 Split `Hud` positioning into desired-position derivation and display clamping: `keepInScreen()` applies the bound `x`/`y` signal values (falling back to the current root coordinates when unbound) and then clamps the display; remove the write-back of clamped values into the signals
- [x] 1.2 Add a clamp-only helper used by the drag path, and order both `touchDragged` and `touchUp` as `moveBy` → clamp → signal write so re-applying desired cannot undo the gesture
- [x] 1.3 Remove the `keepInScreen(boolean updateSignals)` overload and update `HudRootTable.validate()` to call the single display-only clamp path
- [x] 1.4 Add the degenerate-viewport guard (`viewport width or height <= 100` units, or zero-size root) that leaves the root position untouched and writes no signals
- [x] 1.5 Confirm resize/layout paths add no heap allocation, no new listeners or timers, and no bundle lookups; keep `isEnabled`-style early returns in existing per-frame code unchanged

## 2. Orientation Discriminant Timing

- [x] 2.1 Update `Signals` to set `portrait` synchronously in its `ResizeEvent` listener (remove the `Core.app.post`) so the config key switches before HUD resize handlers run
- [x] 2.2 Call `Signals.init()` explicitly at the start of `Main.init()`, before `FeatureManager.register`, so the listener is registered before any `Hud` is constructed
- [x] 2.3 Confirm `Signals` still owns exactly one resize listener and that no new timer or listener ownership is introduced

## 3. Tests

- [x] 3.1 Rewrite `HudTest.resizeEventClampingWithSignals` to assert the displayed position clamps on resize while the bound signals keep their saved values
- [x] 3.2 Add a `HudTest` case for viewport shrink and restore: panel displays clamped while small, returns to the saved coordinates when the viewport grows back, signals unchanged
- [x] 3.3 Add a `HudTest` case for a degenerate viewport (`160x31` and `0x0`): root position and signals remain unchanged
- [x] 3.4 Add a `HudTest` case for content growth/shrink near an edge: display changes only, saved position is re-derived when the content shrinks
- [x] 3.5 Restore `TeamResourcePositionTest.testMinimizeWindowDoesNotResetPosition` from `e60d8a58` (simulated `160x31` minimize/restore and `0x0` collapse) and confirm it passes under the new semantics
- [x] 3.6 Add rotation-context coverage: switching the portrait/landscape discriminant around a resize leaves both stored keys unchanged and returns the display to the outgoing position when rotating back
- [x] 3.7 Add or adjust a `SignalsTest` case asserting `portrait` is already updated for a listener registered after `Signals` during the same `ResizeEvent`; if `MockApplication`'s immediate `post` masks the ordering, add a small deferring-post application in the test setup
- [x] 3.8 Confirm no test asserts the removed write-on-resize behavior anywhere else in `solim-core` or `mod` test sources

## 4. Specs And Verification

- [x] 4.1 Run `openspec validate hud-position-intent --strict` and resolve any delta or scenario format issues
- [x] 4.2 Confirm no user-visible text was added or changed (no bundle keys required)
- [x] 4.3 Run the affected Gradle test tasks (`:solim-core:test`, `:mod:test`) and fix regressions
- [x] 4.4 Confirm no Java 9+ runtime APIs, raw types, unnecessary casts, fully qualified class names, or new `@SuppressWarnings` were introduced
- [ ] 4.5 Have a mobile reporter verify rotation, desktop minimize/restore, and window resize on a build, specifically that the outgoing orientation's position survives rotation
- [ ] 4.6 Create a follow-up proposal for consolidating the per-view `ResizeEvent` / `Effect(isPortrait)` handlers into `Hud` after this change lands

