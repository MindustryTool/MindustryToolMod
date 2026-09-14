## Context

Old `TimeControlFeature` (170-line `Table`) overrides the global Arc clock via `Time.setDeltaProvider(() -> deltaTime * 60 * m)` with no clamp (vanilla default is `min(deltaTime*60, 3)`), never restores the provider on disable, and only *hides* the HUD when `Vars.net.client()` instead of resetting. State is split across `selected + doubleSpeed + SPEEDS` with a hidden `16x→32x` boost. Position persistence checks orientation only at read time, and drag gesture state is rebuilt per `rebuild()`. Current new code is a locked stub (`development=true`). Verified against Mindustry `v160.1` (`Net.java`: `active/server/client` semantics) and Arc `Time.java` (default provider, no getter/reset). `Signals.java` precedent (`isPortrait` via `ResizeEvent`) and `QuickAccessFeature`/`QuickAccessHudView` (ConfigGroup portrait/landscape + `Signal` x/y + Solim `hud().draggable()`) are the healthy patterns to follow. Headless/dedicated servers are out of scope (client mod).

## Goals / Non-Goals

**Goals:**
- Unlock `time-control` as a real Solim-first standalone HUD gated to single-player/hosting, with ephemeral speed and a mode setting (presets + double-tap vs slider).
- Provide shared reactive net-state signals so TimeControl (and later GodMode/Autoplay) gate on one source of truth instead of ad-hoc `Vars.net` checks.
- Eliminate the provider leak and the client desync path.

**Non-Goals:**
- No QuickAccess integration; no editable preset lists or slider bounds (fixed values v1).
- No persisted speed; no headless/dedicated support; no server-admin gating.
- No change to `isPortrait` behavior or other existing signals.

## Decisions

- **Gate predicate `valid = !Vars.net.client()` (i.e. `!active || server`).** Rationale: matches `Net.java` semantics and the old `InternalGodModeProvider` precedent (`singleplayer || hosting`); host speed-up is intended world-wide, client speed-up is pure desync. Alternative (admin-gated clients) rejected as scope explosion. HUD `visible()` AND model-level reset both key off this predicate — hiding alone was the old bug.
- **Net signals live in `solim-core` `Signals.java` alongside `isPortrait`.** Rationale: consistency with the existing ambient-state home and `floatValueKeyed` consumers; `Signals.java` already imports Mindustry types so no new coupling direction. Alternative (mod-side `NetSignals`) rejected to avoid two global-signal homes; revisit if solim is ever extracted game-agnostic.
- **Hybrid driver: multi-event immediate refresh + slow poll backstop.** Rationale: `Net.host()/disconnect()` fire no events themselves, so events-only risks missing paths (kicks, error dialogs, proxy rooms); poll-only is simplest-correct but adds per-frame wakeups and up-to-1s staleness. Hybrid gives snappy HUD plus correctness backstop; all sets via `Core.app.post`, set-only-on-change so `Signal` dedupes. Alternative (events-only with full `NetClient`/`NetServer` audit) kept as a hardening follow-up.
- **Signal split: raw (`active/server/client`) + derived (`singlePlayer=!active`, `localHosting=server`, `clientPlaying=client && inGame`); menu-inclusive raw, game-awareness composed at use sites.** Rationale: one truth per axis, no combinatorial explosion; feature policy (e.g. TimeControl's `!client && isGame`) stays in the feature, keeping `Signals` generic. Headless excluded per scope.
- **Ephemeral speed: memory-only `Signal<Float>` default 1x, reset restores the hardcoded clamped default provider.** Rationale: `Time` exposes no getter/reset, so the default lambda must be re-stated with a comment; persisting speed risks rebooting into `16x`. Reset triggers: disable/dispose, VALID→INVALID flip, world-unload/menu/`ResetEvent`, mode switch. Alternative (persist speed) rejected as footgun.
- **Two HUD modes behind a persisted mode setting; fixed preset list and slider range v1; mode switch resets speed to 1x.** Rationale: preserves old double-tap behavior opt-in while offering discoverable slider; fixed values avoid validation/i18n explosion; reset avoids orphan "custom value with no preset highlight" state. Solim `slider(signal,min,max,step)` and reactive `grid` already proven in QuickAccess.
- **Clamp-after-multiply on apply (`min(dt*60*m, maxStep)`), not vanilla-shape or unclamped.** Rationale: faithful unclamped port reproduces the physics blowup; clamp-before-multiply (`min(dt*60,3)*m`) still steps 48 ticks at 16x. Exact `maxStep` is an open question (spike).
- **Standalone `hud()` with `.draggable(xSignal,ySignal)`, portrait/landscape `ConfigGroup`s, `keepInScreen` on `ResizeEvent`.** Rationale: direct port of the proven QuickAccess pattern fixes the old orientation-at-read and drag-locals bugs for free. Fixed button sizes v1 (no opacity/scale settings) to limit surface.

## Risks / Trade-offs

- [Risk] Vanilla or another mod also calls `Time.setDeltaProvider` (pause/editor/sync) → last-writer-wins silent conflict → Mitigation: spike-search `v160.1` call sites; document assumption; re-assert on world-load if needed.
- [Risk] Missed net transition leaves signals stale despite hybrid driver → Mitigation: poll backstop; log on VALID→INVALID flips during spike testing.
- [Risk] `16x` (and hidden `32x`) steps break physics/AI at low FPS even clamped → Mitigation: cap preset top / slider max conservatively (e.g. 8x) pending playtest; keep `÷2` slow-end behavior under review (`0.0625x` ≈ freeze).
- [Risk] Host at high speed punishes high-ping clients (`maxDeltaClient=4f`) → Mitigation: help/settings safety note; consider future cap-while-clients-connected (explicitly deferred).
- [Risk] `clock.png` asset missing breaks card (old spec anticipated `Icon.*` fallback) → Mitigation: verify asset, keep fallback until supplied.
- [Risk] `Time.run(delay)` tasks and `globalTime`-keyed anims diverge under speed (game ticks vs realtime) → Mitigation: document in help text; no code action.
- [Risk] Static `Signals` state complicates solim unit tests → Mitigation: package-private reset hook or fresh-JVM tests; decide during implementation.
