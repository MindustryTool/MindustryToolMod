## Why

WavePreview exists only as a development placeholder while a complete imperative implementation sits unused in `old/`; defenders get no upcoming-wave composition readout. Rewriting it as a real Solim feature restores the capability with full old parity plus domain-split units and configurable lookahead.

## What Changes

- De-dev WavePreview into a real enable-capable feature (enabled by default), keeping every old ability: spawn-group composition with null/zero skips, campaign difficulty multiplier (boss truncate, else round, min 1), health-sorted icon×count presentation, wave-number label and title, persisted opacity and scale, and visibility gated on `hudfrag.shown`, `isGame`, and `rules.waves`.
- Re-inject the panel into the vanilla `waves/editor` → `waves` table with defensive null handling and error logging on missing nodes.
- Recompute event-driven on world load and wave advance with no tick polling loop.
- Split units into ground/air/naval sections with unmatched units in the ground bucket, arranged wave-major (one section per wave, domain rows inside).
- Add a persisted lookahead-depth setting (default 1 matching old behavior, max 5) controlling how many future waves render.
- Provide a minimal settings dialog with opacity, scale, and lookahead-depth rows (no position reset; the injected panel has no position).
- Rewrite name/description/help bundle copy, dropping the "cannot be enabled yet" wording, with translator comments.

## Capabilities

### New Capabilities

- `wave-preview`: real WavePreview feature covering parity computation, panel injection, event-driven refresh, domain sections, lookahead depth, minimal settings dialog, and localization.

### Modified Capabilities

- `app-settings`: WavePreview leaves the development-stub registry (stub count and size expectations).

## Impact

- New `WavePreviewFeature`, state holder, injected panel view, and settings dialog/entries under `features/wavepreview`; `old/` sources untouched.
- Stub-registry and count expectations change; note the in-progress `reorder-features` and `popup-instead-of-hud` deltas edit the same lines and need merge sequencing at apply time.
- No changes to spawn, difficulty, or wave-progression game logic; read-only consumption of `rules.spawns`.
