## 1. Spikes and verification

- [ ] 1.1 Search Mindustry v160.1 sources for all `Time.setDeltaProvider` call sites and record whether vanilla ever overwrites the provider (pause, editor, net sync)
- [ ] 1.2 Verify `clock.png` asset exists for the real feature icon; keep the `Icon.*` fallback only if missing
- [ ] 1.3 Decide clamp-after-multiply `maxStep` and preset top/slider max from a single-player playtest (hitch frames, wave skip, AI stability)
- [ ] 1.4 Confirm Solim `slider()` supports the chosen float range/step and reactive label formatting

## 2. Net signals in Signals.java

- [ ] 2.1 Add raw `active()`, `server()`, `client()` signals mirroring `Vars.net` semantics with documented contracts
- [ ] 2.2 Add derived `singlePlayer()`, `localHosting()`, `clientPlaying()` signals per the net-signals spec predicates
- [ ] 2.3 Wire event-driven refresh (host/connect/join/leave/state-change/world-load/reset/dispose) with `Core.app.post` and set-only-on-change
- [ ] 2.4 Add the slow poll backstop for missed transition paths
- [ ] 2.5 Cover raw, derived, driver-dedupe, and menu-state scenarios with solim tests (headless behavior stays unspecified)

## 3. TimeControl feature core

- [ ] 3.1 Unlock metadata (`development=false`) and remove time-control from the stub registry; update registry count tests (9 real + 15 stubs)
- [ ] 3.2 Add `ConfigGroup` with persisted mode value plus portrait/landscape position groups, and memory-only speed signal defaulting to 1x
- [ ] 3.3 Implement clamped apply and default-provider restore used by every reset path, with the no-getter caveat documented at the call site
- [ ] 3.4 Implement reset triggers: disable/dispose, VALID-to-INVALID net flip, world-unload/menu, mode switch
- [ ] 3.5 Gate application on `!Vars.net.client()` via the new signals (never apply as client)

## 4. HUD view

- [ ] 4.1 Build standalone Solim `hud()` with drag handle (`.draggable`), separator, and reactive preset-grid XOR slider-row; no `signal.get()` in `build()`
- [ ] 4.2 Implement preset mode with tap-select and double-tap boost plus reactive highlight, without full view rebuilds
- [ ] 4.3 Implement slider mode with clamped range/step and reactive multiplier label
- [ ] 4.4 Apply visibility predicate (enabled, hudfrag shown, `isGame`, net valid) and `keepInScreen` on resize

## 5. Settings dialog and i18n

- [ ] 5.1 Build `TimeControlSettingsDialog`/`View` with mode selector, reset-speed and reset-position actions, and hosting-only safety note
- [ ] 5.2 Add `feature.time-control.*` settings/mode/safety keys with per-key translator comments; use `Core.bundle.format` for dynamic labels
- [ ] 5.3 Rewrite `feature.time-control.help` (gating, modes, auto-reset) and drop the "cannot be enabled yet" text

## 6. Verification and cleanup

- [ ] 6.1 Run the full test suite and update any count-sensitive assertions beyond the registry tests
- [ ] 6.2 Check Java 8 runtime compatibility (no post-8 stdlib APIs), `arc.util.Nullable` usage, no fully qualified names (FQN regex), and ternary-for-simple-conditions
- [ ] 6.3 Confirm Solim rules: declarative UI, automatic bindings only, effects solely for side effects, no `old/` modifications, no leftover provider leak after disable
