## Context

Every draggable HUD overlay binds its root position through `Values.position(Readable, Readable)`, which stores the sources as `Hud.boundXSignal` / `boundYSignal`. Feature positions are `ContextualConfigValue` instances keyed per orientation (and, for chat, per expanded/collapsed state). `ConfigValue` persists every signal change through `invalidate()`.

`Hud.keepInScreen(boolean)` currently clamps `root.x/y` and, when `updateSignals` is true, writes the clamped values back into the bound signals. `Hud` registers a `ResizeEvent` listener that calls it, every HUD view registers another one, and several views call it from `Effect(isPortrait)` or after scale/content changes. `HudRootTable.validate()` also calls `keepInScreen(false)` whenever the preferred size changes.

That write path makes a presentation clamp indistinguishable from user intent:

```
drag ──▶ signal ──▶ ConfigValue.invalidate ──▶ Core.settings
           ▲
           └── keepInScreen() writes clamped root position
```

On Android rotation the damage is deterministic. `Signals.isPortrait` is updated inside `Core.app.post`, while `Hud` resize listeners run synchronously during `Events.fire(ResizeEvent)`:

```
rotation → graphics/scene resized (incoming viewport)
         → ResizeEvent dispatch
             Signals: post(portrait := true)                      [queued]
             Hud + views: clamp outgoing coords into incoming viewport
                          write signal → saved under OUTGOING key  ✗
         → posted portrait.set(true)
             ContextualConfigValue saves the clamped value again under the outgoing key,
             switches key, loads incoming value or its default
```

Rotating back therefore yields a clamped/edge position. The same write path destroys positions on desktop minimize (iconic window around `160x31`), window resize, split screen, IME, and content growth. Commit `e60d8a58` fixed the minimize case with a tiny-viewport guard and a "write only when this call clamped" check; commit `b1543948` (ancestor of `HEAD`) reverted it and deleted the regression test.

Testing constraint discovered during analysis: the shared test harness uses `arc.mock.MockApplication`, whose `post(Runnable)` runs the runnable immediately (verified by decompiling `Mindustry-v159.2.jar`). Tests therefore cannot observe the production one-frame ordering without a deferring test app; this is why `SignalsTest` passes against the posted implementation and why no test caught the rotation bug.

## Goals / Non-Goals

**Goals:**

- Make the displayed HUD position a derived value: `display = clamp(desired, viewport)`.
- Keep persisted position equal to user intent; only drag and explicit reset write signals.
- Guarantee rotation cannot corrupt the outgoing orientation's saved position.
- Preserve on-screen behavior during viewport shrink and restore the saved position when the viewport grows back.
- Keep per-context keys, defaults, and existing settings migrations unchanged.
- Restore and extend regression coverage for minimize, rotation, and content growth.

**Non-Goals:**

- Consolidating the per-view `ResizeEvent`/`Effect(isPortrait)` handlers (follow-up change).
- Replacing pixel coordinates with normalized/fractional coordinates.
- Seeding a missing orientation/context from the current position.
- Changing default positions, adding new UI, or altering drag interaction.
- Instrumentation/logging for field diagnosis.

## Decisions

### 1. One signal represents user intent

- **Choice:** The bound `x`/`y` signals store the desired position. The root's `x`/`y` store the displayed position. `display = clamp(desired, viewport)` is re-derived on every viewport or content-size change. Only `Hud.makeDraggable` and feature `resetPosition()` mutate the signals.
- **Rationale:** A single source of truth for position, matching the project's one-source-of-truth rule. It removes the entire class of "presentation clamp becomes persisted state" bugs at once and makes rotation ordering irrelevant to data integrity.
- **Alternatives considered:**
  - *Dual signals (desired + displayed):* preserves the current "signal mirrors display" contract for external consumers, but duplicates position state and violates the one-source-of-truth rule without a concrete consumer that needs it.
  - *Ordering-only fix:* queue resize handling so the discriminant switches first. Stops outgoing-key corruption but still persists minimize/IME clamps.
  - *Restoring the reverted guard only:* fixes desktop minimize, provably leaves rotation corruption in place.

### 2. `Hud` separates desired-position application from clamping

- **Choice:** `Hud` exposes one public `keepInScreen()` that derives the desired position from the bound signals (falling back to the current root coordinates when unbound) and then clamps the display. The drag path uses a clamp-only helper: `moveBy` → clamp → write signals to the clamped display. `keepInScreen(boolean)` is removed; its only caller is the internal `HudRootTable.validate()`.
- **Rationale:** Re-applying desired is required for the restore path (viewport growth may not change the signal value, so no reactive emission occurs). The drag path must not re-apply desired before writing, or it would undo the frame's movement.
- **Alternatives considered:**
  - *Keep `keepInScreen(boolean)` and pass `false` from drag:* keeps a meaningless flag in public API and invites misuse.
  - *Re-apply desired only when a clamp was needed:* fails to restore after the viewport grows, which is the desired UX.

### 3. Degenerate viewports skip clamping

- **Choice:** If the viewport width or height is at or below `100` units, or the root has no measurable size, leave the root position unchanged.
- **Rationale:** Restores the guard from `e60d8a58`; prevents a visual snap to the corner during minimize/iconify and IME. With decision 1 the value is display-only, so skipping is safe and self-corrects when the viewport returns.
- **Alternatives considered:**
  - *Always clamp:* simplest, but produces a visible corner jump on minimize even though it recovers.
  - *Clamp after the viewport settles:* smooths Android multi-resize bursts, but adds deferred scheduling and lifecycle complexity for no data-integrity benefit.

### 4. Orientation discriminant updates synchronously

