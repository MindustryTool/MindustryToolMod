## Why

The old TimeControl HUD overrides the global game clock with an unbounded delta provider, leaks the speed after disable, and hides (rather than resets) when joining as a client. The current `time-control` stub is locked (`development=true`) so players cannot use it at all. Rewriting it Solim-first with host/single-player gating and ephemeral speed fixes a desync risk while restoring a useful single-player/hosting feature.

## What Changes

- Unlock `time-control` from a locked stub into a real standalone Solim HUD feature (draggable speed bar, own position persistence).
- Gate speed control to single-player (`!Vars.net.active()`) or hosting (`Vars.net.server()`); clients (`Vars.net.client()`) never apply speed and auto-reset to 1x when validity is lost.
- Add shared net ambient signals in `Signals.java` (`active`, `server`, `client` plus derived `singlePlayer`, `localHosting`, `clientPlaying`) driven by net/state events with a slow poll backstop; headless/dedicated behavior is out of scope.
- Keep speed ephemeral (memory-only `Signal<Float>`, default 1x): reset to 1x on disable/dispose, world unload/menu, and VALID-to-INVALID transitions; never persist speed across sessions.
- Offer two HUD interaction modes chosen in a settings dialog: preset buttons with opt-in double-tap boost (old behavior) or a clamped slider; switching modes resets speed to 1x.
- Provide a `TimeControlSettingsDialog` (mode selector, reset-speed and reset-position actions, hosting-only safety note) with fully translated strings; replace the "cannot be enabled yet" help text.
- Restore the default clamped delta provider (`min(deltaTime*60, 3)`) on every reset instead of leaking the override.

## Capabilities

### New Capabilities

- `time-control`: standalone speed-control HUD, host/single-player gating, ephemeral speed lifecycle, presets-vs-slider modes, settings dialog, i18n keys.
- `net-signals`: shared reactive net-state signals (`active`, `server`, `client`, `singlePlayer`, `localHosting`, `clientPlaying`) in `Signals.java` for gating game features.

### Modified Capabilities

- `solim-reactivity`: extends the `Signals` ambient-state contract (currently only `isPortrait`) with net-state signals, update drivers, and threading/dedupe rules.
- `app-settings`: `time-control` leaves the 16-stub development registry and becomes a real enable-capable feature (registry counts and stub-list requirements change).

## Impact

- `mod/src/mindustrytool/features/timecontrol/` (feature, HUD view, settings dialog/view), `assets/bundles/bundle.properties` (new `feature.time-control.*` keys, help rewrite), `solim-core/src/solim/signal/Signals.java` (new signals).
- Behavior: hosts affect world speed for everyone in the room (intended); clients see no HUD effect and cannot change speed; speed never survives disable, world exit, or client join.
- No backend/HTTP changes; no QuickAccess integration (standalone HUD by decision).
