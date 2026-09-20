## Context

`FreeCameraHudView` positions its pill with `hud.position(screenWidth / 2, …)`, which resolves to the Arc actor's **bottom-left corner** — so the pill's left edge lands on the centerline and the whole label sits half-a-pill-width too far right. Inner `.center()` calls only center content within the pill and cannot fix screen placement; compensating would need post-pack width measurement that re-breaks on every language change (pill width follows text length).

Vanilla/Arc ground truth (v160.1 sources the mod compiles against): `Font.draw(CharSequence, x, y, halign)` accepts `Align.center` for exact horizontal centering, returns a `GlyphLayout` for backdrop sizing, and `AutoplayFeature` already renders per-frame status (`Trigger.draw`, `Layer.overlayUI`) without any scene elements.

Constraints: desktop and mobile share the draw path (camera-relative anchoring works for both), Java 8 runtime APIs, no user-visible text outside `bundle.properties`, no scene-graph input to configure.

## Goals / Non-Goals

**Goals:**
- Label horizontally centered on the screen's top-center on every frame, at any window size, zoom level, or locale.
- Same look contract: `Pal.accent` text, dark translucent backdrop, visible only while enabled during gameplay with HUD shown.
- Delete the Solim overlay, its mount/unmount wiring, and the resize bookkeeping.

**Non-Goals:**
- Touching `JoinApprovalHudView` (same latent anchor pattern, different owner — follow-up).
- New bundle keys (reuse `status.free-camera.enabled`), settings, keybinds, or snap/camera changes.
- Constant pixel size under zoom beyond a simple camera-relative scale (see Decision 3).

## Decisions

### Decision 1: Frame-drawn label via `Trigger.draw`, owned by the feature
- **Choice**: Register `Events.run(Trigger.draw, this::draw)` once in the `FreeCameraFeature` constructor (same as `AutoplayFeature`), guard on `isEnabled() && hudfrag.shown && state.isGame()`, and delete `FreeCameraHudView` plus `mountHud`/`unmountHud`.
- **Rationale**: Removes the layout system from the problem entirely — centering becomes a single `halign` argument instead of measured compensation; no scene element means input pass-through is structural and there is nothing to mount, dispose, or keep in screen.
- **Alternatives**: Post-pack `setPosition(x, y, Align.center)` on the HUD root (rejected — re-runs on every text-width change and keeps the overlay lifecycle for zero benefit).

### Decision 2: Camera-relative top-center anchor
- **Choice**: Anchor at `(camera.position.x, camera.position.y + camera.height / 2 - margin)` computed per frame, so the label tracks the visible top-center under panning, zoom, and rotation without resize listeners.
- **Rationale**: The camera is already the source of truth for what's on screen; this replaces `Units.screenWidth/Height` snapshots plus `ResizeEvent` handling with one expression.
- **Alternatives**: Fixed screen-space projection (rejected — world draw pass has no screen-space origin without extra matrix work).

### Decision 3: Camera-relative text scale with measured backdrop
- **Choice**: Scale the font relative to `camera.height` so on-screen size stays roughly constant across zoom levels; size the dark backdrop rect from the returned `GlyphLayout` (width/height plus padding), drawn at `Layer.overlayUI` before the text, with `Draw.reset()` after.
- **Rationale**: World-space text would shrink/grow with zoom; relative scaling keeps the status readable like a HUD element. Measured backdrop preserves the pill look without a `Table`.
- **Alternatives**: Fixed world-size text (rejected — unreadable at far zoom); no backdrop (rejected — breaks the existing look contract).

## Risks / Trade-offs

- [Risk] Drawn text ignores UI scaling/hiding conventions outside the guard (e.g. minimap fullscreen, console open) → Mitigation: mirror the previous visibility guard exactly (`hudfrag.shown`, `isGame()`); minimap/console edge cases match the old pill's behavior since it used the same gate.
- [Risk] Font availability before `Fonts.loadFonts()` in headless/tests → Mitigation: draw method early-returns on null font/camera/state like `AutoplayFeature.draw()` does; no headless UI test required for `mod/` per project rules.
- [Risk] Future vanilla `Font.draw` signature drift → Mitigation: single call site with a comment citing the v160.1 signature; compile breaks loudly rather than silently misrendering.