- **Choice:** `Signals` sets `portrait` directly in its `ResizeEvent` listener (remove the `Core.app.post`), and `Main.init()` calls `Signals.init()` explicitly before feature registration.
- **Rationale:** The config key then switches before any HUD resize handler runs, so the incoming context is applied in the same resize pass and no stale-key window remains. Single-intent signaling already prevents corruption; this removes the one-frame visual flash and the sub-frame window where a drag would write to the wrong context.
- **Alternatives considered:**
  - *Keep the post, defer HUD clamping to the post queue:* works via FIFO ordering but keeps ordering implicit and adds a second scheduled path.
  - *Leave ordering as is:* acceptable for data integrity, but leaves a visible flash on rotation.
- **Ordering guarantee:** features are constructed in `Main.init()`, and `QuickAccessFeature`'s constructor reads `Signals.isPortrait()`, so the `Signals` listener is registered before any `Hud`. The explicit `Signals.init()` makes that a contract instead of an accident.

### 5. Per-context keys and defaults stay as they are

- **Choice:** Keep `portrait`/`landscape` keys, chat's `expanded`/`collapsed` keys, their `defaultFactory` defaults, and all legacy key fallbacks. Do not seed a never-saved context from the current position.
- **Rationale:** Per-context placement is existing UX; the collapsed chat badge and expanded window are intentionally anchored differently, so a generic seeding rule would produce surprises. A missing context keeps its documented default and looks like a deliberate reset rather than data loss.
- **Alternatives considered:**
  - *Seed the target orientation from the current display:* reduces first-rotation surprise, but introduces a deliberate non-user write, the very pattern this change removes.
  - *Single shared key:* simpler config, but forces one position to serve both orientations and would itself look like a reset on every rotation.

### 6. Drag keeps its per-frame write cadence

- **Choice:** Signals are written on each drag frame and again on release, as today.
- **Rationale:** Signal consumers (quick-access popups, feature anchors) follow the panel live; only user gestures write, so volume is unchanged from today.
- **Alternatives considered:**
  - *Write on release only:* fewer settings writes, but signal-driven consumers lag and an interrupted drag saves nothing.
  - *Throttle writes:* middle ground, adds timing state and a second path to test.

### 7. Verification uses contract tests plus a reporter build

- **Choice:** Cover the behavior with unit tests that assert the contract rather than the old write-on-resize behavior, and have a mobile reporter verify rotation after the build. No temporary instrumentation.
- **Test environment constraint:** `MockApplication.post` executes immediately, so a rotation-ordering test needs either a custom deferring `Application` in the test setup or must assert an ordering-independent property. The corruption contract ("clamping never mutates signals") is testable with the existing harness; the ordering guarantee is not.
- **Planned coverage:**
  - Restore `testMinimizeWindowDoesNotResetPosition` (simulated `160x31` minimize/restore) from `e60d8a58`.
  - Rewrite `HudTest.resizeEventClampingWithSignals` to assert the display clamps while signals keep their saved values.
  - Add shrink/restore coverage: clamped while small, restored coordinates after the viewport grows.
  - Add rotation-context coverage at the config level: switching the discriminant back and forth leaves both stored keys unchanged while the HUD is clamped.
  - Add content-growth coverage: expanding a panel near an edge changes the display only, and shrinking restores the saved position.

## Risks / Trade-offs

- **Stale off-screen positions never self-heal** → A position saved on a larger device stays saved and is clamped visually on smaller viewports. Mitigation: users can drag once or use the existing reset-position action; documented as intentional in the proposal.
- **Synchronous discriminant work runs inside `ResizeEvent` dispatch** → key swap and position bindings execute during event dispatch. Mitigation: the write path only sets element coordinates (no layout, no allocation); if a reentrancy issue appears, fall back to deferring HUD clamping to the post queue (alternative in decision 4).
- **Listener-order dependence for the ordering guarantee** → explicit `Signals.init()` in `Main.init()` plus a test that observes the discriminant from a listener registered after `Signals` with a deferring-post app, if the harness supports it.
- **Removing `keepInScreen(boolean)`** → source-level API change in `solim-core`; the only caller is `HudRootTable.validate()`. Mitigation: full module compile and the existing Hud tests.
- **Intentional test expectation flip** → `resizeEventClampingWithSignals` asserted the old contract. Mitigation: the rewritten test names the new contract and the spec delta documents it.
- **Unbound static-position HUDs** (translation pill, join-approval banner) have no desired signal, so they do not restore on viewport growth. Acceptable: they are ephemeral and recreated per show; their positions are not persisted.
- **Drag interrupted by a viewport change** → the last drag frame already wrote the clamped display, so intent matches what the user saw. No extra handling.

## Migration Plan

- No settings migration: key names, per-context suffixes, defaults, and legacy fallbacks are unchanged.
- Rollout: land the `Hud` semantics, `Signals` timing, `Main.init()` call, and tests together; ask a mobile reporter to verify rotation and minimize before release.
- Rollback: revert the change; it touches no persisted data format, so reverting restores the previous write-on-resize behavior without cleanup.

## Open Questions

- Symptom details are still unconfirmed. If reporters describe the *destination* orientation centering, that is the per-context default behavior (decision 5), not corruption, and would only be revisited as a separate UX change.
- Whether the rotation-ordering regression test is worth a deferring-post test app, or whether the ordering-independent contract test is sufficient. Planned default: add the contract test now, add the deferring app only if it stays small.
- Exact placement of the degenerate-viewport threshold constant (`Hud` private constant vs shared utility) is an implementation detail; the value `100` units comes from the reverted fix.
