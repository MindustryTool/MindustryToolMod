## Context

No zoom enforcement exists in-repo: `HealthBarFeature` and `ProgressDisplayFeature` only read `Vars.renderer.getScale()` to gate overlay drawing. Vanilla zoom is driven by desktop wheel and mobile pinch (mobile has its own path, e.g. `JoystickMobileInput.zoom`), with implicit vanilla limits. The vanilla zoom setter and exact limit behavior are unconfirmed in-repo, so implementation starts with a spike. Established patterns to follow: `Feature` subclass + `configGroup()` `ConfigValue`s, `FeatureMetadata` with Lucide icon and QuickAccess entry, `Events.run(Trigger.update, ...)` per-frame hook with enabled/in-game/HUD gates, Solim settings dialog with slider rows and reset.

## Goals / Non-Goals

**Goals:**
- Persisted single shared min/max zoom range for all platforms, defaulting to vanilla limits (fresh install is a no-op).
- Per-frame clamp of live renderer scale into `[min, max]`, allowing values beyond vanilla on both ends.
- `min <= max` invariant enforced by pushing the crossed value.
- Solim settings UI with two sliders, reactive value labels, reset-to-defaults, full i18n.

**Non-Goals:**
- Separate mobile/desktop ranges; per-map presets; keybind toggle.
- Zoom-to-cursor, camera pan, or other camera behavior changes.
- Server-side or multiplayer-synced zoom; this is client-side only.
- Changes to existing features.

## Decisions

- **Per-frame `Trigger.update` clamp over input intercept**: one hook covers wheel, pinch, and any other scale writer uniformly. Input interception rejected — it would need both desktop and mobile hook points maintained. Each gated frame writes the vanilla base `minZoom`/`maxZoom` fields (mapped from config through the current vanilla zoom multiplier settings, which vanilla `Renderer.update()` re-derives into in-game bounds every frame, so this is ordering-proof) and pulls live scale back via `setScale` only when outside `[Scl.scl(min), round(Scl.scl(max))]`, mirroring vanilla's own bound formulas.
- **Allow beyond vanilla**: per user decision, sliders extend past vanilla limits both directions. Narrow-only rejected. Exact slider bounds come from the spike, not guesses.
- **Defaults equal vanilla limits**: install changes nothing until the user opts in. Preset-range defaults rejected as surprising.
- **Push-on-cross invariant**: moving min past max raises max (and vice versa) so invalid state is unrepresentable. Blocking rejected as feeling stuck; swapping rejected as confusing.
- **Single shared range**: one min + one max for all platforms. Per-platform ranges rejected as double settings for an unproven need.
- **Standalone feature**: own card, QuickAccess entry, and dialog for discoverability. Folding into an existing feature rejected as harder to find.

## Risks / Trade-offs

- [Risk] Vanilla zoom setter unknown / write may not stick or may fight vanilla ordering → Mitigation: spike first to confirm setter, frame ordering (`Trigger.update` placement), desktop + mobile paths; tasks blocked on spike findings.
- [Risk] Far-out zoom causes render load and map-edge/fog artifacts → Mitigation: document upper bound in help text; slider max chosen conservatively from spike observations.
- [Risk] 1-frame flicker if vanilla writes after our clamp → Mitigation: observe during spike; prefer latest safe hook point that still precedes render.
- [Risk] Cutscene/spectate/logic camera conflicts → Mitigation: gate on standard play state; note as known limitation if exotic modes fight the clamp.
